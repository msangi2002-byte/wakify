package com.wakilfly.service;

import com.wakilfly.dto.request.UpdateProfileRequest;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.UserResponse;
import com.wakilfly.model.User;
import com.wakilfly.model.Visibility;
import com.wakilfly.model.UserRestriction;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.repository.UserRepository;
import com.wakilfly.repository.UserBlockRepository;
import com.wakilfly.repository.UserRestrictionRepository;
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
    private final UserRestrictionRepository userRestrictionRepository;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;

    public UserService(UserRepository userRepository, UserBlockRepository userBlockRepository,
                       UserRestrictionRepository userRestrictionRepository,
                       NotificationService notificationService, FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.userBlockRepository = userBlockRepository;
        this.userRestrictionRepository = userRestrictionRepository;
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

        if (currentUserId == null || userId.equals(currentUserId)) {
            return mapToUserResponse(user, null);
        }

        boolean isFollowing = userRepository.isFollowing(currentUserId, userId);
        Visibility profileVisibility = user.getProfileVisibility() != null ? user.getProfileVisibility() : Visibility.PUBLIC;
        if (profileVisibility == Visibility.PRIVATE) {
            return mapToMinimalUserResponse(user, isFollowing);
        }
        if (profileVisibility == Visibility.FOLLOWERS && !isFollowing) {
            return mapToMinimalUserResponse(user, isFollowing);
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
        if (request.getProfileVisibility() != null)
            user.setProfileVisibility(request.getProfileVisibility());
        if (request.getFollowingListVisibility() != null)
            user.setFollowingListVisibility(request.getFollowingListVisibility());

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

    /** Discover / suggested users: by same region & country (from profile/register). Excludes self and already following. */
    public PagedResponse<UserResponse> getSuggestedUsers(UUID currentUserId, int page, int size) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));
        String region = currentUser.getRegion();
        String country = currentUser.getCountry();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        Page<User> users = userRepository.findSuggestedUsers(currentUserId, region, country, pageable);

        return PagedResponse.<UserResponse>builder()
                .content(users.getContent().stream()
                        .map(user -> mapToUserResponse(user, userRepository.isFollowing(currentUserId, user.getId())))
                        .collect(Collectors.toList()))
                .page(users.getNumber())
                .size(users.getSize())
                .totalElements(users.getTotalElements())
                .totalPages(users.getTotalPages())
                .last(users.isLast())
                .first(users.isFirst())
                .build();
    }

    /** People nearby: uses your profile/register location (currentCity, region, country). Same country only when set; ordered by proximity (same city first, then region, then country). */
    public PagedResponse<UserResponse> getNearbyUsers(UUID currentUserId, int page, int size) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));
        String currentCity = currentUser.getCurrentCity();
        String region = currentUser.getRegion();
        String country = currentUser.getCountry();
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        Page<User> users = userRepository.findNearbyUsers(currentUserId, currentCity, region, country, pageable);

        return PagedResponse.<UserResponse>builder()
                .content(users.getContent().stream()
                        .map(user -> mapToUserResponse(user, userRepository.isFollowing(currentUserId, user.getId())))
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
        if (userRepository.isFollowing(userId, targetUserId)) {
            throw new BadRequestException("You are already following this user");
        }

        targetUser.addFollower(currentUser);
        currentUser.getFollowing().add(targetUser);
        userRepository.save(targetUser);

        log.info("User {} followed user {}", userId, targetUserId);
        try {
            notificationService.sendNotification(targetUser, currentUser, NotificationType.FOLLOW, currentUser.getId(),
                    currentUser.getName() + " started following you");
        } catch (Exception e) {
            log.warn("Failed to send follow notification: {}", e.getMessage());
        }
    }

    @Transactional
    public void unfollowUser(UUID userId, UUID targetUserId) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        targetUser.removeFollower(currentUser);
        currentUser.getFollowing().remove(targetUser);
        userRepository.save(targetUser);

        log.info("User {} unfollowed user {}", userId, targetUserId);
    }

    public PagedResponse<UserResponse> getFollowers(UUID userId, int page, int size, UUID currentUserId) {
        enforceFollowingListVisibility(userId, currentUserId);
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
        enforceFollowingListVisibility(userId, currentUserId);
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

    private void enforceFollowingListVisibility(UUID profileOwnerId, UUID viewerId) {
        if (viewerId == null || profileOwnerId.equals(viewerId)) return;
        User owner = userRepository.findById(profileOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", profileOwnerId));
        Visibility vis = owner.getFollowingListVisibility() != null ? owner.getFollowingListVisibility() : Visibility.PUBLIC;
        if (vis == Visibility.PRIVATE) {
            throw new BadRequestException("This list is private");
        }
        if (vis == Visibility.FOLLOWERS && !userRepository.isFollowing(viewerId, profileOwnerId)) {
            throw new BadRequestException("You must follow this user to see their list");
        }
    }

    @Transactional
    public void addToRestrictedList(UUID restricterId, UUID restrictedId) {
        if (restricterId.equals(restrictedId)) {
            throw new BadRequestException("You cannot restrict yourself");
        }
        User restricter = userRepository.findById(restricterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", restricterId));
        User restricted = userRepository.findById(restrictedId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", restrictedId));
        if (userRestrictionRepository.existsByRestricterIdAndRestrictedId(restricterId, restrictedId)) {
            return;
        }
        userRestrictionRepository.save(UserRestriction.builder().restricter(restricter).restricted(restricted).build());
        log.info("User {} restricted user {}", restricterId, restrictedId);
    }

    @Transactional
    public void removeFromRestrictedList(UUID restricterId, UUID restrictedId) {
        userRestrictionRepository.deleteByRestricterIdAndRestrictedId(restricterId, restrictedId);
        log.info("User {} unrestricted user {}", restricterId, restrictedId);
    }

    public PagedResponse<UserResponse> getRestrictedList(UUID restricterId, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        Page<UserRestriction> pageResult = userRestrictionRepository.findByRestricterIdOrderByCreatedAtDesc(restricterId, pageable);
        java.util.List<UserResponse> content = pageResult.getContent().stream()
                .map(ur -> mapToUserResponse(ur.getRestricted(), false))
                .collect(Collectors.toList());
        return PagedResponse.<UserResponse>builder()
                .content(content)
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .first(pageResult.isFirst())
                .build();
    }

    public boolean isRestrictedBy(UUID restricterId, UUID restrictedId) {
        return userRestrictionRepository.existsByRestricterIdAndRestrictedId(restricterId, restrictedId);
    }

    private UserResponse mapToMinimalUserResponse(User user, Boolean isFollowing) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .profilePic(user.getProfilePic())
                .isFollowing(isFollowing)
                .build();
    }

    /** Used by PeopleYouMayKnowService etc. to map User to UserResponse. */
    public UserResponse toUserResponse(User user, Boolean isFollowing) {
        return mapToUserResponse(user, isFollowing);
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
                .profileVisibility(user.getProfileVisibility())
                .followingListVisibility(user.getFollowingListVisibility())
                .createdAt(user.getCreatedAt())
                .lastSeen(user.getLastSeen())
                .isOnline(computeIsOnline(user.getLastSeen()))
                .build();
    }

    private static Integer computeAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) return null;
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /** User is online if lastSeen within last 5 minutes. */
    private static Boolean computeIsOnline(java.time.LocalDateTime lastSeen) {
        if (lastSeen == null) return false;
        return lastSeen.isAfter(java.time.LocalDateTime.now().minusMinutes(5));
    }

    @Transactional
    public void updateLastSeen(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setLastSeen(java.time.LocalDateTime.now());
        userRepository.save(user);
    }
}