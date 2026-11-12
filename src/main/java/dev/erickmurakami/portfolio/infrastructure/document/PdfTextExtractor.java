package dev.erickmurakami.portfolio.infrastructure.document;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PdfTextExtractor {

    public String extract(Path path) throws IOException{
        try (var document = Loader.loadPDF(path.toFile())){
            return new PDFTextStripper().getText(document);
        }
    }
}
