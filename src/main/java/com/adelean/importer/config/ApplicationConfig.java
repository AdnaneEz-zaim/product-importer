package com.adelean.importer.config;

import com.adelean.importer.exception.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ApplicationConfig {

    private static final String CONFIG_FILE =
            "application.properties";

    private final Properties properties =
            new Properties();

    public ApplicationConfig() {
        loadProperties();
    }

    public String getElasticsearchHost() {
        return getRequiredProperty(
                "elasticsearch.host"
        );
    }

    public int getElasticsearchPort() {
        String value =
                getRequiredProperty(
                        "elasticsearch.port"
                );

        try {
            return Integer.parseInt(value);

        } catch (NumberFormatException exception) {
            throw new ConfigurationException(
                    "Invalid Elasticsearch port: "
                            + value,
                    exception
            );
        }
    }

    public String getIndexName() {
        return getRequiredProperty(
                "elasticsearch.index"
        );
    }

    private void loadProperties() {
        try (
                InputStream inputStream =
                        ApplicationConfig.class
                                .getClassLoader()
                                .getResourceAsStream(
                                        CONFIG_FILE
                                )
        ) {
            if (inputStream == null) {
                throw new ConfigurationException(
                        "Configuration file not found: "
                                + CONFIG_FILE
                );
            }

            properties.load(inputStream);

        } catch (IOException exception) {
            throw new ConfigurationException(
                    "Unable to load configuration file: "
                            + CONFIG_FILE,
                    exception
            );
        }
    }

    private String getRequiredProperty(
            String key
    ) {
        String value =
                properties.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new ConfigurationException(
                    "Missing configuration property: "
                            + key
            );
        }

        return value.trim();
    }
}