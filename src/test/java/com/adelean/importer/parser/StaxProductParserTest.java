package com.adelean.importer.parser;

import com.adelean.importer.model.ProductDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StaxProductParserTest {

    private final ProductParser parser =
            new StaxProductParser();

    @Test
    void shouldParseProductsOneByOne() {

        String xml =
                "<import>"
                        + "<products>"

                        + "<product>"
                        + "<article_sku>SKU-001</article_sku>"
                        + "<brand>HUAWEI</brand>"
                        + "</product>"

                        + "<product>"
                        + "<article_sku>SKU-002</article_sku>"
                        + "<brand>SONY</brand>"
                        + "</product>"

                        + "</products>"
                        + "</import>";

        List<ProductDocument> products =
                new ArrayList<>();

        parser.parse(
                toInputStream(xml),
                products::add
        );

        assertEquals(
                2,
                products.size()
        );

        assertEquals(
                "SKU-001",
                products.get(0)
                        .getArticleSku()
        );

        assertEquals(
                "HUAWEI",
                products.get(0)
                        .getFields()
                        .get("brand")
        );

        assertEquals(
                "SKU-002",
                products.get(1)
                        .getArticleSku()
        );
    }

    @Test
    void shouldPreserveCdataContent() {

        String html =
                "<p><strong>Product description</strong></p>";

        String xml =
                "<import>"
                        + "<products>"
                        + "<product>"
                        + "<article_sku>SKU-001</article_sku>"
                        + "<full_description><![CDATA["
                        + html
                        + "]]></full_description>"
                        + "</product>"
                        + "</products>"
                        + "</import>";

        List<ProductDocument> products =
                new ArrayList<>();

        parser.parse(
                toInputStream(xml),
                products::add
        );

        assertEquals(
                html,
                products.get(0)
                        .getFields()
                        .get("full_description")
        );
    }

    @Test
    void shouldAcceptEmptyElements() {

        String xml =
                "<import>"
                        + "<products>"
                        + "<product>"
                        + "<article_sku>SKU-001</article_sku>"
                        + "<warranty_scope />"
                        + "</product>"
                        + "</products>"
                        + "</import>";

        List<ProductDocument> products =
                new ArrayList<>();

        parser.parse(
                toInputStream(xml),
                products::add
        );

        assertEquals(
                "",
                products.get(0)
                        .getFields()
                        .get("warranty_scope")
        );
    }

    private ByteArrayInputStream toInputStream(
            String xml
    ) {
        return new ByteArrayInputStream(
                xml.getBytes(
                        StandardCharsets.UTF_8
                )
        );
    }
}