package com.wakilfly.service;

import com.wakilfly.dto.request.UpdateProfileRequest;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.UserResponse;
import com.wakilfly.model.User;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.repository.UserRepository;
import com.wakilfly.repository.UserBlockRepository;
import com.wakilfly.model.NotificationType;
import com.wakilfly.model.UserBlock;
import com.wakilfly.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;

    public UserService(UserRepository userRepository, UserBlockRepository userBlockRepository,
                       NotificationService notificationService, FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.userBlockRepository = userBlockRepository;
        this.notificationService = notificationService;
        this.fileStorageService = fileStorageService;
    }

    public UserResponse getUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return mapToUserResponse(user, null);
    }

    public UserResponse getUserProfile(UUID userId, UUID currentUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        boolean isFollowing = false;
        if (currentUserId != null && !userId.equals(currentUserId)) {
            User currentUser = userRepository.findById(currentUserId).orElse(null);
            if (currentUser != null) {
                isFollowing = user.getFollowers().contains(currentUser);
            }
        }

        return mapToUserResponse(user, isFollowing);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.getName() != null && !request.getName().isEmpty()) {
            user.setName(request.getName());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getProfilePic() != null) {
            user.setProfilePic(request.getProfilePic());
        }
        if (request.getCoverPic() != null) {
            user.setCoverPic(request.getCoverPic());
        }

        // Extended Details
        if (request.getWork() != null)
            user.setWork(request.getWork());
        if (request.getEducation() != null)
            user.setEducation(request.getEducation());
        if (request.getCurrentCity() != null)
            user.setCurrentCity(request.getCurrentCity());
        if (request.getRegion() != null)
            user.setRegion(request.getRegion());
        if (request.getCountry() != null)
            user.setCountry(request.getCountry());
        if (request.getInterests() != null)
            user.setInterests(request.getInterests());
        if (request.getHometown() != null)
            user.setHometown(request.getHometown());
        if (request.getRelationshipStatus() != null)
            user.setRelationshipStatus(request.getRelationshipStatus());
        if (request.getGender() != null)
            user.setGender(request.getGender());
        if (request.getDateOfBirth() != null)
            user.setDateOfBirth(request.getDateOfBirth());
        if (request.getWebsite() != null)
            user.setWebsite(request.getWebsite());

        user = userRepository.save(user);
        return mapToUserResponse(user, null);
    }

    @Transactional
    public UserResponse uploadProfilePic(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Profile picture file is required");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        String url = fileStorageService.storeFile(file, "profile");
        user.setProfilePic(url);
        user = userRepository.save(user);
        return mapToUserResponse(user, null);
    }

    @Transactional
    public UserResponse uploadCoverPic(UUID userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Cover picture file is required");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        String url = fileStorageService.storeFile(file, "covers");
        user.setCoverPic(url);
        user = userRepository.save(user);
        return mapToUserResponse(user, null);
    }

    public PagedResponse<UserResponse> searchUsers(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            return PagedResponse.<UserResponse>builder()
                    .content(Collections.emptyList())
                    .page(page)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .first(true)
                    .build();
        }
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<User> users = userRepository.searchUsers(query.trim(), pageable);

        return PagedResponse.<UserResponse>builder()
                .content(users.getContent().stream()
                        .map(user -> mapToUserResponse(user, null))
                        .collect(Collectors.toList()))
                .page(users.getNumber())
                .size(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .last(users.isLast())
                .first(users.isFirst())
                .build();
    }

    /** Discover / suggested users: by same region, country (and later age/interests). Excludes self and already following. Sorted alphabetically by name. */
    public PagedResponse<UserResponse> getSuggestedUsers(UUID currentUserId, int page, int size) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));
        String region = currentUser.getRegion();
        String country = currentUser.getCountry();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        Page<User> users = userRepository.findSuggestedUsers(currentUserId, region, country, pageable);

        return PagedResponse.<UserResponse>builder()
                .content(users.getContent().stream()
                        .map(user -> mapToUserResponse(user, false))
                        .collect(Collectors.toList()))
                .page(users.getNumber())
                .size(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .last(users.isLast())
                .first(users.isFirst())
                .build();
    }

    // ==================== BLOCK / REPORT ====================
    // Report: use POST /api/v1/reports (ReportController). Block below.

    @Transactional
    public void blockUser(UUID blockerId, UUID blockedUserId) {
        if (blockerId.equals(blockedUserId)) {
            throw new BadRequestException("You cannot block yourself");
        }
        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", blockerId));
        User blocked = userRepository.findById(blockedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", blockedUserId));
        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedUserId)) {
            return; // already blocked
        }
        UserBlock ub = UserBlock.builder().blocker(blocker).blocked(blocked).build();
        userBlockRepository.save(ub);
        log.info("User {} blocked user {}", blockerId, blockedUserId);
    }

    @Transactional
    public void unblockUser(UUID blockerId, UUID blockedUserId) {
        userBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedUserId);
        log.info("User {} unblocked user {}", blockerId, blockedUserId);
    }

    public boolean isBlocked(UUID blockerId, UUID blockedUserId) {
        return userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedUserId);
    }

    public PagedResponse<UserResponse> getBlockedUsers(UUID userId, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        Page<UserBlock> blocks = userBlockRepository.findByBlockerIdOrderByCreatedAtDesc(userId, pageable);
        return PagedResponse.<UserResponse>builder()
                .content(blocks.getContent().stream()
                        .map(ub -> mapToUserResponse(ub.getBlocked(), false))
                        .collect(Collectors.toList()))
                .page(blocks.getNumber())
                .size(blocks.getSize())
                .totalElements(blocks.getTotalElements())
                .totalPages(blocks.getTotalPages())
                .last(blocks.isLast())
                .first(blocks.isFirst())
                .build();
    }

    @Transactional
    public void followUser(UUID userId, UUID targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BadRequestException("You cannot follow yourself");
        }
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        if (userBlockRepository.existsByBlockerIdAndBlockedId(userId, targetUserId)
                || userBlockRepository.existsByBlockerIdAndBlockedId(targetUserId, userId)) {
            throw new BadRequestException("Cannot follow: user is blocked");
        }
        if (targetUser.getFollowers().contains(currentUser)) {
            throw new BadRequestException("You are already following this user");
        }

        targetUser.addFollower(currentUser);
        userRepository.save(targetUser);

        log.info("User {} followed user {}", userId, targetUserId);
        notificationService.sendNotification(targetUser, currentUser, NotificationType.FOLLOW, currentUser.getId(),
                currentUser.getName() + " started following you");
    }

    @Transactional
    public void unfollowUser(UUID userId, UUID targetUserId) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        targetUser.removeFollower(currentUser);
        userRepository.save(targetUser);

        log.info("User {} unfollowed user {}", userId, targetUserId);
    }

    public PagedResponse<UserResponse> getFollowers(UUID userId, int page, int size, UUID currentUserId) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        Page<User> followers = userRepository.findFollowers(userId, pageable);

        return PagedResponse.<UserResponse>builder()
                .content(followers.getContent().stream()
                        .map(user -> {
                            boolean isFollowing = false;
                            if (currentUserId != null) {
                                User currentUser = userRepository.findById(currentUserId).orElse(null);
                                if (currentUser != null) {
                                    isFollowing = user.getFollowers().contains(currentUser);
                                }
                            }
                            return mapToUserResponse(user, isFollowing);
                        })
                        .collect(Collectors.toList()))
                .page(followers.getNumber())
                .size(followers.getSize())
                .totalElements(followers.getTotalElements())
                .totalPages(followers.getTotalPages())
                .last(followers.isLast())
                .first(followers.isFirst())
                .build();
    }

    public PagedResponse<UserResponse> getFollowing(UUID userId, int page, int size, UUID currentUserId) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        Page<User> following = userRepository.findFollowing(userId, pageable);

        return PagedResponse.<UserResponse>builder()
                .content(following.getContent().stream()
                        .map(user -> {
                            boolean isFollowing = true; // They are following, so current user follows them
                            return mapToUserResponse(user, isFollowing);
                        })
                        .collect(Collectors.toList()))
                .page(following.getNumber())
                .size(following.getSize())
                .totalElements(following.getTotalElements())
                .totalPages(following.getTotalPages())
                .last(following.isLast())
                .first(following.isFirst())
                .build();
    }

    /** Mutual follows: users I follow AND who follow me back (malafiki) */
    public PagedResponse<UserResponse> getMutualFollows(UUID userId, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        Page<User> mutual = userRepository.findMutualFollows(userId, pageable);

        return PagedResponse.<UserResponse>builder()
                .content(mutual.getContent().stream()
                        .map(user -> mapToUserResponse(user, true)) // Always true - mutual follow
                        .collect(Collectors.toList()))
                .page(mutual.getNumber())
                .size(mutual.getSize())
                .totalElements(mutual.getTotalElements())
                .totalPages(mutual.getTotalPages())
                .last(mutual.isLast())
                .first(mutual.isFirst())
                .build();
    }

    private UserResponse mapToUserResponse(User user, Boolean isFollowing) {
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
                .isFollowing(isFollowing)
                .work(user.getWork())
                .education(user.getEducation())
                .currentCity(user.getCurrentCity())
                .region(user.getRegion())
                .country(user.getCountry())
                .interests(user.getInterests())
                .age(computeAge(user.getDateOfBirth()))
                .hometown(user.getHometown())
                .relationshipStatus(user.getRelationshipStatus())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .website(user.getWebsite())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private static Integer computeAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) return null;
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}