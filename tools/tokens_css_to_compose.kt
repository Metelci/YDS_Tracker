#!/usr/bin/env kotlin

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.util.Locale

val argMap = parseArgs(args)
val repoRoot = Paths.get("").toAbsolutePath()
val inputPath = argMap["--input"]?.let { repoRoot.resolve(it).normalize() }
    ?: repoRoot.resolve(".claude/CODE_SPECIFICATIONS.MD")
val outputDir = argMap["--output"]?.let { repoRoot.resolve(it).normalize() }
    ?: repoRoot.resolve("app/src/main/java/com/mtlc/studyplan/ui/theme")

if (!Files.exists(inputPath)) {
    error("Input file not found: $inputPath")
}
Files.createDirectories(outputDir)

val tokens = extractTokens(inputPath.toFile())
if (tokens.isEmpty()) {
    error("No HSL tokens found in $inputPath")
}

val generatedTokensPath = outputDir.resolve("GeneratedTokens.kt")
val generatedSemanticsPath = outputDir.resolve("GeneratedSemantics.kt")

Files.writeString(generatedTokensPath, buildTokensFile(tokens))
Files.writeString(generatedSemanticsPath, buildSemanticsFile(tokens))

println("Generated ${generatedTokensPath.toAbsolutePath()}")
println("Generated ${generatedSemanticsPath.toAbsolutePath()}")

data class Token(val name: String, val hue: Float, val saturation: Float, val lightness: Float)

fun parseArgs(args: Array<String>): Map<String, String> {
    val result = mutableMapOf<String, String>()
    var index = 0
    while (index < args.size) {
        val key = args[index]
        if (key.startsWith("--")) {
            val value = args.getOrNull(index + 1)
                ?: error("Missing value for argument $key")
            result[key] = value
            index += 2
        } else {
            index += 1
        }
    }
    return result
}

private val tokenRegex = Regex("--([a-zA-Z0-9-]+)\\s*:\\s*([0-9.]+)\\s+([0-9.]+)%\\s+([0-9.]+)%")

fun extractTokens(file: File): List<Token> {
    return file.readLines()
        .mapNotNull { line ->
            val match = tokenRegex.find(line)
            if (match != null) {
                val name = match.groupValues[1]
                val hue = match.groupValues[2].toFloat()
                val sat = match.groupValues[3].toFloat() / 100f
                val light = match.groupValues[4].toFloat() / 100f
                Token(name, hue, sat, light)
            } else null
        }
}

fun buildTokensFile(tokens: List<Token>): String {
    val packageName = "com.mtlc.studyplan.ui.theme"
    val indent = "    "
    val builder = StringBuilder()
    builder.appendLine("// Generated on ${Instant.now()} by tokens_css_to_compose.kt")
    builder.appendLine("package $packageName")
    builder.appendLine()
    builder.appendLine("import androidx.compose.material3.ColorScheme")
    builder.appendLine("import androidx.compose.material3.darkColorScheme")
    builder.appendLine("import androidx.compose.material3.lightColorScheme")
    builder.appendLine("import androidx.compose.ui.graphics.Color")
    builder.appendLine()
    builder.appendLine("object GeneratedTokens {")
    tokens.forEach { token ->
        val constName = token.name.toCamelCase()
        builder.appendLine("$indent val $constName: Color = Color.hsl(${token.hue}f, ${token.saturation.toCompose()}, ${token.lightness.toCompose()})")
    }
    builder.appendLine("}")
    builder.appendLine()

    val colorSchemeMappings = mapOf(
        "primary" to "primary",
        "primary-foreground" to "onPrimary",
        "primary-container" to "primaryContainer",
        "primary-container-foreground" to "onPrimaryContainer",
        "secondary" to "secondary",
        "secondary-foreground" to "onSecondary",
        "secondary-container" to "secondaryContainer",
        "secondary-container-foreground" to "onSecondaryContainer",
        "tertiary" to "tertiary",
        "tertiary-container" to "tertiaryContainer",
        "tertiary-container-foreground" to "onTertiaryContainer",
        "background" to "background",
        "foreground" to "onBackground",
        "surface" to "surface",
        "surface-variant" to "surfaceVariant",
        "surface-container" to "surfaceContainer",
        "surface-container-high" to "surfaceContainerHigh",
        "card" to "surface",
        "card-foreground" to "onSurface",
        "muted" to "surfaceVariant",
        "muted-foreground" to "onSurfaceVariant",
        "destructive" to "error",
        "destructive-foreground" to "onError",
        "success" to "primary",
        "warning" to "tertiary",
        "border" to "outline",
        "accent" to "surfaceVariant",
        "accent-foreground" to "onSurfaceVariant"
    )

    fun buildSchemeBuilder(functionName: String, factory: String): String {
        val scheme = StringBuilder()
        scheme.appendLine("fun $functionName(): ColorScheme = $factory(")
        val available = tokens.associateBy { it.name }
        val mapped = colorSchemeMappings.entries
            .mapNotNull { (tokenName, schemeProp) ->
                available[tokenName]?.let { token ->
                    val constName = token.name.toCamelCase()
                    schemeProp to "GeneratedTokens.$constName"
                }
            }
        val last = mapped.lastOrNull()
        mapped.forEach { (prop, ref) ->
            val suffix = if (prop == last?.first) "" else ","
            scheme.appendLine("$indent$prop = $ref$suffix")
        }
        scheme.appendLine(")")
        return scheme.toString()
    }

    builder.appendLine(buildSchemeBuilder("generatedLightColorScheme", "lightColorScheme"))
    builder.appendLine()
    builder.appendLine(buildSchemeBuilder("generatedDarkColorScheme", "darkColorScheme"))

    return builder.toString()
}

fun buildSemanticsFile(tokens: List<Token>): String {
    val packageName = "com.mtlc.studyplan.ui.theme"
    val indent = "    "
    val builder = StringBuilder()
    builder.appendLine("// Generated on ${Instant.now()} by tokens_css_to_compose.kt")
    builder.appendLine("package $packageName")
    builder.appendLine()
    builder.appendLine("import androidx.compose.ui.graphics.Color")
    builder.appendLine()
    builder.appendLine("object GeneratedSemantics {")

    val semanticPrefixes = listOf("success", "warning", "destructive", "achievement", "streak")
    tokens.filter { token ->
        semanticPrefixes.any { prefix -> token.name.startsWith(prefix) }
    }.forEach { token ->
        val constName = token.name.toCamelCase()
        builder.appendLine("$indent val $constName: Color = Color.hsl(${token.hue}f, ${token.saturation.toCompose()}, ${token.lightness.toCompose()})")
    }
    builder.appendLine("}")
    return builder.toString()
}

fun String.toCamelCase(): String = split('-', '_').filter { it.isNotBlank() }
    .joinToString(separator = "") { part ->
        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

fun Float.toCompose(): String = String.format(Locale.US, "%.4ff", this)
