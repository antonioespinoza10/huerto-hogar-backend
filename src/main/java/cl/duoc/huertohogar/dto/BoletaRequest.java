package cl.duoc.huertohogar.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/** Compra enviada por el checkout: productos del carrito + datos de despacho. El total lo calcula el servidor. */
public record BoletaRequest(
        @NotEmpty(message = "El carrito está vacío") List<@Valid Item> items,
        @NotNull(message = "Faltan los datos de despacho") @Valid Despacho despacho) {

    public record Item(
            @NotNull(message = "Falta el producto") Long productoId,
            @NotNull @Positive(message = "La cantidad debe ser mayor a 0") Integer cantidad) {
    }

    public record Despacho(
            @NotBlank(message = "El nombre es obligatorio") String nombre,
            @NotBlank(message = "El apellido es obligatorio") String apellido,
            @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo inválido") String correo,
            @NotBlank(message = "La calle es obligatoria") String calle,
            String departamento,
            @NotBlank(message = "La región es obligatoria") String region,
            @NotBlank(message = "La comuna es obligatoria") String comuna,
            String indicaciones) {
    }
}
