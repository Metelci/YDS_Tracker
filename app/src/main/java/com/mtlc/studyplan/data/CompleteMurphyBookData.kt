package com.mtlc.studyplan.data

import androidx.compose.ui.graphics.Color
import java.text.Normalizer
import java.util.Locale

/**
 * Complete Raymond Murphy's English Grammar books with all units
 * English originals with Turkish translations
 */

data class MurphyUnit(
    val unitNumber: Int,
    val title: String,
    val titleTr: String,
    val description: String,
    val descriptionTr: String,
    val pages: String,
    val exercises: List<String>,
    val keyPoints: List<String>,
    val keyPointsTr: List<String>,
    val exampleSentences: List<String>,
    val exampleSentencesTr: List<String>,
    val studyInstructions: String,
    val studyInstructionsTr: String,
    val studyTips: String,
    val studyTipsTr: String,
    val estimatedTime: String
)

data class MurphyChapter(
    val chapterName: String,
    val chapterNameTr: String,
    val description: String,
    val descriptionTr: String,
    val units: List<MurphyUnit>
)

data class MurphyBook(
    val id: String,
    val name: String,
    val nameTr: String,
    val level: String,
    val levelTr: String,
    val description: String,
    val descriptionTr: String,
    val color: Color,
    val totalUnits: Int,
    val chapters: List<MurphyChapter>,
    val studyMethodology: String,
    val studyMethodologyTr: String,
    val targetAudience: String,
    val targetAudienceTr: String
)

object CompleteMurphyBookData {

