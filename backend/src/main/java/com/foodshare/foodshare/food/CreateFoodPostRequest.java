package com.foodshare.foodshare.food;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFoodPostRequest(
        @NotBlank @Size(max = 100) String foodName,
        @Size(max = 500) String description,
        @Min(1) @Max(1000) int servings
) {
}
