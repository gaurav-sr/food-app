package com.foodshare.foodshare.auth;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AuthTokenService {
    private static final long TOKEN_LIFETIME_SECONDS = 60 * 60 * 24;

    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentMap<String, TokenSession> sessions = new ConcurrentHashMap<>();

    public String createToken(AuthenticatedUser user) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessions.put(token, new TokenSession(user, Instant.now().plusSeconds(TOKEN_LIFETIME_SECONDS)));
        return token;
    }

    public AuthenticatedUser findUser(String token) {
        TokenSession session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (!session.expiresAt().isAfter(Instant.now())) {
            sessions.remove(token, session);
            return null;
        }
        return session.user();
    }

    public void revoke(String token) {
        sessions.remove(token);
    }

    private record TokenSession(AuthenticatedUser user, Instant expiresAt) {
    }
}
