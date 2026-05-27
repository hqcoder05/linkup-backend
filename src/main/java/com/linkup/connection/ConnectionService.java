package com.linkup.connection;

import com.linkup.common.BadRequestException;
import com.linkup.common.ForbiddenException;
import com.linkup.common.ResourceNotFoundException;
import com.linkup.connection.dto.ConnectionDto;
import com.linkup.user.User;
import com.linkup.user.UserMapper;
import com.linkup.user.UserService;
import com.linkup.user.dto.UserDto;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConnectionService {
    private final ConnectionRepository connectionRepository;
    private final UserService userService;

    public ConnectionService(ConnectionRepository connectionRepository, UserService userService) {
        this.connectionRepository = connectionRepository;
        this.userService = userService;
    }

    @Transactional
    public ConnectionDto request(Long requesterId, Long targetUserId) {
        if (requesterId.equals(targetUserId)) {
            throw new BadRequestException("Cannot connect with yourself");
        }
        User requester = userService.get(requesterId);
        User addressee = userService.get(targetUserId);
        Connection connection = connectionRepository.findBetween(requesterId, targetUserId).orElse(null);
        if (connection != null) {
            if (connection.getStatus() == ConnectionStatus.ACCEPTED) {
                throw new BadRequestException("Users are already connected");
            }
            if (connection.getStatus() == ConnectionStatus.PENDING) {
                return toDto(connection);
            }
            connectionRepository.delete(connection);
        }
        Connection created = new Connection();
        created.setId(new ConnectionId(requesterId, targetUserId));
        created.setRequester(requester);
        created.setAddressee(addressee);
        created.setStatus(ConnectionStatus.PENDING);
        return toDto(connectionRepository.save(created));
    }

    @Transactional
    public ConnectionDto accept(Long requesterId, Long currentUserId) {
        Connection connection = connectionRepository.findById(new ConnectionId(requesterId, currentUserId))
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found"));
        if (!connection.getAddressee().getId().equals(currentUserId)) {
            throw new ForbiddenException("Only the receiver can accept this connection request");
        }
        connection.setStatus(ConnectionStatus.ACCEPTED);
        connection.setRespondedAt(Instant.now());
        return toDto(connection);
    }

    @Transactional
    public ConnectionDto decline(Long requesterId, Long currentUserId) {
        Connection connection = connectionRepository.findById(new ConnectionId(requesterId, currentUserId))
                .orElseThrow(() -> new ResourceNotFoundException("Connection request not found"));
        if (!connection.getAddressee().getId().equals(currentUserId)) {
            throw new ForbiddenException("Only the receiver can decline this connection request");
        }
        connection.setStatus(ConnectionStatus.DECLINED);
        connection.setRespondedAt(Instant.now());
        return toDto(connection);
    }

    @Transactional
    public void remove(Long currentUserId, Long targetUserId) {
        Connection connection = connectionRepository.findBetween(currentUserId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection not found"));
        if (!connection.getRequester().getId().equals(currentUserId) && !connection.getAddressee().getId().equals(currentUserId)) {
            throw new ForbiddenException("Cannot remove another user's connection");
        }
        connectionRepository.delete(connection);
    }

    public List<UserDto> connections(Long userId) {
        return connectionRepository.findAcceptedForUser(userId).stream()
                .map(connection -> otherUser(connection, userId))
                .map(UserMapper::toDto)
                .toList();
    }

    public List<ConnectionDto> incoming(Long userId) {
        return connectionRepository.findByAddresseeIdAndStatusOrderByCreatedAtDesc(userId, ConnectionStatus.PENDING)
                .stream().map(this::toDto).toList();
    }

    public List<ConnectionDto> outgoing(Long userId) {
        return connectionRepository.findByRequesterIdAndStatusOrderByCreatedAtDesc(userId, ConnectionStatus.PENDING)
                .stream().map(this::toDto).toList();
    }

    public String status(Long currentUserId, Long targetUserId) {
        return connectionRepository.findBetween(currentUserId, targetUserId)
                .map(connection -> {
                    if (connection.getStatus() != ConnectionStatus.PENDING) {
                        return connection.getStatus().name();
                    }
                    return connection.getRequester().getId().equals(currentUserId) ? "PENDING_SENT" : "PENDING_RECEIVED";
                })
                .orElse("NONE");
    }

    private User otherUser(Connection connection, Long userId) {
        return connection.getRequester().getId().equals(userId) ? connection.getAddressee() : connection.getRequester();
    }

    private ConnectionDto toDto(Connection connection) {
        return new ConnectionDto(
                UserMapper.toDto(connection.getRequester()),
                UserMapper.toDto(connection.getAddressee()),
                connection.getStatus().name(),
                connection.getCreatedAt(),
                connection.getRespondedAt());
    }
}
