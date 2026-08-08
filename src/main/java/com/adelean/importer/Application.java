package com.adelean.importer;

import com.adelean.importer.config.ApplicationConfig;
import com.adelean.importer.elasticsearch.ElasticsearchBulkProductIndexer;
import com.adelean.importer.elasticsearch.ElasticsearchClientFactory;
import com.adelean.importer.elasticsearch.ProductIndexManager;
import com.adelean.importer.exception.ConfigurationException;
import com.adelean.importer.exception.IndexingException;
import com.adelean.importer.exception.XmlParsingException;
import com.adelean.importer.indexing.ProductIndexer;
import com.adelean.importer.parser.ProductParser;
import com.adelean.importer.parser.StaxProductParser;
import com.adelean.importer.service.ProductImportService;
import com.adelean.importer.transform.ProductDocumentTransformer;
import com.adelean.importer.zip.ZipXmlReader;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class Application {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        try {
            run(args);

        } catch (ConfigurationException exception) {
            LOGGER.error(
                    "Configuration error: {}",
                    exception.getMessage()
            );
            System.exit(2);

        } catch (XmlParsingException exception) {
            LOGGER.error(
                    "XML parsing error: {}",
                    exception.getMessage(),
                    exception
            );
            System.exit(3);

        } catch (IndexingException exception) {
            LOGGER.error(
                    "Elasticsearch indexing error: {}",
                    exception.getMessage(),
                    exception
            );
            System.exit(4);

        } catch (IllegalArgumentException exception) {
            LOGGER.error(
                    "Invalid argument: {}",
                    exception.getMessage()
            );
            System.exit(5);

        } catch (Exception exception) {
            LOGGER.error(
                    "Unexpected import error",
                    exception
            );
            System.exit(1);
        }
    }

    private static void run(String[] args) throws Exception {

        Path zipPath =
                validateArguments(args);

        ApplicationConfig config =
                new ApplicationConfig();

        LOGGER.info(
                "Starting product import from {}",
                zipPath
        );

        LOGGER.info(
                "Elasticsearch target: {}:{} / {}",
                config.getElasticsearchHost(),
                config.getElasticsearchPort(),
                config.getIndexName()
        );

        try (
                RestHighLevelClient client =
                        ElasticsearchClientFactory.create(
                                config.getElasticsearchHost(),
                                config.getElasticsearchPort()
                        )
        ) {

            ProductIndexManager indexManager =
                    new ProductIndexManager(
                            client,
                            config.getIndexName()
                    );

            indexManager.createIndexIfMissing();

            ProductParser parser =
                    new StaxProductParser();

            ZipXmlReader zipXmlReader =
                    new ZipXmlReader();

            ProductDocumentTransformer transformer =
                    new ProductDocumentTransformer();

            try (
                    ProductIndexer indexer =
                            new ElasticsearchBulkProductIndexer(
                                    client,
                                    config.getIndexName()
                            )
            ) {

                ProductImportService importService =
                        new ProductImportService(
                                zipXmlReader,
                                parser,
                                transformer,
                                indexer
                        );

                importService.importProducts(
                        zipPath
                );

                LOGGER.info(
                        "Parsing completed. Waiting for pending bulk requests."
                );
            }
        }

        LOGGER.info(
                "Product import completed successfully"
        );
    }

    private static Path validateArguments(
            String[] args
    ) {

        if (args == null || args.length != 1) {
            throw new IllegalArgumentException(
                    "Usage: java -jar product-importer.jar <path-to-xml.zip>"
            );
        }

        Path zipPath =
                Path.of(args[0]);

        if (!Files.exists(zipPath)) {
            throw new IllegalArgumentException(
                    "ZIP file does not exist: "
                            + zipPath
            );
        }

        if (!Files.isRegularFile(zipPath)) {
            throw new IllegalArgumentException(
                    "Invalid ZIP file: "
                            + zipPath
            );
        }

        return zipPath;
    }
}