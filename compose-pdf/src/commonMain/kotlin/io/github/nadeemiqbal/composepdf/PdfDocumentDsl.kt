package io.github.nadeemiqbal.composepdf

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Top-level builder for a multi-page PDF document. Inside the block you call [PdfDocumentScope.page]
 * for every page you want to add — each page gets its own [PdfPageScope] where you draw text,
 * shapes, lines, and images.
 *
 * Returns the final PDF document as a `ByteArray` — write to a file with
 * `File("out.pdf").writeBytes(bytes)` on Desktop, or pipe to wherever your platform expects.
 *
 * Example:
 * ```
 * val pdf = pdfDocument {
 *     page {
 *         text("Invoice #12345", x = 50f, y = 50f, fontSize = 24f, color = Color.Black)
 *         line(50f, 80f, 545f, 80f)
 *         text("Total: $1,234.56", x = 50f, y = 200f, fontSize = 16f)
 *     }
 *     page(size = PdfPageSize.A4.landscape()) {
 *         rect(50f, 50f, 200f, 100f, fillColor = Color(0xFFE3F2FD))
 *     }
 * }
 * File("invoice.pdf").writeBytes(pdf)
 * ```
 *
 * The actual rendering goes through Skia's PDF backend, producing real vector PDFs with
 * selectable text on every CMP target where Skia is available (Desktop, iOS, Wasm).
 */
expect fun pdfDocument(
    defaultPageSize: PdfPageSize = PdfPageSize.A4,
    block: PdfDocumentScope.() -> Unit,
): ByteArray

/**
 * Scope for adding pages to a [pdfDocument]. Pages render in the order you add them.
 */
interface PdfDocumentScope {
    /**
     * Add a page to the document. The [block] runs against a [PdfPageScope] that lets you draw
     * onto this page. Coordinates are in PDF points (1 pt = 1/72 inch), with `(0, 0)` at the
     * top-left and y increasing downward — matching how Compose composables flow.
     */
    fun page(size: PdfPageSize? = null, block: PdfPageScope.() -> Unit)
}

/**
 * Scope for drawing onto a single PDF page. All drawing operations are immediate — no layout
 * pass, just direct vector commands written to the PDF stream.
 *
 * Coordinates: origin is top-left, y increases downward (matching Compose, not raw Skia).
 */
interface PdfPageScope {

    /** The page's dimensions in PDF points, set when the page was created. */
    val pageSize: PdfPageSize

    /**
     * Draw a text run at the given coordinates. The point `(x, y)` is the **baseline** of the
     * first glyph (Skia convention). For top-aligned drawing, add `fontSize` to `y`.
     */
    fun text(
        text: String,
        x: Float,
        y: Float,
        fontSize: Float = 12f,
        color: Color = Color.Black,
        bold: Boolean = false,
    )

    /** Draw a filled / stroked rectangle. Pass `null` to either to skip that pass. */
    fun rect(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        fillColor: Color? = null,
        strokeColor: Color? = null,
        strokeWidth: Float = 1f,
        cornerRadius: Float = 0f,
    )

    /** Draw a single line from `(x1, y1)` to `(x2, y2)`. */
    fun line(
        x1: Float, y1: Float, x2: Float, y2: Float,
        color: Color = Color.Black,
        strokeWidth: Float = 1f,
    )

    /**
     * Embed an image. The bitmap is encoded into the PDF as PNG (lossless). If [width] / [height]
     * are `null`, the image renders at its natural pixel size (1px == 1pt).
     */
    fun image(
        bitmap: ImageBitmap,
        x: Float,
        y: Float,
        width: Float? = null,
        height: Float? = null,
    )
}
