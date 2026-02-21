package com.snayvik.kpi.security;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    void apiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/kpi/system/ping").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonAdminUserCanAccessStandardApi() throws Exception {
        mockMvc.perform(get("/api/kpi/system/ping")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", basicAuth("test-user", "test-user-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void nonAdminUserCannotAccessAdminApi() throws Exception {
        mockMvc.perform(get("/api/kpi/admin/not-mapped")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", basicAuth("test-user", "test-user-password")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUserCanAccessAdminApi() throws Exception {
        mockMvc.perform(get("/api/kpi/admin/not-mapped")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", basicAuth("test-admin", "test-admin-password")))
                .andExpect(status().isNotFound());
    }

    @Test
    void webhookEndpointIsPublicForExternalCallbacks() throws Exception {
        mockMvc.perform(post("/webhooks/monday")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"challenge\":\"abc123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.challenge").value("abc123"));
    }

    private String basicAuth(String username, String password) {
        String raw = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
