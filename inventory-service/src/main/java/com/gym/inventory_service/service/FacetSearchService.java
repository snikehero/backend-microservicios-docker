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

@Service
public class FacetSearchService {

    private final RestClient restClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String index;

    public FacetSearchService(
            @Value("${spring.elasticsearch.uris}") String esUris,
            @Value("${spring.elasticsearch.username:}") String username,
            @Value("${spring.elasticsearch.password:}") String password
    ) {
        // Usa la primera URI si vienen varias separadas por coma
        String baseUrl = esUris != null && esUris.contains(",") ? esUris.split(",")[0].trim() : esUris;
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalStateException("Configura spring.elasticsearch.uris");
        }
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);

        // Basic auth si aplica
        if (StringUtils.hasText(username)) {
            String basic = username + ":" + (password == null ? "" : password);
            String token = Base64.getEncoder().encodeToString(basic.getBytes(StandardCharsets.UTF_8));
            builder.defaultHeader("Authorization", "Basic " + token);
        }
        this.restClient = builder.build();

        // AJUSTADO a tu @Document(indexName="productos")
        this.index = "productos";
    }

    public SearchFacets.Response search(SearchFacets.Request req) {
        try {
            String body = buildQueryJson(req);
            String response = restClient.post()
                    .uri("/" + index + "/_search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = mapper.readTree(response);

            long total = root.path("hits").path("total").path("value").asLong(0);

            // Devuelve sólo los campos que pedimos en _source
            List<Map<String,Object>> items = new ArrayList<>();
            JsonNode hitsArr = root.path("hits").path("hits");
            if (hitsArr.isArray()) {
                for (JsonNode h : hitsArr) {
                    JsonNode src = h.path("_source");
                    if (!src.isMissingNode()) {
                        items.add(mapper.convertValue(src, Map.class));
                    }
                }
            }

            // Parseo de facets
            Map<String, List<Map<String,Object>>> facets = new LinkedHashMap<>();

            // category (terms)
            List<Map<String,Object>> category = new ArrayList<>();
            JsonNode catBuckets = root.path("aggregations").path("facet_category").path("buckets");
            if (catBuckets.isArray()) {
                for (JsonNode b : catBuckets) {
                    category.add(Map.of(
                            "value", b.path("key").asText(),
                            "count", b.path("doc_count").asLong(0)
                    ));
                }
            }
            facets.put("category", category);

            // price (range)
            List<Map<String,Object>> price = new ArrayList<>();
            JsonNode priceBuckets = root.path("aggregations").path("facet_price").path("buckets");
            if (priceBuckets.isArray()) {
                for (JsonNode b : priceBuckets) {
                    Map<String,Object> bucket = new LinkedHashMap<>();
                    bucket.put("from", b.hasNonNull("from") ? b.path("from").asDouble() : null);
                    bucket.put("to",   b.hasNonNull("to")   ? b.path("to").asDouble()   : null);
                    bucket.put("count", b.path("doc_count").asLong(0));
                    price.add(bucket);
                }
            }
            facets.put("price", price);

            return new SearchFacets.Response(new SearchFacets.Hits(total, items), facets);

        } catch (Exception e) {
            // Si algo falla (ej: ES caído), devuelve vacío para no tumbar el front
            return new SearchFacets.Response(
                    new SearchFacets.Hits(0, List.of()),
                    Map.of("category", List.of(), "price", List.of())
            );
        }
    }

    // ---------------- helpers ----------------
private String buildQueryJson(SearchFacets.Request req) throws Exception {
    int page = req.getPage() != null ? req.getPage() : 0;
    int size = req.getSize() != null ? req.getSize() : 12;
    int from = page * size;

    // Solo estos campos (según tu Producto.java)
    List<String> sourceIncludes = List.of("id","name","description","largeDescription","category","price","image");

    // sort
    String sortClause = null;
    if (req.getSort() != null && StringUtils.hasText(req.getSort().getField())) {
        String order = "asc";
        if ("desc".equalsIgnoreCase(req.getSort().getOrder())) order = "desc";
        sortClause = "\"sort\": [ { \"" + escape(req.getSort().getField()) + "\": { \"order\": \"" + order + "\" } } ]";
    }

    // query + filtros
    String queryClause = buildQueryAndFilters(req);

    // aggs
    String aggsClause = buildAggs(req);

    String sourceClause = "\"_source\": { \"includes\": " + mapper.writeValueAsString(sourceIncludes) + " }";

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

    // Texto: multi_match sobre tus campos
    if (StringUtils.hasText(req.getQuery())) {
        String q = "{ \"multi_match\": {"
                 + "\"query\": " + mapper.writeValueAsString(req.getQuery()) + ","
                 + "\"fields\": [\"name^3\",\"description\",\"largeDescription\",\"category^2\"],"
                 + "\"fuzziness\": \"AUTO\""
                 + "} }";
        must.add(q);
    }

    if (req.getFilters() != null) {
        // category (terms)
        if (req.getFilters().getCategory() != null && !req.getFilters().getCategory().isEmpty()) {
            String values = mapper.writeValueAsString(req.getFilters().getCategory());
            filter.add("{ \"terms\": { \"category\": " + values + " } }");
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
        String from = r.get(0) != null ? r.get(0).toString() : null;
        String to   = r.get(1) != null ? r.get(1).toString() : null;
        rangeAgg.append("{");
        boolean f2 = true;
        if (from != null) { rangeAgg.append("\"from\": ").append(from); f2 = false; }
        if (to   != null) { if (!f2) rangeAgg.append(", "); rangeAgg.append("\"to\": ").append(to); }
        rangeAgg.append("}");
    }
    rangeAgg.append("] } }");

    String aggs = "{"
                + "\"aggs\": {"
                + "\"facet_category\": { \"terms\": { \"field\": \"category\", \"size\": " + catSize + " } },"
                + "\"facet_price\": " + rangeAgg.toString()
                + "}"
                + "}";
    return aggs;
}


    private List<List<Double>> defaultPriceRanges() {
        return List.of(
                List.of(0d, 500d),
                List.of(500d, 1000d),
                List.of(1000d, 2000d)
        );
    }

    private static String escape(String s) { return s.replace("\"", "\\\""); }
}
