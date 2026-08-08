package com.adelean.importer.transform;

import com.adelean.importer.model.ProductDocument;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProductDocumentTransformerTest {

    private final ProductDocumentTransformer transformer =
            new ProductDocumentTransformer();

    @Test
    void shouldCreatePlainTextVersionOfHtmlDescriptions() {

        Map<String, String> fields =
                new LinkedHashMap<>();

        fields.put(
                "article_sku",
                "SKU-001"
        );

        fields.put(
                "full_description",
                "<p>Un <strong>excellent</strong> produit</p>"
        );

        fields.put(
                "short_description",
                "<p>Téléphone &eacute;l&eacute;gant</p>"
        );

        ProductDocument transformed =
                transformer.transform(
                        new ProductDocument(fields)
                );

        assertEquals(
                "Un excellent produit",
                transformed
                        .getFields()
                        .get("full_description_text")
        );

        assertEquals(
                "Téléphone élégant",
                transformed
                        .getFields()
                        .get("short_description_text")
        );
    }

    @Test
    void shouldPreserveOriginalHtml() {

        String html =
                "<p>Un <strong>excellent</strong> produit</p>";

        Map<String, String> fields =
                new LinkedHashMap<>();

        fields.put(
                "article_sku",
                "SKU-001"
        );

        fields.put(
                "full_description",
                html
        );

        ProductDocument transformed =
                transformer.transform(
                        new ProductDocument(fields)
                );

        assertEquals(
                html,
                transformed
                        .getFields()
                        .get("full_description")
        );
    }

    @Test
    void shouldNotAddTextFieldWhenDescriptionIsMissing() {

        Map<String, String> fields =
                new LinkedHashMap<>();

        fields.put(
                "article_sku",
                "SKU-001"
        );

        ProductDocument transformed =
                transformer.transform(
                        new ProductDocument(fields)
                );

        assertFalse(
                transformed
                        .getFields()
                        .containsKey(
                                "full_description_text"
                        )
        );

        assertFalse(
                transformed
                        .getFields()
                        .containsKey(
                                "short_description_text"
                        )
        );
    }
}