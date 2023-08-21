package se.mkk.springboot3oauth2loginkeycloakangular;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HeaderWriterLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter;
import org.springframework.security.web.header.writers.ClearSiteDataHeaderWriter.Directive;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.nimbusds.jose.util.JSONObjectUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class OAuth2LoginSecurityConfig {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http //
                .sessionManagement(
                        // https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html
                        // https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#appendix.application-properties.server
                        Customizer.withDefaults())
                .authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests //
                        .requestMatchers("/login", "/logout").permitAll() //
//                        .requestMatchers("/api/users/roles").hasRole("USER") //
                        .anyRequest().authenticated()) //
                .oauth2Login(oauth2 -> oauth2 //
                        .userInfoEndpoint(userInfo -> userInfo //
                                .oidcUserService(this.oidcUserService())))
                .csrf(csrf -> csrf //
                        // https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-token-repository-cookie
                        .ignoringRequestMatchers("/logout", "/api"))
                .logout(logout -> logout //
                        .addLogoutHandler(new KeycloakLogoutHandler(restTemplateBuilder.build())) //
                        // https://docs.spring.io/spring-security/reference/servlet/authentication/logout.html#clear-all-site-data
                        .addLogoutHandler(new HeaderWriterLogoutHandler(new ClearSiteDataHeaderWriter(Directive.ALL))));
        return http.build();
    }

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    // OpenID Connect 1.0 Logout Does Not work for Angular app, since redirect will violate CORS (Reason: CORS header
    // ‘Access-Control-Allow-Origin’ missing) and OidcClientInitiatedLogoutSuccessHandler ignores Spring Security CORS
    // https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html#oauth2login-advanced-oidc-logout
    // https://github.com/simasch/vaadin-keycloak/blob/main/src/main/java/ch/martinelli/demo/keycloak/security/KeycloakLogoutHandler.java
    public class KeycloakLogoutHandler implements LogoutHandler {
        private final RestTemplate restTemplate;

        public KeycloakLogoutHandler(RestTemplate restTemplate) {
            this.restTemplate = restTemplate;
        }

        @Override
        public void logout(HttpServletRequest request, HttpServletResponse response, Authentication auth) {
            logoutFromKeycloak((OidcUser) auth.getPrincipal());
        }

        private void logoutFromKeycloak(OidcUser user) {
            // https://access.redhat.com/documentation/en-us/red_hat_single_sign-on/7.6/html-single/securing_applications_and_services_guide/index#logout
            String endSessionEndpoint = user.getIssuer() + "/protocol/openid-connect/logout";
            UriComponentsBuilder builder = UriComponentsBuilder //
                    .fromUriString(endSessionEndpoint) //
                    .queryParam("id_token_hint", user.getIdToken().getTokenValue());

            ResponseEntity<String> logoutResponse = restTemplate.getForEntity(builder.toUriString(), String.class);
            if (logoutResponse.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully logged out from Keycloak");
            } else {
                log.error("Could not propagate logout to Keycloak");
            }
        }
    }

    // https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html#oauth2login-advanced-map-authorities-oauth2userservice
    private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {
            // Delegate to the default implementation for loading a user
            OidcUser oidcUser = delegate.loadUser(userRequest);

            OAuth2AccessToken accessToken = userRequest.getAccessToken();
            Collection<GrantedAuthority> mappedAuthorities = new HashSet<>();

            // 1) Fetch the authority information from the protected resource using accessToken
            // 2) Map the authority information to one or more GrantedAuthority's and add it to mappedAuthorities
            try {
                String[] chunks = accessToken.getTokenValue().split("\\.");
                Base64.Decoder decoder = Base64.getUrlDecoder();
                String header = new String(decoder.decode(chunks[0]));
                String payload = new String(decoder.decode(chunks[1]));

                Map<String, Object> claims = JSONObjectUtils.parse(payload);
                mappedAuthorities = new KeycloakAuthoritiesConverter().convert(claims);
            } catch (Exception e) {
                log.error("Failed to map Authorities", e);
            }

            // 3) Create a copy of oidcUser but use the mappedAuthorities instead
            oidcUser = new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo(),
                    "preferred_username");

            return oidcUser;
        };
    }

    // Spring OAuth2 uses default Scopes Not Roles for Authorization
    // org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
    public class KeycloakAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            return convert(jwt.getClaims());
        }

        public Collection<GrantedAuthority> convert(Map<String, Object> claims) {
            Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
            for (String authority : getAuthorities(claims)) {
                grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + authority));
            }
            return grantedAuthorities;
        }

        private Collection<String> getAuthorities(Map<String, Object> claims) {
            Object realm_access = claims.get("realm_access");
            log.info("Retrieved realm_access {}", realm_access);
            if (realm_access instanceof Map) {
                Map<String, Object> map = castAuthoritiesToMap(realm_access);
                Object roles = map.get("roles");
                if (roles instanceof Collection) {
                    return castAuthoritiesToCollection(roles);
                }
            }
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> castAuthoritiesToMap(Object authorities) {
            return (Map<String, Object>) authorities;
        }

        @SuppressWarnings("unchecked")
        private Collection<String> castAuthoritiesToCollection(Object authorities) {
            return (Collection<String>) authorities;
        }
    }
}
