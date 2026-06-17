package uk.gov.hmcts.opal.rm;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "management.endpoint.health.group.readiness.include=readinessState",
    "management.health.redis.enabled=false",
    "opal.testing-support-endpoints.enabled=true",
    "launchdarkly.offline-mode=true",
    "spring.security.oauth2.client.registration.internal-azure-ad.client-id=test-client-id"
})
@ImportAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    FlywayAutoConfiguration.class
})
@AutoConfigureMockMvc
@SuppressWarnings("java:S1075")
class TestingSupportControllerIntegrationTest {

    private static final String PING_PATH = "/testing-support/ping";
    private static final String AUTH_CHECK_PATH = "/testing-support/auth/check";
    private static final String AUTH_HEADER_PREFIX = "Bearer ";
    private static final String ISSUER_URI = "/issuer";
    private static final String JWK_SET_PATH = "/oauth2/jwks.json";
    private static final String USER_STATE_PATH = "/v2/users/0/state";
    private static final String CLIENT_ID = "test-client-id";
    private static final String PREFERRED_USERNAME = "opal-test@hmcts.net";
    private static final String TOKEN_SUBJECT = "user-123";
    private static final WireMockServer WIRE_MOCK_SERVER = startWireMock();
    private static final RSAKey RSA_KEY = generateRsaKey();

    private static final String USER_STATE_JSON = """
        {
          "user_id": 123,
          "username": "opal-test@hmcts.net",
          "name": "Opal Test",
          "status": "ACTIVE",
          "version": 1,
          "domains": {
            "maintenance": {
              "business_unit_users": [
                {
                  "business_unit_user_id": "BUU-42",
                  "business_unit_id": 42,
                  "permissions": [
                    {
                      "permission_id": 7,
                      "permission_name": "RM Test Permission"
                    }
                  ]
                }
              ]
            }
          }
        }
        """;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private ValueOperations<String, String> valueOperations;

    private final MockMvc mockMvc;

    @Autowired
    TestingSupportControllerIntegrationTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add(
            "spring.security.oauth2.client.registration.internal-azure-ad.issuer-uri",
            () -> WIRE_MOCK_SERVER.baseUrl() + ISSUER_URI
        );
        registry.add(
            "spring.security.oauth2.client.provider.internal-azure-ad-provider.jwk-set-uri",
            () -> WIRE_MOCK_SERVER.baseUrl() + JWK_SET_PATH
        );
        registry.add(
            "spring.security.oauth2.client.provider.internal-azure-ad-provider.authorization-uri",
            () -> WIRE_MOCK_SERVER.baseUrl() + "/oauth2/authorize"
        );
        registry.add(
            "spring.security.oauth2.client.provider.internal-azure-ad-provider.token-uri",
            () -> WIRE_MOCK_SERVER.baseUrl() + "/oauth2/token"
        );
        registry.add("user.service.url", WIRE_MOCK_SERVER::baseUrl);
    }

    @BeforeEach
    void setUp() {
        WIRE_MOCK_SERVER.resetAll();
        stubJwkEndpoint();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
    }

    @AfterAll
    static void tearDown() {
        WIRE_MOCK_SERVER.stop();
    }

    @Test
    void shouldAllowPingWithoutToken() throws Exception {
        mockMvc.perform(get(PING_PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void shouldReturnAuthenticatedSummaryAndRelayBearerToken() throws Exception {
        String token = signedToken(Instant.now().plusSeconds(300));

        WIRE_MOCK_SERVER.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo(USER_STATE_PATH))
            .willReturn(okJson(USER_STATE_JSON)));

        mockMvc.perform(get(AUTH_CHECK_PATH)
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_PREFIX + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.principalName").value(TOKEN_SUBJECT))
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.userStateFound").value(true))
            .andExpect(jsonPath("$.userId").value(123))
            .andExpect(jsonPath("$.userName").value(PREFERRED_USERNAME))
            .andExpect(jsonPath("$.businessUnitIds[0]").value(42));

        WIRE_MOCK_SERVER.verify(getRequestedFor(urlEqualTo(USER_STATE_PATH))
            .withHeader(HttpHeaders.AUTHORIZATION, equalTo(AUTH_HEADER_PREFIX + token)));
    }

    @Test
    void shouldRejectTokenWhenUserServiceReturnsNotFound() throws Exception {
        String token = signedToken(Instant.now().plusSeconds(300));

        WIRE_MOCK_SERVER.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo(USER_STATE_PATH))
            .willReturn(aResponse().withStatus(404)));

        mockMvc.perform(get(AUTH_CHECK_PATH)
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_PREFIX + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectExpiredToken() throws Exception {
        String token = signedToken(Instant.now().minusSeconds(60));

        mockMvc.perform(get(AUTH_CHECK_PATH)
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_PREFIX + token))
            .andExpect(status().isUnauthorized());
    }

    private static void stubJwkEndpoint() {
        String jwkResponse = "{\"keys\": [%s]}".formatted(RSA_KEY.toPublicJWK().toJSONString());
        WIRE_MOCK_SERVER.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo(JWK_SET_PATH))
            .willReturn(okJson(jwkResponse)));
    }

    private static String signedToken(Instant expiry) throws JOSEException {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .issuer(WIRE_MOCK_SERVER.baseUrl() + ISSUER_URI)
            .subject(TOKEN_SUBJECT)
            .audience(CLIENT_ID)
            .claim("preferred_username", PREFERRED_USERNAME)
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(expiry))
            .build();

        SignedJWT signedJwt = new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(RSA_KEY.getKeyID())
                .build(),
            claimsSet
        );
        signedJwt.sign(new RSASSASigner(RSA_KEY.toPrivateKey()));
        return signedJwt.serialize();
    }

    private static WireMockServer startWireMock() {
        WireMockServer server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();
        return server;
    }

    private static RSAKey generateRsaKey() {
        try {
            return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(new Algorithm("RS256"))
                .keyID("rm-security-it-key")
                .generate();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to generate RSA key for integration tests", e);
        }
    }
}
