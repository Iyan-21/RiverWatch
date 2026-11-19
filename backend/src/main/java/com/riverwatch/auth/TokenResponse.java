package com.riverwatch.auth;

public record TokenResponse(String accessToken, int expiresInSeconds, UserResponse user) {}
