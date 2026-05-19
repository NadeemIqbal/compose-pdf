# Contributing

Thanks for considering a contribution! ComposePDF is a small focused library; the bar for
new features is "would this be in the top 5 things someone reaches for in a PDF library?".
For anything bigger than a bug fix, please open an issue first to discuss.

## Building

```bash
./gradlew build
```

Runs the multiplatform compile, lint, and tests across all configured source sets. The CI
workflow in `.github/workflows/build.yml` does exactly this on every push.

## Running tests

```bash
./gradlew :compose-pdf:desktopTest         # round-trip PDF parsing tests
./gradlew :compose-pdf:test                # all tests on all targets
```

The desktop test suite generates PDFs via the public DSL, then re-parses them with PDFBox
to verify text content + page count + page sizes. That covers the round-trip contract: if
the bytes look like a PDF, they actually open as one.

## Running the sample

```bash
./gradlew :sample:desktopApp:run
```

The sample is one window with three buttons that each generate a different PDF (invoice,
report, certificate) and open it in your default viewer.

## Project layout

```
compose-pdf/
├── compose-pdf/                 # the published library
│   └── src/
│       ├── commonMain/          # the expect API: pdfDocument(), scopes, PdfPageSize
│       ├── commonTest/          # PageSize tests
│       ├── desktopMain/         # JVM actual using Apache PDFBox
│       └── desktopTest/         # round-trip parse tests
└── sample/
    └── desktopApp/              # Compose Desktop demo app
```

The v0.2 plan is to add `iosMain`, `wasmJsMain`, and `androidMain` actual implementations
once Skiko's `PDFDocument` API is public — the `commonMain` API surface should not need to
change for that.

## Code style

- Kotlin official style (Gradle property `kotlin.code.style=official`).
- Comments explain *why*, not *what* — leave the code obvious, leave the rationale in prose.
- Prefer `expect`/`actual` boundaries over `if (Platform.isJvm)` runtime checks.
- New public API needs a KDoc with at least one usage example.

## Publishing

Maintainers publish from a local machine using vanniktech's plugin:

```bash
./gradlew publishAndReleaseToMavenCentral --no-configuration-cache
```

A git tag on the form `v0.1.0` cuts the matching release version (driven by `RELEASE_VERSION`
in CI; locally read from `gradle.properties` `version=...`).