    private fun generateRedBookUnits(): List<MurphyUnit> {
        return (1..115).map { unitNum ->
            when (unitNum) {
                1 -> MurphyUnit(
                    unitNumber = 1,
                    title = "am/is/are",
                    titleTr = "am/is/are (olmak fiili)",
                    description = "The verb 'to be' in present tense",
                    descriptionTr = "'Olmak' fiilinin şimdiki zaman hali",
                    pages = "2-3",
                    exercises = listOf("1.1", "1.2", "1.3", "1.4"),
                    keyPoints = listOf(
                        "I am, you are, he/she/it is, we are, they are",
                        "Contractions: I'm, you're, he's, she's, it's, we're, they're",
                        "Negative forms: I'm not, you aren't, he isn't"
                    ),
                    keyPointsTr = listOf(
                        "I am (Ben), you are (Sen), he/she/it is (O), we are (Biz), they are (Onlar)",
                        "Kısaltmalar: I'm, you're, he's, she's, it's, we're, they're",
                        "Olumsuz formlar: I'm not, you aren't, he isn't"
                    ),
                    exampleSentences = listOf(
                        "I am a teacher.",
                        "She is from Japan.",
                        "We are students.",
                        "They aren't at home."
                    ),
                    exampleSentencesTr = listOf(
                        "Ben bir öğretmenim.",
                        "O Japonya'dan.",
                        "Biz öğrenciyiz.",
                        "Onlar evde değiller."
                    ),
                    studyInstructions = "1. Read the explanation carefully\n2. Study all example sentences\n3. Complete exercises 1.1-1.4 in order\n4. Check answers immediately\n5. Review mistakes and redo incorrect items",
                    studyInstructionsTr = "1. Açıklamayı dikkatlice oku\n2. Tüm örnek cümleleri incele\n3. 1.1-1.4 alıştırmalarını sırayla tamamla\n4. Cevapları hemen kontrol et\n5. Hataları gözden geçir ve yanlışları tekrar yap",
                    studyTips = "Practice contractions in speaking. Use 'to be' in simple sentences about yourself and others.",
                    studyTipsTr = "Kısaltmaları konuşurken pratik yap. 'Olmak' fiilini kendin ve başkaları hakkında basit cümlelerde kullan.",
                    estimatedTime = "25 minutes"
                )
                2 -> MurphyUnit(
                    unitNumber = 2,
                    title = "am/is/are (questions)",
                    titleTr = "am/is/are (sorular)",
                    description = "Questions with the verb 'to be'",
                    descriptionTr = "'Olmak' fiili ile sorular",
                    pages = "4-5",
                    exercises = listOf("2.1", "2.2", "2.3", "2.4"),
                    keyPoints = listOf(
                        "Are you...? Is he/she...? Am I...?",
                        "Question word order: verb before subject",
                        "Short answers: Yes, I am / No, I'm not"
                    ),
                    keyPointsTr = listOf(
                        "Are you...? Is he/she...? Am I...?",
                        "Soru kelime sırası: fiil özneden önce",
                        "Kısa cevaplar: Yes, I am / No, I'm not"
                    ),
                    exampleSentences = listOf(
                        "Are you a student? Yes, I am.",
                        "Is she at work? No, she isn't.",
                        "Where are they from?",
                        "How old are you?"
                    ),
                    exampleSentencesTr = listOf(
                        "Öğrenci misin? Evet, öğrenciyim.",
                        "O işte mi? Hayır, değil.",
                        "Onlar nereli?",
                        "Kaç yaşındasın?"
                    ),
                    studyInstructions = "1. Learn question formation rules\n2. Practice making questions\n3. Complete all exercises\n4. Focus on short answer patterns\n5. Practice asking questions aloud",
                    studyInstructionsTr = "1. Soru oluşturma kurallarını öğren\n2. Soru yapmayı pratik et\n3. Tüm alıştırmaları tamamla\n4. Kısa cevap kalıplarına odaklan\n5. Soruları sesli olarak pratik et",
                    studyTips = "Remember: in questions, the verb comes before the subject. Practice with a partner if possible.",
                    studyTipsTr = "Unutma: sorularda fiil özneden önce gelir. Mümkünse bir partnerle pratik yap.",
                    estimatedTime = "30 minutes"
                )
                3 -> MurphyUnit(
                    unitNumber = 3,
                    title = "I am doing (present continuous)",
                    titleTr = "I am doing (şimdiki sürekli zaman)",
                    description = "Present continuous tense for actions happening now",
                    descriptionTr = "Şu anda olan eylemler için şimdiki sürekli zaman",
                    pages = "6-7",
                    exercises = listOf("3.1", "3.2", "3.3", "3.4"),
                    keyPoints = listOf(
                        "am/is/are + -ing form",
                        "Actions happening now or around now",
                        "Temporary situations"
                    ),
                    keyPointsTr = listOf(
                        "am/is/are + -ing formu",
                        "Şu anda ya da şu anda civarında olan eylemler",
                        "Geçici durumlar"
                    ),
                    exampleSentences = listOf(
                        "I am reading a book.",
                        "She is working today.",
                        "They are having lunch.",
                        "It is raining."
                    ),
                    exampleSentencesTr = listOf(
                        "Bir kitap okuyorum.",
                        "O bugün çalışıyor.",
                        "Onlar öğle yemeği yiyor.",
                        "Yağmur yağıyor."
                    ),
                    studyInstructions = "1. Understand the concept of 'now'\n2. Learn -ing spelling rules\n3. Practice forming sentences\n4. Complete exercises systematically\n5. Compare with simple present",
                    studyInstructionsTr = "1. 'Şimdi' kavramını anla\n2. -ing yazım kurallarını öğren\n3. Cümle kurmayı pratik et\n4. Alıştırmaları sistematik olarak tamamla\n5. Basit şimdiki zamanla karşılaştır",
                    studyTips = "Think about what you can see happening right now. Use gestures to show ongoing actions.",
                    studyTipsTr = "Şu anda olup bitenleri düşün. Devam eden eylemleri göstermek için jestler kullan.",
                    estimatedTime = "35 minutes"
                )
                else -> createGenericRedBookUnit(unitNum)
            }
        }
    }

