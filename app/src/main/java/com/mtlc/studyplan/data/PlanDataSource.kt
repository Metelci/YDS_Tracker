package com.mtlc.studyplan.data

import android.content.Context

//region VERİ KAYNAKLARI
object PlanDataSource {

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun getAppContext(): Context {
        return appContext ?: throw IllegalStateException("PlanDataSource not initialized")
    }

    // Kırmızı Kitap'ın 8 haftalık "Sağlam Temel" programı için özel fonksiyon
    private fun createRedBookFoundationWeek(
        week: Int,
        monUnits: String, tueUnits: String, thuUnits: String, friUnits: String
    ): WeekPlan {
        val month = ((week - 1) / 4) + 1
        val weekId = "w${week}"
        val book = "Kırmızı Kitap - Essential Grammar in Use"

        return WeekPlan(week, month, "$month. Ay, $week. Hafta: Sağlam Temel", listOf(
            DayPlan("Pazartesi", listOf(
                PlanTask("$weekId-pzt1", "1. Ders: Gramer Konuları", "$book, Üniteler: $monUnits"),
                PlanTask("$weekId-pzt2", "2. Ders: Hızlı Pratik", "Çalıştığın konuların kitaptaki alıştırmalarını çözerek pekiştir.")
            )),
            DayPlan("Salı", listOf(
                PlanTask("$weekId-sal1", "1. Ders: Gramer Konuları", "$book, Üniteler: $tueUnits"),
                PlanTask("$weekId-sal2", "2. Ders: Hızlı Pratik", "Çalıştığın konuların kitaptaki alıştırmalarını çözerek pekiştir.")
            )),
            DayPlan("Çarşamba", listOf(
                PlanTask("$weekId-car1", "1. Ders: Okuma ve Kelime", "Newsinlevels.com (Level 1-2) sitesinden en az 3 haber oku ve 15 yeni kelime çıkar."),
                PlanTask("$weekId-car2", "2. Ders: Dinleme ve Tekrar", "ESL/British Council podcastlerinden bir bölüm dinle ve öğrendiğin kelimeleri tekrar et.")
            )),
            DayPlan("Perşembe", listOf(
                PlanTask("$weekId-per1", "1. Ders: Gramer Konuları", "$book, Üniteler: $thuUnits"),
                PlanTask("$weekId-per2", "2. Ders: Hızlı Pratik", "Çalıştığın konuların kitaptaki alıştırmalarını çözerek pekiştir.")
            )),
            DayPlan("Cuma", listOf(
                PlanTask("$weekId-cum1", "1. Ders: Gramer Konuları", "$book, Üniteler: $friUnits"),
                PlanTask("$weekId-cum2", "2. Ders: Hızlı Pratik", "Çalıştığın konuların kitaptaki alıştırmalarını çözerek pekiştir.")
            )),
            DayPlan("Cumartesi", listOf(
                PlanTask("$weekId-cmt1", "1. Ders: Haftalık Kelime Tekrarı", "Bu hafta öğrendiğin tüm yeni kelimeleri (yaklaşık 40-50 kelime) flashcard uygulamasıyla tekrar et."),
                PlanTask("$weekId-cmt2", "2. Ders: Keyif için İngilizce (Dizi/Film)", "İngilizce altyazılı bir dizi bölümü veya film izle. Anlamaya değil, kulağını doldurmaya odaklan.")
            )),
            DayPlan("Pazar", listOf(
                PlanTask("$weekId-paz1", "1. Ders: Haftalık Genel Tekrar", "Bu hafta işlenen tüm gramer konularını hızlıca gözden geçir. Anlamadığın yerleri not al."),
                PlanTask("$weekId-paz2", "2. Ders: Serbest Okuma/Dinleme", "İlgini çeken bir konuda İngilizce bir YouTube kanalı izle veya blog oku.")
            ))
        ))
    }

