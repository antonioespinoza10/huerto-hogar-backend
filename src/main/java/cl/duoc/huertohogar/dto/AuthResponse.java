package cl.duoc.huertohogar.dto;

/** Respuesta de login, registro y renovación: token de acceso, token de renovación y datos del usuario. */
public record AuthResponse(String token, String refreshToken, String tipo, long expiraEnSegundos, UsuarioResponse usuario) {
}
