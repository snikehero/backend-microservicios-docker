package com.gym.inventory_service.specification;

import com.gym.inventory_service.Model.Producto;
import org.springframework.data.jpa.domain.Specification;

public class ProductoSpecification {

    public static Specification<Producto> nameContains(String name) {
        return (root, query, builder) ->
            name == null ? null : builder.like(builder.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Producto> categoryEquals(String category) {
        return (root, query, builder) ->
            category == null ? null : builder.equal(root.get("category"), category);
    }

    public static Specification<Producto> priceGreaterThanOrEqual(Double minPrice) {
        return (root, query, builder) ->
            minPrice == null ? null : builder.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Producto> priceLessThanOrEqual(Double maxPrice) {
        return (root, query, builder) ->
            maxPrice == null ? null : builder.lessThanOrEqualTo(root.get("price"), maxPrice);
    }
}
