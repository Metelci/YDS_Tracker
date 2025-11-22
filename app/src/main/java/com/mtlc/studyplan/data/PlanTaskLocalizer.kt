package com.mtlc.studyplan.data

import android.content.Context
import com.mtlc.studyplan.R
import java.text.Normalizer
import java.time.DayOfWeek
import java.util.Locale

/**
 * Normalizes and localizes legacy plan task descriptions that were authored in Turkish
 * so that the Tasks and Schedule surfaces can render meaningful English text when
 * the active locale is English. Turkish content is preserved by default.
 */
object PlanTaskLocalizer {

    private val combiningMarksRegex = "\\p{Mn}+".toRegex()

    private val dayIndexMap = mapOf(
        "pazartesi" to 0,
        "monday" to 0,
        "sali" to 1,
        "tuesday" to 1,
        "carsamba" to 2,
        "wednesday" to 2,
        "persembe" to 3,
        "thursday" to 3,
        "cuma" to 4,
        "friday" to 4,
        "cumartesi" to 5,
        "saturday" to 5,
        "pazar" to 6,
        "sunday" to 6
    )

    private val dayNameResMap = mapOf(
        "pazartesi" to R.string.monday,
        "monday" to R.string.monday,
        "sali" to R.string.tuesday,
        "salı" to R.string.tuesday,
        "tuesday" to R.string.tuesday,
        "carsamba" to R.string.wednesday,
        "çarşamba" to R.string.wednesday,
        "wednesday" to R.string.wednesday,
        "persembe" to R.string.thursday,
        "perşembe" to R.string.thursday,
        "thursday" to R.string.thursday,
        "cuma" to R.string.friday,
        "friday" to R.string.friday,
        "cumartesi" to R.string.saturday,
        "saturday" to R.string.saturday,
        "pazar" to R.string.sunday,
        "sunday" to R.string.sunday
    )

    private val descriptionResMap = mapOf(
        // Lesson 1 titles
        "1. ders: gelecek haftaya hazirlik" to R.string.next_week_preparation_lesson,
        "1. ders: gramer alistirmalari" to R.string.grammar_exercises_lesson,
        "1. ders: gramer konulari" to R.string.grammar_topics_lesson,
        "1. ders: gramer konusu" to R.string.grammar_topic_lesson,
        "1. ders: gramer konusu (devam)" to R.string.grammar_topic_continuation_lesson,
        "1. ders: gramer pekistirme" to R.string.grammar_reinforcement_lesson,
        "1. ders: haftalik analiz ve planlama" to R.string.weekly_analysis_planning_lesson,
        "1. ders: haftalik genel tekrar" to R.string.weekly_general_repeat,
        "1. ders: haftalik gramer tekrari" to R.string.weekly_grammar_review_lesson,
        "1. ders: haftalik kelime tekrari" to R.string.weekly_vocabulary_lesson,
        "1. ders: mini deneme sinavi" to R.string.mini_exam_lesson,
        "1. ders: okuma ve kelime" to R.string.reading_vocabulary_lesson,
        "1. ders: soru tipi pratigi" to R.string.question_type_practice_lesson,
        // Lesson 2 titles
        "2. ders: deneme analizi ve kelime calismasi" to R.string.mini_analysis_lesson,
        "2. ders: dinleme pratigi ve tekrar" to R.string.listening_practice_review_lesson,
        "2. ders: dinleme ve tekrar" to R.string.listening_repeat_lesson,
        "2. ders: haftalik kelime tekrari" to R.string.weekly_vocabulary_review_second_lesson,
        "2. ders: hizli pratik" to R.string.quick_practice_lesson,
        "2. ders: keyif icin ingilizce" to R.string.entertainment_english_fun_lesson,
        "2. ders: keyif icin ingilizce (dizi/film)" to R.string.entertainment_english_lesson,
        "2. ders: okuma pratigi ve kelime" to R.string.reading_practice_vocabulary_lesson,
        "2. ders: serbest dinleme" to R.string.free_listening_lesson,
        "2. ders: serbest okuma" to R.string.free_reading_lesson,
        "2. ders: serbest okuma/dinleme" to R.string.free_reading_listening,
        "2. ders: soru analizi ve tekrar" to R.string.question_analysis_review_lesson,
        "2. ders: zorlu okuma ve kelime" to R.string.challenging_reading_vocabulary_lesson,
        // Standalone titles
        "deneme analizi" to R.string.exam_analysis,
        "genel tekrar ve dinlenme" to R.string.general_repeat,
        "haftalik kelime tekrari" to R.string.weekly_vocabulary_review_title,
        "strateji ve motivasyon" to R.string.strategy_motivation,
        "tam deneme sinavi" to R.string.full_exam
    )

