package cl.duoc.huertohogar.controller;

import cl.duoc.huertohogar.dto.BoletaRequest;
import cl.duoc.huertohogar.dto.BoletaResponse;
import cl.duoc.huertohogar.dto.EstadoRequest;
import cl.duoc.huertohogar.service.BoletaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/boletas")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Boletas (órdenes)", description = "Compras de la tienda. Crear: cualquier usuario con sesión. Ver todas: ADMIN y VENDEDOR. Modificar: ADMIN.")
public class BoletaController {

    private final BoletaService service;

    public BoletaController(BoletaService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar una compra", description = "Valida stock, calcula el total en el servidor y descuenta el stock.")
    @ApiResponse(responseCode = "201", description = "Compra registrada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o carrito vacío")
    @ApiResponse(responseCode = "409", description = "Stock insuficiente")
    public BoletaResponse crear(@Valid @RequestBody BoletaRequest req, Authentication auth) {
        return service.crear(req, auth.getName());
    }

    @GetMapping("/mis-boletas")
    @Operation(summary = "Compras del usuario actual")
    public List<BoletaResponse> misBoletas(Authentication auth) {
        return service.misBoletas(auth.getName());
    }

    @GetMapping
    @Operation(summary = "Listar todas las boletas (ADMIN, VENDEDOR)")
    @ApiResponse(responseCode = "403", description = "El cliente no puede ver todas las órdenes")
    public List<BoletaResponse> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de una boleta (ADMIN, VENDEDOR)")
    @ApiResponse(responseCode = "404", description = "No encontrada")
    public BoletaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PutMapping("/{id}/estado")
    @Operation(summary = "Cambiar estado (ADMIN)", description = "PAGADA, ENVIADA, ENTREGADA o ANULADA. Al anular se devuelve el stock.")
    public BoletaResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoRequest req) {
        return service.cambiarEstado(id, req.estado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar boleta (ADMIN)", description = "Si no estaba anulada, devuelve el stock.")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
