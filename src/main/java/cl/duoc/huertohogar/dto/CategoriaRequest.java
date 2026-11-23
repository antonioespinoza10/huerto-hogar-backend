package cl.duoc.huertohogar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(@NotBlank(message = "El nombre es obligatorio") @Size(max = 60) String nombre) {
}
