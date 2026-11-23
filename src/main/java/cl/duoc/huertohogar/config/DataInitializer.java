package cl.duoc.huertohogar.config;

import cl.duoc.huertohogar.model.*;
import cl.duoc.huertohogar.repository.CategoriaRepository;
import cl.duoc.huertohogar.repository.ProductoRepository;
import cl.duoc.huertohogar.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/** Carga datos de ejemplo la primera vez que se inicia la aplicación (si la base está vacía). */
@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UsuarioRepository usuarios;
    private final CategoriaRepository categorias;
    private final ProductoRepository productos;
    private final PasswordEncoder encoder;

    public DataInitializer(UsuarioRepository usuarios, CategoriaRepository categorias,
                           ProductoRepository productos, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.categorias = categorias;
        this.productos = productos;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarios.count() == 0) {
            usuarios.save(new Usuario("Admin", "Huerto", "admin@huertohogar.cl", encoder.encode("admin123"), Rol.ADMIN));
            usuarios.save(new Usuario("Valentina", "Soto", "vendedor@huertohogar.cl", encoder.encode("vendedor123"), Rol.VENDEDOR));
            usuarios.save(new Usuario("Pedro", "Hacker", "pedro.hacker20@example.com", encoder.encode("pedro123"), Rol.CLIENTE));
            log.info("Usuarios de prueba creados: admin, vendedor y cliente");
        }
        if (categorias.count() == 0) {
            Map<String, Categoria> cat = new HashMap<>();
            for (String nombre : new String[]{"Semillas", "Sustratos", "Herramientas", "Fertilizantes"}) {
                cat.put(nombre, categorias.save(new Categoria(nombre)));
            }
            crear("Semillas de Tomate Limachino", "Semillas", 2500, 45, true, 1990,
                    "https://images.unsplash.com/photo-1592841200221-a6898f307baa?auto=format&fit=crop&w=400&q=80",
                    "Semillas orgánicas de alta germinación.", cat);
            crear("Kit Sustrato Orgánico 10L", "Sustratos", 8990, 12, false, null,
                    "https://images.unsplash.com/photo-1416879595882-3373a0480b5b?auto=format&fit=crop&w=400&q=80",
                    "Mezcla nutritiva ideal para maceteros.", cat);
            crear("Macetero Cerámica Biodegradable", "Herramientas", 4500, 4, true, 3490,
                    "https://images.unsplash.com/photo-1485955900006-10f4d324d411?auto=format&fit=crop&w=400&q=80",
                    "Ecológico y resistente a la intemperie.", cat);
            crear("Fertilizante Humus de Lombriz 5kg", "Fertilizantes", 6200, 2, true, 4990,
                    "https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?auto=format&fit=crop&w=400&q=80",
                    "Aporte de nitrógeno natural para tus plantas.", cat);
            crear("Semillas de Albahaca Silvestre", "Semillas", 1800, 30, false, null,
                    "https://images.unsplash.com/photo-1618160702438-9b02ab6515c9?auto=format&fit=crop&w=400&q=80",
                    "Aromática y fácil de cultivar en casa.", cat);
            log.info("Categorías y productos de ejemplo creados");
        }
    }

    private void crear(String nombre, String categoria, int precio, int stock, boolean oferta, Integer precioOferta,
                       String imagen, String descripcion, Map<String, Categoria> cat) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setCategoria(cat.get(categoria));
        p.setPrecio(precio);
        p.setStock(stock);
        p.setOferta(oferta);
        p.setPrecioOferta(precioOferta);
        p.setImagen(imagen);
        p.setDescripcion(descripcion);
        productos.save(p);
    }
}
