package com.foodshare.foodshare.auth;

import java.util.UUID;

record UserAccount(UUID id, String displayName, String email, String passwordHash) {
    AuthenticatedUser toAuthenticatedUser() {
        return new AuthenticatedUser(id, displayName, email);
    }
}
