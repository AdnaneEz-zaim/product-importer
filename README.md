# Product Importer

Java application that imports products from a ZIP-compressed XML file
into Elasticsearch 7.4.1.

## Requirements

- Java 11
- Maven 3.8+
- Elasticsearch 7.4.1

## Build

```
mvn clean package
```

## Run

Start Elasticsearch first, then run:
```
java -jar target/product-importer-1.0-SNAPSHOT.jar data/xml.zip
```
## Configuration

Configuration is located in:
```
src/main/resources/application.properties
```
Default values:
```
elasticsearch.host=localhost
elasticsearch.port=9200
elasticsearch.index=products
```

## Architecture

The import pipeline is:
```
ZIP archive
↓
ZipXmlReader
↓
ProductParser
↓
StaxProductParser
↓
ProductDocument
↓
ProductDocumentTransformer
↓
ProductIndexer
↓
ElasticsearchBulkProductIndexer
↓
Elasticsearch

```

## Streaming strategy

The input XML file is assumed to potentially be very large.

The application therefore uses:

- ZipInputStream to read the ZIP archive;
- StAX to parse the XML incrementally;
- a Consumer<ProductDocument> to process products one by one;
- Elasticsearch BulkProcessor to send documents in batches.

The application does not load the complete XML file or the complete
product catalog into memory.

## Product model

Products do not all contain the same fields.

For this reason, ProductDocument stores product properties in a:
```
 Map<String, String>
```
This allows the importer to support heterogeneous product attributes.

``article_sku`` is used as the Elasticsearch document ``_id``.

## HTML descriptions

The original HTML descriptions are preserved in Elasticsearch:
```
full_description
short_description
```
but these fields are configured with:
```
"index": false
```
They remain available in ``_source`` for possible rendering but are not
used for full-text search.

Plain-text searchable fields are generated with Jsoup:

```
full_description_text
short_description_text
```
Example:
```html
<p>Une <strong>batterie</strong> puissante</p>
```
becomes:
```
Une batterie puissante
```
## Search behavior
### Raw HTML field

The raw HTML description is intentionally not searchable.

Example PowerShell query:

```
$body = @{
    query = @{
        match = @{
            full_description = "batterie"
        }
    }
} | ConvertTo-Json -Depth 5

Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:9200/products/_search" `
    -ContentType "application/json" `
    -Body $body
```
Expected Elasticsearch error:
```
Cannot search on field [full_description] since it is not indexed.
```
### Searchable text field

Use ``full_description_text`` instead:
```
$body = @{
    query = @{
        match = @{
            full_description_text = "batterie"
        }
    }
} | ConvertTo-Json -Depth 5

$response = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:9200/products/_search" `
    -ContentType "application/json" `
    -Body $body
```
Example result from the provided dataset:
```
Nexus 6P 64 Go argent
```
## Elasticsearch mapping

Important fields are explicitly mapped:
```
article_sku             keyword
brand                   keyword
brand_reference         keyword
category                keyword
ean13                   keyword

long_title              text
short_title             text

full_description        stored, not indexed
short_description       stored, not indexed

full_description_text   text
short_description_text  text
```
Other product-specific attributes are handled by Elasticsearch dynamic
mapping.

## Bulk indexing

Products are sent using Elasticsearch ``BulkProcessor``.

The processor flushes when one of these conditions is reached:
- 500 documents;
- 5 MB;
- 5 seconds.

A retry policy with exponential backoff is configured for temporary
Elasticsearch pressure.

Bulk callbacks may execute asynchronously. The first bulk failure is
therefore stored in an ``AtomicReference`` and propagated back to the main
application flow when the indexer is closed.

## Error handling

The application distinguishes several error categories:
```
ConfigurationException
XmlParsingException
IndexingException
```
The command-line entry point translates these failures into different
exit codes.

## Tests

Run:
```
mvn clean test
```
Tests cover:

- ZIP XML extraction;
- product model;
- StAX parsing;
- CDATA handling;
- empty XML fields;
- HTML transformation;
- configuration loading;
- complete import pipeline using an in-memory ProductIndexer.

The pipeline test does not require Elasticsearch.

## Verify imported documents

Count documents:
```
curl http://localhost:9200/products/_count
```
Expected with the provided XML:
```
{"count":3}
```
Inspect the mapping:
```
curl "http://localhost:9200/products/_mapping?pretty"
```
## Technical choices
- Java 11
- Maven
- StAX
- Elasticsearch 7.4.1 High Level REST Client
- Elasticsearch BulkProcessor
- Jsoup
- JUnit 5
- SLF4J