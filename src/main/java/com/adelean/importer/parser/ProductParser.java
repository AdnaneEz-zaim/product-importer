package com.adelean.importer.parser;

import com.adelean.importer.model.ProductDocument;

import java.io.InputStream;
import java.util.function.Consumer;

public interface ProductParser {

    void parse(
            InputStream inputStream,
            Consumer<ProductDocument> productConsumer
    );
}