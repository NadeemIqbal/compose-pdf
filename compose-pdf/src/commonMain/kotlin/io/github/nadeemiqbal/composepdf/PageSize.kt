package io.github.nadeemiqbal.composepdf

/**
 * Physical page size in PDF points (1 pt = 1/72 inch = ~0.3528 mm).
 *
 * Use the predefined constants on the companion object — `PdfPageSize.A4`, `.Letter`, etc. —
 * or build a custom size via [PdfPageSize.mm].
 */
data class PdfPageSize(val widthPt: Float, val heightPt: Float) {

    /** Returns this size rotated 90° (i.e. swaps width/height). */
    fun landscape(): PdfPageSize = PdfPageSize(widthPt = heightPt, heightPt = widthPt)

    companion object {
        // ISO 216 series (portrait by default).
        val A3 = PdfPageSize(842f, 1191f)
        val A4 = PdfPageSize(595f, 842f)
        val A5 = PdfPageSize(420f, 595f)

        // US sizes.
        val Letter = PdfPageSize(612f, 792f)
        val Legal = PdfPageSize(612f, 1008f)
        val Tabloid = PdfPageSize(792f, 1224f)

        /** Custom size in millimetres. 1 mm = 72/25.4 points. */
        fun mm(widthMm: Float, heightMm: Float): PdfPageSize {
            val k = 72f / 25.4f
            return PdfPageSize(widthMm * k, heightMm * k)
        }

        /** Custom size in inches. */
        fun inches(widthIn: Float, heightIn: Float): PdfPageSize =
            PdfPageSize(widthIn * 72f, heightIn * 72f)
    }
}
