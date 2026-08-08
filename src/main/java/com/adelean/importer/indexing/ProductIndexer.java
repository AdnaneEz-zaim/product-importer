package com.adelean.importer.indexing;

import com.adelean.importer.model.ProductDocument;

public interface ProductIndexer extends AutoCloseable {

    void index(ProductDocument product);

    @Override
    void close();
}