    fun localize(task: PlanTask, context: Context): PlanTask {
        if (!isEnglish(context)) return task
        val localizedDesc = descriptionResMap[normalizedKey(task.desc)]?.let(context::getString) ?: task.desc
        // Clear Turkish details when in English - details contain dynamic Turkish content
        val localizedDetails = if (task.details != null && containsTurkishText(task.details)) {
            null
        } else {
            task.details
        }
        return task.copy(desc = localizedDesc, details = localizedDetails)
    }

    private fun containsTurkishText(text: String): Boolean {
        // Check for common Turkish words or patterns in the details
        val turkishPatterns = listOf(
            "kaynak:", "kitab", "ünite", "çalış", "çöz", "tekrar", "analiz",
            "öğren", "oku", "dinle", "izle", "tamamla", "pekiştir", "not al",
            "kelime", "gramer", "soru", "deneme", "hafta", "gün", "saat"
        )
        val lower = text.lowercase()
        return turkishPatterns.any { lower.contains(it) }
    }

    fun dayIndex(raw: String): Int = dayIndexMap[normalizedKey(raw)] ?: 0

    fun localizeDayName(dayPlan: DayPlan, context: Context, dayNumber: Int? = null): String {
        // Prefer structured day-of-week when available
        val dayOfWeek = dayPlan.dayOfWeek ?: dayNumber?.takeIf { it in 1..7 }?.let { DayOfWeek.of(it) }
        if (dayOfWeek != null) {
            val resId = when (dayOfWeek) {
                DayOfWeek.MONDAY -> R.string.monday
                DayOfWeek.TUESDAY -> R.string.tuesday
                DayOfWeek.WEDNESDAY -> R.string.wednesday
                DayOfWeek.THURSDAY -> R.string.thursday
                DayOfWeek.FRIDAY -> R.string.friday
                DayOfWeek.SATURDAY -> R.string.saturday
                DayOfWeek.SUNDAY -> R.string.sunday
            }
            return context.getString(resId)
        }

        // Fallback to legacy string matching
        return localizeDayName(dayPlan.day, context)
    }

    fun localizeDayName(raw: String, context: Context): String {
        val resId = dayNameResMap[normalizedKey(raw)] ?: return raw
        return context.getString(resId)
    }

    private fun isEnglish(context: Context): Boolean {
        val locale = context.resources.configuration.locales.get(0)
        val lang = locale?.language ?: "en"
        return lang.equals("en", ignoreCase = true)
    }

    private fun normalizedKey(value: String): String {
        val replaced = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace('ç', 'c')
            .replace('Ç', 'C')
            .replace('İ', 'I')
            .replace('ı', 'i')
            .replace('ş', 's')
            .replace('Ş', 'S')
            .replace('ğ', 'g')
            .replace('Ğ', 'G')
            .replace('ü', 'u')
            .replace('Ü', 'U')
            .replace('ö', 'o')
            .replace('Ö', 'O')
        return combiningMarksRegex.replace(replaced, "")
            .lowercase(Locale.ROOT)
            .trim()
    }
}
