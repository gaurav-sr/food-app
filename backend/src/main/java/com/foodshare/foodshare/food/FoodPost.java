package com.foodshare.foodshare.food;

import java.time.Instant;
import java.util.UUID;

public record FoodPost(
        UUID id,
        String foodName,
        String description,
        int servings,
        UUID ownerId,
        String ownerName,
        Instant createdAt
) {
}
