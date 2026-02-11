package com.wakilfly.service;

import com.wakilfly.dto.request.CreateCommunityEventRequest;
import com.wakilfly.dto.request.CreateCommunityPollRequest;
import com.wakilfly.dto.request.CreateCommunityRequest;
import com.wakilfly.dto.request.CommunityInviteRequest;
import com.wakilfly.dto.response.CommunityEventResponse;
import com.wakilfly.dto.response.CommunityInviteResponse;
import com.wakilfly.dto.response.CommunityPollResponse;
import com.wakilfly.dto.response.CommunityResponse;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.*;
import com.wakilfly.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;
    private final CommunityInviteRepository communityInviteRepository;
    private final CommunityPollRepository communityPollRepository;
    private final CommunityPollOptionRepository communityPollOptionRepository;
    private final CommunityPollVoteRepository communityPollVoteRepository;
    private final CommunityEventRepository communityEventRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final PostRepository postRepository;

    @Transactional
    public CommunityResponse createCommunity(UUID creatorId, CreateCommunityRequest request, MultipartFile coverImage) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String coverUrl = null;
        if (coverImage != null && !coverImage.isEmpty()) {
            coverUrl = fileStorageService.storeFile(coverImage, "communities");
        }

        Community community = Community.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .privacy(request.getPrivacy())
                .creator(creator)
                .coverImage(coverUrl)
                .membersCount(1) // Creator starts as member
                .build();

        community = communityRepository.save(community);

        // Add creator as ADMIN member
        CommunityMember member = CommunityMember.builder()
                .community(community)
                .user(creator)
                .role(CommunityRole.ADMIN)
                .build();

        memberRepository.save(member);

        return mapToResponse(community, creatorId);
    }

    /**
     * Join a group. Public = anyone can join; Private = invite only (use acceptInvite after being invited).
     */
    @Transactional
    public void joinCommunity(UUID userId, UUID communityId) {
        if (memberRepository.existsByCommunityIdAndUserId(communityId, userId)) {
            throw new BadRequestException("Already a member");
        }

        User user = userRepository.getReferenceById(userId);
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", "id", communityId));

        if (community.getPrivacy() == Visibility.PRIVATE) {
            throw new BadRequestException("This group is private. You need an invite to join.");
        }

        CommunityMember member = CommunityMember.builder()
                .community(community)
                .user(user)
                .role(CommunityRole.MEMBER)
                .build();

        memberRepository.save(member);

        // Update count
        community.setMembersCount(community.getMembersCount() + 1);
        communityRepository.save(community);
    }

    @Transactional
    public void leaveCommunity(UUID userId, UUID communityId) {
        CommunityMember member = memberRepository.findByCommunityIdAndUserId(communityId, userId)
                .orElseThrow(() -> new RuntimeException("Not a member"));

        // If creator leaves, what happens? (For now, allow leaving, maybe transfer
        // ownership later)
        Community community = member.getCommunity();

        memberRepository.delete(member);

        // Update count
        community.setMembersCount(Math.max(0, community.getMembersCount() - 1));
        communityRepository.save(community);
    }

    @Transactional(readOnly = true)
    public Page<CommunityResponse> getAllCommunities(UUID currentUserId, Pageable pageable) {
        Page<Community> page = communityRepository.findAll(pageable);
        return page.map(c -> mapToResponse(c, currentUserId));
    }

    @Transactional(readOnly = true)
    public Page<CommunityResponse> getMyCommunities(UUID currentUserId, Pageable pageable) {
        // Find communities where the user is a member
        Page<CommunityMember> memberships = memberRepository.findByUserId(currentUserId, pageable);
        return memberships.map(m -> mapToResponse(m.getCommunity(), currentUserId));
    }

    @Transactional(readOnly = true)
    public CommunityResponse getCommunityById(UUID communityId, UUID currentUserId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", "id", communityId));
        return mapToResponse(community, currentUserId);
    }

    private CommunityResponse mapToResponse(Community c, UUID currentUserId) {
        boolean isMember = false;
        boolean isAdmin = false;

        if (currentUserId != null) {
            Optional<CommunityMember> memberOpt = memberRepository.findByCommunityIdAndUserId(c.getId(), currentUserId);
            if (memberOpt.isPresent()) {
                isMember = true;
                isAdmin = memberOpt.get().getRole() == CommunityRole.ADMIN;
            }
        }

        return CommunityResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .type(c.getType())
                .privacy(c.getPrivacy())
                .coverImage(c.getCoverImage())
                .membersCount(c.getMembersCount())
                .createdAt(c.getCreatedAt())
                .creatorId(c.getCreator().getId())
                .creatorName(c.getCreator().getName())
                .isMember(isMember)
                .isAdmin(isAdmin)
                .allowMemberPosts(c.getAllowMemberPosts() != null ? c.getAllowMemberPosts() : true)
                .build();
    }

    @Transactional
    public CommunityResponse updateSettings(UUID communityId, UUID userId, boolean allowMemberPosts) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", "id", communityId));
        CommunityMember member = memberRepository.findByCommunityIdAndUserId(communityId, userId)
                .orElseThrow(() -> new BadRequestException("You are not a member of this community"));
        if (member.getRole() != CommunityRole.ADMIN) {
            throw new BadRequestException("Only group admins can change this setting");
        }
        community.setAllowMemberPosts(allowMemberPosts);
        communityRepository.save(community);
        return mapToResponse(community, userId);
    }

    // ==================== INVITES ====================

    @Transactional
    public List<CommunityInviteResponse> inviteUsers(UUID communityId, UUID inviterId, CommunityInviteRequest request) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", "id", communityId));
        CommunityMember inviterMember = memberRepository.findByCommunityIdAndUserId(communityId, inviterId)
                .orElseThrow(() -> new BadRequestException("You are not a member"));
        if (inviterMember.getRole() != CommunityRole.ADMIN && inviterMember.getRole() != CommunityRole.MODERATOR) {
            throw new BadRequestException("Only admins and moderators can invite");
        }
        User inviter = userRepository.getReferenceById(inviterId);
        List<CommunityInviteResponse> result = new ArrayList<>();
        for (UUID inviteeId : request.getUserIds()) {
            if (inviteeId.equals(inviterId)) continue;
            if (memberRepository.existsByCommunityIdAndUserId(communityId, inviteeId)) continue;
            if (communityInviteRepository.findByCommunityIdAndInviteeId(communityId, inviteeId)
                    .filter(i -> i.getStatus() == CommunityInviteStatus.PENDING).isPresent()) continue;
            User invitee = userRepository.findById(inviteeId).orElse(null);
            if (invitee == null) continue;
            CommunityInvite invite = CommunityInvite.builder()
                    .community(community)
                    .inviter(inviter)
                    .invitee(invitee)
                    .status(CommunityInviteStatus.PENDING)
                    .build();
            invite = communityInviteRepository.save(invite);
            result.add(mapInviteToResponse(invite));
            notificationService.sendNotification(invitee, inviter, NotificationType.COMMUNITY_INVITE,
                    community.getId(), inviter.getName() + " invited you to join " + community.getName());
        }
        return result;
    }

    @Transactional
    public void acceptInvite(UUID inviteId, UUID userId) {
        CommunityInvite invite = communityInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Invite", "id", inviteId));
        if (!invite.getInvitee().getId().equals(userId)) {
            throw new BadRequestException("This invite is not for you");
        }
        if (invite.getStatus() != CommunityInviteStatus.PENDING) {
            throw new BadRequestException("Invite already processed");
        }
        invite.setStatus(CommunityInviteStatus.ACCEPTED);
        communityInviteRepository.save(invite);
        joinCommunity(userId, invite.getCommunity().getId());
    }

    @Transactional
    public void declineInvite(UUID inviteId, UUID userId) {
        CommunityInvite invite = communityInviteRepository.findById(inviteId)
                .orElseThrow(() -> new ResourceNotFoundException("Invite", "id", inviteId));
        if (!invite.getInvitee().getId().equals(userId)) {
            throw new BadRequestException("This invite is not for you");
        }
        if (invite.getStatus() != CommunityInviteStatus.PENDING) {
            throw new BadRequestException("Invite already processed");
        }
        invite.setStatus(CommunityInviteStatus.DECLINED);
        communityInviteRepository.save(invite);
    }

    public Page<CommunityInviteResponse> getMyInvites(UUID userId, Pageable pageable) {
        return communityInviteRepository.findByInviteeIdAndStatus(userId, CommunityInviteStatus.PENDING, pageable)
                .map(this::mapInviteToResponse);
    }

    // ==================== PIN POST (admin) ====================

    /**
     * Pin a post in the group (admin only). Pinned posts show first in group feed.
     */
    @Transactional
    public void pinPost(UUID communityId, UUID postId, UUID userId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", "id", communityId));
        CommunityMember member = memberRepository.findByCommunityIdAndUserId(communityId, userId)
                .orElseThrow(() -> new BadRequestException("You are not a member"));
        if (member.getRole() != CommunityRole.ADMIN) {
            throw new BadRequestException("Only group admins can pin posts");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        if (post.getCommunity() == null || !post.getCommunity().getId().equals(communityId)) {
            throw new BadRequestException("Post is not in this group");
        }
        post.setIsPinned(true);
        post.setPinnedAt(LocalDateTime.now());
        postRepository.save(post);
    }

    /**
     * Unpin a post in the group (admin only).
     */
    @Transactional
    public void unpinPost(UUID communityId, UUID postId, UUID userId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", "id", communityId));
        CommunityMember member = memberRepository.findByCommunityIdAndUserId(communityId, userId)
                .orElseThrow(() -> new BadRequestException("You are not a member"));
        if (member.getRole() != CommunityRole.ADMIN) {
            throw new BadRequestException("Only group admins can unpin posts");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        if (post.getCommunity() == null || !post.getCommunity().getId().equals(communityId)) {
            throw new BadRequestException("Post is not in this group");
        }
        post.setIsPinned(false);
        post.setPinnedAt(null);
        postRepository.save(post);
    }

    private CommunityInviteResponse mapInviteToResponse(CommunityInvite i) {
        return CommunityInviteResponse.builder()
                .id(i.getId())
                .communityId(i.getCommunity().getId())
                .communityName(i.getCommunity().getName())
                .inviterId(i.getInviter().getId())
                .inviterName(i.getInviter().getName())
                .inviteeId(i.getInvitee().getId())
                .inviteeName(i.getInvitee().getName())
                .status(i.getStatus())
                .createdAt(i.getCreatedAt())
                .build();
    }

    // ==================== POLLS ====================

    @Transactional
    public CommunityPollResponse createPoll(UUID communityId, UUID userId, CreateCommunityPollRequest request) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", "id", communityId));
        CommunityMember member = memberRepository.findByCommunityIdAndUserId(communityId, userId)
                .orElseThrow(() -> new BadRequestException("You are not a member"));
        User creator = userRepository.getReferenceById(userId);
        CommunityPoll poll = CommunityPoll.builder()
                .community(community)
                .creator(creator)
                .question(request.getQuestion())
                .endsAt(request.getEndsAt())
                .build();
        poll = communityPollRepository.save(poll);
        int order = 0;
        for (String optText : request.getOptions()) {
            if (optText == null || optText.isBlank()) continue;
            CommunityPollOption opt = CommunityPollOption.builder()
                    .poll(poll)
                    .text(optText.trim())
                    .displayOrder(order++)
                    .votesCount(0)
                    .build();
            communityPollOptionRepository.save(opt);
        }
        poll = communityPollRepository.findById(poll.getId()).orElse(poll);
        return mapPollToResponse(poll, userId);
    }

    public Page<CommunityPollResponse> getCommunityPolls(UUID communityId, UUID currentUserId, Pageable pageable) {
        communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", "id", communityId));
        return communityPollRepository.findByCommunityIdOrderByCreatedAtDesc(communityId, pageable)
                .map(p -> mapPollToResponse(p, currentUserId));
    }

    @Transactional
    public CommunityPollResponse votePoll(UUID pollId, UUID optionId, UUID userId) {
        CommunityPoll poll = communityPollRepository.findById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException("Poll", "id", pollId));
        if (poll.getEndsAt() != null && java.time.LocalDateTime.now().isAfter(poll.getEndsAt())) {
            throw new BadRequestException("Poll has ended");
        }
        if (!memberRepository.existsByCommunityIdAndUserId(poll.getCommunity().getId(), userId)) {
            throw new BadRequestException("You must be a member to vote");
        }
        if (communityPollVoteRepository.existsByPollIdAndUserId(pollId, userId)) {
            throw new BadRequestException("You have already voted");
        }
        CommunityPollOption option = communityPollOptionRepository.findById(optionId)
                .orElseThrow(() -> new ResourceNotFoundException("Option", "id", optionId));
        if (!option.getPoll().getId().equals(pollId)) {
            throw new BadRequestException("Option does not belong to this poll");
        }
        User user = userRepository.getReferenceById(userId);
        communityPollVoteRepository.save(CommunityPollVote.builder()
                .poll(poll)
                .option(option)
                .user(user)
                .build());
        option.setVotesCount(option.getVotesCount() != null ? option.getVotesCount() + 1 : 1);
        communityPollOptionRepository.save(option);
        poll = communityPollRepository.findById(pollId).orElse(poll);
        return mapPollToResponse(poll, userId);
    }

    private CommunityPollResponse mapPollToResponse(CommunityPoll p, UUID currentUserId) {
        List<CommunityPollResponse.PollOptionSummary> opts = p.getOptions().stream()
                .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
                .map(o -> CommunityPollResponse.PollOptionSummary.builder()
                        .id(o.getId())
                        .text(o.getText())
                        .votesCount(o.getVotesCount() != null ? o.getVotesCount() : 0)
                        .build())
                .collect(Collectors.toList());
        UUID userVoteOptionId = null;
        if (currentUserId != null) {
            userVoteOptionId = communityPollVoteRepository.findByPollIdAndUserId(p.getId(), currentUserId)
                    .map(v -> v.getOption().getId())
                    .orElse(null);
        }
        return CommunityPollResponse.builder()
                .id(p.getId())
                .communityId(p.getCommunity().getId())
                .creatorId(p.getCreator().getId())
                .creatorName(p.getCreator().getName())
                .question(p.getQuestion())
                .endsAt(p.getEndsAt())
                .options(opts)
                .userVoteOptionId(userVoteOptionId)
                .createdAt(p.getCreatedAt())
                .build();
    }

    // ==================== EVENTS ====================

    @Transactional
    public CommunityEventResponse createEvent(UUID communityId, UUID userId, CreateCommunityEventRequest request) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", "id", communityId));
        memberRepository.findByCommunityIdAndUserId(communityId, userId)
                .orElseThrow(() -> new BadRequestException("You are not a member"));
        User creator = userRepository.getReferenceById(userId);
        CommunityEvent event = CommunityEvent.builder()
                .community(community)
                .creator(creator)
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
        event = communityEventRepository.save(event);
        return mapEventToResponse(event);
    }

    public Page<CommunityEventResponse> getCommunityEvents(UUID communityId, Pageable pageable) {
        communityRepository.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community", "id", communityId));
        return communityEventRepository.findByCommunityIdOrderByStartTimeAsc(communityId, pageable)
                .map(this::mapEventToResponse);
    }

    private CommunityEventResponse mapEventToResponse(CommunityEvent e) {
        return CommunityEventResponse.builder()
                .id(e.getId())
                .communityId(e.getCommunity().getId())
                .creatorId(e.getCreator().getId())
                .creatorName(e.getCreator().getName())
                .title(e.getTitle())
                .description(e.getDescription())
                .location(e.getLocation())
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .coverImage(e.getCoverImage())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
