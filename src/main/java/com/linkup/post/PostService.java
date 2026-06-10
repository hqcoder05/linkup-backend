package com.linkup.post;

import com.linkup.common.ForbiddenException;
import com.linkup.common.ResourceNotFoundException;
import com.linkup.comment.CommentRepository;
import com.linkup.follow.FollowService;
import com.linkup.like.LikeRepository;
import com.linkup.media.Media;
import com.linkup.post.dto.PostDtos.CreatePostRequest;
import com.linkup.post.dto.PostDtos.PostMediaDto;
import com.linkup.post.dto.PostDtos.PostDto;
import com.linkup.post.dto.PostDtos.TaggedUserDto;
import com.linkup.post.dto.PostDtos.UpdatePostRequest;
import com.linkup.user.User;
import com.linkup.user.UserMapper;
import com.linkup.user.UserService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final UserService userService;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final HashtagRepository hashtagRepository;
    private final PostTagRepository postTagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final SavedPostRepository savedPostRepository;
    private final FollowService followService;

    public PostService(
            PostRepository postRepository,
            UserService userService,
            LikeRepository likeRepository,
            CommentRepository commentRepository,
            HashtagRepository hashtagRepository,
            PostTagRepository postTagRepository,
            PostHashtagRepository postHashtagRepository,
            SavedPostRepository savedPostRepository,
            FollowService followService) {
        this.postRepository = postRepository;
        this.userService = userService;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.hashtagRepository = hashtagRepository;
        this.postTagRepository = postTagRepository;
        this.postHashtagRepository = postHashtagRepository;
        this.savedPostRepository = savedPostRepository;
        this.followService = followService;
    }

    @Transactional
    public PostDto create(Long userId, CreatePostRequest request) {
        User user = userService.get(userId);
        Post post = new Post();
        post.setUser(user);
        post.setCaption(request.caption() == null ? "" : request.caption());
        Post saved = postRepository.save(post);
        attachMedia(saved, user, request);
        attachTags(saved, request);
        attachHashtags(saved, saved.getCaption());
        return toDto(saved, userId);
    }

    @Transactional
    public PostDto update(Long postId, Long userId, UpdatePostRequest request) {
        Post post = get(postId);
        ensureOwner(post, userId);
        post.setCaption(request.caption() == null ? "" : request.caption());
        post.getHashtags().clear();
        attachHashtags(post, post.getCaption());
        return toDto(post, userId);
    }

    @Transactional
    public void delete(Long postId, Long userId) {
        Post post = get(postId);
        ensureOwner(post, userId);
        postRepository.delete(post);
    }

    public PostDto getDto(Long postId, Long currentUserId) {
        Post post = get(postId);
        ensureVisible(post, currentUserId);
        return toDto(post, currentUserId);
    }

    public Post get(Long postId) {
        return postRepository.findById(postId).orElseThrow(() -> new ResourceNotFoundException("Post not found"));
    }

    public List<PostDto> feed(Long userId, int page, int size) {
        return postRepository.findFeed(userId, PageRequest.of(page, size)).stream().map(p -> toDto(p, userId)).toList();
    }

    public List<PostDto> byUser(Long userId, Long currentUserId, int page, int size) {
        User owner = userService.get(userId);
        if (owner.isPrivateAccount()
                && !owner.getId().equals(currentUserId)
                && (currentUserId == null || !followService.isAcceptedFollower(currentUserId, owner.getId()))) {
            return List.of();
        }
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size)).stream()
                .map(p -> toDto(p, currentUserId)).toList();
    }

    public List<PostDto> search(String keyword, Long currentUserId, int page, int size) {
        String value = keyword == null ? "" : keyword.trim();
        if (value.isBlank()) {
            return List.of();
        }
        return postRepository.findByCaptionContainingIgnoreCaseOrderByCreatedAtDesc(value, PageRequest.of(page, size)).stream()
                .filter(post -> isVisible(post, currentUserId))
                .map(post -> toDto(post, currentUserId))
                .toList();
    }

    public List<PostDto> byHashtag(String name, Long currentUserId, int page, int size) {
        String value = name == null ? "" : name.trim().replaceFirst("^#", "").toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            return List.of();
        }
        return postHashtagRepository.findByHashtagNameOrderByPostCreatedAtDesc(value, PageRequest.of(page, size)).stream()
                .map(PostHashtag::getPost)
                .filter(post -> isVisible(post, currentUserId))
                .map(post -> toDto(post, currentUserId))
                .toList();
    }

    @Transactional
    public void savePost(Long postId, Long userId) {
        Post post = get(postId);
        ensureVisible(post, userId);
        savedPostRepository.savePost(userId, postId);
    }

    @Transactional
    public void unsavePost(Long postId, Long userId) {
        savedPostRepository.unsavePost(userId, postId);
    }

    public List<PostDto> savedPosts(Long userId, int page, int size) {
        return savedPostRepository.findByIdUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size)).stream()
                .map(SavedPost::getPost)
                .filter(post -> isVisible(post, userId))
                .map(post -> toDto(post, userId))
                .toList();
    }

    private void ensureOwner(Post post, Long userId) {
        if (!post.getUser().getId().equals(userId)) {
            throw new ForbiddenException("Only the post owner can change this post");
        }
    }

    public PostDto toDto(Post post, Long currentUserId) {
        return new PostDto(
                post.getId(),
                UserMapper.toDto(post.getUser()),
                post.getCaption(),
                post.getMedia().stream()
                        .map(media -> new PostMediaDto(media.getId(), media.getUrl(), media.getThumbnailUrl(), media.getType(), media.getPosition(), media.getWidth(), media.getHeight()))
                        .toList(),
                postTagRepository.findByPostId(post.getId()).stream()
                        .map(tag -> new TaggedUserDto(UserMapper.toDto(tag.getTaggedUser()), tag.getMediaPosition(), tag.getX(), tag.getY()))
                        .toList(),
                postHashtagRepository.findByPostId(post.getId()).stream()
                        .map(postHashtag -> postHashtag.getHashtag().getName())
                        .toList(),
                post.getLikesCount(),
                post.getCommentsCount(),
                currentUserId != null && likeRepository.existsByPostIdAndUserId(post.getId(), currentUserId),
                currentUserId != null && savedPostRepository.existsById(new SavedPostId(currentUserId, post.getId())),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }

    public void ensureVisible(Post post, Long viewerId) {
        if (post.getUser().getId().equals(viewerId)) {
            return;
        }
        if (!post.getUser().isPrivateAccount() && post.getUser().isContentVisibleToPublic()) {
            return;
        }
        if (viewerId == null || !followService.isAcceptedFollower(viewerId, post.getUser().getId())) {
            throw new ForbiddenException("This post belongs to a private account");
        }
    }

    private boolean isVisible(Post post, Long viewerId) {
        if (post.getUser().getId().equals(viewerId)) {
            return true;
        }
        if (!post.getUser().isPrivateAccount() && post.getUser().isContentVisibleToPublic()) {
            return true;
        }
        return viewerId != null && followService.isAcceptedFollower(viewerId, post.getUser().getId());
    }

    private void attachMedia(Post post, User user, CreatePostRequest request) {
        if (request.media() == null || request.media().isEmpty()) {
            return;
        }
        for (int i = 0; i < request.media().size(); i++) {
            var item = request.media().get(i);
            Media media = new Media();
            media.setPost(post);
            media.setUser(user);
            media.setUrl(item.url());
            media.setThumbnailUrl(item.thumbnailUrl());
            media.setType(item.type() == null ? "image" : item.type());
            media.setPosition(i);
            media.setWidth(item.width());
            media.setHeight(item.height());
            media.setFileSize(0L);
            post.getMedia().add(media);
        }
    }

    private void attachTags(Post post, CreatePostRequest request) {
        if (request.tags() == null || request.tags().isEmpty()) {
            return;
        }
        for (var item : request.tags()) {
            PostTag tag = new PostTag();
            tag.setPost(post);
            tag.setTaggedUser(userService.get(item.userId()));
            tag.setMediaPosition(item.mediaPosition());
            tag.setX(item.x());
            tag.setY(item.y());
            post.getTags().add(tag);
        }
    }

    private void attachHashtags(Post post, String caption) {
        for (String name : extractHashtags(caption)) {
            Hashtag hashtag = hashtagRepository.findByName(name).orElseGet(() -> {
                Hashtag created = new Hashtag();
                created.setName(name);
                return hashtagRepository.save(created);
            });
            PostHashtag postHashtag = new PostHashtag();
            postHashtag.setPost(post);
            postHashtag.setHashtag(hashtag);
            post.getHashtags().add(postHashtag);
        }
    }

    private Set<String> extractHashtags(String text) {
        Set<String> tags = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return tags;
        }
        Matcher matcher = Pattern.compile("(?<!\\w)#([\\p{L}\\p{N}_]{1,100})").matcher(text);
        while (matcher.find()) {
            tags.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }
        return tags;
    }
}
