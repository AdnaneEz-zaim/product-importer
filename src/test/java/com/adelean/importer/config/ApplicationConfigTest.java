package com.adelean.importer.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationConfigTest {

    @Test
    void shouldLoadElasticsearchConfiguration() {

        ApplicationConfig config =
                new ApplicationConfig();

        assertEquals(
                "localhost",
                config.getElasticsearchHost()
        );

        assertEquals(
                9200,
                config.getElasticsearchPort()
        );

        assertEquals(
                "products",
                config.getIndexName()
        );
    }
}