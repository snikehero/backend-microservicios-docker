package com.gym.inventory_service.controller;

import com.gym.inventory_service.Model.Producto;
import com.gym.inventory_service.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.gym.inventory_service.dto.SearchFacets;
import com.gym.inventory_service.service.FacetSearchService;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/productos")

public class ProductoController {

    @Autowired
    private ProductoRepository repository;
    @Autowired
    private FacetSearchService facetSearchService;
    // Obtener todos los productos
    @GetMapping
    public Iterable<Producto> listar() {
        return repository.findAll();
    }

    // Crear un nuevo producto
    @PostMapping
    public Producto crear(@RequestBody Producto producto) {
        return repository.save(producto);
    }

    // Obtener producto por id
    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtener(@PathVariable Long id) {
        Optional<Producto> producto = repository.findById(id);
        return producto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // Actualizar producto
    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(@PathVariable Long id, @RequestBody Producto producto) {
        Optional<Producto> existente = repository.findById(id);
        if (existente.isPresent()) {
            Producto p = existente.get();
            p.setImage(producto.getImage());
            p.setName(producto.getName());
            p.setCategory(producto.getCategory());
            p.setPrice(producto.getPrice());
            p.setDescription(producto.getDescription());
            p.setLargeDescription(producto.getLargeDescription());
            return ResponseEntity.ok(repository.save(p));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Eliminar producto
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Agregar en bulk 
    @PostMapping("/bulk")
    public Iterable<Producto> cargarMockData(@RequestBody List<Producto> productos) {
        return repository.saveAll(productos);
    }

    // Buscar por categoría exacta
    @GetMapping("/categoria/{category}")
    public List<Producto> getByCategory(@PathVariable String category) {
        return repository.findByCategory(category);
    }

    // Búsqueda por nombre parcial (full-text search)
    @GetMapping("/search")
    public List<Producto> buscarPorNombre(@RequestParam String name) {
        return repository.findByNameContaining(name); // método definido en el ElasticsearchRepository
    }
    @PostMapping("/search-agg")
    public SearchFacets.Response searchWithFacets(@RequestBody SearchFacets.Request request) {
        return facetSearchService.search(request);
    }
    // 1) Ping GET para verificar que el path base está siendo mapeado
@GetMapping("/search-agg/ping")
public java.util.Map<String,Object> pingSearchAgg() {
    return java.util.Map.of("ok", true, "msg", "GET /api/productos/search-agg/ping reachable");
}

// 2) Eco POST sin dependencias del service (descarta problemas en FacetSearchService)
@PostMapping(value = "/search-agg/echo", consumes = {"application/json", "*/*"}, produces = "application/json")
public java.util.Map<String,Object> echo(@RequestBody(required = false) java.util.Map<String,Object> body) {
    return java.util.Map.of("echo", body == null ? java.util.Map.of() : body);
}
}
