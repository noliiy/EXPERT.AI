package util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

/**
 * Utility class for handling PDF-related operations using Apache PDFBox.
 * Provides functionality to extract plain text from PDF documents.
 */
public class PdfUtils {

    /**
     * Extracts plain text content from a given PDF file using Apache PDFBox.
     *
     * @param file the PDF file to extract text from
     * @return the extracted text as a string
     * @throws IOException if the file cannot be read or parsed
     */
    public static String extractText(File file) throws IOException {
        // Load the PDF document from the given file
        try (PDDocument document = PDDocument.load(file)) {
            // Create a text stripper to extract text from the document
            PDFTextStripper stripper = new PDFTextStripper();
            // Return the full extracted text
            return stripper.getText(document);
        }
    }
}
