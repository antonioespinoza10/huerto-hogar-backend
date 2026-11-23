package cl.duoc.huertohogar.dto;

import cl.duoc.huertohogar.model.EstadoBoleta;
import jakarta.validation.constraints.NotNull;

public record EstadoRequest(@NotNull(message = "El estado es obligatorio") EstadoBoleta estado) {
}
