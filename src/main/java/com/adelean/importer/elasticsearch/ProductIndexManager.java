package com.adelean.importer.elasticsearch;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.util.Objects;

public class ProductIndexManager {

    private final RestHighLevelClient client;
    private final String indexName;

    public ProductIndexManager(
            RestHighLevelClient client,
            String indexName
    ) {
        this.client = Objects.requireNonNull(
                client,
                "client must not be null"
        );

        if (indexName == null || indexName.isBlank()) {
            throw new IllegalArgumentException(
                    "indexName must not be empty"
            );
        }

        this.indexName = indexName;
    }

    public void createIndexIfMissing() throws IOException {

        GetIndexRequest getIndexRequest =
                new GetIndexRequest(indexName);

        boolean exists =
                client.indices().exists(
                        getIndexRequest,
                        RequestOptions.DEFAULT
                );

        if (exists) {
            return;
        }

        CreateIndexRequest createIndexRequest =
                new CreateIndexRequest(indexName);

        createIndexRequest.mapping(
                buildMapping(),
                XContentType.JSON
        );

        CreateIndexResponse response =
                client.indices().create(
                        createIndexRequest,
                        RequestOptions.DEFAULT
                );

        if (!response.isAcknowledged()) {
            throw new IOException(
                    "Elasticsearch did not acknowledge index creation: "
                            + indexName
            );
        }
    }

    private String buildMapping() {
        return "{"
                + "\"properties\":{"
                + "\"article_sku\":{\"type\":\"keyword\"},"
                + "\"brand\":{\"type\":\"keyword\"},"
                + "\"brand_reference\":{\"type\":\"keyword\"},"
                + "\"category\":{\"type\":\"keyword\"},"
                + "\"ean13\":{\"type\":\"keyword\"},"
                + "\"long_title\":{\"type\":\"text\"},"
                + "\"short_title\":{\"type\":\"text\"},"
                + "\"full_description\":{\"type\":\"text\",\"index\":false},"
                + "\"full_description_text\":{\"type\":\"text\"},"
                + "\"short_description\":{\"type\":\"text\",\"index\":false},"
                + "\"short_description_text\":{\"type\":\"text\"},"
                + "\"warranty\":{\"type\":\"keyword\"}"
                + "}"
                + "}";
    }
}