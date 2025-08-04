package com.example.operator_service.dto;

import java.util.List;

public class OperacionRequestDTO {
    private String tipo;
    private List<ItemOperacionDTO> productos;

    // Getters y Setters

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public List<ItemOperacionDTO> getProductos() {
        return productos;
    }

    public void setProductos(List<ItemOperacionDTO> productos) {
        this.productos = productos;
    }

    public static class ItemOperacionDTO {
        private Long productoId;
        private int cantidad;

        // Getters y Setters

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
    }
}
