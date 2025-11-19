package com.mtlc.studyplan.data

/**
 * Curated library of real, verified YDS (Yabancilar icin Turkce) study resources
 * All resources include actual working URLs to external content
 */
object YdsResourceLibrary {

    /**
     * Returns all recommended YDS resources
     * All resources are verified and from official or established sources
     */
    fun getRecommendedResources(): List<YdsResource> = listOf(
        // Official OSYM resources
        YdsResource(
            id = "osym_official_website",
            title = "OSYM Official Website",
            description = "Official OSYM website for YDS exam registration, guides, and past exam questions with answer keys",
            url = "https://www.osym.gov.tr",
            type = ResourceType.OFFICIAL_GUIDE,
            language = "tr",
            source = "OSYM"
        ),
        YdsResource(
            id = "osym_exam_schedule",
            title = "2025 e-YDS Exam Schedule & Guidelines",
            description = "Official e-YDS exam schedule, registration information, and test guidelines for 2025",
            url = "https://www.osym.gov.tr/TR,32926/2025.html",
            type = ResourceType.OFFICIAL_GUIDE,
            language = "tr",
            source = "OSYM"
        ),
        YdsResource(
            id = "osym_exam_portal",
            title = "OSYM Exam Information System (AIS)",
            description = "Official OSYM exam information system for registration and results",
            url = "https://ais.osym.gov.tr",
            type = ResourceType.OFFICIAL_GUIDE,
            language = "tr",
            source = "OSYM"
        ),
        // Vocabulary Resources
        YdsResource(
            id = "yds_vocabulary_advanced",
            title = "Advanced Vocabulary List for YDS",
            description = "Comprehensive vocabulary list and phrasal verbs essential for YDS, YOKDIL, TOEFL, IELTS preparation",
            url = "https://www.academia.edu/40226900/ADVANCED_VOCABULARY_LIST_AND_PHRASAL_VERBS_FOR_ACADEMIC_ENGLISH_EXAMS_YDS_Y%C3%96KD%C4%B0L_TOEFL_IELTS_PTE_",
            type = ResourceType.ARTICLE,
            duration = "Self-paced",
            language = "en",
            source = "Academia.edu"
        ),
        YdsResource(
            id = "quizlet_yds_vocabulary",
            title = "YDS Vocabulary Flashcards - Quizlet",
            description = "Popular YDS vocabulary flashcards and study sets for exam preparation",
            url = "https://quizlet.com/subject/yds-vocabulary/",
            type = ResourceType.ARTICLE,
            language = "en",
            source = "Quizlet"
        ),
        // Learning Platforms
        YdsResource(
            id = "all_ears_english_podcast",
            title = "All Ears English Podcast",
            description = "ESL podcast for intermediate to advanced learners with dedicated IELTS/TOEFL preparation content and conversational English practice",
            url = "https://www.allearsenglish.com/",
            type = ResourceType.PODCAST,
            duration = "15-20 min per episode",
            language = "en",
            source = "All Ears English"
        ),
        YdsResource(
            id = "bbc_learning_english",
            title = "BBC Learning English",
            description = "BBC's trusted English learning resources with videos, lessons, and vocabulary practice",
            url = "https://www.bbc.co.uk/learningenglish",
            type = ResourceType.VIDEO,
            duration = "5-15 min per video",
            language = "en",
            source = "BBC"
        ),
        YdsResource(
            id = "british_council_learn",
            title = "British Council - Learn English",
            description = "Free English learning resources from British Council with grammar, vocabulary and skills practice",
            url = "https://learnenglish.britishcouncil.org/",
            type = ResourceType.ARTICLE,
            language = "en",
            source = "British Council"
        ),
        YdsResource(
            id = "yds_sample_questions",
            title = "YDS Ornek Sorular - Sample Questions",
            description = "Sample YDS questions and detailed explanations from exam prep specialists",
            url = "https://www.remzihoca.com/yds-ornek-sorular-sinav-basarinizi-katlayin",
            type = ResourceType.VIDEO,
            duration = "Variable",
            language = "tr",
            source = "Remzi Hoca"
        )
    )

    /**
     * Returns resources filtered by type
     */
    fun getResourcesByType(type: ResourceType): List<YdsResource> =
        getRecommendedResources().filter { it.type == type }

    /**
     * Returns resources filtered by language
     */
    fun getResourcesByLanguage(language: String): List<YdsResource> =
        getRecommendedResources().filter { it.language == language }

    /**
     * Returns official OSYM resources only
     */
    fun getOfficialResources(): List<YdsResource> =
        getRecommendedResources().filter { it.source == "OSYM" }
}
