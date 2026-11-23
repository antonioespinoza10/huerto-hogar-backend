package cl.duoc.huertohogar.security;

import cl.duoc.huertohogar.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Genera y valida los tokens JWT.
 * - Token de acceso: dura poco (por defecto 60 min) y se envía en cada solicitud.
 * - Token de renovación (refresh): dura más (por defecto 7 días) y solo sirve para pedir un token de acceso nuevo.
 * Ambos van firmados con HS256 usando una clave secreta que solo conoce el servidor.
 */
@Service
public class JwtService {

    public static final String TIPO_ACCESO = "access";
    public static final String TIPO_REFRESH = "refresh";

    private final SecretKey clave;
    private final Duration duracionAcceso;
    private final Duration duracionRefresh;

    public JwtService(@Value("${app.jwt.secret}") String secretoBase64,
                      @Value("${app.jwt.expiracion-minutos:60}") long minutosAcceso,
                      @Value("${app.jwt.refresh-dias:7}") long diasRefresh) {
        this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretoBase64));
        this.duracionAcceso = Duration.ofMinutes(minutosAcceso);
        this.duracionRefresh = Duration.ofDays(diasRefresh);
    }

    public String generarTokenAcceso(Usuario usuario) {
        return generar(usuario, TIPO_ACCESO, duracionAcceso);
    }

    public String generarTokenRefresh(Usuario usuario) {
        return generar(usuario, TIPO_REFRESH, duracionRefresh);
    }

    public long segundosAcceso() {
        return duracionAcceso.toSeconds();
    }

    private String generar(Usuario usuario, String tipo, Duration duracion) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getCorreo())
                .claim("rol", usuario.getRol().name())
                .claim("nombre", usuario.getNombre())
                .claim("tipo", tipo)
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(duracion)))
                .signWith(clave)
                .compact();
    }

    /** Verifica firma y vencimiento. Lanza JwtException si el token no es válido. */
    public Claims validar(String token) throws JwtException {
        return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
    }

    /** Devuelve el correo si el token es válido y del tipo pedido; si no, lanza JwtException. */
    public String correoDe(String token, String tipoEsperado) {
        Claims claims = validar(token);
        if (!tipoEsperado.equals(claims.get("tipo", String.class))) {
            throw new JwtException("Tipo de token incorrecto");
        }
        return claims.getSubject();
    }
}
