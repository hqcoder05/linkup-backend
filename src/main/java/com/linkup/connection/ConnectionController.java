package com.linkup.connection;

import com.linkup.common.ApiResponse;
import com.linkup.connection.dto.ConnectionDto;
import com.linkup.security.CurrentUser;
import com.linkup.user.dto.UserDto;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConnectionController {
    private final ConnectionService connectionService;
    private final CurrentUser currentUser;

    public ConnectionController(ConnectionService connectionService, CurrentUser currentUser) {
        this.connectionService = connectionService;
        this.currentUser = currentUser;
    }

    @PostMapping("/api/connections/{targetUserId}")
    ApiResponse<ConnectionDto> request(@PathVariable Long targetUserId, Authentication authentication) {
        return ApiResponse.ok("Connection request sent", connectionService.request(currentUser.id(authentication), targetUserId));
    }

    @PostMapping("/api/connections/{requesterId}/accept")
    ApiResponse<ConnectionDto> accept(@PathVariable Long requesterId, Authentication authentication) {
        return ApiResponse.ok("Connection accepted", connectionService.accept(requesterId, currentUser.id(authentication)));
    }

    @PostMapping("/api/connections/{requesterId}/decline")
    ApiResponse<ConnectionDto> decline(@PathVariable Long requesterId, Authentication authentication) {
        return ApiResponse.ok("Connection declined", connectionService.decline(requesterId, currentUser.id(authentication)));
    }

    @DeleteMapping("/api/connections/{targetUserId}")
    ApiResponse<Void> remove(@PathVariable Long targetUserId, Authentication authentication) {
        connectionService.remove(currentUser.id(authentication), targetUserId);
        return ApiResponse.message("Connection removed");
    }

    @GetMapping("/api/users/{userId}/connections")
    ApiResponse<List<UserDto>> connections(@PathVariable Long userId) {
        return ApiResponse.ok(connectionService.connections(userId));
    }

    @GetMapping("/api/connections/incoming")
    ApiResponse<List<ConnectionDto>> incoming(Authentication authentication) {
        return ApiResponse.ok(connectionService.incoming(currentUser.id(authentication)));
    }

    @GetMapping("/api/connections/outgoing")
    ApiResponse<List<ConnectionDto>> outgoing(Authentication authentication) {
        return ApiResponse.ok(connectionService.outgoing(currentUser.id(authentication)));
    }

    @GetMapping("/api/users/{userId}/connection-status")
    ApiResponse<Map<String, String>> status(@PathVariable Long userId, Authentication authentication) {
        return ApiResponse.ok(Map.of("status", connectionService.status(currentUser.id(authentication), userId)));
    }
}