    // Mavi ve Yeşil Kitaplar için kullanılan standart fonksiyon
    private fun createAdvancedPreparationWeek(
        week: Int,
        level: String,
        book: String,
        grammarTopics: String,
        nextGrammarTopic: String,
        readingFocus: String,
        listeningFocus: String,
        questionType: String
    ): WeekPlan {
        val month = ((week - 1) / 4) + 1
        val weekId = "w${week}"

        val shouldStartAdvancedPractice = week >= 13

        val miniExamDayPlan = DayPlan(
            day = "",
            tasks = listOf(
                PlanTask("$weekId-mini_exam", "1. Ders: Mini Deneme Sınavı", "40-50 soruluk bir deneme çöz (Süre: 60-75 dk). Okuma, gramer ve kelime ağırlıklı olmasına dikkat et."),
                PlanTask("$weekId-mini_analysis", "2. Ders: Deneme Analizi ve Kelime Çalışması", "Tüm yanlışlarını ve boşlarını detaylıca analiz et. Bilmediğin kelimeleri not al ve kelime setine ekle.")
            )
        )

        val regularDays = mutableListOf<DayPlan>()

        regularDays.add(DayPlan("Pazartesi", listOf(
            PlanTask("$weekId-t1", "1. Ders: Gramer Konusu", "Kaynak: $book. Haftanın konusu olan '$grammarTopics' üzerine detaylıca çalış."),
            PlanTask("$weekId-t2", "2. Ders: Okuma Pratiği ve Kelime", "Kaynak: $readingFocus. Okuma yaparken en az 10 yeni kelime belirle ve anlamlarıyla birlikte not al.")
        )))
        regularDays.add(DayPlan("Salı", listOf(
            PlanTask("$weekId-t3", "1. Ders: Gramer Alıştırmaları", "$book kitabından dünkü konunun alıştırmalarını eksiksiz tamamla."),
            PlanTask("$weekId-t4", "2. Ders: Dinleme Pratiği ve Tekrar", "Kaynak: $listeningFocus. Aktif dinleme yap ve dünkü kelimeleri tekrar et.")
        )))
        if (shouldStartAdvancedPractice) {
            regularDays.add(miniExamDayPlan.copy(day = "Çarşamba"))
        } else {
            regularDays.add(DayPlan("Çarşamba", listOf(
                PlanTask("$weekId-t5", "1. Ders: Gramer Pekiştirme", "Öğrendiğin gramer yapılarını kullanarak çeviri veya cümle kurma alıştırmaları yap."),
                PlanTask("$weekId-t6", "2. Ders: Serbest Okuma", "İlgini çeken bir konuda İngilizce blog/makale oku.")
            )))
        }
        regularDays.add(DayPlan("Perşembe", listOf(
            PlanTask("$weekId-t7", "1. Ders: Gramer Konusu (Devam)", "Haftanın gramer konusunu pekiştir ve ek alıştırmalar çöz."),
            PlanTask("$weekId-t8", "2. Ders: Zorlu Okuma ve Kelime", "Kaynak: $readingFocus (Zor seviye). Yeni 10 kelime daha öğren.")
        )))
        if (shouldStartAdvancedPractice) {
            regularDays.add(DayPlan("Cuma", listOf(
                PlanTask("$weekId-t9", "1. Ders: Soru Tipi Pratiği", "$questionType soru tipinden en az 20 soru çöz."),
                PlanTask("$weekId-t10", "2. Ders: Soru Analizi ve Tekrar", "Çözdüğün sorulardaki yanlışlarını analiz et ve haftalık kelimeleri tekrar et.")
            )))
        } else {
            regularDays.add(DayPlan("Cuma", listOf(
                PlanTask("$weekId-t9", "1. Ders: Haftalık Gramer Tekrarı", "Bu hafta işlenen tüm gramer konularını ve kurallarını tekrar et."),
                PlanTask("$weekId-t10", "2. Ders: Serbest Dinleme", "İlgini çeken bir konuda İngilizce podcast/video izle.")
            )))
        }
        regularDays.add(DayPlan("Cumartesi", listOf(
            PlanTask("$weekId-t11", "1. Ders: Gelecek Haftaya Hazırlık", "Gelecek haftanın konusu olan '$nextGrammarTopic' konusuna kısaca göz atarak ön hazırlık yap."),
            PlanTask("$weekId-t12", "2. Ders: Haftalık Kelime Tekrarı", "Bu hafta öğrendiğin tüm kelimeleri (yaklaşık 20-30 kelime) flashcard uygulamasıyla tekrar et.")
        )))
        if (shouldStartAdvancedPractice) {
            regularDays.add(miniExamDayPlan.copy(day = "Pazar"))
        } else {
            regularDays.add(DayPlan("Pazar", listOf(
                PlanTask("$weekId-t13", "1. Ders: Haftalık Analiz ve Planlama", "Haftanın genel bir değerlendirmesini yap. Güçlü ve zayıf yönlerini belirle."),
                PlanTask("$weekId-t14", "2. Ders: Keyif için İngilizce", "İngilizce bir film/dizi izle veya oyun oyna. Amaç sadece dilin keyfini çıkarmak.")
            )))
        }

        return WeekPlan(week, month, "$month. Ay, $week. Hafta: $level Seviyesi", regularDays)
    }

