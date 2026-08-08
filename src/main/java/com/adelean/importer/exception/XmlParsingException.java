package com.adelean.importer.exception;

public class XmlParsingException extends RuntimeException {

    public XmlParsingException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}