package com.ridelink.account.dto;

import java.time.Instant;

public record AuthResponse(String token, Instant expiresAt, UserResponse user) {}