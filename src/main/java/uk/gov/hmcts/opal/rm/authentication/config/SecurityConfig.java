package uk.gov.hmcts.opal.rm.authentication.config;

import static org.springframework.security.config.Customizer.withDefaults;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import uk.gov.hmcts.opal.common.user.authentication.exception.CustomAuthenticationExceptions;
import uk.gov.hmcts.opal.rm.authentication.config.internal.InternalAuthConfigurationProperties;
import uk.gov.hmcts.opal.rm.authentication.config.internal.InternalAuthProviderConfigurationProperties;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] AUTH_WHITELIST = {
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/swagger-resources/**",
        "/v3/**",
        "/favicon.ico",
        "/health/**",
        "/info",
        "/"
    };

    private final CustomAuthenticationExceptions customAuthenticationExceptions;

    @Bean
    @SuppressWarnings("squid:S4502")
    public SecurityFilterChain filterChain(HttpSecurity http) {
        try {
            applyCommonConfig(http)
                .authorizeHttpRequests(authorize ->
                    authorize.requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        .requestMatchers("/testing-support/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling ->
                    exceptionHandling
                        .authenticationEntryPoint(customAuthenticationExceptions)
                        .accessDeniedHandler(customAuthenticationExceptions)
                )
                .oauth2ResourceServer(oauth2 ->
                    oauth2
                        .authenticationEntryPoint(customAuthenticationExceptions)
                        .jwt(withDefaults())
                );

            return http.build();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to build Spring Security filter chain", exception);
        }
    }

    @Bean
    JwtDecoder internalJwtDecoder(
        InternalAuthProviderConfigurationProperties providerProps,
        InternalAuthConfigurationProperties authProps) {

        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(providerProps.getJwkSetUri())
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .build();

        jwtDecoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(authProps.getIssuerUri()));
        return jwtDecoder;
    }

    private HttpSecurity applyCommonConfig(HttpSecurity http) {
        return http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(FormLoginConfigurer::disable)
            .logout(LogoutConfigurer::disable);
    }
}
