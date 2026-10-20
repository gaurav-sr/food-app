package com.foodshare.foodshare.auth;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String displayName, String email) {
}
