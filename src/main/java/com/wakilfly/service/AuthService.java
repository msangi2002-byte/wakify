package com.wakilfly.service;

import com.wakilfly.dto.request.*;
import com.wakilfly.dto.response.AuthResponse;
import com.wakilfly.dto.response.RegisterResponse;
import com.wakilfly.dto.response.UserResponse;
import com.wakilfly.model.Role;
import com.wakilfly.model.User;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.BusinessRequestStatus;
import com.wakilfly.model.PaymentType;
import com.wakilfly.repository.BusinessRepository;
import com.wakilfly.repository.BusinessRequestRepository;
import com.wakilfly.repository.PaymentRepository;
import com.wakilfly.repository.UserRepository;
import com.wakilfly.security.CustomUserDetailsService;
import com.wakilfly.security.JwtTokenProvider;
import com.wakilfly.service.otp.EmailOtpSender;
import com.wakilfly.service.otp.OtpSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    @Value("${app.dev-mode:false}")
    private boolean devMode;

    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final BusinessRequestRepository businessRequestRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final OtpSender otpSender;
    private final EmailOtpSender emailOtpSender;
    private final AuthEventService authEventService;

    @Transactional
    public RegisterResponse register(RegisterRequest request, com.wakilfly.dto.request.AuthRequestContext ctx) {
        // Check if phone already exists
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone number already registered");
        }

        // Check if email already exists (if provided)
        if (request.getEmail() != null && !request.getEmail().isEmpty() &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // Generate OTP
        String otp = generateOtp();

        // Create user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .isVerified(false)
                .isActive(true)
                .onboardingCompleted(false)
                .otpCode(otp)
                .otpExpiresAt(LocalDateTime.now().plusMinutes(10))
                .referredByAgentCode(request.getReferralCode())
                .currentCity(request.getCurrentCity())
                .region(request.getRegion())
                .country(request.getCountry())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .interests(request.getInterests())
                .build();

        user = userRepository.save(user);

        otpSender.sendOtp(request.getPhone(), otp);
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            emailOtpSender.sendOtp(request.getEmail(), otp);
        }

        if (request.getReferralCode() != null && !request.getReferralCode().isEmpty()) {
            log.info("User {} registered with referral code: {}", request.getPhone(), request.getReferralCode());
        }

        authEventService.recordEvent(com.wakilfly.model.AuthEventType.REGISTRATION, user, null, ctx, true);

        return RegisterResponse.builder()
                .user(mapToUserResponse(user))
                .otpForDev(devMode ? otp : null)
                .build();
    }

    /**
     * Resend OTP for unverified user (e.g. "Resend code" on OTP screen).
     * TODO: Rate limit per phone (e.g. 1 per 60 seconds). TODO: Send via SMS in production.
     */
    @Transactional
    public void resendOtp(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new BadRequestException("Phone already verified");
        }

        String otp = generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        otpSender.sendOtp(phone, otp);
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            emailOtpSender.sendOtp(user.getEmail(), otp);
        }
    }

    @Transactional
    public boolean verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP");
        }

        if (user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired");
        }

        user.setIsVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        userRepository.save(user);

        return true;
    }

    public AuthResponse login(LoginRequest request, com.wakilfly.dto.request.AuthRequestContext ctx) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmailOrPhone(), request.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userDetailsService.loadUserEntityByUsername(userDetails.getUsername());

        user.setLastSeen(java.time.LocalDateTime.now());
        userRepository.save(user);

        authEventService.recordEvent(com.wakilfly.model.AuthEventType.LOGIN, user, null, ctx, true);

        String accessToken = tokenProvider.generateAccessToken(userDetails);
        String refreshToken = tokenProvider.generateRefreshToken(userDetails);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

    /**
     * Record failed login attempt (no user; for security/fraud). Call from controller on BadCredentialsException.
     */
    public void recordFailedLogin(String emailOrPhone, com.wakilfly.dto.request.AuthRequestContext ctx) {
        authEventService.recordEvent(com.wakilfly.model.AuthEventType.LOGIN_FAILED, null, emailOrPhone, ctx, false);
    }

    public String refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        return tokenProvider.generateAccessToken(userDetails);
    }

    @Transactional
    public void forgotPassword(String phone) {
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String otp = generateOtp();
        user.setOtpCode(otp);
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        otpSender.sendOtp(phone, otp);
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            emailOtpSender.sendOtp(user.getEmail(), otp);
        }
    }

    @Transactional
    public boolean resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP");
        }

        if (user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        userRepository.save(user);

        return true;
    }

    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    private UserResponse mapToUserResponse(User user) {
        var business = businessRepository.findByOwnerId(user.getId());
        boolean hasPending = businessRequestRepository.existsByUserIdAndStatus(user.getId(), BusinessRequestStatus.PENDING);
        String pendingOrderId = null;
        if (hasPending) {
            pendingOrderId = paymentRepository
                    .findFirstByUserIdAndTypeAndStatusAndRelatedEntityTypeOrderByCreatedAtDesc(
                            user.getId(), PaymentType.BUSINESS_ACTIVATION,
                            com.wakilfly.model.PaymentStatus.PENDING, "BUSINESS_REQUEST")
                    .map(p -> p.getTransactionId())
                    .orElse(null);
        }
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .bio(user.getBio())
                .profilePic(user.getProfilePic())
                .coverPic(user.getCoverPic())
                .role(user.getRole())
                .isVerified(user.getIsVerified())
                .followersCount(user.getFollowersCount())
                .followingCount(user.getFollowingCount())
                .postsCount(user.getPostsCount())
                .onboardingCompleted(Boolean.TRUE.equals(user.getOnboardingCompleted()))
                .createdAt(user.getCreatedAt())
                .isBusiness(business.isPresent())
                .businessId(business.map(b -> b.getId()).orElse(null))
                .hasPendingBusinessRequest(hasPending)
                .pendingBusinessPaymentOrderId(pendingOrderId)
                .build();
    }
}
