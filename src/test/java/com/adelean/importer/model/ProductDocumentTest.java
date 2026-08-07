package com.adelean.importer.model;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductDocumentTest {

    @Test
    void shouldExposeArticleSkuAndFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("article_sku", "SKU-001");
        fields.put("brand", "HUAWEI");

        ProductDocument product =
                new ProductDocument(fields);

        assertEquals(
                "SKU-001",
                product.getArticleSku()
        );

        assertEquals(
                "HUAWEI",
                product.getFields().get("brand")
        );
    }

    @Test
    void shouldExposeFieldsAsReadOnly() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("article_sku", "SKU-001");

        ProductDocument product =
                new ProductDocument(fields);

        assertThrows(
                UnsupportedOperationException.class,
                () -> product.getFields()
                        .put("brand", "SONY")
        );
    }
}