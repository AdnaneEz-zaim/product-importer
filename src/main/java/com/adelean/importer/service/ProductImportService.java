package com.adelean.importer.service;

import com.adelean.importer.indexing.ProductIndexer;
import com.adelean.importer.parser.ProductParser;
import com.adelean.importer.transform.ProductDocumentTransformer;
import com.adelean.importer.zip.ZipXmlReader;

import java.nio.file.Path;
import java.util.Objects;

public class ProductImportService {

    private final ZipXmlReader zipXmlReader;
    private final ProductParser productParser;
    private final ProductDocumentTransformer transformer;
    private final ProductIndexer productIndexer;

    public ProductImportService(
            ZipXmlReader zipXmlReader,
            ProductParser productParser,
            ProductDocumentTransformer transformer,
            ProductIndexer productIndexer
    ) {
        this.zipXmlReader = Objects.requireNonNull(
                zipXmlReader,
                "zipXmlReader must not be null"
        );

        this.productParser = Objects.requireNonNull(
                productParser,
                "productParser must not be null"
        );

        this.transformer = Objects.requireNonNull(
                transformer,
                "transformer must not be null"
        );

        this.productIndexer = Objects.requireNonNull(
                productIndexer,
                "productIndexer must not be null"
        );
    }

    public void importProducts(Path zipPath) throws Exception {

        Objects.requireNonNull(
                zipPath,
                "zipPath must not be null"
        );

        zipXmlReader.readXmlEntry(
                zipPath,
                inputStream ->
                        productParser.parse(
                                inputStream,
                                product ->
                                        productIndexer.index(
                                                transformer.transform(product)
                                        )
                        )
        );
    }
}