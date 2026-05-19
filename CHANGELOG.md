# Changelog

## 0.1.0 — 2026-05-19

Initial release.

- `pdfDocument { page { ... } }` Compose-style builder DSL returning `ByteArray`.
- `PdfPageSize` with predefined `A3`/`A4`/`A5`/`Letter`/`Legal`/`Tabloid` constants plus
  `landscape()` and custom `mm()` / `inches()` factories.
- `PdfPageScope` drawing primitives: `text(...)`, `rect(...)`, `line(...)`, `image(...)`.
- Top-down coordinate system (matches Compose, not raw PDFBox).
- Selectable text + vector output (no rasterisation).
- Image embedding: lossless for alpha images, JPEG for opaque (keeps file sizes sane).
- **Platforms:** Desktop / JVM 11 (Apache PDFBox 3.0.5 backend).
- **v0.2 roadmap:** iOS, Web (wasmJs), Android — pending public Skiko `PDFDocument` API.
