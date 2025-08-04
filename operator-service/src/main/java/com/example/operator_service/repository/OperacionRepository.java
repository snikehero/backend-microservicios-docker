package com.example.operator_service.repository;
import com.example.operator_service.model.Operacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperacionRepository extends JpaRepository<Operacion, Long> {
}
