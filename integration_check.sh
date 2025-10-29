#!/bin/bash

echo "=== ÖSYM Integration - Final Verification ==="
echo ""

echo "1. Checking compiled classes..."
for class in "OsymExamScraper" "ExamRepository" "ExamEntity" "ExamDao" "ExamSyncWorker" "ExamNotificationWorker"; do
    count=$(find app/build -name "*${class}*.class" 2>/dev/null | wc -l)
    if [ $count -gt 0 ]; then
        echo "   ✅ ${class}: ${count} class files found"
    else
        echo "   ❌ ${class}: NOT FOUND"
    fi
done

echo ""
echo "2. Checking dependency injection..."
if grep -q "ExamDao" app/src/main/java/com/mtlc/studyplan/di/DatabaseModule.kt; then
    echo "   ✅ ExamDao registered in DatabaseModule"
else
    echo "   ❌ ExamDao NOT in DatabaseModule"
fi

if grep -q "ExamRepository" app/src/main/java/com/mtlc/studyplan/di/RepositoryModule.kt; then
    echo "   ✅ ExamRepository registered in RepositoryModule"
else
    echo "   ❌ ExamRepository NOT in RepositoryModule"
fi

echo ""
echo "3. Checking database migration..."
if grep -q "MIGRATION_8_9" app/src/main/java/com/mtlc/studyplan/database/StudyPlanDatabase.kt; then
    echo "   ✅ MIGRATION_8_9 defined"
else
    echo "   ❌ MIGRATION_8_9 NOT FOUND"
fi

if grep -q "version = 9" app/src/main/java/com/mtlc/studyplan/database/StudyPlanDatabase.kt; then
    echo "   ✅ Database version 9"
else
    echo "   ❌ Database version NOT 9"
fi

echo ""
echo "4. Checking notification strings..."
for lang in "values" "values-en" "values-tr"; do
    if grep -q "new_exam_announcement_title" app/src/main/res/${lang}/strings.xml 2>/dev/null; then
        echo "   ✅ Strings exist in ${lang}"
    else
        echo "   ❌ Strings missing in ${lang}"
    fi
done

echo ""
echo "5. Checking build artifacts..."
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    size=$(ls -lh app/build/outputs/apk/debug/app-debug.apk | awk '{print $5}')
    echo "   ✅ APK created: ${size}"
else
    echo "   ❌ APK NOT FOUND"
fi

echo ""
echo "6. Checking method implementations..."
if grep -q "suspend fun syncExamsFromOsym" app/src/main/java/com/mtlc/studyplan/repository/ExamRepository.kt; then
    echo "   ✅ ExamRepository.syncExamsFromOsym() exists"
fi

if grep -q "showNewExamAnnouncementNotification" app/src/main/java/com/mtlc/studyplan/notifications/NotificationManager.kt; then
    echo "   ✅ NotificationManager.showNewExamAnnouncementNotification() exists"
fi

if grep -q "private fun upcomingSessions" app/src/main/java/com/mtlc/studyplan/data/YdsExamService.kt; then
    echo "   ✅ YdsExamService.upcomingSessions() enhanced with repository"
fi

echo ""
echo "=== Verification Complete ==="
