package uk.gov.hmcts.opal.rm.controllers;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.opal.common.user.authorisation.client.service.UserStateClientService;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserState;

@RestController
@RequestMapping("/testing-support")
@ConditionalOnProperty(prefix = "opal.testing-support-endpoints", name = "enabled", havingValue = "true")
public class TestingSupportController {

    private final UserStateClientService userStateClientService;

    public TestingSupportController(UserStateClientService userStateClientService) {
        this.userStateClientService = userStateClientService;
    }

    @GetMapping("/auth-check")
    public ResponseEntity<AuthCheckResponse> authCheck(Authentication authentication) {
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
