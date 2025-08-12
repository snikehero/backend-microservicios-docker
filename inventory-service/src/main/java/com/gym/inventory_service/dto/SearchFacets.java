package com.gym.inventory_service.dto;

import java.util.List;
import java.util.Map;

public class SearchFacets {

    // -------- REQUEST --------
    public static class Request {
        private String query;                 // texto libre (opcional)
        private Integer page;                 // 0 por defecto
        private Integer size;                 // 12 por defecto
        private Sort sort;                    // {field, order}
        private Filters filters;              // filtros opcionales
        private Facets facets;                // config de facets opcional

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
        public Sort getSort() { return sort; }
        public void setSort(Sort sort) { this.sort = sort; }
        public Filters getFilters() { return filters; }
        public void setFilters(Filters filters) { this.filters = filters; }
        public Facets getFacets() { return facets; }
        public void setFacets(Facets facets) { this.facets = facets; }
    }

    public static class Sort {
        private String field;   // "price", "name", etc.
        private String order;   // "asc"|"desc"
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getOrder() { return order; }
        public void setOrder(String order) { this.order = order; }
    }

    public static class Filters {
        private List<String> category;        // OR múltiple
        private Double minPrice;              // mínimo
        private Double maxPrice;              // máximo

        public List<String> getCategory() { return category; }
        public void setCategory(List<String> category) { this.category = category; }
        public Double getMinPrice() { return minPrice; }
        public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }
        public Double getMaxPrice() { return maxPrice; }
        public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }
    }

    public static class Facets {
        private Integer categorySize;         // default 20
        // rangos default si no mandas nada: [0-500], [500-1000], [1000-2000]
        private List<List<Double>> priceRanges;

        public Integer getCategorySize() { return categorySize; }
        public void setCategorySize(Integer categorySize) { this.categorySize = categorySize; }
        public List<List<Double>> getPriceRanges() { return priceRanges; }
        public void setPriceRanges(List<List<Double>> priceRanges) { this.priceRanges = priceRanges; }
    }

    // -------- RESPONSE --------
    public static class Response {
        private Hits hits;
        private Map<String, List<Map<String,Object>>> facets;

        public Response() {}
        public Response(Hits hits, Map<String, List<Map<String,Object>>> facets) {
            this.hits = hits; this.facets = facets;
        }

        public Hits getHits() { return hits; }
        public void setHits(Hits hits) { this.hits = hits; }
        public Map<String, List<Map<String,Object>>> getFacets() { return facets; }
        public void setFacets(Map<String, List<Map<String,Object>>> facets) { this.facets = facets; }
    }

    public static class Hits {
        private long total;
        private List<Map<String,Object>> items;

        public Hits() {}
        public Hits(long total, List<Map<String,Object>> items) {
            this.total = total; this.items = items;
        }

        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        public List<Map<String,Object>> getItems() { return items; }
        public void setItems(List<Map<String,Object>> items) { this.items = items; }
    }
}
