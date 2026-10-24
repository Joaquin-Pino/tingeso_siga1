package cl.joaquin.siga.Config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Documentación de la API en /swagger-ui.html (JSON en /v3/api-docs). Los endpoints exigen el JWT de Keycloak:
// el botón "Authorize" de Swagger UI recibe el access token y lo envía como Bearer.
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "keycloak-jwt";

    @Bean
    public OpenAPI sigaOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SIGA IPT API")
                        .description("REST API of the Integrated Academic Management System (SIGA IPT). "
                                + "Secured with Keycloak JWT tokens; roles: ADMIN, TEACHER, STUDENT.")
                        .version("1.0"))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
