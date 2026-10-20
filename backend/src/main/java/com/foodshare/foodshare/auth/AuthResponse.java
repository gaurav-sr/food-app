package com.foodshare.foodshare.auth;

public record AuthResponse(String token, AuthenticatedUser user) {
}
