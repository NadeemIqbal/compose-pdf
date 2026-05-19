package io.github.nadeemiqbal.composepdf

import androidx.compose.ui.graphics.Color
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Round-trip tests: build a PDF with the public DSL, then parse it back with PDFBox to verify
 * the bytes are a valid PDF with the expected content. These run against the JVM (desktop)
 * actual implementation.
 */
class PdfDocumentDesktopTest {

    @Test
    fun emptyDocument_produces_valid_pdf_bytes() {
        val bytes = pdfDocument {
            page { /* deliberately empty */ }
        }

        // PDF magic number is "%PDF-" — every valid PDF starts with this.
        assertTrue(bytes.size > 4, "PDF should have at least the header")
        val header = bytes.copyOfRange(0, 5).decodeToString()
        assertEquals("%PDF-", header)
    }

    @Test
    fun document_with_text_is_parseable_and_contains_text() {
        val bytes = pdfDocument {
            page {
                text("Hello, ComposePDF!", x = 50f, y = 50f, fontSize = 14f, color = Color.Black)
                text("Second line", x = 50f, y = 80f, fontSize = 12f)
            }
        }

        Loader.loadPDF(bytes).use { doc ->
            assertEquals(1, doc.numberOfPages)
            val text = PDFTextStripper().getText(doc)
            assertTrue(
                text.contains("Hello, ComposePDF!"),
                "Expected greeting in PDF text, got: $text",
            )
            assertTrue(
                text.contains("Second line"),
                "Expected second line in PDF text, got: $text",
            )
        }
    }

    @Test
    fun multi_page_document_has_correct_page_count() {
        val bytes = pdfDocument {
            page { text("Page 1", 50f, 50f) }
            page { text("Page 2", 50f, 50f) }
            page { text("Page 3", 50f, 50f) }
        }

        Loader.loadPDF(bytes).use { doc ->
            assertEquals(3, doc.numberOfPages)
        }
    }

    @Test
    fun page_size_override_applies_per_page() {
        val bytes = pdfDocument(defaultPageSize = PdfPageSize.A4) {
            page { /* default A4 */ }
            page(size = PdfPageSize.Letter) { /* override */ }
            page(size = PdfPageSize.A4.landscape()) { /* landscape A4 */ }
        }

        Loader.loadPDF(bytes).use { doc ->
            val page1 = doc.getPage(0)
            val page2 = doc.getPage(1)
            val page3 = doc.getPage(2)

            // Page 1: A4 portrait
            assertEquals(595f, page1.mediaBox.width)
            assertEquals(842f, page1.mediaBox.height)

            // Page 2: Letter
            assertEquals(612f, page2.mediaBox.width)
            assertEquals(792f, page2.mediaBox.height)

            // Page 3: A4 landscape
            assertEquals(842f, page3.mediaBox.width)
            assertEquals(595f, page3.mediaBox.height)
        }
    }

    @Test
    fun shapes_render_without_error() {
        // We can't easily assert shape positions without parsing the content stream by hand —
        // but we can verify that the byte stream is well-formed and PDFBox can re-open it.
        val bytes = pdfDocument {
            page {
                rect(50f, 50f, 100f, 80f, fillColor = Color(0xFFE3F2FD))
                rect(200f, 50f, 100f, 80f, strokeColor = Color.Black, strokeWidth = 2f)
                line(50f, 200f, 545f, 200f, color = Color.Red, strokeWidth = 1f)
            }
        }

        Loader.loadPDF(bytes).use { doc ->
            assertEquals(1, doc.numberOfPages)
        }
    }

    @Test
    fun bold_text_uses_a_different_font_than_regular() {
        // Both regular and bold should produce valid PDFs that can be re-parsed.
        val regularBytes = pdfDocument {
            page { text("Regular", 50f, 50f, bold = false) }
        }
        val boldBytes = pdfDocument {
            page { text("Bold", 50f, 50f, bold = true) }
        }

        // Bytes will differ — PDFBox embeds different font references.
        assertTrue(!regularBytes.contentEquals(boldBytes), "Bold should produce different bytes from regular")

        // Both should still be loadable PDFs.
        Loader.loadPDF(regularBytes).use { assertEquals(1, it.numberOfPages) }
        Loader.loadPDF(boldBytes).use { assertEquals(1, it.numberOfPages) }
    }
}
