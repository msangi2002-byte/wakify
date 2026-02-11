package com.wakilfly.service;

import com.wakilfly.dto.request.UploadContactsRequest;
import com.wakilfly.dto.response.PagedResponse;
import com.wakilfly.dto.response.UserResponse;
import com.wakilfly.model.User;
import com.wakilfly.model.UserContactHash;
import com.wakilfly.repository.UserContactHashRepository;
import com.wakilfly.repository.UserRepository;
import com.wakilfly.util.ContactHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import com.wakilfly.exception.ResourceNotFoundException;

/**
 * Facebook-style "People You May Know":
 * - Contact upload (phones/emails stored hashed); match existing users.
 * - Unified scoring: contact match + location + mutual friends + interest similarity.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PeopleYouMayKnowService {

    private static final int MAX_CONTACT_SUGGESTIONS_SCAN = 1000;
    private static final int DEFAULT_PAGE_SIZE = 200;
    private static final double WEIGHT_CONTACT = 10.0;
    private static final double WEIGHT_LOCATION = 3.0;
    private static final double WEIGHT_MUTUAL = 2.0;
    private static final double WEIGHT_INTEREST = 1.0;

    private final UserRepository userRepository;
    private final UserContactHashRepository userContactHashRepository;
    private final UserService userService;

    /**
     * Upload contact list (phones/emails). Replaces previous hashes for this user.
     * Returns users on Wakify whose phone or email is in the uploaded list (excludes self and already following).
     */
    @Transactional
    public PagedResponse<UserResponse> uploadContactsAndGetSuggestions(UUID currentUserId, UploadContactsRequest request) {
        userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

        userContactHashRepository.deleteAllByUserId(currentUserId);
        Set<String> seenPhone = new HashSet<>();
        Set<String> seenEmail = new HashSet<>();
        List<UserContactHash> toSave = new ArrayList<>();
        if (request.getPhones() != null) {
            for (String p : request.getPhones()) {
                String h = ContactHashUtil.hashPhone(p);
                if (!h.isEmpty() && seenPhone.add(h))
                    toSave.add(UserContactHash.builder().userId(currentUserId).contactType(UserContactHash.ContactHashType.PHONE).hash(h).build());
            }
        }
        if (request.getEmails() != null) {
            for (String e : request.getEmails()) {
                String h = ContactHashUtil.hashEmail(e);
                if (!h.isEmpty() && seenEmail.add(h))
                    toSave.add(UserContactHash.builder().userId(currentUserId).contactType(UserContactHash.ContactHashType.EMAIL).hash(h).build());
            }
        }
        if (!toSave.isEmpty())
            userContactHashRepository.saveAll(toSave);

        Set<String> contactHashes = userContactHashRepository.findHashesByUserId(currentUserId);
        if (contactHashes.isEmpty()) {
            return PagedResponse.<UserResponse>builder()
                    .content(Collections.emptyList())
                    .page(0).size(50).totalElements(0).totalPages(0).last(true).first(true)
                    .build();
        }

        List<User> matched = new ArrayList<>();
        int page = 0;
        while (matched.size() < 50 && page * DEFAULT_PAGE_SIZE < MAX_CONTACT_SUGGESTIONS_SCAN) {
            Pageable pageable = PageRequest.of(page, DEFAULT_PAGE_SIZE);
            var candidates = userRepository.findCandidatesForPeopleYouMayKnow(currentUserId, pageable);
            for (User u : candidates.getContent()) {
                String ph = ContactHashUtil.hashPhone(u.getPhone());
                String eh = u.getEmail() != null ? ContactHashUtil.hashEmail(u.getEmail()) : "";
                if (contactHashes.contains(ph) || contactHashes.contains(eh))
                    matched.add(u);
            }
            if (!candidates.hasNext()) break;
            page++;
        }

        List<UserResponse> responses = matched.stream()
                .map(u -> mapToUserResponse(u, userRepository.isFollowing(currentUserId, u.getId())))
                .collect(Collectors.toList());
        return PagedResponse.<UserResponse>builder()
                .content(responses)
                .page(0).size(responses.size()).totalElements(responses.size()).totalPages(1).last(true).first(true)
                .build();
    }

    /**
     * Unified "People You May Know": score = contact + location + mutual friends + interests; sorted by score desc.
     */
    public PagedResponse<UserResponse> getPeopleYouMayKnow(UUID currentUserId, int page, int size) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));
        Set<String> contactHashes = userContactHashRepository.findHashesByUserId(currentUserId);
        String currentCity = currentUser.getCurrentCity();
        String region = currentUser.getRegion();
        String country = currentUser.getCountry();
        Set<String> myInterests = parseInterests(currentUser.getInterests());

        Pageable pageable = PageRequest.of(page, size);
        var candidatesPage = userRepository.findCandidatesForPeopleYouMayKnow(currentUserId, pageable);
        List<User> candidates = candidatesPage.getContent();

        List<ScoredUser> scored = new ArrayList<>();
        for (User u : candidates) {
            double contactScore = contactHashes.contains(ContactHashUtil.hashPhone(u.getPhone()))
                    || (u.getEmail() != null && contactHashes.contains(ContactHashUtil.hashEmail(u.getEmail()))) ? 1 : 0;
            double locationScore = locationScore(currentCity, region, country, u.getCurrentCity(), u.getRegion(), u.getCountry());
            long mutual = userRepository.countMutualFollowing(currentUserId, u.getId());
            double interestScore = interestOverlap(myInterests, parseInterests(u.getInterests()));

            double total = WEIGHT_CONTACT * contactScore + WEIGHT_LOCATION * locationScore + WEIGHT_MUTUAL * mutual + WEIGHT_INTEREST * interestScore;
            scored.add(new ScoredUser(u, total));
        }
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        List<UserResponse> content = scored.stream()
                .map(su -> mapToUserResponse(su.user, userRepository.isFollowing(currentUserId, su.user.getId())))
                .collect(Collectors.toList());

        return PagedResponse.<UserResponse>builder()
                .content(content)
                .page(candidatesPage.getNumber())
                .size(candidatesPage.getSize())
                .totalElements(candidatesPage.getTotalElements())
                .totalPages(candidatesPage.getTotalPages())
                .last(candidatesPage.isLast())
                .first(candidatesPage.isFirst())
                .build();
    }

    private static double locationScore(String myCity, String myRegion, String myCountry, String theirCity, String theirRegion, String theirCountry) {
        if (myCountry != null && !myCountry.isBlank() && myCountry.equalsIgnoreCase(theirCountry)) {
            if (myRegion != null && !myRegion.isBlank() && myRegion.equalsIgnoreCase(theirRegion)) {
                if (myCity != null && !myCity.isBlank() && myCity.equalsIgnoreCase(theirCity)) return 3.0;
                return 2.0;
            }
            return 1.0;
        }
        return 0.0;
    }

    private static Set<String> parseInterests(String interests) {
        if (interests == null || interests.isBlank()) return Set.of();
        return Arrays.stream(interests.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    private static double interestOverlap(Set<String> mine, Set<String> theirs) {
        if (mine.isEmpty() || theirs.isEmpty()) return 0;
        long overlap = mine.stream().filter(theirs::contains).count();
        return (double) overlap;
    }

    private UserResponse mapToUserResponse(User user, Boolean isFollowing) {
        return userService.toUserResponse(user, isFollowing);
    }

    private static class ScoredUser {
        final User user;
        final double score;
        ScoredUser(User user, double score) { this.user = user; this.score = score; }
    }
}
