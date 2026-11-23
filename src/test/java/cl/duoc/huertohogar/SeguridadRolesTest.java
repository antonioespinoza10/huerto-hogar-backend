package cl.duoc.huertohogar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Permisos por rol en la API")
class SeguridadRolesTest extends BaseTest {

    private String productoJson() throws Exception {
        return json.writeValueAsString(Map.of("nombre", "Pala", "precio", 3000, "stock", 10, "categoriaId", herramientas.getId()));
    }

    @Test
    @DisplayName("El catálogo es público: productos y categorías se leen sin token")
    void catalogoPublico() throws Exception {
        mvc.perform(get("/api/v1/productos")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/v1/categorias")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/productos/" + tomate.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.categoria.nombre").value("Semillas"));
    }

    @Test
    @DisplayName("Crear productos: sin token 401, cliente 403, vendedor 403, admin 201")
    void crearProductoSoloAdmin() throws Exception {
        mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content(productoJson()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/productos").header("Authorization", bearer(cliente))
                        .contentType(MediaType.APPLICATION_JSON).content(productoJson()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/productos").header("Authorization", bearer(vendedor))
                        .contentType(MediaType.APPLICATION_JSON).content(productoJson()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/productos").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(productoJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @DisplayName("Órdenes: el vendedor y el admin las ven; el cliente no (403)")
    void verOrdenes() throws Exception {
        mvc.perform(get("/api/v1/boletas").header("Authorization", bearer(vendedor))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/boletas").header("Authorization", bearer(admin))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/boletas").header("Authorization", bearer(cliente))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Usuarios: solo el admin accede; el vendedor recibe 403")
    void usuariosSoloAdmin() throws Exception {
        mvc.perform(get("/api/v1/usuarios").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].password").doesNotExist());
        mvc.perform(get("/api/v1/usuarios").header("Authorization", bearer(vendedor))).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Un cliente puede comprar y ver sus compras, pero no cambiar el estado de una boleta")
    void clienteCompra() throws Exception {
        String compra = json.writeValueAsString(Map.of(
                "items", new Object[]{Map.of("productoId", tomate.getId(), "cantidad", 2)},
                "despacho", Map.of("nombre", "Pedro", "apellido", "Hacker", "correo", "pedro@example.com",
                        "calle", "Av. Siempre Viva 123", "region", "Metropolitana", "comuna", "Cerrillos")));
        String creada = mvc.perform(post("/api/v1/boletas").header("Authorization", bearer(cliente))
                        .contentType(MediaType.APPLICATION_JSON).content(compra))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(3980))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(creada).get("id").asLong();

        mvc.perform(get("/api/v1/boletas/mis-boletas").header("Authorization", bearer(cliente)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(put("/api/v1/boletas/" + id + "/estado").header("Authorization", bearer(cliente))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ENVIADA\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/boletas/" + id + "/estado").header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estado\":\"ENVIADA\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("ENVIADA"));
    }

    @Test
    @DisplayName("Comprar requiere sesión iniciada (401 sin token)")
    void comprarSinSesion() throws Exception {
        mvc.perform(post("/api/v1/boletas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
