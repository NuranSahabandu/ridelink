package com.ridelink.account.service;

import com.ridelink.account.dto.UpdateProfileRequest;
import com.ridelink.account.dto.UserResponse;
import com.ridelink.account.exception.ApiException;
import com.ridelink.account.model.User;
import com.ridelink.account.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ridelink.account.dto.UpdateRoleRequest;
import com.ridelink.account.dto.UpdateStatusRequest;
import com.ridelink.account.model.Role;
import java.util.List;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return UserResponse.from(findOrThrow(id));
    }

    @Transactional
    public UserResponse updateProfile(UUID id, UpdateProfileRequest req) {
        User user = findOrThrow(id);
        user.setFullName(req.fullName().trim());
        user.setPhone(req.phone());
        return UserResponse.from(user);   // dirty-checked and flushed by @Transactional
    }

    private User findOrThrow(UUID id) {
        return users.findById(id)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "User not found"));
    }

        @Transactional(readOnly = true)
    public List<UserResponse> list(Role role) {
        List<User> result = (role == null) ? users.findAll() : users.findByRole(role);
        return result.stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse updateStatus(UUID targetId, UUID actingAdminId, UpdateStatusRequest req) {
        if (targetId.equals(actingAdminId)) {
            throw ApiException.conflict("SELF_MODIFICATION", "Admins cannot change their own status");
        }
        User user = findOrThrow(targetId);
        user.setStatus(req.status());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateRole(UUID targetId, UUID actingAdminId, UpdateRoleRequest req) {
        if (targetId.equals(actingAdminId)) {
            throw ApiException.conflict("SELF_MODIFICATION", "Admins cannot change their own role");
        }
        User user = findOrThrow(targetId);
        user.setRole(req.role());
        return UserResponse.from(user);
    }
}