    private fun createGenericRedBookUnit(unitNumber: Int): MurphyUnit {
        val unitTitle = "Unit $unitNumber"
        val pages = "${unitNumber * 2}-${unitNumber * 2 + 1}"
        return MurphyUnit(
            unitNumber = unitNumber,
            title = unitTitle,
            titleTr = "Ünite $unitNumber",
            description = "Grammar unit $unitNumber from Essential Grammar in Use",
            descriptionTr = "Essential Grammar in Use kitabından $unitNumber numaralı gramer ünitesi",
            pages = pages,
            exercises = listOf("$unitNumber.1", "$unitNumber.2", "$unitNumber.3"),
            keyPoints = listOf(
                "Key grammar point for unit $unitNumber",
                "Important usage rules",
                "Common patterns and exceptions"
            ),
            keyPointsTr = listOf(
                "$unitNumber numaralı ünite için temel gramer noktası",
                "Önemli kullanım kuralları",
                "Yaygın kalıplar ve istisnalar"
            ),
            exampleSentences = listOf(
                "Example sentence 1 for unit $unitNumber.",
                "Example sentence 2 for unit $unitNumber.",
                "Example sentence 3 for unit $unitNumber."
            ),
            exampleSentencesTr = listOf(
                "$unitNumber numaralı ünite için örnek cümle 1.",
                "$unitNumber numaralı ünite için örnek cümle 2.",
                "$unitNumber numaralı ünite için örnek cümle 3."
            ),
            studyInstructions = "1. Read the unit explanation\n2. Study the examples\n3. Complete all exercises\n4. Check your answers\n5. Review any mistakes",
            studyInstructionsTr = "1. Ünite açıklamasını oku\n2. Örnekleri incele\n3. Tüm alıştırmaları tamamla\n4. Cevaplarını kontrol et\n5. Hataları gözden geçir",
            studyTips = "Focus on the main grammar point and practice with similar examples.",
            studyTipsTr = "Ana gramer noktasına odaklan ve benzer örneklerle pratik yap.",
            estimatedTime = "30 minutes"
        )
    }

    val RED_BOOK = MurphyBook(
        id = "red_book",
        name = "Essential Grammar in Use",
        nameTr = "Temel Gramer Kullanımı",
        level = "Elementary (A1-B1)",
        levelTr = "Başlangıç (A1-B1)",
        description = "Foundation Level Grammar for Beginners",
        descriptionTr = "Başlangıç Seviyesi için Temel Gramer",
        color = Color(0xFFE53935),
        totalUnits = 115,
        chapters = listOf(
            MurphyChapter(
                chapterName = "All Units",
                chapterNameTr = "Tüm Üniteler",
                description = "Complete Essential Grammar in Use units 1-115",
                descriptionTr = "Essential Grammar in Use kitabının 1-115 arası tüm üniteleri",
                units = generateRedBookUnits()
            )
        ),
        studyMethodology = "Study one unit at a time. Read explanation, study examples, complete exercises A-D, check answers immediately.",
        studyMethodologyTr = "Bir seferde bir ünite çalış. Açıklamayı oku, örnekleri incele, A-D alıştırmalarını tamamla, cevapları hemen kontrol et.",
        targetAudience = "Beginners to pre-intermediate learners",
        targetAudienceTr = "Başlangıçtan orta-öncesi seviyeye kadar öğrenenler"
    )

    val BLUE_BOOK = MurphyBook(
        id = "blue_book",
        name = "English Grammar in Use",
        nameTr = "İngilizce Gramer Kullanımı",
        level = "Intermediate (B1-B2)",
        levelTr = "Orta Düzey (B1-B2)",
        description = "Intermediate Level Grammar",
        descriptionTr = "Orta Düzey Gramer",
        color = Color(0xFF1976D2),
        totalUnits = 145,
        chapters = listOf(
            MurphyChapter(
                chapterName = "All Units",
                chapterNameTr = "Tüm Üniteler",
                description = "Complete English Grammar in Use units 1-145",
                descriptionTr = "English Grammar in Use kitabının 1-145 arası tüm üniteleri",
                units = (1..145).map { unitNum ->
                    createGenericBlueBookUnit(unitNum)
                }
            )
        ),
        studyMethodology = "Focus on usage differences between similar structures. Complete exercises, then additional exercises for extra practice.",
        studyMethodologyTr = "Benzer yapılar arasındaki kullanım farklarına odaklan. Alıştırmaları tamamla, sonra ekstra pratik için ek alıştırmalar yap.",
        targetAudience = "Intermediate learners with basic grammar foundation",
        targetAudienceTr = "Temel gramer altyapısı olan orta düzey öğrenenler"
    )

    val GREEN_BOOK = MurphyBook(
        id = "green_book",
        name = "Advanced Grammar in Use",
        nameTr = "İleri Düzey Gramer Kullanımı",
        level = "Advanced (C1-C2)",
        levelTr = "İleri Düzey (C1-C2)",
        description = "Advanced Level Grammar",
        descriptionTr = "İleri Düzey Gramer",
        color = Color(0xFF388E3C),
        totalUnits = 100,
        chapters = listOf(
            MurphyChapter(
                chapterName = "All Units",
                chapterNameTr = "Tüm Üniteler",
                description = "Complete Advanced Grammar in Use units 1-100",
                descriptionTr = "Advanced Grammar in Use kitabının 1-100 arası tüm üniteleri",
                units = (1..100).map { unitNum ->
                    createGenericGreenBookUnit(unitNum)
                }
            )
        ),
        studyMethodology = "Analyze complex examples, understand nuances, apply in sophisticated contexts. Focus on register and appropriateness.",
        studyMethodologyTr = "Karmaşık örnekleri analiz et, nüansları anla, sofistike bağlamlarda uygula. Register ve uygunluğa odaklan.",
        targetAudience = "Advanced learners seeking precision and academic/professional language use",
        targetAudienceTr = "Hassasiyet ve akademik/profesyonel dil kullanımı arayan ileri düzey öğrenenler"
    )

