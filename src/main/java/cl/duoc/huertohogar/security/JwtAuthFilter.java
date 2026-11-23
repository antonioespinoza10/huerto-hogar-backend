package cl.duoc.huertohogar.security;

import cl.duoc.huertohogar.repository.UsuarioRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Se ejecuta en cada solicitud: lee el encabezado "Authorization: Bearer <token>",
 * valida el JWT y, si es correcto, deja al usuario autenticado con su rol (ROLE_ADMIN, ROLE_VENDEDOR o ROLE_CLIENTE).
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String correo = jwtService.correoDe(token, JwtService.TIPO_ACCESO);
                // Se lee el usuario desde la base de datos: si fue eliminado o cambió de rol, se usa el dato actual.
                usuarioRepository.findByCorreoIgnoreCase(correo).ifPresent(usuario -> {
                    var auth = new UsernamePasswordAuthenticationToken(
                            usuario.getCorreo(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            } catch (ExpiredJwtException e) {
                request.setAttribute("jwt_error", "El token expiró");
            } catch (JwtException | IllegalArgumentException e) {
                request.setAttribute("jwt_error", "Token inválido");
            }
        }
        chain.doFilter(request, response);
    }
}
