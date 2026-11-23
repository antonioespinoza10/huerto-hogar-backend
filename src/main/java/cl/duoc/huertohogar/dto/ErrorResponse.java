package cl.duoc.huertohogar.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Formato único de error que devuelve la API. */
public record ErrorResponse(LocalDateTime fecha, int estado, String error, String mensaje, List<String> detalles) {
    public static ErrorResponse de(int estado, String error, String mensaje) {
        return new ErrorResponse(LocalDateTime.now(), estado, error, mensaje, List.of());
    }
}
