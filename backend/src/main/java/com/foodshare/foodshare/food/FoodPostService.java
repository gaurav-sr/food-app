package com.foodshare.foodshare.food;

import com.foodshare.foodshare.auth.AuthenticatedUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class FoodPostService {
    private final JdbcTemplate jdbcTemplate;

    public FoodPostService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<FoodPost> findAll() {
        return jdbcTemplate.query("""
                        SELECT id, food_name, description, servings, owner_id, owner_name, created_at
                        FROM food_posts
                        ORDER BY created_at DESC, id DESC
                        """,
                (resultSet, rowNumber) -> new FoodPost(
                        UUID.fromString(resultSet.getString("id")),
                        resultSet.getString("food_name"),
                        resultSet.getString("description"),
                        resultSet.getInt("servings"),
                        UUID.fromString(resultSet.getString("owner_id")),
                        resultSet.getString("owner_name"),
                        Instant.parse(resultSet.getString("created_at"))));
    }

    public FoodPost create(CreateFoodPostRequest request, AuthenticatedUser owner) {
        FoodPost post = new FoodPost(
                UUID.randomUUID(),
                request.foodName().trim(),
                request.description() == null ? "" : request.description().trim(),
                request.servings(),
                owner.id(),
                owner.displayName(),
                Instant.now()
        );
        jdbcTemplate.update("""
                        INSERT INTO food_posts (
                            id, food_name, description, servings, owner_id, owner_name, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                post.id().toString(),
                post.foodName(),
                post.description(),
                post.servings(),
                post.ownerId().toString(),
                post.ownerName(),
                post.createdAt().toString());
        return post;
    }
}
