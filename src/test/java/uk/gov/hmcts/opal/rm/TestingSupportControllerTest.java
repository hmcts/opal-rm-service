package uk.gov.hmcts.opal.rm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.opal.common.user.authorisation.client.service.UserStateClientService;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;

@SpringBootTest(properties = {
    "management.endpoint.health.group.readiness.include=readinessState",
    "management.health.redis.enabled=false",
    "opal.testing-support-endpoints.enabled=true",
    "launchdarkly.offline-mode=true",
    "spring.security.oauth2.client.registration.internal-azure-ad.client-id=test-client-id",
    "spring.security.oauth2.client.registration.internal-azure-ad.issuer-uri=https://issuer.example.com/",
    "spring.security.oauth2.client.provider.internal-azure-ad-provider.jwk-set-uri=https://issuer.example.com/keys"
})
@ImportAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    FlywayAutoConfiguration.class
})
@AutoConfigureMockMvc
class TestingSupportControllerTest {

    private static final String AUTH_CHECK_PATH = "/testing-support/auth-check";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserStateClientService userStateClientService;

    @Test
    void shouldRejectAuthCheckWithoutToken() throws Exception {
        mockMvc.perform(get(AUTH_CHECK_PATH))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnAuthSummaryForAuthenticatedCaller() throws Exception {
        UserState userState = UserState.builder()
            .userId(123L)
            .userName("someone@hmcts.net")
            .businessUnitUser(Set.of(
                BusinessUnitUser.builder()
                    .businessUnitUserId("buu-1")
                    .businessUnitId((short) 42)
                    .permissions(Collections.emptySet())
                    .build()
            ))
            .build();
        when(userStateClientService.getUserStateByAuthenticatedUser()).thenReturn(Optional.of(userState));

        MvcResult response = mockMvc.perform(
                get(AUTH_CHECK_PATH)
                    .with(jwt().jwt(jwt -> jwt
                        .subject("123")
                        .claim("preferred_username", "someone@hmcts.net")
                    )))
            .andExpect(status().isOk())
            .andReturn();

        String body = response.getResponse().getContentAsString();
        assertThat(body).contains(
            "\"principalName\":\"123\"",
            "\"authenticated\":true",
            "\"userStateFound\":true",
            "\"userId\":123",
            "\"userName\":\"someone@hmcts.net\"",
            "42"
        );
    }
}
