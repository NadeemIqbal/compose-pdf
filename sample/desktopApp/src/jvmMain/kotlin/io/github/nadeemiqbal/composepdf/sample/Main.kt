package io.github.nadeemiqbal.composepdf.sample

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.nadeemiqbal.composepdf.PdfPageSize
import io.github.nadeemiqbal.composepdf.pdfDocument
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.Desktop
import java.io.File

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ComposePDF Sample",
        state = rememberWindowState(width = 900.dp, height = 720.dp),
    ) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                SampleScreen()
            }
        }
    }
}

@Composable
private fun SampleScreen() {
    var lastFile by remember { mutableStateOf<File?>(null) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var status by remember { mutableStateOf("Click a button to generate a PDF.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "ComposePDF — Desktop sample",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Each button generates a PDF in your temp directory using the compose-pdf DSL, " +
                "then opens it in your default viewer.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = {
                val file = generateInvoice()
                lastFile = file
                preview = renderFirstPage(file)
                status = "Generated ${file.name} (${file.length() / 1024} KB)"
            }) { Text("Generate invoice") }

            Button(onClick = {
                val file = generateReport()
                lastFile = file
                preview = renderFirstPage(file)
                status = "Generated ${file.name} (${file.length() / 1024} KB)"
            }) { Text("Generate report") }

            Button(
                onClick = {
                    val file = generateCertificate()
                    lastFile = file
                    preview = renderFirstPage(file)
                    status = "Generated ${file.name} (${file.length() / 1024} KB)"
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
            ) { Text("Generate certificate") }

            Button(
                onClick = { lastFile?.let { openInViewer(it) } },
                enabled = lastFile != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            ) { Text("Open in Preview") }
        }

        Spacer(Modifier.height(4.dp))

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Status", fontWeight = FontWeight.SemiBold)
                Text(
                    status,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                )
                lastFile?.let { f ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        f.absolutePath,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Inline preview — render the first page of the last generated PDF back to a bitmap
        // using PDFBox's renderer, so you can see the actual output without leaving the app.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.414f) // landscape A4-ish; preview clips to fit
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF5F5F5))
                .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            preview?.let { bmp ->
                Image(
                    bitmap = bmp,
                    contentDescription = "Preview of generated PDF",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            } ?: Text(
                "Preview will appear here",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Text(
            "All three PDFs are real vector PDFs — text is selectable and copyable in your viewer.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Renders the first page of a PDF back to an [ImageBitmap] using PDFBox's renderer. */
private fun renderFirstPage(file: File): ImageBitmap? = runCatching {
    Loader.loadPDF(file).use { doc ->
        val img = PDFRenderer(doc).renderImageWithDPI(0, 110f)
        img.toComposeImageBitmap()
    }
}.getOrNull()

private fun openInViewer(file: File) {
    // On macOS, force Preview specifically (instead of using Desktop.open which picks the
    // user's default PDF app — that can be Chrome / Firefox / Acrobat and changes per machine).
    // On other OSes, fall back to the platform default.
    runCatching {
        val osName = System.getProperty("os.name")?.lowercase().orEmpty()
        if (osName.contains("mac")) {
            ProcessBuilder("open", "-a", "Preview", file.absolutePath).start()
        } else if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file)
        }
    }
}

private fun generateInvoice(): File {
    val bytes = pdfDocument {
        page(size = PdfPageSize.A4) {
            // Header band
            rect(0f, 0f, pageSize.widthPt, 80f, fillColor = Color(0xFF1565C0))
            text("INVOICE", x = 40f, y = 50f, fontSize = 28f, color = Color.White, bold = true)
            text("# 2026-0042", x = pageSize.widthPt - 130f, y = 50f, fontSize = 14f, color = Color.White)

            // Billed-to block
            text("Billed to:", x = 40f, y = 130f, fontSize = 11f, color = Color(0xFF666666))
            text("Acme Corporation", x = 40f, y = 150f, fontSize = 14f, bold = true)
            text("123 Industrial Way", x = 40f, y = 168f, fontSize = 11f)
            text("Springfield, IL 62704", x = 40f, y = 184f, fontSize = 11f)

            // Date / due block
            text("Issued:", x = pageSize.widthPt - 200f, y = 130f, fontSize = 11f, color = Color(0xFF666666))
            text("May 19, 2026", x = pageSize.widthPt - 200f, y = 150f, fontSize = 12f, bold = true)
            text("Due:", x = pageSize.widthPt - 200f, y = 170f, fontSize = 11f, color = Color(0xFF666666))
            text("June 18, 2026", x = pageSize.widthPt - 200f, y = 190f, fontSize = 12f, bold = true)

            // Line items table
            val tableTop = 240f
            rect(40f, tableTop, pageSize.widthPt - 80f, 28f, fillColor = Color(0xFFE3F2FD))
            text("Description", x = 50f, y = tableTop + 18f, fontSize = 11f, bold = true)
            text("Qty", x = 340f, y = tableTop + 18f, fontSize = 11f, bold = true)
            text("Rate", x = 400f, y = tableTop + 18f, fontSize = 11f, bold = true)
            text("Amount", x = 480f, y = tableTop + 18f, fontSize = 11f, bold = true)

            val items = listOf(
                Triple("Compose Multiplatform consulting", 8 to 150.0, 1200.0),
                Triple("PDF library integration", 4 to 150.0, 600.0),
                Triple("Code review + cleanup", 3 to 100.0, 300.0),
                Triple("Documentation pass", 2 to 100.0, 200.0),
            )
            var y = tableTop + 50f
            for ((desc, qtyRate, total) in items) {
                val (qty, rate) = qtyRate
                text(desc, x = 50f, y = y, fontSize = 11f)
                text(qty.toString(), x = 340f, y = y, fontSize = 11f)
                text("$${rate.toInt()}", x = 400f, y = y, fontSize = 11f)
                text("$${total.toInt()}", x = 480f, y = y, fontSize = 11f)
                y += 22f
            }

            line(40f, y + 4f, pageSize.widthPt - 40f, y + 4f, color = Color(0xFFBBBBBB))

            text("Subtotal", x = 400f, y = y + 26f, fontSize = 11f)
            text("$2,300", x = 480f, y = y + 26f, fontSize = 11f)
            text("Tax (8%)", x = 400f, y = y + 44f, fontSize = 11f)
            text("$184", x = 480f, y = y + 44f, fontSize = 11f)
            text("Total", x = 400f, y = y + 70f, fontSize = 14f, bold = true)
            text("$2,484", x = 480f, y = y + 70f, fontSize = 14f, bold = true, color = Color(0xFF1565C0))

            // Footer
            line(40f, pageSize.heightPt - 60f, pageSize.widthPt - 40f, pageSize.heightPt - 60f, color = Color(0xFFDDDDDD))
            text(
                "Generated with ComposePDF — compose-pdf.io.github.nadeemiqbal",
                x = 40f, y = pageSize.heightPt - 40f, fontSize = 9f, color = Color(0xFF999999),
            )
        }
    }
    return writeAndReturn(bytes, "compose-pdf-invoice.pdf")
}

private fun generateReport(): File {
    val bytes = pdfDocument {
        // Cover page
        page(size = PdfPageSize.A4) {
            rect(0f, 0f, pageSize.widthPt, pageSize.heightPt, fillColor = Color(0xFF0D1B2A))
            text("Q1 2026", x = 40f, y = 180f, fontSize = 16f, color = Color(0xFF90CAF9))
            text("Engineering Report", x = 40f, y = 240f, fontSize = 36f, color = Color.White, bold = true)
            line(40f, 270f, 250f, 270f, color = Color(0xFF90CAF9), strokeWidth = 2f)
            text("ComposePDF — multi-page sample", x = 40f, y = 310f, fontSize = 12f, color = Color(0xFFCCCCCC))
            text(
                "This PDF was generated entirely from a Compose-style DSL —",
                x = 40f, y = pageSize.heightPt - 100f, fontSize = 10f, color = Color(0xFF999999),
            )
            text(
                "no templating, no runtime layout engine, just direct vector commands.",
                x = 40f, y = pageSize.heightPt - 86f, fontSize = 10f, color = Color(0xFF999999),
            )
        }

        // Bar chart page
        page(size = PdfPageSize.A4) {
            text("Tickets shipped per month", x = 40f, y = 60f, fontSize = 18f, bold = true)
            text("Drawn with rect() + line() primitives", x = 40f, y = 82f, fontSize = 10f, color = Color(0xFF666666))

            val chartLeft = 60f
            val chartTop = 130f
            val chartH = 280f
            val barW = 50f
            val gap = 30f

            // Axes
            line(chartLeft, chartTop + chartH, chartLeft + 480f, chartTop + chartH, color = Color.Black)
            line(chartLeft, chartTop, chartLeft, chartTop + chartH, color = Color.Black)

            // Bars
            val data = listOf("Jan" to 18, "Feb" to 22, "Mar" to 31, "Apr" to 28, "May" to 35)
            val maxVal = 40
            for ((i, datum) in data.withIndex()) {
                val (label, value) = datum
                val barH = (value.toFloat() / maxVal) * (chartH - 20f)
                val x = chartLeft + 10f + i * (barW + gap)
                val color = when (i % 3) {
                    0 -> Color(0xFF1976D2)
                    1 -> Color(0xFF388E3C)
                    else -> Color(0xFFE64A19)
                }
                rect(x, chartTop + chartH - barH, barW, barH, fillColor = color)
                text(label, x = x + 12f, y = chartTop + chartH + 18f, fontSize = 11f)
                text(value.toString(), x = x + 14f, y = chartTop + chartH - barH - 6f, fontSize = 10f, bold = true)
            }

            // Body text
            text(
                "May was our strongest month — three shipping milestones converged: " +
                    "the v0.4 cut, the new design system tokens, and the migration to Kotlin 2.3.",
                x = 40f, y = 470f, fontSize = 11f,
            )
            text(
                "Drag and drop velocity improved 18% quarter-over-quarter — see appendix for " +
                    "raw per-engineer breakdowns.",
                x = 40f, y = 495f, fontSize = 11f,
            )
        }

        // Landscape data page
        page(size = PdfPageSize.A4.landscape()) {
            text("Quarterly hiring funnel", x = 40f, y = 50f, fontSize = 18f, bold = true)

            val stages = listOf(
                "Applications" to 412,
                "Phone screens" to 84,
                "Technicals" to 31,
                "Onsites" to 14,
                "Offers" to 8,
                "Hired" to 6,
            )
            val maxN = stages.first().second
            var y = 100f
            for ((stage, count) in stages) {
                val w = (count.toFloat() / maxN) * 600f
                rect(180f, y, w, 26f, fillColor = Color(0xFF1976D2))
                text(stage, x = 40f, y = y + 18f, fontSize = 12f, bold = true)
                text(count.toString(), x = 190f + w, y = y + 18f, fontSize = 11f)
                y += 36f
            }
        }
    }
    return writeAndReturn(bytes, "compose-pdf-report.pdf")
}

private fun generateCertificate(): File {
    val bytes = pdfDocument {
        page(size = PdfPageSize.A4.landscape()) {
            // Outer border
            rect(20f, 20f, pageSize.widthPt - 40f, pageSize.heightPt - 40f,
                strokeColor = Color(0xFFB8860B), strokeWidth = 4f)
            rect(34f, 34f, pageSize.widthPt - 68f, pageSize.heightPt - 68f,
                strokeColor = Color(0xFFD4A017), strokeWidth = 1f)

            // Title
            text("Certificate of Achievement",
                x = (pageSize.widthPt - 360f) / 2f, y = 130f,
                fontSize = 32f, color = Color(0xFFB8860B), bold = true)

            // Subtitle
            line(220f, 160f, pageSize.widthPt - 220f, 160f,
                color = Color(0xFFD4A017), strokeWidth = 1f)
            text("This is to certify that",
                x = (pageSize.widthPt - 130f) / 2f, y = 210f, fontSize = 14f, color = Color(0xFF555555))

            // Recipient name
            text("Nadeem Iqbal",
                x = (pageSize.widthPt - 220f) / 2f, y = 270f, fontSize = 34f, bold = true)

            line(180f, 290f, pageSize.widthPt - 180f, 290f, color = Color(0xFFAAAAAA), strokeWidth = 1f)

            text(
                "has successfully shipped a Compose Multiplatform PDF library to Maven Central",
                x = 100f, y = 340f, fontSize = 13f,
            )
            text(
                "as part of the cmp-libs portfolio Batch 5.",
                x = (pageSize.widthPt - 260f) / 2f, y = 360f, fontSize = 13f,
            )

            // Date + signature
            text("May 19, 2026", x = 100f, y = pageSize.heightPt - 90f, fontSize = 12f)
            line(80f, pageSize.heightPt - 75f, 240f, pageSize.heightPt - 75f, color = Color.Black)
            text("Date", x = 100f, y = pageSize.heightPt - 60f, fontSize = 10f, color = Color(0xFF666666))

            text("ComposePDF", x = pageSize.widthPt - 250f, y = pageSize.heightPt - 90f, fontSize = 12f, bold = true)
            line(pageSize.widthPt - 280f, pageSize.heightPt - 75f, pageSize.widthPt - 80f, pageSize.heightPt - 75f, color = Color.Black)
            text("Signature", x = pageSize.widthPt - 250f, y = pageSize.heightPt - 60f, fontSize = 10f, color = Color(0xFF666666))
        }
    }
    return writeAndReturn(bytes, "compose-pdf-certificate.pdf")
}

private fun writeAndReturn(bytes: ByteArray, name: String): File {
    val dir = File(System.getProperty("java.io.tmpdir"), "compose-pdf-sample")
    dir.mkdirs()
    val file = File(dir, name)
    file.writeBytes(bytes)
    return file
}
