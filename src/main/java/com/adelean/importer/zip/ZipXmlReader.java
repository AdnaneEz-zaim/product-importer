package com.adelean.importer.zip;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipXmlReader {

    public void readXmlEntry(
            Path zipPath,
            XmlInputStreamConsumer consumer
    ) throws Exception {

        Objects.requireNonNull(
                zipPath,
                "zipPath must not be null"
        );

        Objects.requireNonNull(
                consumer,
                "consumer must not be null"
        );

        if (!Files.isRegularFile(zipPath)) {
            throw new IOException(
                    "Invalid ZIP file: " + zipPath
            );
        }

        try (
                InputStream fileInputStream =
                        Files.newInputStream(zipPath);

                ZipInputStream zipInputStream =
                        new ZipInputStream(fileInputStream)
        ) {
            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {

                if (isXmlEntry(entry)) {
                    consumer.accept(zipInputStream);
                    return;
                }

                zipInputStream.closeEntry();
            }
        }

        throw new IOException(
                "No XML file found in ZIP archive: " + zipPath
        );
    }

    private boolean isXmlEntry(ZipEntry entry) {
        return !entry.isDirectory()
                && entry.getName()
                .toLowerCase()
                .endsWith(".xml");
    }

    @FunctionalInterface
    public interface XmlInputStreamConsumer {

        void accept(InputStream inputStream) throws Exception;
    }
}