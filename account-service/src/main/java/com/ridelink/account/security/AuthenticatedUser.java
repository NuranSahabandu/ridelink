package com.ridelink.account.security;

import com.ridelink.account.model.AccountStatus;
import com.ridelink.account.model.Role;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, Role role, AccountStatus status) {}