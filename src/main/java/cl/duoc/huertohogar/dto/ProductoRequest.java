package cl.duoc.huertohogar.dto;

import jakarta.validation.constraints.*;

public record ProductoRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 120) String nombre,
        @Size(max = 500) String descripcion,
        @NotNull(message = "El precio es obligatorio") @Positive(message = "El precio debe ser mayor a 0") Integer precio,
        @NotNull(message = "El stock es obligatorio") @PositiveOrZero(message = "El stock no puede ser negativo") Integer stock,
        Boolean oferta,
        @Positive(message = "El precio de oferta debe ser mayor a 0") Integer precioOferta,
        @Size(max = 500) String imagen,
        @NotNull(message = "La categoría es obligatoria") Long categoriaId) {
}
