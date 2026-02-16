<!-- PLAYSTORE_CHANGELOG.md
     Play Store release notes for all published versions, in all supported languages.
     Source files: metadata/android/{locale}/changelogs/{versionCode}.txt

     USAGE:
     - When publishing a new release, add a new ## section at the TOP of this file.
     - Copy the matching .txt file content under the correct language subsection.
     - Update the Version Code Index table below.
     - Keep the individual .txt files in sync — Fastlane and Play Store tools read those directly.
-->

# Play Store Changelog

> Consolidated release notes for all versions and languages.
> Individual source files are at `metadata/android/{locale}/changelogs/{versionCode}.txt`

## Version Code Index

| Version Code | Version Name | Release Date |
|:---:|:---:|:---:|
| 107 | 2.9.78 | 2026-02-16 |
| 106 | 2.9.77 | 2026-02-16 |
| 105 | 2.9.76 | 2026-02-15 |

---

## Version 2.9.78 (Version Code 107) — 2026-02-16

### English

🎯 Settings Automation (v2.9.78)

✨ Dynamic About Dialog - Zero-maintenance release notes
• Automatically reads from CHANGELOG.md
• No more hardcoded updates needed
• Always shows current version info

📝 Markdown Support - Beautiful formatting
• Bold text for feature names
• Code snippets in monospace
• Bullet lists with proper indentation
• Section headers clearly displayed

🛡️ Smart Error Handling - Reliable experience
• Three-level fallback system
• Version mismatch detection
• Graceful degradation on errors
• Debug logging for troubleshooting

🔄 Maintenance Workflow - Developer friendly
• Update CHANGELOG.md for new version
• Build app (changelog auto-bundled)
• About dialog updates automatically
• Single source of truth

Technical Implementation:
• Custom lightweight parser (~150 lines)
• Context.assets integration
• Compose AnnotatedString rendering
• Zero external dependencies

### Turkish / Türkçe

🎯 Ayarlar Otomasyonu (v2.9.78)

✨ Dinamik Hakkında Dialog - Sıfır bakım
• CHANGELOG.md'den otomatik okur
• Manuel güncellemeye gerek yok
• Her zaman güncel versiyon bilgisi

📝 Markdown Desteği - Güzel formatlama
• Özellik adları kalın yazıyla
• Kod parçacıkları monospace yazıyla
• Girintili madde işaretli listeler
• Bölüm başlıkları net görünüm

🛡️ Akıllı Hata Yönetimi - Güvenilir deneyim
• Üç seviyeli yedekleme sistemi
• Versiyon uyumsuzluğu algılama
• Hatalarda zarif düşüş
• Hata ayıklama için günlük kaydı

🔄 Bakım İş Akışı - Geliştirici dostu
• Yeni versiyon için CHANGELOG.md güncelle
• Uygulamayı derle (changelog otomatik)
• Hakkında dialog otomatik güncellenir
• Tek doğruluk kaynağı

Teknik Uygulama:
• Özel hafif parser (~150 satır)
• Context.assets entegrasyonu
• Compose AnnotatedString render
• Harici bağımlılık yok

---

## Version 2.9.77 (Version Code 106) — 2026-02-16

### English

🔔 Notification System Overhaul (v2.9.77)

✅ Android 13+ Support - Never miss notifications again
• Permission request added for modern Android devices
• Full compatibility with latest Android versions

📊 Smarter Daily Goals - Only real achievements count
• Tracks today's completed tasks accurately
• No more false "goal achieved" notifications
• Timestamp-based progress tracking

🔥 Intelligent Streak Warnings - Alerts when you need them
• Only warns if streak is actually at risk
• Checks if you studied today before alerting
• No unnecessary interruptions

📢 Enhanced Reliability - Room for what matters
• Daily notification limit increased (2→10)
• Exam deadline reminders won't be missed
• Better mix of achievements and reminders

