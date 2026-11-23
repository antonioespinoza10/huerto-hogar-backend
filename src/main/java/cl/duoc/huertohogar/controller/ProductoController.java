package cl.duoc.huertohogar.controller;

import cl.duoc.huertohogar.dto.ProductoRequest;
import cl.duoc.huertohogar.model.Producto;
import cl.duoc.huertohogar.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "CRUD de productos. Lectura pública; escritura solo ADMIN.")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar productos", description = "Opcionalmente filtra por categoría.")
    public List<Producto> listar(@Parameter(description = "Id de categoría") @RequestParam(required = false) Long categoriaId) {
        return service.listar(categoriaId);
    }

    @GetMapping("/stock-critico")
    @Operation(summary = "Productos con stock crítico", description = "Productos con menos de 5 unidades.")
    public List<Producto> stockCritico() {
        return service.stockCritico();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un producto")
    @ApiResponse(responseCode = "404", description = "No encontrado")
    public Producto obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Crear producto (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "403", description = "Sin permisos")
    public Producto crear(@Valid @RequestBody ProductoRequest req) {
        return service.crear(req);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Actualizar producto (ADMIN)")
    @ApiResponse(responseCode = "404", description = "No encontrado")
    public Producto actualizar(@PathVariable Long id, @Valid @RequestBody ProductoRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Eliminar producto (ADMIN)", description = "No permite eliminar productos que ya tienen ventas.")
    @ApiResponse(responseCode = "204", description = "Eliminado")
    @ApiResponse(responseCode = "409", description = "Tiene ventas registradas")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
