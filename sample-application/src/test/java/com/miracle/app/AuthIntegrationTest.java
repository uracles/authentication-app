package com.miracle.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miracle.app.model.AuthDtos;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthIntegrationTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    private static String userToken;
    private static String adminToken;

    @Test
    @Order(1)
    @DisplayName("GET /api/public/health → 200 UP (no auth required)")
    void publicHealthIsAccessible() throws Exception {
        mockMvc.perform(get("/api/public/health"))
               .andExpect(status().isOk())
               .andExpect(content().string("UP"));
    }


    // Authentication
    @Test @Order(2)
    @DisplayName("POST /api/public/auth/login with valid admin credentials → 200 + JWT")
    void loginAdminReturnsToken() throws Exception {
        AuthDtos.LoginRequest req = new AuthDtos.LoginRequest();
        req.setUsername("admin");
        req.setPassword("AdminPass1!");

        MvcResult result = mockMvc.perform(post("/api/public/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyString())))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.username", is("admin")))
                .andExpect(jsonPath("$.roles", hasItem("ROLE_ADMIN")))
                .andReturn();

        adminToken = objectMapper.readTree(result.getResponse().getContentAsString())
                                 .get("accessToken").asText();
    }

    @Test @Order(3)
    @DisplayName("POST /api/public/auth/login with valid user credentials → 200 + JWT")
    void loginUserReturnsToken() throws Exception {
        AuthDtos.LoginRequest req = new AuthDtos.LoginRequest();
        req.setUsername("john");
        req.setPassword("UserPass1!");

        MvcResult result = mockMvc.perform(post("/api/public/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyString())))
                .andExpect(jsonPath("$.roles", hasItem("ROLE_USER")))
                .andExpect(jsonPath("$.roles", not(hasItem("ROLE_ADMIN"))))
                .andReturn();

        userToken = objectMapper.readTree(result.getResponse().getContentAsString())
                                .get("accessToken").asText();
    }

    @Test @Order(4)
    @DisplayName("POST /api/public/auth/login with wrong password → 401")
    void loginWithBadPasswordReturns401() throws Exception {
        AuthDtos.LoginRequest req = new AuthDtos.LoginRequest();
        req.setUsername("admin");
        req.setPassword("WrongPassword!");

        mockMvc.perform(post("/api/public/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isUnauthorized());
    }

    @Test @Order(5)
    @DisplayName("POST /api/public/auth/login with blank fields → 400 validation error")
    void loginWithBlankFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/public/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.status", is(400)));
    }

    // /api/user/me (authenticated)
    @Test @Order(6)
    @DisplayName("GET /api/user/me without token → 401")
    void getMeWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/user/me"))
               .andExpect(status().isUnauthorized())
               .andExpect(jsonPath("$.status", is(401)))
               .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test @Order(7)
    @DisplayName("GET /api/user/me with valid user token → 200 + profile")
    void getMeWithValidTokenReturns200() throws Exception {
        mockMvc.perform(get("/api/user/me")
                        .header("Authorization", "Bearer " + userToken))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.username", is("john")))
               .andExpect(jsonPath("$.roles", hasItem("ROLE_USER")));
    }

    @Test @Order(8)
    @DisplayName("GET /api/user/me with malformed token → 401")
    void getMeWithMalformedTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/user/me")
                        .header("Authorization", "Bearer not.a.real.jwt"))
               .andExpect(status().isUnauthorized());
    }

    // /api/admin/users  (ROLE_ADMIN)
    @Test @Order(9)
    @DisplayName("GET /api/admin/users with ROLE_USER token → 403")
    void adminEndpointWithUserTokenReturns403() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + userToken))
               .andExpect(status().isForbidden())
               .andExpect(jsonPath("$.status", is(403)))
               .andExpect(jsonPath("$.error", is("Forbidden")));
    }

    @Test @Order(10)
    @DisplayName("GET /api/admin/users with ROLE_ADMIN token → 200 + user list")
    void adminEndpointWithAdminTokenReturns200() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
               .andExpect(jsonPath("$[*].username", hasItem("admin")));
    }

    @Test @Order(11)
    @DisplayName("GET /api/admin/users without token → 401")
    void adminEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
               .andExpect(status().isUnauthorized());
    }

    // Registration flow
    @Test @Order(12)
    @DisplayName("POST /api/public/auth/register new user → 200 + JWT with ROLE_USER")
    void registerNewUserReturnsToken() throws Exception {
        AuthDtos.RegisterRequest req = new AuthDtos.RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("NewPass123!");

        mockMvc.perform(post("/api/public/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.accessToken", not(emptyString())))
               .andExpect(jsonPath("$.username", is("newuser")))
               .andExpect(jsonPath("$.roles", hasItem("ROLE_USER")));
    }

    @Test @Order(13)
    @DisplayName("POST /api/public/auth/register duplicate username → 409")
    void registerDuplicateUsernameReturns409() throws Exception {
        AuthDtos.RegisterRequest req = new AuthDtos.RegisterRequest();
        req.setUsername("john");   // already seeded
        req.setPassword("SomePass1!");

        mockMvc.perform(post("/api/public/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
               .andExpect(status().isConflict())
               .andExpect(jsonPath("$.status", is(409)));
    }
}
