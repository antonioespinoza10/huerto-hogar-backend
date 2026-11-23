package cl.duoc.huertohogar.service;

import cl.duoc.huertohogar.dto.BoletaRequest;
import cl.duoc.huertohogar.dto.BoletaResponse;
import cl.duoc.huertohogar.exception.RecursoNoEncontradoException;
import cl.duoc.huertohogar.exception.ReglaNegocioException;
import cl.duoc.huertohogar.model.*;
import cl.duoc.huertohogar.repository.BoletaRepository;
import cl.duoc.huertohogar.repository.ProductoRepository;
import cl.duoc.huertohogar.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BoletaService {

    private final BoletaRepository boletaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    public BoletaService(BoletaRepository boletaRepository, ProductoRepository productoRepository,
                         UsuarioRepository usuarioRepository) {
        this.boletaRepository = boletaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Registra una compra. Todo ocurre en una sola transacción:
     * 1) valida que haya stock de cada producto, 2) calcula precios y total en el servidor
     * (usando el precio de oferta si corresponde), 3) descuenta el stock y 4) guarda la boleta con sus detalles.
     * Si algo falla, no se guarda nada ni se descuenta stock.
     */
    @Transactional
    public BoletaResponse crear(BoletaRequest req, String correoUsuario) {
        Boleta boleta = new Boleta();
        usuarioRepository.findByCorreoIgnoreCase(correoUsuario).ifPresent(boleta::setUsuario);

        int total = 0;
        for (BoletaRequest.Item item : req.items()) {
            Producto producto = productoRepository.findById(item.productoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Producto " + item.productoId() + " no encontrado"));
            if (producto.getStock() < item.cantidad()) {
                throw new ReglaNegocioException("Stock insuficiente para " + producto.getNombre()
                        + ": quedan " + producto.getStock() + " unidad(es)");
            }
            producto.setStock(producto.getStock() - item.cantidad());
            DetalleBoleta detalle = new DetalleBoleta(producto, item.cantidad(), producto.precioVigente());
            boleta.agregarDetalle(detalle);
            total += detalle.getSubtotal();
        }

        BoletaRequest.Despacho d = req.despacho();
        boleta.setTotal(total);
        boleta.setNombreCliente(d.nombre().trim());
        boleta.setApellidoCliente(d.apellido().trim());
        boleta.setCorreoCliente(d.correo().trim().toLowerCase());
        boleta.setCalle(d.calle().trim());
        boleta.setDepartamento(d.departamento());
        boleta.setRegion(d.region());
        boleta.setComuna(d.comuna());
        boleta.setIndicaciones(d.indicaciones());
        return BoletaResponse.de(boletaRepository.save(boleta));
    }

    @Transactional(readOnly = true)
    public List<BoletaResponse> listar() {
        return boletaRepository.findAllByOrderByFechaDesc().stream().map(BoletaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public List<BoletaResponse> misBoletas(String correoUsuario) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correoUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        return boletaRepository.findByUsuarioIdOrderByFechaDesc(usuario.getId()).stream().map(BoletaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public BoletaResponse obtener(Long id) {
        return BoletaResponse.de(buscar(id));
    }

    /** Al anular una boleta se devuelve el stock de sus productos. */
    @Transactional
    public BoletaResponse cambiarEstado(Long id, EstadoBoleta nuevo) {
        Boleta boleta = buscar(id);
        if (boleta.getEstado() == EstadoBoleta.ANULADA) {
            throw new ReglaNegocioException("La boleta ya está anulada");
        }
        if (nuevo == EstadoBoleta.ANULADA) {
            devolverStock(boleta);
        }
        boleta.setEstado(nuevo);
        return BoletaResponse.de(boleta);
    }

    @Transactional
    public void eliminar(Long id) {
        Boleta boleta = buscar(id);
        if (boleta.getEstado() != EstadoBoleta.ANULADA) {
            devolverStock(boleta);
        }
        boletaRepository.delete(boleta);
    }

    private void devolverStock(Boleta boleta) {
        boleta.getDetalles().forEach(d -> d.getProducto().setStock(d.getProducto().getStock() + d.getCantidad()));
    }

    private Boleta buscar(Long id) {
        return boletaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Boleta " + id + " no encontrada"));
    }
}
