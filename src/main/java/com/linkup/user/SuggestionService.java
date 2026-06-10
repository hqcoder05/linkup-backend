package com.linkup.user;

import com.linkup.follow.FollowRepository;
import com.linkup.post.PostHashtagRepository;
import com.linkup.profile.Profile;
import com.linkup.profile.ProfileRepository;
import com.linkup.user.dto.SuggestionDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SuggestionService {
    private static final int DISCOVERY_LIMIT = 50;
    private static final int DEFAULT_RESULT_LIMIT = 10;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PostHashtagRepository postHashtagRepository;

    public SuggestionService(
            FollowRepository followRepository,
            UserRepository userRepository,
            ProfileRepository profileRepository,
            PostHashtagRepository postHashtagRepository) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.postHashtagRepository = postHashtagRepository;
    }

    @Transactional(readOnly = true)
    public List<SuggestionDto> suggestions(Long currentUserId, int limit) {
        int resultLimit = limit <= 0 ? DEFAULT_RESULT_LIMIT : Math.min(limit, 20);
        Map<Long, CandidateScore> scores = new LinkedHashMap<>();

        for (var candidate : followRepository.findMutualFollowCandidates(currentUserId)) {
            if (candidate.getUserId().equals(currentUserId)) {
                continue;
            }
            scores.put(candidate.getUserId(), new CandidateScore(candidate.getUserId(), candidate.getMutualCount()));
        }

        if (scores.size() < resultLimit) {
            int fallbackLimit = Math.max(DISCOVERY_LIMIT, resultLimit * 3);
            for (User fallback : userRepository.findTrendingSuggestionFallback(currentUserId, fallbackLimit)) {
                if (fallback.getId().equals(currentUserId)) {
                    continue;
                }
                scores.putIfAbsent(fallback.getId(), new CandidateScore(fallback.getId(), 0));
                if (scores.size() >= DISCOVERY_LIMIT) {
                    break;
                }
            }
        }

        scores.remove(currentUserId);
        if (scores.isEmpty()) {
            return List.of();
        }

        List<Long> candidateIds = new ArrayList<>(scores.keySet());
        Map<Long, User> usersById = userRepository.findAllByIdsUnordered(candidateIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        applyLocationScores(currentUserId, candidateIds, scores);
        applyInterestScores(currentUserId, candidateIds, scores);

        return scores.values().stream()
                .filter(score -> usersById.containsKey(score.userId()))
                .sorted(Comparator.comparingInt(CandidateScore::totalScore).reversed()
                        .thenComparing(score -> usersById.get(score.userId()).getFullName(), String.CASE_INSENSITIVE_ORDER))
                .limit(resultLimit)
                .map(score -> new SuggestionDto(
                        UserMapper.toDto(usersById.get(score.userId()), false),
                        score.mutualCount(),
                        followRepository.findMutualFriendNames(currentUserId, score.userId())))
                .toList();
    }

    private void applyLocationScores(Long currentUserId, List<Long> candidateIds, Map<Long, CandidateScore> scores) {
        String currentLocation = profileRepository.findByUserId(currentUserId)
                .map(Profile::getLocation)
                .map(this::normalizeLocation)
                .orElse(null);
        if (currentLocation == null) {
            return;
        }
        for (Profile profile : profileRepository.findByUserIds(candidateIds)) {
            if (Objects.equals(currentLocation, normalizeLocation(profile.getLocation()))) {
                scores.get(profile.getUser().getId()).addLocationScore(5);
            }
        }
    }

    private void applyInterestScores(Long currentUserId, List<Long> candidateIds, Map<Long, CandidateScore> scores) {
        List<String> interestHashtags = postHashtagRepository.findInterestHashtagsForUser(currentUserId);
        if (interestHashtags.isEmpty()) {
            return;
        }
        for (var overlap : postHashtagRepository.countSharedHashtagsByCandidates(candidateIds, interestHashtags)) {
            scores.get(overlap.getUserId()).addInterestScore(overlap.getSharedCount() * 2);
        }
    }

    private String normalizeLocation(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        return location.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static final class CandidateScore {
        private final Long userId;
        private final int mutualCount;
        private int interestScore;
        private int locationScore;

        private CandidateScore(Long userId, int mutualCount) {
            this.userId = userId;
            this.mutualCount = mutualCount;
        }

        private Long userId() { return userId; }
        private int mutualCount() { return mutualCount; }
        private void addInterestScore(int score) { this.interestScore += score; }
        private void addLocationScore(int score) { this.locationScore += score; }
        private int totalScore() { return (mutualCount * 10) + interestScore + locationScore; }
    }
}
