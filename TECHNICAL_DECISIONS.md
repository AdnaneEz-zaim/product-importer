# Technical Decisions

This document describes the main technical decisions made during the
implementation of the Product Importer.

The goal was to keep the solution simple while taking into account that
the input catalog may become large and that product structures may vary.

## 1. Why StAX for XML parsing?

The input XML file may potentially contain a large number of products.

Loading the entire XML document into memory using a DOM-based parser would
make memory consumption grow with the size of the catalog.

StAX was chosen because it provides a streaming API: the XML document is
read sequentially and products can be processed as they are encountered.

The processing flow is therefore:

```text
XML stream
    ↓
StAX parser
    ↓
one product
    ↓
consumer
    ↓
next product
```
This keeps memory usage relatively stable and makes the importer more
appropriate for large files.

It also avoids building an intermediate list containing the complete
catalog before indexing.

## 2. Why Elasticsearch BulkProcessor?

Sending one Elasticsearch request for every product would introduce
unnecessary network overhead.

The Elasticsearch ``BulkProcessor`` allows several indexing operations to be
grouped into bulk requests.

The importer can therefore continue parsing products while indexing
operations are handled in batches.

The processor is configured to flush according to limits such as:

- number of documents;
- accumulated bulk size;
- elapsed time.

A retry policy with exponential backoff is also configured to better handle
temporary Elasticsearch pressure.

This provides a good compromise between throughput, memory usage and
implementation complexity.

## 3. Why AtomicReference for bulk failures?

``BulkProcessor`` callbacks may execute asynchronously.

An exception occurring inside an ``afterBulk`` callback therefore cannot simply
be thrown and expected to propagate to the main import flow.

The first asynchronous failure is stored in an:
````
AtomicReference<Throwable>
````
The failure is then checked when the indexer is closed, after waiting for
pending bulk operations to complete.
````
Bulk callback
↓
failure occurs
↓
AtomicReference
↓
close()
↓
IndexingException
↓
Application
````
``AtomicReference`` provides the thread-safe communication required between the
callback and the main application flow.

Only the first failure is retained because it is generally the most useful
one for identifying the original cause of the indexing problem.
## 4. Why Map<String, String> for product attributes?

The products contained in the catalog do not necessarily expose exactly the
same attributes.

For example, a smartphone may contain technical properties that do not exist
for another type of product.

Creating a Java field for every possible XML property would tightly couple
the application model to the current catalog structure.

Product properties are therefore represented using:
````
Map<String, String>
````
This makes the importer tolerant of heterogeneous and evolving product
attributes.

New product-specific properties can be imported without requiring a new Java
field for each one.

Important Elasticsearch fields still receive an explicit mapping, while
additional product-specific attributes can rely on Elasticsearch dynamic
mapping.

## 5. Why keep both HTML and plain-text descriptions?

The original catalog contains HTML inside description fields.

The original value is ``useful`` because another consumer may need it later to
render the product description.

However, HTML markup does not provide useful information for full-text
product search.

The importer therefore keeps two representations.

### Original representation
````
full_description
short_description
````

These fields preserve the original content but are configured with:
````
"index": false
````
They remain available from Elasticsearch ``_source``, but they are not
searchable.
### Search representation
````
full_description_text
short_description_text
````

These fields contain plain text generated from the HTML using Jsoup and are
indexed as ``text``.

For example:
````
<p>Une <strong>batterie</strong> puissante</p>
````
becomes:
````
Une batterie puissante
````
This separates two different concerns:
````
original HTML → representation / rendering
clean text    → full-text search
````
The behavior can also be verified directly against Elasticsearch: querying
``full_description`` is rejected because the field is not indexed, while the
same query against ``full_description_text`` returns matching products.

## 6. Why ProductParser and ProductIndexer interfaces?

Parsing and indexing are represented by abstractions:
````
ProductParser
ProductIndexer
````
The current implementations are:
````
ProductParser
└── StaxProductParser
ProductIndexer
└── ElasticsearchBulkProductIndexer
````

This keeps the orchestration logic independent from the concrete
implementation.

For example, another parser implementation could later be introduced without
changing the import workflow.

Similarly, the indexing destination could evolve without coupling the import
service directly to Elasticsearch.

The interfaces also improve testability. The complete import pipeline can be
tested with an in-memory ``ProductIndexer`` instead of requiring a running
Elasticsearch instance.

The objective is not to anticipate every possible future implementation, but
to isolate the two main technical boundaries of the application: reading
products and indexing them.

## 7. Why ProductImportService?

The application needs to coordinate several independent responsibilities:
````
ZIP reading
↓
product parsing
↓
document transformation
↓
indexing
````
Putting this complete workflow directly inside ``Application`` would make the
entry point responsible for both infrastructure setup and business
orchestration.

``ProductImportService`` therefore owns the import workflow.

``Application`` is mainly responsible for assembling the concrete
implementations:
````
Application
↓
configuration
↓
Elasticsearch client
↓
concrete implementations
↓
ProductImportService
````
while ``ProductImportService`` coordinates the actual import:
````
ProductImportService
↓
ZipXmlReader
↓
ProductParser
↓
ProductDocumentTransformer
↓
ProductIndexer
````
This separation keeps the entry point small, makes the workflow easier to
understand and allows the import pipeline to be tested independently from
the command-line application.

## Summary

The main design goal is to keep the importer simple while preserving clear
boundaries between responsibilities.

The resulting pipeline is:
````
ZIP
│
▼
ZipXmlReader
│
▼
ProductParser
│
└── StaxProductParser
│
▼
ProductDocument
│
▼
ProductDocumentTransformer
│
▼
ProductIndexer
│
└── ElasticsearchBulkProductIndexer
│
▼
Elasticsearch
````
The main decisions support three objectives:

- Scalability: streaming XML parsing and bulk indexing.
- Extensibility: interfaces and flexible product attributes.
- Maintainability and testability: separated responsibilities and an
independently testable import workflow.