package cl.duoc.huertohogar;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Autenticación con JWT")
class AuthControllerTest extends BaseTest {

    private String cuerpo(Object o) throws Exception {
        return json.writeValueAsString(o);
    }

    @Test
    @DisplayName("Login correcto entrega token de acceso, refresh token y el rol del usuario")
    void loginCorrecto() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(Map.of("correo", "ADMIN@huertohogar.cl", "password", "admin123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.usuario.rol").value("ADMIN"))
                .andExpect(jsonPath("$.usuario.password").doesNotExist());
    }

    @Test
    @DisplayName("Login con contraseña incorrecta responde 401")
    void loginIncorrecto() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(Map.of("correo", "admin@huertohogar.cl", "password", "mala"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"));
    }

    @Test
    @DisplayName("El registro crea una cuenta CLIENTE con la contraseña cifrada")
    void registroCreaCliente() throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(Map.of("nombre", "Ana", "apellido", "Rojas", "correo", "Ana@Duoc.cl", "password", "secreto1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.usuario.correo").value("ana@duoc.cl"));
        String guardada = usuarioRepository.findByCorreoIgnoreCase("ana@duoc.cl").orElseThrow().getPassword();
        assertThat(guardada).isNotEqualTo("secreto1").startsWith("$2");
    }

    @Test
    @DisplayName("El registro rechaza correos repetidos (409) y datos inválidos (400)")
    void registroInvalido() throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(Map.of("nombre", "X", "apellido", "Y", "correo", "admin@huertohogar.cl", "password", "secreto1"))))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(Map.of("nombre", "", "apellido", "Y", "correo", "no-es-correo", "password", "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalles").isArray());
    }

    @Test
    @DisplayName("El refresh token entrega un token nuevo; un token de acceso no sirve como refresh")
    void renovarToken() throws Exception {
        String respuesta = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(Map.of("correo", "pedro@example.com", "password", "pedro123"))))
                .andReturn().getResponse().getContentAsString();
        JsonNode login = json.readTree(respuesta);

        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(Map.of("refreshToken", login.get("refreshToken").asText()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(Map.of("refreshToken", login.get("token").asText()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/auth/me devuelve el usuario del token; sin token o con token alterado responde 401")
    void usuarioActual() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(vendedor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("vendedor@huertohogar.cl"));
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        // Token alterado: se cambia el contenido (payload) sin volver a firmarlo
        String[] partes = bearer(vendedor).substring(7).split("\\.");
        String payloadFalso = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"admin@huertohogar.cl\",\"rol\":\"ADMIN\",\"tipo\":\"access\"}".getBytes());
        String alterado = partes[0] + "." + payloadFalso + "." + partes[2];
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + alterado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Token inválido"));
    }
}
