package com.adelean.importer.transform;

import com.adelean.importer.model.ProductDocument;
import org.jsoup.Jsoup;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ProductDocumentTransformer {

    private static final String FULL_DESCRIPTION =
            "full_description";

    private static final String FULL_DESCRIPTION_TEXT =
            "full_description_text";

    private static final String SHORT_DESCRIPTION =
            "short_description";

    private static final String SHORT_DESCRIPTION_TEXT =
            "short_description_text";

    public ProductDocument transform(
            ProductDocument product
    ) {
        Objects.requireNonNull(
                product,
                "product must not be null"
        );

        Map<String, String> transformedFields =
                new LinkedHashMap<>(
                        product.getFields()
                );

        addPlainTextVersion(
                transformedFields,
                FULL_DESCRIPTION,
                FULL_DESCRIPTION_TEXT
        );

        addPlainTextVersion(
                transformedFields,
                SHORT_DESCRIPTION,
                SHORT_DESCRIPTION_TEXT
        );

        return new ProductDocument(
                transformedFields
        );
    }

    private void addPlainTextVersion(
            Map<String, String> fields,
            String sourceField,
            String targetField
    ) {
        String html =
                fields.get(sourceField);

        if (html == null || html.isBlank()) {
            return;
        }

        String plainText =
                Jsoup.parse(html).text();

        fields.put(
                targetField,
                plainText
        );
    }
}