package cl.duoc.huertohogar.dto;

import cl.duoc.huertohogar.model.Boleta;
import cl.duoc.huertohogar.model.EstadoBoleta;

import java.time.LocalDateTime;
import java.util.List;

public record BoletaResponse(
        Long id, LocalDateTime fecha, Integer total, EstadoBoleta estado, Long usuarioId,
        String nombreCliente, String apellidoCliente, String correoCliente,
        String calle, String departamento, String region, String comuna, String indicaciones,
        List<Detalle> detalles) {

    public record Detalle(Long id, Long productoId, String nombreProducto, Integer cantidad, Integer precioUnitario, Integer subtotal) {
    }

    public static BoletaResponse de(Boleta b) {
        List<Detalle> detalles = b.getDetalles().stream()
                .map(d -> new Detalle(d.getId(), d.getProducto().getId(), d.getProducto().getNombre(),
                        d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal()))
                .toList();
        return new BoletaResponse(b.getId(), b.getFecha(), b.getTotal(), b.getEstado(),
                b.getUsuario() != null ? b.getUsuario().getId() : null,
                b.getNombreCliente(), b.getApellidoCliente(), b.getCorreoCliente(),
                b.getCalle(), b.getDepartamento(), b.getRegion(), b.getComuna(), b.getIndicaciones(), detalles);
    }
}
