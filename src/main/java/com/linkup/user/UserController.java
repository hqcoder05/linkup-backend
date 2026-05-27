package com.linkup.user;

import com.linkup.common.ApiResponse;
import com.linkup.security.CurrentUser;
import com.linkup.user.dto.UserDto;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final CurrentUser currentUser;

    public UserController(UserService userService, CurrentUser currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    ApiResponse<UserDto> me(Authentication authentication) {
        return ApiResponse.ok(UserMapper.toDto(currentUser.user(authentication)));
    }

    @GetMapping("/{id}")
    ApiResponse<UserDto> byId(@PathVariable Long id) {
        return ApiResponse.ok(UserMapper.toDto(userService.get(id)));
    }

    @GetMapping("/search")
    ApiResponse<List<UserDto>> search(@RequestParam(defaultValue = "") String keyword, Authentication authentication) {
        return ApiResponse.ok(userService.search(keyword, currentUser.id(authentication)));
    }
}
