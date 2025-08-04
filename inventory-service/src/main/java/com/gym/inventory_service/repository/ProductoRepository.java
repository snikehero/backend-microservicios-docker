package com.gym.inventory_service.repository;

import com.gym.inventory_service.Model.Producto;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface ProductoRepository extends ElasticsearchRepository<Producto, Long> {
    List<Producto> findByCategory(String category);
    List<Producto> findByNameContaining(String name); // búsqueda parcial (full-text)
}
