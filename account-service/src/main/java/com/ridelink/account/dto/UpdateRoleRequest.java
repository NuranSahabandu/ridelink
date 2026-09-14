package com.ridelink.account.dto;

import com.ridelink.account.model.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull Role role) {}