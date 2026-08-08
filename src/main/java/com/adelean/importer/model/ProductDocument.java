package com.adelean.importer.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ProductDocument {

    private final Map<String, String> fields;

    public ProductDocument(Map<String, String> fields) {
        Objects.requireNonNull(
                fields,
                "fields must not be null"
        );

        this.fields = new LinkedHashMap<>(fields);
    }

    public String getArticleSku() {
        return fields.get("article_sku");
    }

    public Map<String, String> getFields() {
        return Collections.unmodifiableMap(fields);
    }
}