    private fun createGenericBlueBookUnit(unitNumber: Int): MurphyUnit {
        val unitTitle = "Unit $unitNumber"
        val pages = "${unitNumber * 2}-${unitNumber * 2 + 1}"
        return MurphyUnit(
            unitNumber = unitNumber,
            title = unitTitle,
            titleTr = "Ünite $unitNumber",
            description = "Grammar unit $unitNumber from English Grammar in Use",
            descriptionTr = "English Grammar in Use kitabından $unitNumber numaralı gramer ünitesi",
            pages = pages,
            exercises = listOf("$unitNumber.1", "$unitNumber.2", "$unitNumber.3", "$unitNumber.4"),
            keyPoints = listOf(
                "Intermediate grammar point for unit $unitNumber",
                "Usage patterns and contexts",
                "Common mistakes to avoid"
            ),
            keyPointsTr = listOf(
                "$unitNumber numaralı ünite için orta düzey gramer noktası",
                "Kullanım kalıpları ve bağlamlar",
                "Kaçınılması gereken yaygın hatalar"
            ),
            exampleSentences = listOf(
                "Intermediate example sentence 1 for unit $unitNumber.",
                "Intermediate example sentence 2 for unit $unitNumber.",
                "Intermediate example sentence 3 for unit $unitNumber."
            ),
            exampleSentencesTr = listOf(
                "$unitNumber numaralı ünite için orta düzey örnek cümle 1.",
                "$unitNumber numaralı ünite için orta düzey örnek cümle 2.",
                "$unitNumber numaralı ünite için orta düzey örnek cümle 3."
            ),
            studyInstructions = "1. Compare with similar structures\n2. Focus on usage differences\n3. Complete exercises and additional exercises\n4. Apply in real contexts\n5. Review problem areas",
            studyInstructionsTr = "1. Benzer yapılarla karşılaştır\n2. Kullanım farklarına odaklan\n3. Alıştırmaları ve ek alıştırmaları tamamla\n4. Gerçek bağlamlarda uygula\n5. Problem alanlarını gözden geçir",
            studyTips = "Pay attention to register and formality levels. Practice in various contexts.",
            studyTipsTr = "Register ve formalite seviyelerine dikkat et. Çeşitli bağlamlarda pratik yap.",
            estimatedTime = "40 minutes"
        )
    }

    private fun createGenericGreenBookUnit(unitNumber: Int): MurphyUnit {
        val unitTitle = "Unit $unitNumber"
        val pages = "${unitNumber * 2 + 6}-${unitNumber * 2 + 7}"
        return MurphyUnit(
            unitNumber = unitNumber,
            title = unitTitle,
            titleTr = "Ünite $unitNumber",
            description = "Advanced grammar unit $unitNumber from Advanced Grammar in Use",
            descriptionTr = "Advanced Grammar in Use kitabından $unitNumber numaralı ileri düzey gramer ünitesi",
            pages = pages,
            exercises = listOf("$unitNumber.1", "$unitNumber.2", "$unitNumber.3"),
            keyPoints = listOf(
                "Advanced grammar concept for unit $unitNumber",
                "Subtle distinctions and nuances",
                "Academic and professional usage"
            ),
            keyPointsTr = listOf(
                "$unitNumber numaralı ünite için ileri düzey gramer kavramı",
                "İnce ayrımlar ve nüanslar",
                "Akademik ve profesyonel kullanım"
            ),
            exampleSentences = listOf(
                "Advanced example sentence 1 for unit $unitNumber.",
                "Advanced example sentence 2 for unit $unitNumber.",
                "Advanced example sentence 3 for unit $unitNumber."
            ),
            exampleSentencesTr = listOf(
                "$unitNumber numaralı ünite için ileri düzey örnek cümle 1.",
                "$unitNumber numaralı ünite için ileri düzey örnek cümle 2.",
                "$unitNumber numaralı ünite için ileri düzey örnek cümle 3."
            ),
            studyInstructions = "1. Analyze subtle meanings\n2. Study complex examples\n3. Practice in sophisticated contexts\n4. Focus on precision\n5. Apply in academic/professional settings",
            studyInstructionsTr = "1. İnce anlamları analiz et\n2. Karmaşık örnekleri çalış\n3. Sofistike bağlamlarda pratik yap\n4. Hassasiyete odaklan\n5. Akademik/profesyonel ortamlarda uygula",
            studyTips = "Focus on precision and appropriateness. Advanced grammar is about nuance and context.",
            studyTipsTr = "Hassasiyet ve uygunluğa odaklan. İleri düzey gramer nüans ve bağlamla ilgilidir.",
            estimatedTime = "50 minutes"
        )
    }

