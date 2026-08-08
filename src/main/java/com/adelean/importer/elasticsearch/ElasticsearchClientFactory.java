package com.adelean.importer.elasticsearch;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public final class ElasticsearchClientFactory {

    private ElasticsearchClientFactory() {
    }

    public static RestHighLevelClient create(
            String host,
            int port
    ) {
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(
                                host,
                                port,
                                "http"
                        )
                )
        );
    }
}