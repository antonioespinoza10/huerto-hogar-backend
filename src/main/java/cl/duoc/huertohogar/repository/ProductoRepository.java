package cl.duoc.huertohogar.repository;

import cl.duoc.huertohogar.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByCategoriaId(Long categoriaId);
    List<Producto> findByStockLessThan(Integer stock);
    long countByCategoriaId(Long categoriaId);
}
