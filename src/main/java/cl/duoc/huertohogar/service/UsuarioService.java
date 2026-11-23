package cl.duoc.huertohogar.service;

import cl.duoc.huertohogar.dto.UsuarioRequest;
import cl.duoc.huertohogar.dto.UsuarioResponse;
import cl.duoc.huertohogar.exception.RecursoNoEncontradoException;
import cl.duoc.huertohogar.exception.ReglaNegocioException;
import cl.duoc.huertohogar.model.Rol;
import cl.duoc.huertohogar.model.Usuario;
import cl.duoc.huertohogar.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::de).toList();
    }

    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.de(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest req) {
        String correo = req.correo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ReglaNegocioException("Ese correo ya está registrado");
        }
        if (req.password() == null || req.password().length() < 6) {
            throw new ReglaNegocioException("La contraseña debe tener al menos 6 caracteres");
        }
        Usuario u = new Usuario(req.nombre().trim(), req.apellido().trim(), correo,
                passwordEncoder.encode(req.password()), req.rol());
        return UsuarioResponse.de(usuarioRepository.save(u));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest req, String correoActual) {
        Usuario u = buscar(id);
        String correo = req.correo().trim().toLowerCase();
        usuarioRepository.findByCorreoIgnoreCase(correo)
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> { throw new ReglaNegocioException("Ese correo ya está registrado"); });
        if (u.getCorreo().equalsIgnoreCase(correoActual) && req.rol() != Rol.ADMIN) {
            throw new ReglaNegocioException("No puedes quitarte el rol de administrador a ti mismo");
        }
        u.setNombre(req.nombre().trim());
        u.setApellido(req.apellido().trim());
        u.setCorreo(correo);
        u.setRol(req.rol());
        if (req.password() != null && !req.password().isBlank()) {
            if (req.password().length() < 6) {
                throw new ReglaNegocioException("La contraseña debe tener al menos 6 caracteres");
            }
            u.setPassword(passwordEncoder.encode(req.password()));
        }
        return UsuarioResponse.de(usuarioRepository.save(u));
    }

    /** Reglas: un administrador no puede eliminarse a sí mismo ni eliminar al último administrador. */
    @Transactional
    public void eliminar(Long id, String correoActual) {
        Usuario u = buscar(id);
        if (u.getCorreo().equalsIgnoreCase(correoActual)) {
            throw new ReglaNegocioException("No puedes eliminar tu propia cuenta");
        }
        if (u.getRol() == Rol.ADMIN && usuarioRepository.countByRol(Rol.ADMIN) <= 1) {
            throw new ReglaNegocioException("Debe quedar al menos un administrador");
        }
        usuarioRepository.delete(u);
    }

    private Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario " + id + " no encontrado"));
    }
}
