package com.foodshare.foodshare.auth;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.EmptyResultDataAccessException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final AuthTokenService tokenService;

    public AuthService(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            AuthTokenService tokenService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    public AuthResponse register(RegisterRequest request) {
        validatePasswordLength(request.password());
        String email = normalizeEmail(request.email());
        UserAccount account = new UserAccount(
                UUID.randomUUID(),
                request.displayName().trim(),
                email,
                passwordEncoder.encode(request.password())
        );
        if (account.displayName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name cannot be blank.");
        }
        int inserted = jdbcTemplate.update("""
                        INSERT INTO users (id, display_name, email, password_hash)
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT(email) DO NOTHING
                        """,
                account.id().toString(),
                account.displayName(),
                account.email(),
                account.passwordHash());
        if (inserted == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        return createAuthResponse(account);
    }

    public AuthResponse login(AuthRequest request) {
        validatePasswordLength(request.password());
        UserAccount account = findByEmail(normalizeEmail(request.email()));
        if (account == null || !passwordEncoder.matches(request.password(), account.passwordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
        }
        return createAuthResponse(account);
    }

    private UserAccount findByEmail(String email) {
        try {
            return jdbcTemplate.queryForObject("""
                            SELECT id, display_name, email, password_hash
                            FROM users WHERE email = ?
                            """,
                    (resultSet, rowNumber) -> new UserAccount(
                            UUID.fromString(resultSet.getString("id")),
                            resultSet.getString("display_name"),
                            resultSet.getString("email"),
                            resultSet.getString("password_hash")),
                    email);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        }
    }

    private AuthResponse createAuthResponse(UserAccount account) {
        AuthenticatedUser user = account.toAuthenticatedUser();
        return new AuthResponse(tokenService.createToken(user), user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be 72 bytes or fewer.");
        }
    }
}
