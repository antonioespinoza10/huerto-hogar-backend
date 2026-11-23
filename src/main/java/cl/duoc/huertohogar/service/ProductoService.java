package cl.duoc.huertohogar.service;

import cl.duoc.huertohogar.dto.ProductoRequest;
import cl.duoc.huertohogar.exception.RecursoNoEncontradoException;
import cl.duoc.huertohogar.exception.ReglaNegocioException;
import cl.duoc.huertohogar.model.Producto;
import cl.duoc.huertohogar.repository.DetalleBoletaRepository;
import cl.duoc.huertohogar.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {

    /** Bajo este número de unidades un producto está en stock crítico. */
    public static final int STOCK_CRITICO = 5;

    private final ProductoRepository productoRepository;
    private final DetalleBoletaRepository detalleBoletaRepository;
    private final CategoriaService categoriaService;

    public ProductoService(ProductoRepository productoRepository, DetalleBoletaRepository detalleBoletaRepository,
                           CategoriaService categoriaService) {
        this.productoRepository = productoRepository;
        this.detalleBoletaRepository = detalleBoletaRepository;
        this.categoriaService = categoriaService;
    }

    public List<Producto> listar(Long categoriaId) {
        return categoriaId == null ? productoRepository.findAll() : productoRepository.findByCategoriaId(categoriaId);
    }

    public List<Producto> stockCritico() {
        return productoRepository.findByStockLessThan(STOCK_CRITICO);
    }

    public Producto obtener(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto " + id + " no encontrado"));
    }

    @Transactional
    public Producto crear(ProductoRequest req) {
        Producto producto = new Producto();
        aplicar(producto, req);
        return productoRepository.save(producto);
    }

    @Transactional
    public Producto actualizar(Long id, ProductoRequest req) {
        Producto producto = obtener(id);
        aplicar(producto, req);
        return productoRepository.save(producto);
    }

    /** Un producto que ya aparece en boletas no se borra, para no perder el historial de ventas. */
    @Transactional
    public void eliminar(Long id) {
        Producto producto = obtener(id);
        if (detalleBoletaRepository.existsByProductoId(id)) {
            throw new ReglaNegocioException("No se puede eliminar " + producto.getNombre()
                    + ": ya tiene ventas registradas. Puedes dejar su stock en 0.");
        }
        productoRepository.delete(producto);
    }

    private void aplicar(Producto p, ProductoRequest req) {
        boolean oferta = Boolean.TRUE.equals(req.oferta());
        if (oferta && (req.precioOferta() == null || req.precioOferta() >= req.precio())) {
            throw new ReglaNegocioException("El precio de oferta debe ser menor que el precio normal");
        }
        p.setNombre(req.nombre().trim());
        p.setDescripcion(req.descripcion() != null ? req.descripcion().trim() : "");
        p.setPrecio(req.precio());
        p.setStock(req.stock());
        p.setOferta(oferta);
        p.setPrecioOferta(oferta ? req.precioOferta() : null);
        p.setImagen(req.imagen());
        p.setCategoria(categoriaService.obtener(req.categoriaId()));
    }
}
