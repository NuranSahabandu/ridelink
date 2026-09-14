package com.ridelink.account.controller;

import com.ridelink.account.dto.UpdateRoleRequest;
import com.ridelink.account.dto.UpdateStatusRequest;
import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.model.Role;
import com.ridelink.account.security.AuthenticatedUser;
import com.ridelink.account.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list(@RequestParam(required = false) Role role) {
        return userService.list(role);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable UUID id) {
        return userService.getById(id);
    }

    @PatchMapping("/{id}/status")
    public UserResponse updateStatus(@PathVariable UUID id,
                                     @AuthenticationPrincipal AuthenticatedUser admin,
                                     @Valid @RequestBody UpdateStatusRequest request) {
        return userService.updateStatus(id, admin.id(), request);
    }

    @PatchMapping("/{id}/role")
    public UserResponse updateRole(@PathVariable UUID id,
                                   @AuthenticationPrincipal AuthenticatedUser admin,
                                   @Valid @RequestBody UpdateRoleRequest request) {
        return userService.updateRole(id, admin.id(), request);
    }
}