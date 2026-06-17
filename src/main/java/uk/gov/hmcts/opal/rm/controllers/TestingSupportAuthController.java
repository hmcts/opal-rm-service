package uk.gov.hmcts.opal.rm.controllers;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.opal.common.config.OpalCommonConfiguration;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.client.service.UserStateClientService;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;

@RestController
@RequestMapping("/testing-support/auth")
@ConditionalOnProperty(prefix = "opal.testing-support-endpoints", name = "enabled", havingValue = "true")
public class TestingSupportAuthController {

    private final UserStateClientService userStateClientService;
    private final Domain domain;

    public TestingSupportAuthController(
        UserStateClientService userStateClientService,
        OpalCommonConfiguration commonConfiguration) {
        this.userStateClientService = userStateClientService;
        this.domain = Domain.findByDisplayName(commonConfiguration.getDomain());
    }

    @GetMapping("/check")
    public ResponseEntity<AuthCheckResponse> authCheck(Authentication authentication) {
        if (authentication instanceof OpalJwtAuthenticationToken opalAuthentication) {
            return ResponseEntity.ok(AuthCheckResponse.from(opalAuthentication, domain));
        }

        return ResponseEntity.ok(
            userStateClientService.getUserStateByAuthenticatedUser()
                .map(userState -> AuthCheckResponse.from(authentication, userState))
                .orElseGet(() -> AuthCheckResponse.withoutUserState(authentication))
        );
    }

    public record AuthCheckResponse(
        String principalName,
        boolean authenticated,
        boolean userStateFound,
        Long userId,
        String userName,
        Set<Short> businessUnitIds
    ) {
        static AuthCheckResponse from(OpalJwtAuthenticationToken authentication, Domain domain) {
            return new AuthCheckResponse(
                authentication.getName(),
                authentication.isAuthenticated(),
                true,
                authentication.getUserId(),
                authentication.getUsername(),
                authentication.getUserState().getDomainBusinessUnitUsers(domain).getBusinessUnitUsers().stream()
                    .map(uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser::getBusinessUnitId)
                    .collect(Collectors.toSet())
            );
        }

        static AuthCheckResponse from(Authentication authentication, UserState userState) {
            return new AuthCheckResponse(
                authentication.getName(),
                authentication.isAuthenticated(),
                true,
                userState.getUserId(),
                userState.getUserName(),
                userState.getBusinessUnitUser().stream()
                    .map(uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser::getBusinessUnitId)
                    .collect(Collectors.toSet())
            );
        }

        static AuthCheckResponse withoutUserState(Authentication authentication) {
            return new AuthCheckResponse(
                authentication.getName(),
                authentication.isAuthenticated(),
                false,
                null,
                null,
                Set.of()
            );
        }
    }
}
