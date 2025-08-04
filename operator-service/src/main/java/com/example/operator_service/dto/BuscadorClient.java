package com.example.operator_service.dto;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "inventory-service")
public interface BuscadorClient {

    @GetMapping("/api/productos/{id}")
    ProductoDto getProductoById(@PathVariable("id") Long id);

    class ProductoDto {
        private Long id;
        private String name;
        private String image; // Agregado para traer la imagen

        // Getters y setters
        public Long getId() {
            return id;
        }
        public void setId(Long id) {
            this.id = id;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public String getImage() {
            return image;
        }
        public void setImage(String image) {
            this.image = image;
        }
    }
}