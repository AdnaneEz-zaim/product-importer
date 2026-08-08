package com.adelean.importer.zip;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ZipXmlReaderTest {

    @TempDir
    Path temporaryDirectory;

    private final ZipXmlReader zipXmlReader =
            new ZipXmlReader();

    @Test
    void shouldReadXmlEntryFromZipArchive() throws Exception {
        Path zipPath =
                temporaryDirectory.resolve("products.zip");

        String xmlContent =
                "<products><product>"
                        + "<article_sku>SKU-001</article_sku>"
                        + "</product></products>";

        createZip(
                zipPath,
                "products.xml",
                xmlContent
        );

        AtomicReference<String> extractedContent =
                new AtomicReference<>();

        zipXmlReader.readXmlEntry(
                zipPath,
                inputStream ->
                        extractedContent.set(
                                new String(
                                        inputStream.readAllBytes(),
                                        StandardCharsets.UTF_8
                                )
                        )
        );

        assertEquals(
                xmlContent,
                extractedContent.get()
        );
    }

    @Test
    void shouldThrowExceptionWhenZipContainsNoXmlFile()
            throws Exception {

        Path zipPath =
                temporaryDirectory.resolve("products.zip");

        createZip(
                zipPath,
                "products.txt",
                "not xml"
        );

        assertThrows(
                IOException.class,
                () -> zipXmlReader.readXmlEntry(
                        zipPath,
                        inputStream -> {
                        }
                )
        );
    }

    @Test
    void shouldThrowExceptionWhenZipFileDoesNotExist() {

        Path missingZip =
                temporaryDirectory.resolve("missing.zip");

        assertThrows(
                IOException.class,
                () -> zipXmlReader.readXmlEntry(
                        missingZip,
                        inputStream -> {
                        }
                )
        );
    }

    private void createZip(
            Path zipPath,
            String entryName,
            String content
    ) throws IOException {

        try (
                ZipOutputStream output =
                        new ZipOutputStream(
                                Files.newOutputStream(zipPath)
                        )
        ) {
            output.putNextEntry(
                    new ZipEntry(entryName)
            );

            output.write(
                    content.getBytes(StandardCharsets.UTF_8)
            );

            output.closeEntry();
        }
    }
}