package com.wakilfly.service;

import com.wakilfly.model.Friendship;
import com.wakilfly.model.FriendshipStatus;
import com.wakilfly.model.User;
import com.wakilfly.repository.FriendshipRepository;
import com.wakilfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.wakilfly.model.NotificationType;
import com.wakilfly.service.NotificationService;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public Friendship sendRequest(UUID requesterId, UUID addresseeId) {
        if (requesterId.equals(addresseeId)) {
            throw new IllegalArgumentException("You cannot add yourself as a friend");
        }

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new RuntimeException("Requester not found"));
        User addressee = userRepository.findById(addresseeId)
                .orElseThrow(() -> new RuntimeException("User to add not found"));

        // Check if friendship already exists
        Optional<Friendship> existing = friendshipRepository.findFriendshipBetween(requester, addressee);
        if (existing.isPresent()) {
            Friendship f = existing.get();
            if (f.getStatus() == FriendshipStatus.BLOCKED) {
                throw new RuntimeException("Cannot add this user");
            }
            if (f.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new RuntimeException("You are already friends");
            }
            if (f.getStatus() == FriendshipStatus.PENDING) {
                throw new RuntimeException("Friend request already pending");
            }
            // If declined, we might allow resending? For now, let's reuse and reset to
            // PENDING
            f.setStatus(FriendshipStatus.PENDING);
            f.setRequester(requester); // Reset requester in case roles swapped
            f.setAddressee(addressee);
            return friendshipRepository.save(f);
        }

        Friendship friendship = Friendship.builder()
                .requester(requester)
                .addressee(addressee)
                .status(FriendshipStatus.PENDING)
                .build();

        Friendship savedFriendship = friendshipRepository.save(friendship);

        // Notify addressee
        notificationService.sendNotification(addressee, requester, NotificationType.FRIEND_REQUEST,
                savedFriendship.getId(), requester.getName() + " sent you a friend request");

        return savedFriendship;
    }

    @Transactional
    public Friendship acceptRequest(UUID userId, UUID friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (!friendship.getAddressee().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to accept this request");
        }

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new RuntimeException("Request is not pending");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);

        // Auto-follow logic (Make them follow each other)
        User requester = friendship.getRequester();
        User addressee = friendship.getAddressee();

        // Addressee follows Requester
        addressee.getFollowing().add(requester);
        requester.getFollowers().add(addressee);

        // Requester follows Addressee (Usually already follows if public, but enforce
        // it)
        requester.getFollowing().add(addressee);
        addressee.getFollowers().add(requester);

        userRepository.save(addressee);
        userRepository.save(requester);

        Friendship savedFriendship = friendshipRepository.save(friendship);

        // Notify requester
        notificationService.sendNotification(requester, addressee, NotificationType.FRIEND_ACCEPT,
                savedFriendship.getId(), addressee.getName() + " accepted your friend request");

        return savedFriendship;
    }

    @Transactional
    public void declineRequest(UUID userId, UUID friendshipId) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
                .orElseThrow(() -> new RuntimeException("Friend request not found"));

        if (!friendship.getAddressee().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to decline this request");
        }

        friendshipRepository.delete(friendship); // Simply delete the request for now
    }

    @Transactional
    public void unfriend(UUID userId, UUID friendId) {
        User user1 = userRepository.findById(userId).orElseThrow();
        User user2 = userRepository.findById(friendId).orElseThrow();

        Optional<Friendship> friendship = friendshipRepository.findFriendshipBetween(user1, user2);

        if (friendship.isPresent()) {
            friendshipRepository.delete(friendship.get());

            // Should we unfollow? Usually yes.
            user1.getFollowing().remove(user2);
            user2.getFollowers().remove(user1);
            user2.getFollowing().remove(user1);
            user1.getFollowers().remove(user2);

            userRepository.save(user1);
            userRepository.save(user2);
        }
    }

    @Transactional(readOnly = true)
    public Page<Friendship> getPendingRequests(UUID userId, Pageable pageable) {
        return friendshipRepository.findIncomingRequests(userId, FriendshipStatus.PENDING, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Friendship> getFriends(UUID userId, Pageable pageable) {
        return friendshipRepository.findAllFriends(userId, pageable);
    }
}
