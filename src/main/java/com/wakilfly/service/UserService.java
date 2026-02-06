package com.wakilfly.service;

import com.wakilfly.dto.request.UpdateProfileRequest;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.UserResponse;
import com.wakilfly.model.User;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

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

        user = userRepository.save(user);
        return mapToUserResponse(user, null);
    }

    public PagedResponse<UserResponse> searchUsers(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> users = userRepository.searchUsers(query, pageable);

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

    @Transactional
    public void followUser(UUID userId, UUID targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BadRequestException("You cannot follow yourself");
        }

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        if (targetUser.getFollowers().contains(currentUser)) {
            throw new BadRequestException("You are already following this user");
        }

        targetUser.addFollower(currentUser);
        userRepository.save(targetUser);

        // TODO: Create notification for targetUser
        log.info("User {} followed user {}", userId, targetUserId);
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
        Pageable pageable = PageRequest.of(page, size);
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
        Pageable pageable = PageRequest.of(page, size);
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
                .createdAt(user.getCreatedAt())
                .build();
    }
}
