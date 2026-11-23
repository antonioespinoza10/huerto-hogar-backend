package cl.duoc.huertohogar;

import cl.duoc.huertohogar.model.*;
import cl.duoc.huertohogar.repository.*;
import cl.duoc.huertohogar.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Base común de las pruebas: crea usuarios de los 3 roles, 2 categorías y 2 productos en una base H2 limpia. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseTest {

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected JwtService jwtService;
    @Autowired protected PasswordEncoder encoder;
    @Autowired protected UsuarioRepository usuarioRepository;
    @Autowired protected CategoriaRepository categoriaRepository;
    @Autowired protected ProductoRepository productoRepository;
    @Autowired protected BoletaRepository boletaRepository;

    protected Usuario admin, vendedor, cliente;
    protected Categoria semillas, herramientas;
    protected Producto tomate, humus;

    @BeforeEach
    void prepararDatos() {
        boletaRepository.deleteAll();
        productoRepository.deleteAll();
        categoriaRepository.deleteAll();
        usuarioRepository.deleteAll();

        admin = usuarioRepository.save(new Usuario("Admin", "Huerto", "admin@huertohogar.cl", encoder.encode("admin123"), Rol.ADMIN));
        vendedor = usuarioRepository.save(new Usuario("Valentina", "Soto", "vendedor@huertohogar.cl", encoder.encode("vendedor123"), Rol.VENDEDOR));
        cliente = usuarioRepository.save(new Usuario("Pedro", "Hacker", "pedro@example.com", encoder.encode("pedro123"), Rol.CLIENTE));

        semillas = categoriaRepository.save(new Categoria("Semillas"));
        herramientas = categoriaRepository.save(new Categoria("Herramientas"));

        tomate = producto("Semillas de Tomate", semillas, 2500, 45, true, 1990);
        humus = producto("Humus de Lombriz", semillas, 6200, 2, false, null);
    }

    protected Producto producto(String nombre, Categoria categoria, int precio, int stock, boolean oferta, Integer precioOferta) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setCategoria(categoria);
        p.setPrecio(precio);
        p.setStock(stock);
        p.setOferta(oferta);
        p.setPrecioOferta(precioOferta);
        p.setDescripcion("");
        return productoRepository.save(p);
    }

    protected String bearer(Usuario u) {
        return "Bearer " + jwtService.generarTokenAcceso(u);
    }
}
