package cl.duoc.huertohogar.exception;

/** Una operación válida en forma pero que rompe una regla del negocio (stock insuficiente, duplicados...). HTTP 409. */
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
