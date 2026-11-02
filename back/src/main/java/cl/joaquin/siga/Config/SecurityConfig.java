package cl.joaquin.siga.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

// Resource server de Keycloak: el rol sale de realm_access.roles del JWT (ADMIN/TEACHER/STUDENT).
// Aquí vive la matriz de roles por endpoint; que un STUDENT/TEACHER solo toque sus propios datos
// lo verifica @PreAuthorize en los controllers (CurrentUserService).
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // API stateless sin cookies de sesión
                .cors(Customizer.withDefaults()) // usa el bean CorsConfigurationSource
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // documentación de la API (Swagger UI): pública; sus endpoints siguen protegidos
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // el usuario autenticado
                        .requestMatchers(HttpMethod.GET, "/api/students/me").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.GET, "/api/teachers/me").hasRole("TEACHER")

                        // estudiante: sus datos, historial e inscripciones (ADMIN solo consulta)
                        .requestMatchers(HttpMethod.GET, "/api/students/{id}", "/api/students/{id}/academic-history",
                                "/api/students/{id}/academic-average", "/api/students/{id}/registrations")
                        .hasAnyRole("ADMIN", "STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/students/{id}/registrations").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.DELETE, "/api/students/{id}/registrations/{registrationId}")
                        .hasRole("STUDENT")

                        // docente: sus secciones y las notas de sus inscritos
                        .requestMatchers(HttpMethod.GET, "/api/teachers/{id}/sections", "/api/teachers/{id}/teaching-history")
                        .hasAnyRole("ADMIN", "TEACHER")
                        .requestMatchers("/api/teachers/{id}/sections/{sectionId}/registrations/**").hasRole("TEACHER")

                        // consulta de catálogo y oferta para cualquier usuario autenticado
                        .requestMatchers(HttpMethod.GET, "/api/academic-periods", "/api/academic-periods/{id}",
                                "/api/academic-periods/{id}/sections", "/api/sections/{id}", "/api/careers",
                                "/api/careers/{id}", "/api/careers/{id}/study-plans",
                                "/api/study-plans/{id}/subjects", "/api/subjects/{id}")
                        .authenticated()

                        // todo lo demás es gestión
                        .requestMatchers("/api/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            Object realmAccess = jwt.getClaims().get("realm_access");
            if (realmAccess instanceof Map<?, ?> realmAccessMap && realmAccessMap.get("roles") instanceof List<?> roles) {
                for (Object role : roles) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
            }
            return authorities;
        });
        return converter;
    }

    // orígenes permitidos del frontend, separados por coma (env CORS_ALLOWED_ORIGINS)
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${siga.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
