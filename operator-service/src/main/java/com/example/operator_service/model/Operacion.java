package com.example.operator_service.model;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;
@Entity
@Table(name = "operaciones")
public class Operacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipo; // Ej: "compra", "alquiler"

    @OneToMany(mappedBy = "operacion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<OperacionProducto> productos = new ArrayList<>();

    private int cantidad;

    @Column(name = "fecha")
    private LocalDateTime fecha = LocalDateTime.now();

    // Getters y Setters
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

  public List<OperacionProducto> getProductos() {
    return productos;
}

public void setProductos(List<OperacionProducto> productos) {
    this.productos = productos;
}

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
