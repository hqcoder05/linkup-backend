package com.linkup.follow;

import com.linkup.common.BadRequestException;
import com.linkup.common.ForbiddenException;
import com.linkup.common.ResourceNotFoundException;
import com.linkup.follow.dto.FollowDto;
import com.linkup.user.User;
import com.linkup.user.UserMapper;
import com.linkup.user.UserService;
import com.linkup.user.dto.UserDto;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowService {
    private final FollowRepository followRepository;
    private final UserService userService;

    public FollowService(FollowRepository followRepository, UserService userService) {
        this.followRepository = followRepository;
        this.userService = userService;
    }

    @Transactional
    public FollowDto follow(Long followerId, Long targetUserId) {
        if (followerId.equals(targetUserId)) {
            throw new BadRequestException("Cannot follow yourself");
        }
        User follower = userService.get(followerId);
        User following = userService.get(targetUserId);
        Follow existing = followRepository.findByIdFollowerIdAndIdFollowingId(followerId, targetUserId).orElse(null);
        if (existing != null) {
            return toDto(existing);
        }

        Follow follow = new Follow();
        follow.setId(new FollowId(followerId, targetUserId));
        follow.setFollower(follower);
        follow.setFollowing(following);
        if (following.isPrivateAccount()) {
            follow.setStatus(FollowStatus.PENDING);
        } else {
            follow.setStatus(FollowStatus.ACCEPTED);
            follow.setApprovedAt(Instant.now());
        }
        return toDto(followRepository.save(follow));
    }

    @Transactional
    public FollowDto approve(Long followerId, Long currentUserId) {
        Follow follow = followRepository.findByIdFollowerIdAndIdFollowingId(followerId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow request not found"));
        if (!follow.getFollowing().getId().equals(currentUserId)) {
            throw new ForbiddenException("Only the private account owner can approve this follow request");
        }
        follow.setStatus(FollowStatus.ACCEPTED);
        follow.setApprovedAt(Instant.now());
        return toDto(follow);
    }

    @Transactional
    public void decline(Long followerId, Long currentUserId) {
        Follow follow = followRepository.findByIdFollowerIdAndIdFollowingId(followerId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow request not found"));
        if (!follow.getFollowing().getId().equals(currentUserId)) {
            throw new ForbiddenException("Only the private account owner can decline this follow request");
        }
        followRepository.delete(follow);
    }

    @Transactional
    public void unfollow(Long followerId, Long targetUserId) {
        Follow follow = followRepository.findByIdFollowerIdAndIdFollowingId(followerId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Follow not found"));
        followRepository.delete(follow);
    }

    public List<UserDto> followers(Long userId) {
        return followRepository.findByIdFollowingIdAndStatusOrderByCreatedAtDesc(userId, FollowStatus.ACCEPTED)
                .stream().map(Follow::getFollower).map(UserMapper::toDto).toList();
    }

    public List<UserDto> following(Long userId) {
        return followRepository.findByIdFollowerIdAndStatusOrderByCreatedAtDesc(userId, FollowStatus.ACCEPTED)
                .stream().map(Follow::getFollowing).map(UserMapper::toDto).toList();
    }

    public List<FollowDto> pendingRequests(Long userId) {
        return followRepository.findByIdFollowingIdAndStatus(userId, FollowStatus.PENDING).stream().map(this::toDto).toList();
    }

    public boolean isAcceptedFollower(Long followerId, Long followingId) {
        return followRepository.existsByIdFollowerIdAndIdFollowingIdAndStatus(followerId, followingId, FollowStatus.ACCEPTED);
    }

    public long followersCount(Long userId) {
        return followRepository.countByIdFollowingIdAndStatus(userId, FollowStatus.ACCEPTED);
    }

    public long followingCount(Long userId) {
        return followRepository.countByIdFollowerIdAndStatus(userId, FollowStatus.ACCEPTED);
    }

    public String status(Long followerId, Long followingId) {
        return followRepository.findByIdFollowerIdAndIdFollowingId(followerId, followingId)
                .map(follow -> follow.getStatus().name())
                .orElse("NONE");
    }

    private FollowDto toDto(Follow follow) {
        return new FollowDto(
                UserMapper.toDto(follow.getFollower()),
                UserMapper.toDto(follow.getFollowing()),
                follow.getStatus().name(),
                follow.getCreatedAt(),
                follow.getApprovedAt());
    }
}
