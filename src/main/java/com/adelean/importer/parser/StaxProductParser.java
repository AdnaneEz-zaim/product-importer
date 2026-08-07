package com.adelean.importer.parser;

import com.adelean.importer.exception.XmlParsingException;
import com.adelean.importer.model.ProductDocument;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class StaxProductParser implements ProductParser {

    private static final String PRODUCT_ELEMENT = "product";

    @Override
    public void parse(
            InputStream inputStream,
            Consumer<ProductDocument> productConsumer
    ) {
        Objects.requireNonNull(
                inputStream,
                "inputStream must not be null"
        );

        Objects.requireNonNull(
                productConsumer,
                "productConsumer must not be null"
        );

        XMLStreamReader reader = null;

        try {
            XMLInputFactory factory =
                    createSecureXmlInputFactory();

            reader = factory.createXMLStreamReader(
                    inputStream
            );

            parseProducts(
                    reader,
                    productConsumer
            );

        } catch (XMLStreamException exception) {
            throw new XmlParsingException(
                    "Unable to parse XML product file",
                    exception
            );

        } finally {
            closeReader(reader);
        }
    }

    private void parseProducts(
            XMLStreamReader reader,
            Consumer<ProductDocument> productConsumer
    ) throws XMLStreamException {

        Map<String, String> productFields = null;
        String currentFieldName = null;
        StringBuilder currentFieldValue = null;

        while (reader.hasNext()) {

            int event = reader.next();

            if (event == XMLStreamConstants.START_ELEMENT) {

                String elementName =
                        reader.getLocalName();

                if (PRODUCT_ELEMENT.equals(elementName)) {

                    productFields =
                            new LinkedHashMap<>();

                } else if (productFields != null) {

                    currentFieldName =
                            elementName;

                    currentFieldValue =
                            new StringBuilder();
                }

            } else if (
                    event == XMLStreamConstants.CHARACTERS
                            || event == XMLStreamConstants.CDATA
            ) {

                if (currentFieldValue != null) {
                    currentFieldValue.append(
                            reader.getText()
                    );
                }

            } else if (
                    event == XMLStreamConstants.END_ELEMENT
            ) {

                String elementName =
                        reader.getLocalName();

                if (
                        productFields != null
                                && currentFieldName != null
                                && currentFieldName.equals(
                                elementName
                        )
                ) {

                    productFields.put(
                            currentFieldName,
                            currentFieldValue
                                    .toString()
                                    .trim()
                    );

                    currentFieldName = null;
                    currentFieldValue = null;

                } else if (
                        PRODUCT_ELEMENT.equals(elementName)
                                && productFields != null
                ) {

                    productConsumer.accept(
                            new ProductDocument(
                                    productFields
                            )
                    );

                    productFields = null;
                }
            }
        }
    }

    private XMLInputFactory createSecureXmlInputFactory() {

        XMLInputFactory factory =
                XMLInputFactory.newFactory();

        factory.setProperty(
                XMLInputFactory.SUPPORT_DTD,
                false
        );

        factory.setProperty(
                "javax.xml.stream.isSupportingExternalEntities",
                false
        );

        return factory;
    }

    private void closeReader(
            XMLStreamReader reader
    ) {

        if (reader == null) {
            return;
        }

        try {
            reader.close();
        } catch (XMLStreamException ignored) {}
    }
}