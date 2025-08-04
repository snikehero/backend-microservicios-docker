package com.example.operator_service.model;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;

@Entity
@Table(name = "operacion_productos")
public class OperacionProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productoId;

    private int cantidad;

    @ManyToOne
    @JoinColumn(name = "operacion_id")
    @JsonBackReference
    private Operacion operacion;

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Operacion getOperacion() {
        return operacion;
    }

    public void setOperacion(Operacion operacion) {
        this.operacion = operacion;
    }
}
