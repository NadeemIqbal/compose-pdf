package io.github.nadeemiqbal.composepdf

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * JVM implementation of [pdfDocument]. Uses Apache PDFBox under the hood — produces real PDFs
 * with selectable text, vector shapes, and embedded raster images.
 *
 * Note on coordinates: the public [PdfPageScope] API has y increasing downward (matching
 * Compose). PDFBox's native PDF coordinates have y increasing upward from the bottom-left. We
 * convert inside each draw call so the consumer never has to think about it.
 */
actual fun pdfDocument(
    defaultPageSize: PdfPageSize,
    block: PdfDocumentScope.() -> Unit,
): ByteArray {
    val doc = PDDocument()
    try {
        val scope = PdfBoxDocumentScope(doc, defaultPageSize)
        scope.block()
        val out = ByteArrayOutputStream()
        doc.save(out)
        return out.toByteArray()
    } finally {
        doc.close()
    }
}

private class PdfBoxDocumentScope(
    private val doc: PDDocument,
    private val defaultPageSize: PdfPageSize,
) : PdfDocumentScope {
    override fun page(size: PdfPageSize?, block: PdfPageScope.() -> Unit) {
        val resolved = size ?: defaultPageSize
        val page = PDPage(PDRectangle(resolved.widthPt, resolved.heightPt))
        doc.addPage(page)
        PDPageContentStream(doc, page).use { stream ->
            val scope = PdfBoxPageScope(doc = doc, stream = stream, pageSize = resolved)
            scope.block()
        }
    }
}

private class PdfBoxPageScope(
    private val doc: PDDocument,
    private val stream: PDPageContentStream,
    override val pageSize: PdfPageSize,
) : PdfPageScope {

    /** Flip a "Compose y" (top-down) to PDFBox y (bottom-up). */
    private fun flipY(y: Float): Float = pageSize.heightPt - y

    override fun text(text: String, x: Float, y: Float, fontSize: Float, color: Color, bold: Boolean) {
        val font: PDType1Font = if (bold) {
            PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
        } else {
            PDType1Font(Standard14Fonts.FontName.HELVETICA)
        }
        stream.setFont(font, fontSize)
        stream.setNonStrokingColor(color.toAwt())
        stream.beginText()
        // PDFBox y is bottom-up. Our API has (x, y) at the baseline (top-down y). Convert.
        stream.newLineAtOffset(x, flipY(y))
        stream.showText(text)
        stream.endText()
    }

    override fun rect(
        x: Float, y: Float, width: Float, height: Float,
        fillColor: Color?, strokeColor: Color?, strokeWidth: Float, cornerRadius: Float,
    ) {
        // cornerRadius support requires manually drawing rounded corners with curve segments;
        // v0.1 ignores cornerRadius and treats it as 0 (sharp corners). Add curve math in v0.2.
        if (fillColor != null) {
            stream.setNonStrokingColor(fillColor.toAwt())
            stream.addRect(x, flipY(y) - height, width, height)
            stream.fill()
        }
        if (strokeColor != null) {
            stream.setStrokingColor(strokeColor.toAwt())
            stream.setLineWidth(strokeWidth)
            stream.addRect(x, flipY(y) - height, width, height)
            stream.stroke()
        }
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color, strokeWidth: Float) {
        stream.setStrokingColor(color.toAwt())
        stream.setLineWidth(strokeWidth)
        stream.moveTo(x1, flipY(y1))
        stream.lineTo(x2, flipY(y2))
        stream.stroke()
    }

    override fun image(bitmap: ImageBitmap, x: Float, y: Float, width: Float?, height: Float?) {
        val awt: BufferedImage = bitmap.toAwtImage()
        // Encode losslessly — gives selectable in-PDF images without re-compression artifacts.
        val pdImage: PDImageXObject = if (hasAlpha(awt)) {
            LosslessFactory.createFromImage(doc, awt)
        } else {
            // Re-encode opaque images as JPEG to keep file size sane on large bitmaps.
            JPEGFactory.createFromImage(doc, awt, 0.85f)
        }
        val w = width ?: awt.width.toFloat()
        val h = height ?: awt.height.toFloat()
        stream.drawImage(pdImage, x, flipY(y) - h, w, h)
    }
}

private fun hasAlpha(image: BufferedImage): Boolean = image.colorModel.hasAlpha()

private fun Color.toAwt(): java.awt.Color = java.awt.Color(
    (red * 255f).toInt().coerceIn(0, 255),
    (green * 255f).toInt().coerceIn(0, 255),
    (blue * 255f).toInt().coerceIn(0, 255),
    (alpha * 255f).toInt().coerceIn(0, 255),
)

@Suppress("unused")
private fun ensureImageIOImported() = ImageIO.getReaderFormatNames()