    private fun createExamCampWeek(week: Int): WeekPlan {
        val month = ((week - 1) / 4) + 1
        val weekId = "w${week}"
        return WeekPlan(week, month, "$month. Ay, $week. Hafta: Sınav Kampı", listOf(
            DayPlan("Pazartesi", listOf(PlanTask("$weekId-exam-1", "Tam Deneme Sınavı", "Kaynak: Son yıllara ait çıkmış bir YDS/YÖKDİL sınavı. 80 soruyu tam 180 dakika içinde çöz."), PlanTask("$weekId-analysis-1", "Deneme Analizi", "Sınav sonrası en az 1 saat ara ver. Ardından yanlışlarını, boşlarını ve doğru yapsan bile emin olmadıklarını detaylıca analiz et. Bilmediğin kelimeleri listele."))),
            DayPlan("Salı", listOf(PlanTask("$weekId-exam-2", "Tam Deneme Sınavı", "Kaynak: Güvenilir bir yayınevinin deneme sınavı. 80 soruyu tam 180 dakika içinde çöz."), PlanTask("$weekId-analysis-2", "Deneme Analizi", "Dünkü gibi detaylı analiz yap. Özellikle tekrar eden hata tiplerine odaklan."))),
            DayPlan("Çarşamba", listOf(PlanTask("$weekId-exam-3", "Tam Deneme Sınavı", "Kaynak: Çıkmış bir sınav. 80 soruyu tam 180 dakika içinde çöz."), PlanTask("$weekId-analysis-3", "Deneme Analizi", "Analizini yap ve bu denemede öğrendiğin yeni kelimeleri tekrar et."))),
            DayPlan("Perşembe", listOf(PlanTask("$weekId-exam-4", "Tam Deneme Sınavı", "Kaynak: Çıkmış bir sınav. 80 soruyu tam 180 dakika içinde çöz."), PlanTask("$weekId-analysis-4", "Deneme Analizi", "Analizini yap ve bu denemede öğrendiğin yeni kelimeleri tekrar et."))),
            DayPlan("Cuma", listOf(PlanTask("$weekId-exam-5", "Tam Deneme Sınavı", "Kaynak: Çıkmış bir sınav. 80 soruyu tam 180 dakika içinde çöz."), PlanTask("$weekId-analysis-5", "Deneme Analizi", "Analizini yap ve bu denemede öğrendiğin yeni kelimeleri tekrar et."))),
            DayPlan("Cumartesi", listOf(PlanTask("$weekId-exam-6", "Tam Deneme Sınavı", "Kaynak: Çıkmış bir sınav. 80 soruyu tam 180 dakika içinde çöz."), PlanTask("$weekId-t12", "Haftalık Kelime Tekrarı", "Bu hafta denemelerde çıkan bilmediğin tüm kelimeleri flashcard uygulaması üzerinden tekrar et."))),
            DayPlan("Pazar", listOf(PlanTask("$weekId-t13", "Genel Tekrar ve Dinlenme", "Haftanın denemelerindeki genel hata tiplerini (örn: zaman yönetimi, belirli soru tipi) gözden geçir."), PlanTask("$weekId-t14", "Strateji ve Motivasyon", "Gelecek haftanın stratejisini belirle ve zihnini dinlendir. Sınava az kaldı!")))
        ))
    }