🎨 Perfect Emoji Display - Beautiful notifications
• 🔔 Study reminders with proper emoji
• 🔥 Streak notifications look great
• 🌟 Achievement alerts sparkle
• No more ?? placeholder characters

🎯 UI Refinements - More content, less clutter
• Minimized header spacing in Home & Tasks
• Consistent design across all screens
• Professional visual hierarchy

Bug Fixes:
• Fixed notification permission on Android 13+
• Fixed daily goal false positives
• Fixed streak warning spam
• Fixed emoji encoding issues
• Removed legacy notification code

### Turkish / Türkçe

🔔 Bildirim Sistemi Yenilendi (v2.9.77)

✅ Android 13+ Desteği - Bildirimleri kaçırmayın
• Modern Android cihazlar için izin sistemi eklendi
• En son Android sürümleriyle tam uyumluluk

📊 Akıllı Günlük Hedefler - Sadece gerçek başarılar
• Bugün tamamlanan görevleri doğru takip eder
• Artık yanlış "hedef ulaşıldı" bildirimleri yok
• Zaman damgası tabanlı ilerleme takibi

🔥 Akıllı Seri Uyarıları - İhtiyacınız olduğunda uyarır
• Sadece seriniz gerçekten risk altındaysa uyarır
• Uyarmadan önce bugün çalışıp çalışmadığınızı kontrol eder
• Gereksiz kesintiler yok

📢 Gelişmiş Güvenilirlik - Önemli şeyler için alan
• Günlük bildirim limiti artırıldı (2→10)
• Sınav tarih hatırlatıcıları kaçırılmaz
• Başarılar ve hatırlatıcılar daha iyi karışım

🎨 Mükemmel Emoji Gösterimi - Güzel bildirimler
• 🔔 Düzgün emoji ile çalışma hatırlatıcıları
• 🔥 Seri bildirimleri harika görünüyor
• 🌟 Başarı uyarıları parlıyor
• Artık ?? yer tutucu karakterler yok

🎯 Arayüz İyileştirmeleri - Daha fazla içerik
• Ana Sayfa ve Görevler'de başlık boşluğu azaltıldı
• Tüm ekranlarda tutarlı tasarım
• Profesyonel görsel hiyerarşi

Hata Düzeltmeleri:
• Android 13+ bildirim izni düzeltildi
• Günlük hedef yanlış pozitifleri düzeltildi
• Seri uyarısı spam'i düzeltildi
• Emoji kodlama sorunları düzeltildi
• Eski bildirim kodu temizlendi

---

## Version 2.9.76 (Version Code 105) — 2026-02-15

### English

🎯 UI Refinements (v2.9.76)

✨ Cleaner Interface - More content visibility
• Minimized header spacing in Home screen
• Optimized Tasks screen layout
• Consistent spacing across all main screens

📐 Professional Design - Visual consistency
• Home, Tasks, and Settings now match perfectly
• Reduced unnecessary whitespace
• Improved content-to-chrome ratio

Technical Improvements:
• Restructured layout hierarchy for efficiency
• Better padding management
• Proper system inset handling

### Turkish / Türkçe

🎯 Arayüz İyileştirmeleri (v2.9.76)

✨ Daha Temiz Arayüz - Daha fazla içerik görünürlüğü
• Ana Sayfa'da başlık boşluğu minimize edildi
• Görevler ekranı düzeni optimize edildi
• Tüm ana ekranlarda tutarlı boşluk

📐 Profesyonel Tasarım - Görsel tutarlılık
• Ana Sayfa, Görevler ve Ayarlar artık mükemmel uyumlu
• Gereksiz boşluklar azaltıldı
• İyileştirilmiş içerik-arayüz oranı

Teknik İyileştirmeler:
• Verimlilik için düzen hiyerarşisi yeniden yapılandırıldı
• Daha iyi dolgu yönetimi
• Uygun sistem kenar boşluğu işleme

---
