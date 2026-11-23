package cl.duoc.huertohogar.service;

import cl.duoc.huertohogar.dto.*;
import cl.duoc.huertohogar.exception.RecursoNoEncontradoException;
import cl.duoc.huertohogar.exception.ReglaNegocioException;
import cl.duoc.huertohogar.model.Rol;
import cl.duoc.huertohogar.model.Usuario;
import cl.duoc.huertohogar.repository.UsuarioRepository;
import cl.duoc.huertohogar.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest req) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(req.correo().trim())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Correo o contraseña incorrectos"));
        return emitirTokens(usuario);
    }

    /** Registro público: siempre crea una cuenta con rol CLIENTE. */
    @Transactional
    public AuthResponse registrar(RegistroRequest req) {
        String correo = req.correo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ReglaNegocioException("Ese correo ya está registrado");
        }
        Usuario usuario = new Usuario(req.nombre().trim(), req.apellido().trim(), correo,
                passwordEncoder.encode(req.password()), Rol.CLIENTE);
        return emitirTokens(usuarioRepository.save(usuario));
    }

    /** Renueva la sesión: con un refresh token válido entrega un token de acceso nuevo (y un refresh nuevo). */
    public AuthResponse renovar(RefreshRequest req) {
        try {
            String correo = jwtService.correoDe(req.refreshToken(), JwtService.TIPO_REFRESH);
            Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                    .orElseThrow(() -> new BadCredentialsException("El usuario ya no existe"));
            return emitirTokens(usuario);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadCredentialsException("Refresh token inválido o expirado");
        }
    }

    public UsuarioResponse usuarioActual(String correo) {
        return usuarioRepository.findByCorreoIgnoreCase(correo)
                .map(UsuarioResponse::de)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private AuthResponse emitirTokens(Usuario usuario) {
        return new AuthResponse(
                jwtService.generarTokenAcceso(usuario),
                jwtService.generarTokenRefresh(usuario),
                "Bearer",
                jwtService.segundosAcceso(),
                UsuarioResponse.de(usuario));
    }
}
