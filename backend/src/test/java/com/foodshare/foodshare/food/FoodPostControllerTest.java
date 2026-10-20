package com.foodshare.foodshare.food;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodshare.foodshare.auth.AuthRequest;
import com.foodshare.foodshare.auth.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:foodshare-test?mode=memory&cache=shared"
})
class FoodPostControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearFoodPosts() {
        jdbcTemplate.update("DELETE FROM food_posts");
    }

    @Test
    void registrationLoginAndFoodPostRequireValidAuthentication() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        String registerBody = objectMapper.writeValueAsString(
                new RegisterRequest("Jamie", email, "sharedfood123"));

        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.displayName").value("Jamie"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn();

        String registrationToken = objectMapper.readTree(
                registration.getResponse().getContentAsString()).get("token").asText();
        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE email = ?", String.class, email);
        assertTrue(storedHash.startsWith("$2"));
        assertNotEquals("sharedfood123", storedHash);

        mockMvc.perform(post("/api/food-posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"foodName":"Vegetable curry","description":"Mild and filling","servings":4}
                                """))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isConflict());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest(email, "sharedfood123"))))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(login.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(post("/api/food-posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"foodName":"Vegetable curry","description":"Mild and filling","servings":4}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.foodName").value("Vegetable curry"))
                .andExpect(jsonPath("$.servings").value(4))
                .andExpect(jsonPath("$.ownerName").value("Jamie"))
                .andExpect(jsonPath("$.ownerId").isNotEmpty())
                .andExpect(jsonPath("$.id").isNotEmpty());

        mockMvc.perform(get("/api/food-posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + registrationToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + registrationToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidRegistrationAndFoodPostInput() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName":"Jamie","email":"not-an-email","password":"short"}
                                """))
                .andExpect(status().isBadRequest());

        String email = UUID.randomUUID() + "@example.com";
        MvcResult registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest("Alex", email, "sharedfood123"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(registration.getResponse().getContentAsString());
        String token = response.get("token").asText();

        mockMvc.perform(post("/api/food-posts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"foodName":"  ","description":"","servings":0}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthRequest(email, "wrongpassword"))))
                .andExpect(status().isUnauthorized());
    }
}
