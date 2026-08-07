package com.adelean.importer.elasticsearch;

import com.adelean.importer.indexing.ProductIndexer;
import com.adelean.importer.model.ProductDocument;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class ElasticsearchBulkProductIndexer
        implements ProductIndexer {

    private final String indexName;
    private final BulkProcessor bulkProcessor;

    public ElasticsearchBulkProductIndexer(
            RestHighLevelClient client,
            String indexName
    ) {
        Objects.requireNonNull(
                client,
                "client must not be null"
        );

        if (indexName == null || indexName.isBlank()) {
            throw new IllegalArgumentException(
                    "indexName must not be empty"
            );
        }

        this.indexName = indexName;

        this.bulkProcessor = BulkProcessor.builder(
                        (request, listener) ->
                                client.bulkAsync(
                                        request,
                                        RequestOptions.DEFAULT,
                                        listener
                                ),
                        createListener()
                )
                .setBulkActions(500)
                .setBulkSize(
                        new ByteSizeValue(
                                5,
                                ByteSizeUnit.MB
                        )
                )
                .setFlushInterval(
                        TimeValue.timeValueSeconds(5)
                )
                .setConcurrentRequests(1)
                .setBackoffPolicy(
                        BackoffPolicy.exponentialBackoff(
                                TimeValue.timeValueMillis(200),
                                3
                        )
                )
                .build();
    }

    @Override
    public void index(ProductDocument product) {
        Objects.requireNonNull(
                product,
                "product must not be null"
        );

        String articleSku =
                product.getArticleSku();

        if (articleSku == null || articleSku.isBlank()) {
            throw new IllegalArgumentException(
                    "Product article_sku must not be empty"
            );
        }

        IndexRequest request =
                new IndexRequest(indexName)
                        .id(articleSku)
                        .source(product.getFields());

        bulkProcessor.add(request);
    }

    @Override
    public void close() {
        try {
            boolean completed =
                    bulkProcessor.awaitClose(
                            30,
                            TimeUnit.SECONDS
                    );

            if (!completed) {
                throw new IllegalStateException(
                        "Bulk processor did not terminate within 30 seconds"
                );
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Interrupted while waiting for bulk requests",
                    exception
            );
        }
    }

    private BulkProcessor.Listener createListener() {
        return new BulkProcessor.Listener() {

            @Override
            public void beforeBulk(
                    long executionId,
                    BulkRequest request
            ) {
                // Logging (pour apres
            }

            @Override
            public void afterBulk(
                    long executionId,
                    BulkRequest request,
                    BulkResponse response
            ) {
                if (response.hasFailures()) {
                    throw new IllegalStateException(
                            "Bulk indexing completed with failures: "
                                    + response.buildFailureMessage()
                    );
                }
            }

            @Override
            public void afterBulk(
                    long executionId,
                    BulkRequest request,
                    Throwable failure
            ) {
                throw new IllegalStateException(
                        "Bulk indexing failed",
                        failure
                );
            }
        };
    }
}