package cl.duoc.huertohogar;

import cl.duoc.huertohogar.dto.BoletaRequest;
import cl.duoc.huertohogar.dto.BoletaResponse;
import cl.duoc.huertohogar.dto.CategoriaRequest;
import cl.duoc.huertohogar.dto.ProductoRequest;
import cl.duoc.huertohogar.dto.UsuarioRequest;
import cl.duoc.huertohogar.exception.RecursoNoEncontradoException;
import cl.duoc.huertohogar.exception.ReglaNegocioException;
import cl.duoc.huertohogar.model.EstadoBoleta;
import cl.duoc.huertohogar.model.Rol;
import cl.duoc.huertohogar.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Lógica de negocio de los servicios")
class ReglasNegocioTest extends BaseTest {

    @Autowired BoletaService boletaService;
    @Autowired ProductoService productoService;
    @Autowired CategoriaService categoriaService;
    @Autowired UsuarioService usuarioService;

    private BoletaRequest compra(BoletaRequest.Item... items) {
        return new BoletaRequest(List.of(items), new BoletaRequest.Despacho("Pedro", "Hacker", "pedro@example.com",
                "Av. Siempre Viva 123", null, "Metropolitana", "Cerrillos", null));
    }

    private int stock(Long id) {
        return productoRepository.findById(id).orElseThrow().getStock();
    }

    @Test
    @DisplayName("Una compra usa el precio de oferta, calcula el total en el servidor y descuenta el stock")
    void compraDescuentaStock() {
        BoletaResponse b = boletaService.crear(compra(
                new BoletaRequest.Item(tomate.getId(), 3),
                new BoletaRequest.Item(humus.getId(), 2)), "pedro@example.com");

        assertThat(b.total()).isEqualTo(3 * 1990 + 2 * 6200);
        assertThat(b.detalles()).hasSize(2);
        assertThat(b.usuarioId()).isEqualTo(cliente.getId());
        assertThat(stock(tomate.getId())).isEqualTo(42);
        assertThat(stock(humus.getId())).isZero();
    }

    @Test
    @DisplayName("Sin stock suficiente la compra se rechaza y no se descuenta nada (transacción completa)")
    void stockInsuficiente() {
        assertThatThrownBy(() -> boletaService.crear(compra(
                new BoletaRequest.Item(tomate.getId(), 1),
                new BoletaRequest.Item(humus.getId(), 3)), "pedro@example.com"))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Stock insuficiente para Humus de Lombriz");
        assertThat(stock(tomate.getId())).isEqualTo(45);
        assertThat(boletaRepository.count()).isZero();
    }

    @Test
    @DisplayName("Anular una boleta devuelve el stock y no se puede anular dos veces")
    void anularDevuelveStock() {
        BoletaResponse b = boletaService.crear(compra(new BoletaRequest.Item(tomate.getId(), 5)), "pedro@example.com");
        assertThat(stock(tomate.getId())).isEqualTo(40);
        boletaService.cambiarEstado(b.id(), EstadoBoleta.ANULADA);
        assertThat(stock(tomate.getId())).isEqualTo(45);
        assertThatThrownBy(() -> boletaService.cambiarEstado(b.id(), EstadoBoleta.ENVIADA))
                .isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("No se elimina una categoría con productos ni se repiten nombres")
    void reglasCategoria() {
        assertThatThrownBy(() -> categoriaService.eliminar(semillas.getId()))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("2 producto(s)");
        assertThatThrownBy(() -> categoriaService.crear(new CategoriaRequest("semillas")))
                .isInstanceOf(ReglaNegocioException.class);
        categoriaService.eliminar(herramientas.getId());
        assertThat(categoriaRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("El precio de oferta debe ser menor al normal y no se borra un producto con ventas")
    void reglasProducto() {
        assertThatThrownBy(() -> productoService.crear(new ProductoRequest("Pala", "", 3000, 5, true, 3500, null, herramientas.getId())))
                .isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> productoService.crear(new ProductoRequest("Pala", "", 3000, 5, false, null, null, 999L)))
                .isInstanceOf(RecursoNoEncontradoException.class);

        boletaService.crear(compra(new BoletaRequest.Item(tomate.getId(), 1)), "pedro@example.com");
        assertThatThrownBy(() -> productoService.eliminar(tomate.getId()))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("ventas registradas");
        assertThat(productoService.stockCritico()).extracting("nombre").containsExactly("Humus de Lombriz");
    }

    @Test
    @DisplayName("Un administrador no puede eliminarse a sí mismo; sí puede crear vendedores")
    void reglasUsuario() {
        assertThatThrownBy(() -> usuarioService.eliminar(admin.getId(), "admin@huertohogar.cl"))
                .isInstanceOf(ReglaNegocioException.class);
        var nuevo = usuarioService.crear(new UsuarioRequest("Luis", "Pérez", "luis@huertohogar.cl", "clave123", Rol.VENDEDOR));
        assertThat(nuevo.rol()).isEqualTo(Rol.VENDEDOR);
        assertThatThrownBy(() -> usuarioService.crear(new UsuarioRequest("Otro", "X", "luis@huertohogar.cl", "clave123", Rol.CLIENTE)))
                .isInstanceOf(ReglaNegocioException.class);
        usuarioService.eliminar(cliente.getId(), "admin@huertohogar.cl");
        assertThat(usuarioRepository.findById(cliente.getId())).isEmpty();
    }
}
