package com.ridelink.account.controller;

import com.ridelink.account.dto.UpdateProfileRequest;
import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.security.AuthenticatedUser;
import com.ridelink.account.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser me) {
        return userService.getById(me.id());
    }

    @PutMapping("/me")
    public UserResponse updateMe(@AuthenticationPrincipal AuthenticatedUser me,
                                 @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(me.id(), request);
    }
}