package com.example.operator_service.dto;
import java.time.LocalDateTime;
import java.util.List;
public class OperacionResponseDTO {
    private Long id;
    private String tipo;
    private LocalDateTime fecha;
    private List<ItemOperacionResponseDTO> productos;

    // getters y setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public List<ItemOperacionResponseDTO> getProductos() {
        return productos;
    }

    public void setProductos(List<ItemOperacionResponseDTO> productos) {
        this.productos = productos;
    }

    public static class ItemOperacionResponseDTO {
        private Long productoId;
        public Long getProductoId() {
            return productoId;
        }
        public void setProductoId(Long productoId) {
            this.productoId = productoId;
        }
        public int getCantidad() {
            return cantidad;
        }
        public void setCantidad(int cantidad) {
            this.cantidad = cantidad;
        }
        public String getNombre() {
            return nombre;
        }
        public void setNombre(String nombre) {
            this.nombre = nombre;
        }
        public String getImagen() {
            return imagen;
        }
        public void setImagen(String imagen) {
            this.imagen = imagen;
        }
        private int cantidad;
        private String nombre;
        private String imagen;

        // getters y setters
    }
}