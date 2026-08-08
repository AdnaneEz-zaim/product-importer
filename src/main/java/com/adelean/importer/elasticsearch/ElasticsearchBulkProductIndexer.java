package com.adelean.importer.elasticsearch;

import com.adelean.importer.exception.IndexingException;
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

import com.adelean.importer.exception.IndexingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ElasticsearchBulkProductIndexer
        implements ProductIndexer {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    ElasticsearchBulkProductIndexer.class
            );

    // Bulk callbacks may run asynchronously.
    // Store the first failure so it can be propagated
    // from close() back to the main application flow.
    private final AtomicReference<Throwable> bulkFailure =
            new AtomicReference<>();

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
            throw new IndexingException(
                    "Cannot index product without article_sku"
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
                throw new IndexingException(
                        "Bulk processor did not terminate within 30 seconds"
                );
            }

            Throwable failure =
                    bulkFailure.get();

            // Exceptions thrown inside asynchronous callbacks do not
            // propagate to the main thread, so rethrow the stored failure here.
            if (failure != null) {
                throw new IndexingException(
                        "One or more Elasticsearch bulk requests failed",
                        failure
                );
            }

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IndexingException(
                    "Interrupted while waiting for Elasticsearch bulk requests",
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
                LOGGER.info(
                        "Sending bulk {} containing {} documents",
                        executionId,
                        request.numberOfActions()
                );
            }

            @Override
            public void afterBulk(
                    long executionId,
                    BulkRequest request,
                    BulkResponse response
            ) {
                if (response.hasFailures()) {

                    IndexingException failure =
                            new IndexingException(
                                    "Bulk indexing completed with failures: "
                                            + response.buildFailureMessage()
                            );

                    // Keep the first failure, which is usually the most useful
                    // one for diagnosing the root cause.
                    bulkFailure.compareAndSet(
                            null,
                            failure
                    );

                    LOGGER.error(
                            "Bulk {} completed with failures: {}",
                            executionId,
                            response.buildFailureMessage()
                    );

                    return;
                }

                LOGGER.info(
                        "Bulk {} successfully indexed {} documents",
                        executionId,
                        request.numberOfActions()
                );
            }

            @Override
            public void afterBulk(
                    long executionId,
                    BulkRequest request,
                    Throwable failure
            ) {
                bulkFailure.compareAndSet(
                        null,
                        failure
                );

                LOGGER.error(
                        "Bulk {} failed",
                        executionId,
                        failure
                );
            }
        };
    }
}