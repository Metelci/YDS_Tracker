@file:Suppress("CyclomaticComplexMethod")
package com.mtlc.studyplan.ui.components

internal fun estimateTaskMinutes(desc: String, details: String?): Int {
    val s = (desc + " " + (details ?: "")).lowercase()
    Regex("(\\d{2,3})\\s*(-\\s*(\\d{2,3}))?\\s*(dk|dakika|minute|min)").find(s)?.let { m ->
        val a = m.groupValues[1].toIntOrNull()
        val b = m.groupValues.getOrNull(3)?.toIntOrNull()
        if (a != null) return b?.let { (a + it) / 2 } ?: a
    }
    return when {
        s.contains("full exam") || s.contains("tam deneme") -> 180
        s.contains("mini exam") || s.contains("mini deneme") -> 70
        s.contains("reading") || s.contains("okuma") -> 45
        s.contains("vocab") || s.contains("kelime") || s.contains("vocabulary") -> 30
        s.contains("analysis") || s.contains("analiz") -> 30
        s.contains("listening") || s.contains("dinleme") -> 30
        s.contains("practice") || s.contains("pratik") || s.contains("drill") -> 25
        s.contains("grammar") || s.contains("gramer") -> 40
        else -> 30
    }
}

