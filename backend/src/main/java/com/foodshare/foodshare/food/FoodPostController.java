package com.foodshare.foodshare.food;

import com.foodshare.foodshare.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/food-posts")
public class FoodPostController {
    private final FoodPostService foodPostService;

    public FoodPostController(FoodPostService foodPostService) {
        this.foodPostService = foodPostService;
    }

    @GetMapping
    public List<FoodPost> findAll() {
        return foodPostService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FoodPost create(
            @Valid @RequestBody CreateFoodPostRequest request,
            @AuthenticationPrincipal AuthenticatedUser owner
    ) {
        return foodPostService.create(request, owner);
    }
}
