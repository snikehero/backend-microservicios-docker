package com.example.operator_service.repository;

import com.example.operator_service.model.OperacionProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperacionProductoRepository extends JpaRepository<OperacionProducto, Long> {
}