package cl.duoc.huertohogar.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuración de Swagger: título de la API y botón "Authorize" para probar con el token JWT. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI huertoHogarOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("HuertoHogar API")
                        .version("v1")
                        .description("API REST de la tienda HuertoHogar: productos, categorías, usuarios y boletas. "
                                + "Para los endpoints protegidos, inicia sesión en /api/v1/auth/login, copia el token "
                                + "y pégalo en el botón Authorize.")
                        .contact(new Contact().name("Equipo HuertoHogar - DSY1104 Sección 003D")))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
