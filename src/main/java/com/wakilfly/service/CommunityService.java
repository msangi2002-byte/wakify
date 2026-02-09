package com.wakilfly.service;

import com.wakilfly.dto.request.CreateCommunityRequest;
import com.wakilfly.dto.response.CommunityResponse;
import com.wakilfly.exception.BadRequestException;
import com.wakilfly.exception.ResourceNotFoundException;
import com.wakilfly.model.Community;
import com.wakilfly.model.CommunityMember;
import com.wakilfly.model.CommunityRole;
import com.wakilfly.model.User;
import com.wakilfly.repository.CommunityMemberRepository;
import com.wakilfly.repository.CommunityRepository;
import com.wakilfly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

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

    @Transactional
    public void joinCommunity(UUID userId, UUID communityId) {
        if (memberRepository.existsByCommunityIdAndUserId(communityId, userId)) {
            throw new RuntimeException("Already a member");
        }

        User user = userRepository.getReferenceById(userId);
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Community not found"));

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
}
