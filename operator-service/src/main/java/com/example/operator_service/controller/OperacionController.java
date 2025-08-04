package com.example.operator_service.controller;

import com.example.operator_service.dto.BuscadorClient;
import com.example.operator_service.dto.OperacionRequestDTO;
import com.example.operator_service.dto.OperacionResponseDTO;
import com.example.operator_service.dto.OperacionResponseDTO.ItemOperacionResponseDTO;
import com.example.operator_service.model.Operacion;
import com.example.operator_service.model.OperacionProducto;
import com.example.operator_service.repository.OperacionProductoRepository;
import com.example.operator_service.repository.OperacionRepository;
import feign.FeignException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/operaciones")
public class OperacionController {

    @Autowired
    private OperacionRepository operacionRepository;

    @Autowired
    private OperacionProductoRepository operacionProductoRepository;

    @Autowired
    private BuscadorClient buscadorClient;

    // Obtener todas las operaciones con productos enriquecidos (nombre, imagen)
    @GetMapping
    public ResponseEntity<List<OperacionResponseDTO>> obtenerTodas() {
    List<Operacion> operaciones = operacionRepository.findAll();
    List<OperacionResponseDTO> response = new ArrayList<>();

    for (Operacion operacion : operaciones) {
        OperacionResponseDTO dto = new OperacionResponseDTO();
        dto.setId(operacion.getId());
        dto.setTipo(operacion.getTipo());
        dto.setFecha(operacion.getFecha());

        List<OperacionResponseDTO.ItemOperacionResponseDTO> productosDto = new ArrayList<>();

        for (OperacionProducto op : operacion.getProductos()) {
            BuscadorClient.ProductoDto productoDto = null;

            try {
                productoDto = buscadorClient.getProductoById(op.getProductoId());
            } catch (FeignException e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al contactar con inventory-service: " + e.contentUTF8());  
            }

            OperacionResponseDTO.ItemOperacionResponseDTO itemDto =
                new OperacionResponseDTO.ItemOperacionResponseDTO();

            itemDto.setProductoId(op.getProductoId());
            itemDto.setCantidad(op.getCantidad());

            if (productoDto != null) {
                itemDto.setNombre(productoDto.getName());
                itemDto.setImagen(productoDto.getImage());
            } else {
                itemDto.setNombre("Producto no encontrado");
                itemDto.setImagen(null);
            }

            productosDto.add(itemDto);
        }

        dto.setProductos(productosDto);
        response.add(dto);
    }

    return ResponseEntity.ok(response);
}
    // Registrar nueva operación con múltiples productos
    @PostMapping
    public ResponseEntity<?> registrarOperacion(@RequestBody OperacionRequestDTO request) {
        try {
            Operacion operacion = new Operacion();
            operacion.setTipo(request.getTipo());
            operacion.setFecha(LocalDateTime.now());
            Operacion operacionGuardada = operacionRepository.save(operacion);

            List<OperacionProducto> productos = new ArrayList<>();

            for (OperacionRequestDTO.ItemOperacionDTO item : request.getProductos()) {
                BuscadorClient.ProductoDto producto = buscadorClient.getProductoById(item.getProductoId());

                if (producto == null) {
                    return ResponseEntity.badRequest()
                            .body("Producto con ID " + item.getProductoId() + " no existe");
                }

                OperacionProducto op = new OperacionProducto();
                op.setOperacion(operacionGuardada);
                op.setProductoId(item.getProductoId());
                op.setCantidad(item.getCantidad());
                productos.add(op);
            }

            operacionProductoRepository.saveAll(productos);

            return ResponseEntity.ok(operacionGuardada);

        } catch (FeignException.NotFound e) {
            return ResponseEntity.badRequest().body("Producto no existe");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error en el servidor");
        }
    }
}
