package com.linkup.post;

import com.linkup.comment.CommentRepository;
import com.linkup.like.LikeRepository;
import com.linkup.post.dto.PostDtos.PostDto;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeedService {
    private static final int HOME_CANDIDATE_LIMIT = 200;
    private final PostRepository postRepository;
    private final PostService postService;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;

    public FeedService(PostRepository postRepository, PostService postService, LikeRepository likeRepository, CommentRepository commentRepository) {
        this.postRepository = postRepository;
        this.postService = postService;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional(readOnly = true)
    public List<PostDto> homeFeed(Long viewerId, int page, int size) {
        Map<Long, Double> affinityCache = new HashMap<>();
        List<Post> ranked = postRepository.findFeed(viewerId, PageRequest.of(0, HOME_CANDIDATE_LIMIT)).stream()
                .sorted(Comparator.comparingDouble((Post post) -> finalHomeScore(viewerId, post, affinityCache)).reversed())
                .toList();

        int fromIndex = Math.min(Math.max(page, 0) * size, ranked.size());
        int toIndex = Math.min(fromIndex + size, ranked.size());
        return ranked.subList(fromIndex, toIndex).stream()
                .map(post -> postService.toDto(post, viewerId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PostDto> explore(Long viewerId, int page, int size) {
        return postRepository.findExplore(viewerId, PageRequest.of(page, size)).stream()
                .map(post -> postService.toDto(post, viewerId))
                .toList();
    }

    private double finalHomeScore(Long viewerId, Post post, Map<Long, Double> affinityCache) {
        double recencyScore = recencyScore(post.getCreatedAt());
        double affinityScore = affinityCache.computeIfAbsent(post.getUser().getId(), authorId -> relationshipScore(viewerId, authorId));
        return (recencyScore * 0.7) + (affinityScore * 0.3);
    }

    private double recencyScore(Instant createdAt) {
        if (createdAt == null) {
            return 0.0;
        }
        double hours = Math.max(0.0, Duration.between(createdAt, Instant.now()).toMinutes() / 60.0);
        return 1.0 / (hours + 2.0);
    }

    private double relationshipScore(Long viewerId, Long authorId) {
        if (viewerId == null || viewerId.equals(authorId)) {
            return 0.0;
        }
        long likes = likeRepository.countByUserIdAndPostUserId(viewerId, authorId);
        long comments = commentRepository.countByUserIdAndPostUserId(viewerId, authorId);
        return likes + (comments * 2.0);
    }
}
