package com.gym.inventory_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.inventory_service.dto.SearchFacets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64;

@Service
public class FacetSearchService {

    private final RestClient restClient;
    private final ObjectMapper mapper = new ObjectMapper();

    private final String index;

    // Lee la primera URI de spring.elasticsearch.uris (formato: http(s)://host:port)
    public FacetSearchService(
            @Value("${spring.elasticsearch.uris}") String esUris,
            @Value("${spring.elasticsearch.username:}") String username,
            @Value("${spring.elasticsearch.password:}") String password
    ) {
        String baseUrl = esUris;
        // Si hay múltiples URIs separadas por coma, usa la primera
        if (esUris != null && esUris.contains(",")) {
            baseUrl = esUris.split(",")[0].trim();
        }
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("Falta configurar spring.elasticsearch.uris");
        }

        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
        if (StringUtils.hasText(username)) {
            String basic = username + ":" + password;
            String token = Base64.getEncoder().encodeToString(basic.getBytes(StandardCharsets.UTF_8));
            builder.defaultHeader("Authorization", "Basic " + token);
        }
        this.restClient = builder.build();
        this.index = "productos"; // si tu @Document usa otro nombre, cámbialo aquí
    }

    public SearchFacets.Response search(SearchFacets.Request req) {
        try {
            // 1) Construir body JSON para _search
            String body = buildQueryJson(req);

            // 2) POST al endpoint _search
            String path = "/" + index + "/_search";
            String response = restClient.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            // 3) Parsear resultados
            JsonNode root = mapper.readTree(response);

            long total = 0L;
            JsonNode totalNode = root.path("hits").path("total").path("value");
            if (totalNode.isNumber()) total = totalNode.asLong();

            List<Map<String,Object>> items = new ArrayList<>();
            JsonNode hitsArr = root.path("hits").path("hits");
            if (hitsArr.isArray()) {
                for (JsonNode h : hitsArr) {
                    JsonNode src = h.path("_source");
                    if (!src.isMissingNode()) {
                        // Devuelve sólo algunos campos (ya filtrados en _source)
                        Map<String,Object> product = mapper.convertValue(src, Map.class);
                        items.add(product);
                    }
                }
            }

            Map<String, List<Map<String,Object>>> facets = new LinkedHashMap<>();

            // category terms
            List<Map<String,Object>> category = new ArrayList<>();
            JsonNode catAgg = root.path("aggregations").path("facet_category").path("buckets");
            if (catAgg.isArray()) {
                for (JsonNode b : catAgg) {
                    String key = b.path("key").asText();
                    long count = b.path("doc_count").asLong();
                    category.add(Map.of("value", key, "count", count));
                }
            }
            facets.put("category", category);

            // price range
            List<Map<String,Object>> price = new ArrayList<>();
            JsonNode priceAgg = root.path("aggregations").path("facet_price").path("buckets");
            if (priceAgg.isArray()) {
                for (JsonNode b : priceAgg) {
                    Double from = b.hasNonNull("from") ? b.path("from").asDouble() : null;
                    Double to   = b.hasNonNull("to")   ? b.path("to").asDouble()   : null;
                    long count  = b.path("doc_count").asLong();
                    Map<String,Object> bucket = new LinkedHashMap<>();
                    bucket.put("from", from);
                    bucket.put("to", to);
                    bucket.put("count", count);
                    price.add(bucket);
                }
            }
            facets.put("price", price);

            return new SearchFacets.Response(
                    new SearchFacets.Hits(total, items),
                    facets
            );
        } catch (Exception e) {
            // En producción puedes registrar el error y devolver vacío con mensaje
            return new SearchFacets.Response(
                    new SearchFacets.Hits(0, List.of()),
                    Map.of("category", List.of(), "price", List.of())
            );
        }
    }

    // ----------------- helpers -----------------

    private String buildQueryJson(SearchFacets.Request req) throws Exception {
        int page = (req.getPage() != null) ? req.getPage() : 0;
        int size = (req.getSize() != null) ? req.getSize() : 12;
        int from = page * size;

        // _source filtering
        List<String> sourceIncludes = List.of("id","name","price","category","image");

        // sort
        String sortClause = null;
        if (req.getSort() != null && StringUtils.hasText(req.getSort().getField())) {
            String order = "asc";
            if ("desc".equalsIgnoreCase(req.getSort().getOrder())) order = "desc";
            sortClause = """
                "sort": [
                  { "%s": { "order": "%s" } }
                ]
            """.formatted(escape(req.getSort().getField()), order);
        }

        // query + filtros
        String queryClause = buildQueryAndFilters(req);

        // aggs
        String aggsClause = buildAggs(req);

        String sourceClause = """
            "_source": { "includes": %s }
        """.formatted(mapper.writeValueAsString(sourceIncludes));

        List<String> parts = new ArrayList<>();
        parts.add("\"from\": " + from);
        parts.add("\"size\": " + size);
        parts.add(sourceClause);
        if (sortClause != null) parts.add(sortClause);
        if (queryClause != null) parts.add(queryClause);
        parts.add(aggsClause);

        return "{\n" + String.join(",\n", parts) + "\n}";
    }

    private String buildQueryAndFilters(SearchFacets.Request req) throws Exception {
        List<String> must = new ArrayList<>();
        List<String> filter = new ArrayList<>();

        // texto
        if (StringUtils.hasText(req.getQuery())) {
            // multi_match sencillo
            String q = """
              { "multi_match": {
                  "query": %s,
                  "fields": ["name^3","description","largeDescription","category^2"],
                  "fuzziness": "AUTO"
              }}""".formatted(mapper.writeValueAsString(req.getQuery()));
            must.add(q);
        }

        // filtros
        if (req.getFilters() != null) {
            // category (terms)
            if (req.getFilters().getCategory() != null && !req.getFilters().getCategory().isEmpty()) {
                String values = mapper.writeValueAsString(req.getFilters().getCategory());
                filter.add("""
                  { "terms": { "category": %s } }
                """.formatted(values));
            }
            // price min/max
            if (req.getFilters().getMinPrice() != null || req.getFilters().getMaxPrice() != null) {
                StringBuilder range = new StringBuilder();
                range.append("{ \"range\": { \"price\": {");
                boolean first = true;
                if (req.getFilters().getMinPrice() != null) {
                    range.append("\"gte\": ").append(req.getFilters().getMinPrice());
                    first = false;
                }
                if (req.getFilters().getMaxPrice() != null) {
                    if (!first) range.append(", ");
                    range.append("\"lte\": ").append(req.getFilters().getMaxPrice());
                }
                range.append(" } } }");
                filter.add(range.toString());
            }
        }

        if (must.isEmpty() && filter.isEmpty()) return null;

        StringBuilder bool = new StringBuilder();
        bool.append("\"query\": { \"bool\": {");
        if (!must.isEmpty()) {
            bool.append("\"must\": [").append(String.join(",", must)).append("]");
            if (!filter.isEmpty()) bool.append(", ");
        }
        if (!filter.isEmpty()) {
            bool.append("\"filter\": [").append(String.join(",", filter)).append("]");
        }
        bool.append(" } }");
        return bool.toString();
    }

    private String buildAggs(SearchFacets.Request req) throws Exception {
        int catSize = 20;
        if (req.getFacets() != null && req.getFacets().getCategorySize() != null) {
            catSize = req.getFacets().getCategorySize();
        }

        List<List<Double>> ranges = defaultPriceRanges();
        if (req.getFacets() != null && req.getFacets().getPriceRanges() != null
                && !req.getFacets().getPriceRanges().isEmpty()) {
            ranges = req.getFacets().getPriceRanges();
        }
        StringBuilder rangeAgg = new StringBuilder();
        rangeAgg.append("{ \"range\": { \"field\": \"price\", \"ranges\": [");
        boolean first = true;
        for (List<Double> r : ranges) {
            if (r == null || r.size() < 2) continue;
            if (!first) rangeAgg.append(", ");
            first = false;
            String from = (r.get(0) != null) ? r.get(0).toString() : null;
            String to   = (r.get(1) != null) ? r.get(1).toString() : null;
            rangeAgg.append("{");
            boolean f2 = true;
            if (from != null) { rangeAgg.append("\"from\": ").append(from); f2 = false; }
            if (to   != null) { if (!f2) rangeAgg.append(", "); rangeAgg.append("\"to\": ").append(to); }
            rangeAgg.append("}");
        }
        rangeAgg.append("] } }");

        String aggs = """
          "aggs": {
            "facet_category": { "terms": { "field": "category", "size": %d } },
            "facet_price": %s
          }
        """.formatted(catSize, rangeAgg.toString());

        return aggs;
    }

    private List<List<Double>> defaultPriceRanges() {
        return List.of(
                List.of(0d, 500d),
                List.of(500d, 1000d),
                List.of(1000d, 2000d)
        );
    }

    private static String escape(String s) {
        return s.replace("\"", "\\\"");
    }
}
