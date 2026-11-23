package cl.duoc.huertohogar.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import cl.duoc.huertohogar.dto.ErrorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Reglas de acceso de la API. La sesión es "stateless": el servidor no guarda sesiones,
 * cada solicitud se autentica con su token JWT.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String API = "/api/v1";

    private final JwtAuthFilter jwtAuthFilter;
    private final String[] origenesPermitidos;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, @Value("${app.cors.origenes}") String origenes) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.origenesPermitidos = origenes.split(",");
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Documentación Swagger
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Autenticación pública (login, registro, renovar token)
                .requestMatchers(HttpMethod.POST, API + "/auth/login", API + "/auth/registro", API + "/auth/refresh").permitAll()
                // Catálogo público: cualquiera puede ver productos y categorías
                .requestMatchers(HttpMethod.GET, API + "/productos/**", API + "/categorias/**").permitAll()
                // Mantenedores: solo administrador
                .requestMatchers(API + "/productos/**", API + "/categorias/**").hasRole("ADMIN")
                .requestMatchers(API + "/usuarios/**").hasRole("ADMIN")
                // Órdenes
                .requestMatchers(HttpMethod.POST, API + "/boletas").authenticated()
                .requestMatchers(HttpMethod.GET, API + "/boletas/mis-boletas").authenticated()
                .requestMatchers(HttpMethod.GET, API + "/boletas", API + "/boletas/*").hasAnyRole("ADMIN", "VENDEDOR")
                .requestMatchers(API + "/boletas/**").hasRole("ADMIN")
                // Cualquier otra ruta requiere sesión
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    Object detalle = req.getAttribute("jwt_error");
                    escribirError(res, 401, "Unauthorized", detalle != null ? detalle.toString() : "Debes iniciar sesión");
                })
                .accessDeniedHandler((req, res, e) ->
                    escribirError(res, 403, "Forbidden", "No tienes permisos para esta acción")))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void escribirError(jakarta.servlet.http.HttpServletResponse res, int estado, String error, String mensaje)
            throws java.io.IOException {
        res.setStatus(estado);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        json.writeValue(res.getOutputStream(), ErrorResponse.de(estado, error, mensaje));
    }

    /** Permite que el frontend React (http://localhost:5173) llame a la API desde el navegador. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origenesPermitidos).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Las contraseñas se guardan cifradas con BCrypt. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
