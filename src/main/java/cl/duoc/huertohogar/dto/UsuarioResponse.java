package cl.duoc.huertohogar.dto;

import cl.duoc.huertohogar.model.Rol;
import cl.duoc.huertohogar.model.Usuario;

/** Datos públicos del usuario: nunca incluye la contraseña. */
public record UsuarioResponse(Long id, String nombre, String apellido, String correo, Rol rol) {
    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getApellido(), u.getCorreo(), u.getRol());
    }
}