    val planData: List<WeekPlan> = mutableListOf<WeekPlan>().apply {

        // --- 1. FAZ: KIRMIZI KİTAP "SAĞLAM TEMEL" PROGRAMI --- (8 Hafta)
        add(createRedBookFoundationWeek(1, "1-4", "5-8", "9-12", "13-16"))
        add(createRedBookFoundationWeek(2, "17-20", "21-24", "25-28", "29-32"))
        add(createRedBookFoundationWeek(3, "33-36", "37-40", "41-44", "45-48"))
        add(createRedBookFoundationWeek(4, "49-52", "53-56", "57-60", "61-64"))
        add(createRedBookFoundationWeek(5, "65-68", "69-72", "73-76", "77-80"))
        add(createRedBookFoundationWeek(6, "81-84", "85-88", "89-92", "93-96"))
        add(createRedBookFoundationWeek(7, "97-100", "101-104", "105-108", "109-112"))
        add(createRedBookFoundationWeek(8, "113-115", "Genel Tekrar 1-50", "Genel Tekrar 51-115", "Zayıf Konu Analizi"))


        // --- 2. FAZ: MAVİ KİTAP (B1-B2 GELİŞİMİ) --- (10 Hafta)
        val blueBook = "Mavi Kitap - English Grammar in Use"
        val blueBookTopics = listOf(
            "Tenses Review (Tüm Zamanların Karşılaştırması)", "Future in Detail (Continuous/Perfect)",
            "Modals 1 (Ability, Permission, Advice)", "Modals 2 (Deduction, Obligation, Regret)",
            "Conditionals & Wish (Tüm Tipler & İleri Düzey)", "Passive Voice (Tüm Zamanlar) & 'have something done'",
            "Reported Speech (Sorular, Komutlar, İleri Düzey)", "Noun Clauses & Relative Clauses",
            "Gerunds & Infinitives (İleri kalıplar)", "Conjunctions & Connectors"
        )
        blueBookTopics.forEachIndexed { index, topic ->
            val weekNumber = index + 9 // 8 hafta bitti, 9. haftadan başlıyoruz
            val nextTopic = if (index + 1 < blueBookTopics.size) blueBookTopics[index + 1] else "Yeşil Kitap - Advanced Tenses"
            add(createAdvancedPreparationWeek(weekNumber, "B1-B2 Gelişimi", blueBook, topic, nextTopic, "The Guardian, BBC News", "TED-Ed Videoları", "Cümle Tamamlama"))
        }

        // --- 3. FAZ: YEŞİL KİTAP (C1 USTALIĞI) --- (8 Hafta)
        val greenBook = "Yeşil Kitap - Advanced Grammar in Use"
        val greenBookTopics = listOf(
            "Advanced Tense Nuances & Narrative Tenses", "Inversion & Emphasis (Not only, Hardly...)",
            "Advanced Modals (Speculation, Hypothetical)", "Participle Clauses (-ing ve -ed clauses)",
            "Advanced Connectors & Discourse Markers", "Hypothetical Meaning & Subjunctives",
            "Adjectives & Adverbs (İleri Kullanımlar)", "Prepositions & Phrasal Verbs (İleri Düzey)"
        )
        greenBookTopics.forEachIndexed { index, topic ->
            val weekNumber = index + 19 // 8+10=18 hafta bitti, 19. haftadan başlıyoruz
            val nextTopic = if (index + 1 < greenBookTopics.size) greenBookTopics[index + 1] else "Genel Tekrar ve Sınav Kampı"
            add(createAdvancedPreparationWeek(weekNumber, "C1 Ustalığı", greenBook, topic, nextTopic, "National Geographic, Scientific American", "NPR, BBC Radio 4 Podcast'leri", "Paragraf Tamamlama & Anlam Bütünlüğünü Bozan Cümle"))
        }

        // --- 4. FAZ: SINAV KAMPI --- (4 Hafta)
        addAll(List(4) { i -> createExamCampWeek(i + 27) })
    }
}
//endregion