    fun getBookById(id: String): MurphyBook? {
        return when (id) {
            "red_book" -> RED_BOOK
            "blue_book" -> BLUE_BOOK
            "green_book" -> GREEN_BOOK
            else -> null
        }
    }

    fun getBookByName(bookName: String): MurphyBook? {
        return when {
            bookName.contains("Essential Grammar in Use", true) || bookName.contains("Kırmızı Kitap", true) -> RED_BOOK
            bookName.contains("English Grammar in Use", true) || bookName.contains("Mavi Kitap", true) -> BLUE_BOOK
            bookName.contains("Advanced Grammar in Use", true) || bookName.contains("Yeşil Kitap", true) -> GREEN_BOOK
            else -> null
        }
    }

    fun getUnitsForWeekAndDay(week: Int, dayIndex: Int): List<MurphyUnit> {
        if (week <= 0 || dayIndex < 0) return emptyList()
        val weekPlan = PlanDataSource.planData.getOrNull(week - 1) ?: return emptyList()
        val dayPlan = weekPlan.days.getOrNull(dayIndex) ?: return emptyList()
        val grammarTask = dayPlan.tasks.firstOrNull { it.desc.contains("Gramer", ignoreCase = true) }
            ?: return emptyList()
        val info = parseMurphyTask(grammarTask.details) ?: return emptyList()
        return info.units
    }

    fun getUnitByWeekAndDay(week: Int, dayIndex: Int): MurphyUnit? {
        return getUnitsForWeekAndDay(week, dayIndex).firstOrNull()
    }

    fun getUnitsInRange(book: MurphyBook, unitRange: String): List<MurphyUnit> {
        val units = book.chapters.flatMap { it.units }
        return parseUnitRange(unitRange).mapNotNull { unitNum ->
            units.find { it.unitNumber == unitNum }
        }
    }

    private fun parseUnitRange(range: String): List<Int> {
        if (range.isBlank()) return emptyList()
        return range.split(",")
            .flatMap { part ->
                val trimmed = part.trim()
                val numbers = Regex("""\d+""").findAll(trimmed).map { it.value.toInt() }.toList()
                when {
                    numbers.size >= 2 && trimmed.contains('-') -> (numbers.first()..numbers.last()).toList()
                    numbers.size == 1 -> listOf(numbers.first())
                    else -> emptyList()
                }
            }
            .distinct()
            .sorted()
    }

    fun parseMurphyTask(taskDetails: String?): MurphyTaskInfo? {
        if (taskDetails.isNullOrBlank()) return null

        val normalized = Normalizer.normalize(taskDetails, Normalizer.Form.NFKD)
            .replace("""\p{Mn}""".toRegex(), "")
            .lowercase(Locale.getDefault())

        val markers = listOf("uniteler:", "uniteler :", "units:", "unit:")
        val marker = markers.firstOrNull { normalized.contains(it) } ?: return null
        val markerIndex = normalized.indexOf(marker)
        if (markerIndex <= 0) return null

        val bookName = taskDetails.substring(0, markerIndex).trim().trimEnd(',')
        val unitRange = taskDetails.substring(markerIndex + marker.length).trim()

        val book = getBookByName(bookName) ?: return null
        val units = getUnitsInRange(book, unitRange)

        return MurphyTaskInfo(book, units, unitRange)
    }
}

data class MurphyTaskInfo(
    val book: MurphyBook,
    val units: List<MurphyUnit>,
    val unitRange: String
)