package cl.duoc.huertohogar.dto;

import cl.duoc.huertohogar.model.Rol;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alta o edición de usuarios por el administrador. En edición, password vacío = se mantiene la actual. */
public record UsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio") @Size(max = 60) String nombre,
        @NotBlank(message = "El apellido es obligatorio") @Size(max = 60) String apellido,
        @NotBlank(message = "El correo es obligatorio") @Email(message = "Correo inválido") String correo,
        String password,
        @NotNull(message = "El rol es obligatorio") Rol rol) {
}
