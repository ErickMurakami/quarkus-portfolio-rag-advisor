package dev.erickmurakami.portfolio.infrastructure.document;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PdfTextExtractorTest {
    @TempDir Path directory;

    @Test
    void shouldRejectPdfExceedingPageLimit() throws Exception {
        Path file = directory.resolve("synthetic-many-pages.pdf");
        try (var document = new PDDocument()) {
            for (int page = 0; page < 101; page++) document.addPage(new PDPage());
            document.save(file.toFile());
        }
        assertThrows(IllegalArgumentException.class, () -> new PdfTextExtractor().extract(file));
    }
}
