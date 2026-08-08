package com.adelean.importer.service;

import com.adelean.importer.indexing.ProductIndexer;
import com.adelean.importer.model.ProductDocument;
import com.adelean.importer.parser.ProductParser;
import com.adelean.importer.parser.StaxProductParser;
import com.adelean.importer.transform.ProductDocumentTransformer;
import com.adelean.importer.zip.ZipXmlReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductImportServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void shouldImportProductsThroughCompletePipeline()
            throws Exception {

        Path zipPath =
                temporaryDirectory.resolve("products.zip");

        String xml =
                "<import>"
                        + "<products>"

                        + "<product>"
                        + "<article_sku>SKU-001</article_sku>"
                        + "<brand>HUAWEI</brand>"
                        + "<full_description>"
                        + "<![CDATA["
                        + "<p>Great <strong>phone</strong></p>"
                        + "]]>"
                        + "</full_description>"
                        + "</product>"

                        + "<product>"
                        + "<article_sku>SKU-002</article_sku>"
                        + "<brand>SONY</brand>"
                        + "</product>"

                        + "</products>"
                        + "</import>";

        createZip(
                zipPath,
                xml
        );

        InMemoryProductIndexer indexer =
                new InMemoryProductIndexer();

        ProductParser parser =
                new StaxProductParser();

        ProductImportService service =
                new ProductImportService(
                        new ZipXmlReader(),
                        parser,
                        new ProductDocumentTransformer(),
                        indexer
                );

        service.importProducts(zipPath);

        assertEquals(
                2,
                indexer.products.size()
        );

        ProductDocument firstProduct =
                indexer.products.get(0);

        assertEquals(
                "SKU-001",
                firstProduct.getArticleSku()
        );

        assertEquals(
                "HUAWEI",
                firstProduct
                        .getFields()
                        .get("brand")
        );

        assertEquals(
                "Great phone",
                firstProduct
                        .getFields()
                        .get("full_description_text")
        );

        assertTrue(
                firstProduct
                        .getFields()
                        .get("full_description")
                        .contains("<strong>")
        );

        ProductDocument secondProduct =
                indexer.products.get(1);

        assertEquals(
                "SKU-002",
                secondProduct.getArticleSku()
        );

        assertEquals(
                "SONY",
                secondProduct
                        .getFields()
                        .get("brand")
        );
    }

    private void createZip(
            Path zipPath,
            String xml
    ) throws Exception {

        try (
                ZipOutputStream output =
                        new ZipOutputStream(
                                Files.newOutputStream(
                                        zipPath
                                )
                        )
        ) {
            output.putNextEntry(
                    new ZipEntry("products.xml")
            );

            output.write(
                    xml.getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            output.closeEntry();
        }
    }

    private static class InMemoryProductIndexer
            implements ProductIndexer {

        private final List<ProductDocument> products =
                new ArrayList<>();

        @Override
        public void index(
                ProductDocument product
        ) {
            products.add(product);
        }

        @Override
        public void close() {}
    }
}