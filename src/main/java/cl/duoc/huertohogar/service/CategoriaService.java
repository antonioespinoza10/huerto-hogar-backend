package cl.duoc.huertohogar.service;

import cl.duoc.huertohogar.dto.CategoriaRequest;
import cl.duoc.huertohogar.exception.RecursoNoEncontradoException;
import cl.duoc.huertohogar.exception.ReglaNegocioException;
import cl.duoc.huertohogar.model.Categoria;
import cl.duoc.huertohogar.repository.CategoriaRepository;
import cl.duoc.huertohogar.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, ProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    public List<Categoria> listar() {
        return categoriaRepository.findAll();
    }

    public Categoria obtener(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Categoría " + id + " no encontrada"));
    }

    @Transactional
    public Categoria crear(CategoriaRequest req) {
        String nombre = req.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ReglaNegocioException("Ya existe la categoría " + nombre);
        }
        return categoriaRepository.save(new Categoria(nombre));
    }

    @Transactional
    public Categoria actualizar(Long id, CategoriaRequest req) {
        Categoria categoria = obtener(id);
        String nombre = req.nombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ReglaNegocioException("Ya existe la categoría " + nombre);
        }
        categoria.setNombre(nombre);
        return categoriaRepository.save(categoria);
    }

    /** Regla de negocio: no se elimina una categoría que todavía tiene productos. */
    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = obtener(id);
        long productos = productoRepository.countByCategoriaId(id);
        if (productos > 0) {
            throw new ReglaNegocioException("No se puede eliminar " + categoria.getNombre()
                    + ": tiene " + productos + " producto(s) asociado(s)");
        }
        categoriaRepository.delete(categoria);
    }
}
