package io.github.nadeemiqbal.composepdf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PdfPageSizeTest {

    @Test
    fun a4_has_standard_iso_dimensions() {
        // ISO 216 A4 is 210mm × 297mm = 595.276... × 841.890... points. The constants are rounded
        // to whole points (PDFBox / Acrobat / PDF.js all use 595×842).
        assertEquals(595f, PdfPageSize.A4.widthPt)
        assertEquals(842f, PdfPageSize.A4.heightPt)
    }

    @Test
    fun letter_size_matches_us_standard() {
        assertEquals(612f, PdfPageSize.Letter.widthPt)
        assertEquals(792f, PdfPageSize.Letter.heightPt)
    }

    @Test
    fun landscape_swaps_width_and_height() {
        val portrait = PdfPageSize.A4
        val landscape = portrait.landscape()
        assertEquals(portrait.heightPt, landscape.widthPt)
        assertEquals(portrait.widthPt, landscape.heightPt)
    }

    @Test
    fun landscape_is_involution() {
        // Two landscape() calls should bring us back to the original portrait.
        val original = PdfPageSize.Letter
        val doubleFlipped = original.landscape().landscape()
        assertEquals(original, doubleFlipped)
    }

    @Test
    fun mm_factory_converts_to_points() {
        // 210mm × 297mm should produce A4 (within a fraction of a point).
        val a4 = PdfPageSize.mm(210f, 297f)
        assertTrue(kotlin.math.abs(a4.widthPt - 595f) < 1f, "width was ${a4.widthPt}, expected ~595")
        assertTrue(kotlin.math.abs(a4.heightPt - 842f) < 1f, "height was ${a4.heightPt}, expected ~842")
    }

    @Test
    fun inches_factory_converts_to_points() {
        // 8.5" × 11" should produce Letter exactly (72 pt/inch is exact).
        val letter = PdfPageSize.inches(8.5f, 11f)
        assertEquals(612f, letter.widthPt)
        assertEquals(792f, letter.heightPt)
    }

    @Test
    fun all_predefined_sizes_are_portrait_by_default() {
        // Sanity: every named constant should have height >= width (portrait orientation).
        val sizes = listOf(
            "A3" to PdfPageSize.A3,
            "A4" to PdfPageSize.A4,
            "A5" to PdfPageSize.A5,
            "Letter" to PdfPageSize.Letter,
            "Legal" to PdfPageSize.Legal,
            "Tabloid" to PdfPageSize.Tabloid,
        )
        for ((name, size) in sizes) {
            assertTrue(
                size.heightPt >= size.widthPt,
                "$name should be portrait (height >= width), got ${size.widthPt}x${size.heightPt}",
            )
        }
    }
}
