package uk.gov.hmcts.opal.rm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthSmokeTest {

    @Test
    void healthEndpointReturnsUp() throws IOException, InterruptedException {
        String baseUrl = System.getenv("TEST_URL");
        Assumptions.assumeTrue(baseUrl != null && !baseUrl.isBlank(), "TEST_URL must be set for smoke tests");

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(withoutTrailingSlash(baseUrl) + "/health"))
            .GET()
            .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"UP\""));
    }

    private static String withoutTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
