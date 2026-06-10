package com.linkup.story;

import com.linkup.chat.MessageRepository;
import com.linkup.comment.CommentRepository;
import com.linkup.common.ForbiddenException;
import com.linkup.common.ResourceNotFoundException;
import com.linkup.like.LikeRepository;
import com.linkup.media.Media;
import com.linkup.post.dto.PostDtos.PostMediaDto;
import com.linkup.story.dto.StoryDtos.CreateStoryRequest;
import com.linkup.story.dto.StoryDtos.StoryDto;
import com.linkup.story.dto.StoryDtos.StoryViewerDto;
import com.linkup.story.dto.StoryDtos.UserStoriesDto;
import com.linkup.user.User;
import com.linkup.user.UserMapper;
import com.linkup.user.UserService;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoryService {
    private final StoryRepository storyRepository;
    private final StoryViewRepository storyViewRepository;
    private final UserService userService;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final MessageRepository messageRepository;

    public StoryService(
            StoryRepository storyRepository,
            StoryViewRepository storyViewRepository,
            UserService userService,
            LikeRepository likeRepository,
            CommentRepository commentRepository,
            MessageRepository messageRepository) {
        this.storyRepository = storyRepository;
        this.storyViewRepository = storyViewRepository;
        this.userService = userService;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    public StoryDto create(Long userId, CreateStoryRequest request) {
        User user = userService.get(userId);
        Story story = new Story();
        story.setUser(user);
        story.setCaption(request.caption());
        story.setExpiresAt(Instant.now().plusSeconds(24 * 60 * 60));
        if (request.media() != null) {
            for (int i = 0; i < request.media().size(); i++) {
                var item = request.media().get(i);
                Media media = new Media();
                media.setStory(story);
                media.setUser(user);
                media.setUrl(item.url());
                media.setThumbnailUrl(item.thumbnailUrl());
                media.setType(item.type() == null ? "image" : item.type());
                media.setPosition(i);
                media.setWidth(item.width());
                media.setHeight(item.height());
                media.setFileSize(0L);
                story.getMedia().add(media);
            }
        }
        return toDto(storyRepository.save(story));
    }

    @Transactional(readOnly = true)
    public List<UserStoriesDto> visibleStories(Long viewerId) {
        List<Story> stories = storyRepository.findVisibleStories(viewerId, Instant.now());
        Set<Long> seenStoryIds = storyViewRepository.findSeenStoryIds(viewerId).stream().collect(Collectors.toSet());
        Map<Long, List<Story>> storiesByUser = stories.stream()
                .collect(Collectors.groupingBy(
                        story -> story.getUser().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        return storiesByUser.values().stream()
                .map(group -> toUserStoriesDto(viewerId, group, seenStoryIds))
                .sorted((left, right) -> compareStoryBubbles(viewerId, left, right))
                .toList();
    }

    @Transactional
    public void markAsSeen(Long storyId, Long viewerId) {
        Story story = storyRepository.findById(storyId).orElseThrow(() -> new ResourceNotFoundException("Story not found"));
        if (story.getExpiresAt().isBefore(Instant.now()) || !story.isActive()) {
            return;
        }
        if (storyViewRepository.existsByViewerIdAndStoryId(viewerId, storyId)) {
            return;
        }
        StoryView storyView = new StoryView();
        storyView.setStory(story);
        storyView.setViewer(userService.get(viewerId));
        storyViewRepository.save(storyView);
    }

    @Transactional(readOnly = true)
    public StoryDto detail(Long storyId, Long viewerId) {
        Story story = storyRepository.findWithUserAndMediaById(storyId).orElseThrow(() -> new ResourceNotFoundException("Story not found"));
        ensureVisible(story, viewerId);
        return toDto(story);
    }

    @Transactional
    public void delete(Long storyId, Long userId) {
        Story story = storyRepository.findWithUserAndMediaById(storyId).orElseThrow(() -> new ResourceNotFoundException("Story not found"));
        if (!story.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Only the story owner can delete this story");
        }
        story.setActive(false);
    }

    @Transactional(readOnly = true)
    public List<StoryViewerDto> viewers(Long storyId, Long userId) {
        Story story = storyRepository.findWithUserAndMediaById(storyId).orElseThrow(() -> new ResourceNotFoundException("Story not found"));
        if (!story.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Only the story owner can view story viewers");
        }
        return storyViewRepository.findByStoryIdOrderByCreatedAtDesc(storyId).stream()
                .map(view -> new StoryViewerDto(UserMapper.toDto(view.getViewer()), view.getCreatedAt()))
                .toList();
    }

    @Scheduled(fixedDelay = 15 * 60 * 1000)
    @Transactional
    public void deactivateExpiredStories() {
        storyRepository.deactivateExpired(Instant.now());
    }

    private StoryDto toDto(Story story) {
        return new StoryDto(
                story.getId(),
                UserMapper.toDto(story.getUser()),
                story.getCaption(),
                story.getMedia().stream()
                        .map(media -> new PostMediaDto(media.getId(), media.getUrl(), media.getThumbnailUrl(), media.getType(), media.getPosition(), media.getWidth(), media.getHeight()))
                        .toList(),
                story.getCreatedAt(),
                story.getExpiresAt());
    }

    private void ensureVisible(Story story, Long viewerId) {
        if (story.getExpiresAt().isBefore(Instant.now()) || !story.isActive()) {
            throw new ResourceNotFoundException("Story not found");
        }
        if (story.getUser().getId().equals(viewerId)) {
            return;
        }
        if (viewerId == null) {
            throw new ForbiddenException("This story is not visible");
        }
        boolean visible = visibleStories(viewerId).stream()
                .flatMap(group -> group.stories().stream())
                .anyMatch(item -> item.id().equals(story.getId()));
        if (!visible) {
            throw new ForbiddenException("This story is not visible");
        }
    }

    private UserStoriesDto toUserStoriesDto(Long viewerId, List<Story> stories, Set<Long> seenStoryIds) {
        List<Story> orderedStories = stories.stream()
                .sorted(Comparator.comparing(Story::getCreatedAt))
                .toList();
        boolean hasUnseen = orderedStories.stream().anyMatch(story -> !seenStoryIds.contains(story.getId()));
        Instant latestStoryTime = orderedStories.stream()
                .map(Story::getCreatedAt)
                .max(Comparator.naturalOrder())
                .orElse(Instant.EPOCH);
        return new UserStoriesDto(
                UserMapper.toDto(orderedStories.getFirst().getUser()),
                orderedStories.stream().map(this::toDto).toList(),
                hasUnseen,
                latestStoryTime);
    }

    private double affinityScore(Long viewerId, Long authorId) {
        if (viewerId == null || viewerId.equals(authorId)) {
            return 0.0;
        }
        long likes = likeRepository.countByUserIdAndPostUserId(viewerId, authorId);
        long comments = commentRepository.countByUserIdAndPostUserId(viewerId, authorId);
        long messages = messageRepository.countMessagesBetween(viewerId, authorId);
        return likes + (comments * 2.0) + Math.min(messages, 50);
    }

    private int compareStoryBubbles(Long viewerId, UserStoriesDto left, UserStoriesDto right) {
        int unseenCompare = Boolean.compare(right.hasUnseen(), left.hasUnseen());
        if (unseenCompare != 0) {
            return unseenCompare;
        }
        int affinityCompare = Double.compare(
                affinityScore(viewerId, right.user().id()),
                affinityScore(viewerId, left.user().id()));
        if (affinityCompare != 0) {
            return affinityCompare;
        }
        return right.latestStoryTime().compareTo(left.latestStoryTime());
    }
}
