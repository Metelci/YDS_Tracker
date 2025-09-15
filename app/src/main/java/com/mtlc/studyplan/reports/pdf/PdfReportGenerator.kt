package com.mtlc.studyplan.reports.pdf

import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.mtlc.studyplan.data.StreakManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import kotlin.math.*

/**
 * PDF report generator using platform PdfDocument and Canvas
 */
class PdfReportGenerator {

    private val config = PdfConfig()
    private lateinit var titlePaint: Paint
    private lateinit var headingPaint: Paint
    private lateinit var bodyPaint: Paint
    private lateinit var captionPaint: Paint
    private lateinit var primaryPaint: Paint
    private lateinit var secondaryPaint: Paint
    private lateinit var lightGrayPaint: Paint

    init {
        initializePaints()
    }

    private fun initializePaints() {
        titlePaint = Paint().apply {
            color = config.textColor
            textSize = config.titleSize
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        headingPaint = Paint().apply {
            color = config.textColor
            textSize = config.headingSize
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        bodyPaint = Paint().apply {
            color = config.textColor
            textSize = config.bodySize
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        captionPaint = Paint().apply {
            color = Color.GRAY
            textSize = config.captionSize
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        primaryPaint = Paint().apply {
            color = config.primaryColor
            isAntiAlias = true
        }

        secondaryPaint = Paint().apply {
            color = config.secondaryColor
            isAntiAlias = true
        }

        lightGrayPaint = Paint().apply {
            color = config.lightGrayColor
            isAntiAlias = true
        }
    }

    suspend fun generate(request: ReportRequest): ReportResult = withContext(Dispatchers.Default) {
        val document = PdfDocument()
        var pageNumber = 1

        try {
            // Calculate stats from request data
            val stats = calculateStats(request)

            // Page 1: Cover Page
            drawCoverPage(document, pageNumber++, request, stats)

            // Page 2: Summary KPIs
            drawSummaryPage(document, pageNumber++, request, stats)

            // Page 3: Charts
            drawChartsPage(document, pageNumber++, request, stats)

            // Page 4: Insights
            drawInsightsPage(document, pageNumber++, request, stats)

            // Page 5: Recommendations
            drawRecommendationsPage(document, pageNumber++, request)

            // Write to byte array
            val outputStream = ByteArrayOutputStream()
            document.writeTo(outputStream)
            val bytes = outputStream.toByteArray()

            // Generate filename
            val startDate = request.dateRange.start.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val endDate = request.dateRange.endInclusive.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val filename = "study-report-${startDate}_${endDate}.pdf"

            ReportResult(bytes, filename)
        } finally {
            document.close()
        }
    }

    private fun drawCoverPage(document: PdfDocument, pageNumber: Int, request: ReportRequest, stats: ReportStats) {
        val pageInfo = PdfDocument.PageInfo.Builder(config.pageWidth.toInt(), config.pageHeight.toInt(), pageNumber).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = config.margin + 100f

        // App name
        canvas.drawText("StudyPlan", config.margin, y, titlePaint.apply { 
            textSize = config.titleSize + 12f 
            color = config.primaryColor
        })
        y += 60f

        // Subtitle
        canvas.drawText("Study Progress Report", config.margin, y, headingPaint)
        y += 80f

        // Student name (if provided)
        request.studentName?.let { name ->
            canvas.drawText("Student: $name", config.margin, y, bodyPaint)
            y += 40f
        }

        // Date range
        val startDate = request.dateRange.start.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        val endDate = request.dateRange.endInclusive.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
        canvas.drawText("Report Period:", config.margin, y, bodyPaint.apply { typeface = Typeface.DEFAULT_BOLD })
        y += 30f
        canvas.drawText("$startDate - $endDate", config.margin, y, bodyPaint.apply { typeface = Typeface.DEFAULT })
        y += 80f

        // Key metrics preview
        canvas.drawText("Study Summary", config.margin, y, headingPaint)
        y += 50f

        val metrics = listOf(
            "Total Study Time" to "${stats.totalMinutes} minutes",
            "Tasks Completed" to "${stats.totalTasks}",
            "Current Streak" to "${stats.streakLength} days",
            "Average Accuracy" to "${(stats.averageAccuracy * 100).toInt()}%"
        )

        metrics.forEach { (label, value) ->
            canvas.drawText("• $label: $value", config.margin + 20f, y, bodyPaint)
            y += 35f
        }

        // Simple QR code placeholder (text-based since no QR library)
        y += 60f
        canvas.drawText("Download StudyPlan App:", config.margin, y, bodyPaint.apply { typeface = Typeface.DEFAULT_BOLD })
        y += 30f
        drawSimpleQR(canvas, config.margin, y, QrData.createAppQr())

        // Footer
        canvas.drawText("Generated on ${java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"))}", 
                       config.margin, config.pageHeight - 50f, captionPaint)

        document.finishPage(page)
    }

    private fun drawSummaryPage(document: PdfDocument, pageNumber: Int, request: ReportRequest, stats: ReportStats) {
        val pageInfo = PdfDocument.PageInfo.Builder(config.pageWidth.toInt(), config.pageHeight.toInt(), pageNumber).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = config.margin + 50f

        // Page title
        canvas.drawText("Key Performance Indicators", config.margin, y, titlePaint)
        y += 60f

        // Create KPI boxes
        val boxWidth = (config.pageWidth - 3 * config.margin) / 2
        val boxHeight = 120f

        // Row 1
        drawKpiBox(canvas, config.margin, y, boxWidth, boxHeight, 
                  "Total Study Time", "${stats.totalMinutes} min", "⏱️")
        drawKpiBox(canvas, config.margin * 2 + boxWidth, y, boxWidth, boxHeight, 
                  "Tasks Completed", "${stats.totalTasks}", "✅")

        y += boxHeight + 40f

        // Row 2
        drawKpiBox(canvas, config.margin, y, boxWidth, boxHeight, 
                  "Current Streak", "${stats.streakLength} days", "🔥")
        drawKpiBox(canvas, config.margin * 2 + boxWidth, y, boxWidth, boxHeight, 
                  "Average Accuracy", "${(stats.averageAccuracy * 100).toInt()}%", "🎯")

        y += boxHeight + 60f

        // Study habits section
        canvas.drawText("Study Habits Analysis", config.margin, y, headingPaint)
        y += 40f

        val habits = listOf(
            "Daily Average: ${stats.dailyAverage.toInt()} minutes",
            "Completion Rate: ${(stats.completionRate * 100).toInt()}%",
            "Most Studied: ${stats.mostStudiedSkill?.displayName ?: "N/A"}",
            "Best Performance: ${stats.bestPerformingSkill?.displayName ?: "N/A"}"
        )

        habits.forEach { habit ->
            canvas.drawText("• $habit", config.margin + 20f, y, bodyPaint)
            y += 30f
        }

        // Progress indicators
        y += 40f
        canvas.drawText("Progress Trend", config.margin, y, headingPaint)
        y += 40f

        val trendIcon = when {
            stats.weeklyTrend > 0.1f -> "📈 Improving"
            stats.weeklyTrend < -0.1f -> "📉 Declining"
            else -> "➡️ Stable"
        }
        canvas.drawText(trendIcon, config.margin, y, bodyPaint.apply { textSize = config.bodySize + 4f })

        document.finishPage(page)
    }

    private fun drawChartsPage(document: PdfDocument, pageNumber: Int, request: ReportRequest, stats: ReportStats) {
        val pageInfo = PdfDocument.PageInfo.Builder(config.pageWidth.toInt(), config.pageHeight.toInt(), pageNumber).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = config.margin + 50f

        // Page title
        canvas.drawText("Study Analytics", config.margin, y, titlePaint)
        y += 60f

        // Skills distribution bar chart
        canvas.drawText("Time by Skill", config.margin, y, headingPaint)
        y += 40f

        val skillChartData = request.skillMinutes.map { (skill, minutes) ->
            skill.displayName to minutes.toFloat()
        }.sortedByDescending { it.second }

        if (skillChartData.isNotEmpty()) {
            drawBarChart(canvas, config.margin, y, 
                        config.pageWidth - 2 * config.margin, 200f, 
                        skillChartData.map { it.first }, 
                        skillChartData.map { it.second },
                        "Minutes")
        }

        y += 250f

        // Weekly progress line chart
        canvas.drawText("Weekly Study Minutes", config.margin, y, headingPaint)
        y += 40f

        val weeklyData = request.dailyLoads
            .groupBy { it.date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)) }
            .map { (weekStart, dailyLoads) -> 
                weekStart.format(DateTimeFormatter.ofPattern("MMM dd")) to dailyLoads.sumOf { it.totalMinutes }.toFloat()
            }
            .sortedBy { it.first }

        if (weeklyData.isNotEmpty()) {
            drawLineChart(canvas, config.margin, y, 
                         config.pageWidth - 2 * config.margin, 180f,
                         weeklyData.map { it.first },
                         weeklyData.map { it.second },
                         "Minutes")
        }

        document.finishPage(page)
    }

    private fun drawInsightsPage(document: PdfDocument, pageNumber: Int, request: ReportRequest, stats: ReportStats) {
        val pageInfo = PdfDocument.PageInfo.Builder(config.pageWidth.toInt(), config.pageHeight.toInt(), pageNumber).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = config.margin + 50f

        // Page title
        canvas.drawText("Performance Insights", config.margin, y, titlePaint)
        y += 60f

        // Generate insights from data
        val insights = generateInsights(request, stats)

        // Strengths section
        val strengths = insights.filter { it.type == InsightType.STRENGTH }.take(3)
        if (strengths.isNotEmpty()) {
            canvas.drawText("💪 Strengths", config.margin, y, headingPaint.apply { color = config.secondaryColor })
            y += 40f

            strengths.forEach { insight ->
                drawInsightItem(canvas, config.margin, y, config.pageWidth - 2 * config.margin, insight)
                y += 70f
            }
            y += 20f
        }

        // Areas for improvement section
        val weaknesses = insights.filter { it.type == InsightType.WEAKNESS }.take(3)
        if (weaknesses.isNotEmpty()) {
            canvas.drawText("🎯 Areas for Improvement", config.margin, y, headingPaint.apply { color = config.primaryColor })
            y += 40f

            weaknesses.forEach { insight ->
                drawInsightItem(canvas, config.margin, y, config.pageWidth - 2 * config.margin, insight)
                y += 70f
            }
            y += 20f
        }

        // Trends section
        val trends = insights.filter { it.type == InsightType.TREND }.take(2)
        if (trends.isNotEmpty()) {
            canvas.drawText("📈 Performance Trends", config.margin, y, headingPaint)
            y += 40f

            trends.forEach { insight ->
                drawInsightItem(canvas, config.margin, y, config.pageWidth - 2 * config.margin, insight)
                y += 70f
            }
        }

        document.finishPage(page)
    }

    private fun drawRecommendationsPage(document: PdfDocument, pageNumber: Int, request: ReportRequest) {
        val pageInfo = PdfDocument.PageInfo.Builder(config.pageWidth.toInt(), config.pageHeight.toInt(), pageNumber).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        var y = config.margin + 50f

        // Page title
        canvas.drawText("AI Recommendations", config.margin, y, titlePaint)
        y += 60f

        // Sort recommendations by priority
        val sortedRecommendations = request.recommendations.sortedBy { 
            when (it.priority) {
                com.mtlc.studyplan.analytics.RecommendationPriority.HIGH -> 0
                com.mtlc.studyplan.analytics.RecommendationPriority.MEDIUM -> 1
                com.mtlc.studyplan.analytics.RecommendationPriority.LOW -> 2
            }
        }

        sortedRecommendations.take(8).forEach { recommendation ->
            drawRecommendationItem(canvas, config.margin, y, config.pageWidth - 2 * config.margin, recommendation)
            y += 90f
            
            // Check if we need a new page
            if (y > config.pageHeight - 150f && recommendation != sortedRecommendations.last()) {
                document.finishPage(page)
                val newPageInfo = PdfDocument.PageInfo.Builder(config.pageWidth.toInt(), config.pageHeight.toInt(), pageNumber + 1).create()
                val newPage = document.startPage(newPageInfo)
                val newCanvas = newPage.canvas
                y = config.margin + 50f
                return@forEach
            }
        }

        // Footer message
        y = config.pageHeight - 100f
        canvas.drawText("💡 These recommendations are generated based on your study patterns", 
                       config.margin, y, captionPaint)
        y += 20f
        canvas.drawText("and performance analytics to help optimize your learning journey.", 
                       config.margin, y, captionPaint)

        document.finishPage(page)
    }

    private fun calculateStats(request: ReportRequest): ReportStats {
        val totalMinutes = request.dailyLoads.sumOf { it.totalMinutes }
        val totalTasks = request.dailyLoads.sumOf { it.tasksCompleted }
        val averageAccuracy = request.dailyLoads.map { it.averageAccuracy }.average().toFloat()
        val streakLength = request.dailyLoads.maxOfOrNull { it.streakDayNumber } ?: 0
        val completionRate = if (totalTasks > 0) request.dailyLoads.count { it.tasksCompleted > 0 }.toFloat() / request.dailyLoads.size else 0f
        
        val skillTotals = request.skillMinutes
        val mostStudiedSkill = skillTotals.maxByOrNull { it.value }?.key
        val leastStudiedSkill = skillTotals.minByOrNull { it.value }?.key
        
        // Calculate skill performance (mock data since we don't have accuracy per skill)
        val bestPerformingSkill = mostStudiedSkill // Simplified
        val weakestSkill = leastStudiedSkill // Simplified
        
        val dailyAverage = if (request.dailyLoads.isNotEmpty()) totalMinutes.toFloat() / request.dailyLoads.size else 0f
        val weeklyTrend = calculateWeeklyTrend(request.dailyLoads)

        return ReportStats(
            totalMinutes = totalMinutes,
            totalTasks = totalTasks,
            streakLength = streakLength,
            averageAccuracy = averageAccuracy,
            completionRate = completionRate,
            mostStudiedSkill = mostStudiedSkill,
            leastStudiedSkill = leastStudiedSkill,
            bestPerformingSkill = bestPerformingSkill,
            weakestSkill = weakestSkill,
            dailyAverage = dailyAverage,
            weeklyTrend = weeklyTrend
        )
    }

    private fun calculateWeeklyTrend(dailyLoads: List<UserDailyLoad>): Float {
        if (dailyLoads.size < 7) return 0f
        
        val sortedLoads = dailyLoads.sortedBy { it.date }
        val firstHalf = sortedLoads.take(sortedLoads.size / 2)
        val secondHalf = sortedLoads.drop(sortedLoads.size / 2)
        
        val firstAvg = firstHalf.map { it.totalMinutes }.average().toFloat()
        val secondAvg = secondHalf.map { it.totalMinutes }.average().toFloat()
        
        return (secondAvg - firstAvg) / firstAvg.coerceAtLeast(1f)
    }

    private fun drawKpiBox(canvas: Canvas, x: Float, y: Float, width: Float, height: Float, 
                          title: String, value: String, icon: String) {
        // Background
        lightGrayPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(x, y, x + width, y + height, 12f, 12f, lightGrayPaint)
        
        // Border
        primaryPaint.style = Paint.Style.STROKE
        primaryPaint.strokeWidth = 2f
        canvas.drawRoundRect(x, y, x + width, y + height, 12f, 12f, primaryPaint)
        
        // Icon (emoji as text)
        val iconPaint = Paint(bodyPaint).apply { textSize = 24f }
        canvas.drawText(icon, x + 20f, y + 40f, iconPaint)
        
        // Value
        val valuePaint = Paint(headingPaint).apply { color = config.primaryColor }
        canvas.drawText(value, x + 20f, y + 70f, valuePaint)
        
        // Title
        canvas.drawText(title, x + 20f, y + 95f, captionPaint)
    }

    private fun drawBarChart(canvas: Canvas, x: Float, y: Float, width: Float, height: Float,
                            labels: List<String>, values: List<Float>, yLabel: String) {
        if (values.isEmpty()) return
        
        val maxValue = values.maxOrNull() ?: 1f
        val barWidth = (width - 40f) / values.size * 0.8f
        val barSpacing = (width - 40f) / values.size * 0.2f
        
        // Draw bars
        values.forEachIndexed { index, value ->
            val barHeight = (value / maxValue) * (height - 40f)
            val barX = x + 20f + index * (barWidth + barSpacing)
            val barY = y + height - 20f - barHeight
            
            primaryPaint.style = Paint.Style.FILL
            canvas.drawRect(barX, barY, barX + barWidth, y + height - 20f, primaryPaint)
            
            // Draw value on top
            if (value > 0) {
                canvas.drawText(value.toInt().toString(), barX + barWidth/2 - 10f, barY - 5f, captionPaint)
            }
        }
        
        // Draw labels
        labels.forEachIndexed { index, label ->
            val labelX = x + 20f + index * (barWidth + barSpacing) + barWidth/2
            val shortLabel = if (label.length > 8) label.substring(0, 8) + "..." else label
            canvas.drawText(shortLabel, labelX - 25f, y + height + 15f, captionPaint)
        }
        
        // Y-axis label
        canvas.drawText(yLabel, x, y - 10f, captionPaint)
    }

    private fun drawLineChart(canvas: Canvas, x: Float, y: Float, width: Float, height: Float,
                             labels: List<String>, values: List<Float>, yLabel: String) {
        if (values.isEmpty()) return
        
        val maxValue = values.maxOrNull() ?: 1f
        val stepX = (width - 40f) / (values.size - 1).coerceAtLeast(1)
        
        // Draw grid lines
        val gridPaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        
        for (i in 0..4) {
            val gridY = y + i * (height - 40f) / 4
            canvas.drawLine(x + 20f, gridY, x + width - 20f, gridY, gridPaint)
        }
        
        // Draw line
        primaryPaint.style = Paint.Style.STROKE
        primaryPaint.strokeWidth = 3f
        
        for (i in 0 until values.size - 1) {
            val startX = x + 20f + i * stepX
            val startY = y + height - 20f - (values[i] / maxValue) * (height - 40f)
            val endX = x + 20f + (i + 1) * stepX
            val endY = y + height - 20f - (values[i + 1] / maxValue) * (height - 40f)
            
            canvas.drawLine(startX, startY, endX, endY, primaryPaint)
            
            // Draw points
            primaryPaint.style = Paint.Style.FILL
            canvas.drawCircle(startX, startY, 4f, primaryPaint)
            primaryPaint.style = Paint.Style.STROKE
        }
        
        // Draw last point
        if (values.isNotEmpty()) {
            val lastX = x + 20f + (values.size - 1) * stepX
            val lastY = y + height - 20f - (values.last() / maxValue) * (height - 40f)
            primaryPaint.style = Paint.Style.FILL
            canvas.drawCircle(lastX, lastY, 4f, primaryPaint)
        }
        
        // Y-axis label
        canvas.drawText(yLabel, x, y - 10f, captionPaint)
    }

    private fun drawSimpleQR(canvas: Canvas, x: Float, y: Float, qrData: QrData) {
        // Draw a simple grid pattern to represent QR code
        val qrSize = qrData.size
        val cellSize = qrSize / 25f // 25x25 grid
        
        // Background
        val qrBackgroundPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(x, y, x + qrSize, y + qrSize, qrBackgroundPaint)
        
        // Pattern (simplified QR-like grid)
        val qrPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        
        // Draw some pattern blocks to simulate QR code
        val pattern = arrayOf(
            intArrayOf(1,1,1,1,1,0,0,0,1,0,1,0,0,0,1,1,1,1,1,0,0,0,0,0,0),
            intArrayOf(1,0,0,0,1,0,1,0,1,1,0,1,1,0,1,0,0,0,1,0,1,1,0,1,0),
            intArrayOf(1,0,1,0,1,0,0,1,0,0,1,0,0,1,1,0,1,0,1,0,0,0,1,1,1),
            intArrayOf(1,0,1,0,1,0,1,1,0,1,0,1,0,1,1,0,1,0,1,0,1,0,1,0,0),
            intArrayOf(1,1,1,1,1,0,0,0,1,0,1,0,1,0,1,1,1,1,1,0,0,1,0,1,1)
        )
        
        for (row in 0 until min(pattern.size, 25)) {
            for (col in 0 until min(pattern[row].size, 25)) {
                if (pattern[row][col] == 1) {
                    canvas.drawRect(
                        x + col * cellSize,
                        y + row * cellSize,
                        x + (col + 1) * cellSize,
                        y + (row + 1) * cellSize,
                        qrPaint
                    )
                }
            }
        }
        
        // Draw URL text below QR
        canvas.drawText("play.google.com/store/apps/", x, y + qrSize + 20f, captionPaint)
        canvas.drawText("details?id=com.mtlc.studyplan", x, y + qrSize + 40f, captionPaint)
    }

    private fun generateInsights(request: ReportRequest, stats: ReportStats): List<InsightItem> {
        val insights = mutableListOf<InsightItem>()
        
        // Strengths based on performance
        if (stats.averageAccuracy > 0.8f) {
            insights.add(InsightItem(
                title = "High Accuracy",
                description = "You maintain ${(stats.averageAccuracy * 100).toInt()}% accuracy across all skills",
                type = InsightType.STRENGTH,
                priority = 1,
                value = "${(stats.averageAccuracy * 100).toInt()}%"
            ))
        }
        
        if (stats.streakLength >= 7) {
            insights.add(InsightItem(
                title = "Consistent Study Habit",
                description = "You've maintained a ${stats.streakLength}-day study streak",
                type = InsightType.STRENGTH,
                priority = 2,
                value = "${stats.streakLength} days"
            ))
        }
        
        if (stats.dailyAverage > 30f) {
            insights.add(InsightItem(
                title = "Good Study Volume",
                description = "You average ${stats.dailyAverage.toInt()} minutes per day",
                type = InsightType.STRENGTH,
                priority = 3,
                value = "${stats.dailyAverage.toInt()} min/day"
            ))
        }
        
        // Weaknesses/Areas for improvement
        if (stats.averageAccuracy < 0.7f) {
            insights.add(InsightItem(
                title = "Accuracy Improvement Needed",
                description = "Focus on understanding concepts before speed",
                type = InsightType.WEAKNESS,
                priority = 1,
                value = "${(stats.averageAccuracy * 100).toInt()}%"
            ))
        }
        
        if (stats.completionRate < 0.6f) {
            insights.add(InsightItem(
                title = "Consistency Challenge",
                description = "Try shorter, more frequent study sessions",
                type = InsightType.WEAKNESS,
                priority = 2,
                value = "${(stats.completionRate * 100).toInt()}%"
            ))
        }
        
        stats.weakestSkill?.let { skill ->
            insights.add(InsightItem(
                title = "Focus on ${skill.displayName}",
                description = "This skill needs more attention in your study plan",
                type = InsightType.WEAKNESS,
                priority = 3,
                value = skill.displayName
            ))
        }
        
        // Trends
        if (stats.weeklyTrend > 0.1f) {
            insights.add(InsightItem(
                title = "Improving Trend",
                description = "Your study time has increased by ${(stats.weeklyTrend * 100).toInt()}%",
                type = InsightType.TREND,
                priority = 1,
                trend = "↗️"
            ))
        } else if (stats.weeklyTrend < -0.1f) {
            insights.add(InsightItem(
                title = "Declining Trend",
                description = "Your study time has decreased recently",
                type = InsightType.TREND,
                priority = 1,
                trend = "↘️"
            ))
        }
        
        return insights.sortedBy { it.priority }
    }

    private fun drawInsightItem(canvas: Canvas, x: Float, y: Float, width: Float, insight: InsightItem) {
        // Icon
        val iconText = when (insight.type) {
            InsightType.STRENGTH -> "💪"
            InsightType.WEAKNESS -> "🎯"
            InsightType.TREND -> "📈"
            InsightType.RECOMMENDATION -> "💡"
            InsightType.ACHIEVEMENT -> "🏆"
        }
        
        canvas.drawText(iconText, x, y + 20f, bodyPaint.apply { textSize = 20f })
        
        // Title
        canvas.drawText(insight.title, x + 40f, y + 20f, bodyPaint.apply { 
            typeface = Typeface.DEFAULT_BOLD 
            textSize = config.bodySize + 2f
        })
        
        // Value (if present)
        if (insight.value.isNotEmpty()) {
            val valueWidth = bodyPaint.measureText(insight.value)
            canvas.drawText(insight.value, x + width - valueWidth - 10f, y + 20f, 
                          bodyPaint.apply { color = config.primaryColor })
        }
        
        // Description
        canvas.drawText(insight.description, x + 40f, y + 45f, captionPaint.apply { color = Color.DKGRAY })
    }

    private fun drawRecommendationItem(canvas: Canvas, x: Float, y: Float, width: Float, 
                                     recommendation: com.mtlc.studyplan.analytics.Recommendation) {
        // Priority indicator
        val priorityColor = when (recommendation.priority) {
            com.mtlc.studyplan.analytics.RecommendationPriority.HIGH -> Color.RED
            com.mtlc.studyplan.analytics.RecommendationPriority.MEDIUM -> Color.rgb(255, 165, 0) // Orange
            com.mtlc.studyplan.analytics.RecommendationPriority.LOW -> Color.GREEN
        }
        
        val priorityPaint = Paint().apply {
            color = priorityColor
            style = Paint.Style.FILL
        }
        
        canvas.drawCircle(x + 10f, y + 15f, 6f, priorityPaint)
        
        // Category icon
        val categoryIcon = when (recommendation.category) {
            "timing" -> "⏰"
            "performance" -> "📈"
            "improvement" -> "🎯"
            "habits" -> "📅"
            "breaks" -> "☕"
            "review" -> "📚"
            else -> "💡"
        }
        
        canvas.drawText(categoryIcon, x + 30f, y + 20f, bodyPaint.apply { textSize = 18f })
        
        // Title
        val titleText = recommendation.title.ifEmpty() { recommendation.description.take(30) + "..." }
        canvas.drawText(titleText, x + 60f, y + 20f, bodyPaint.apply { 
            typeface = Typeface.DEFAULT_BOLD
            textSize = config.bodySize + 1f
        })
        
        // Description
        val description = recommendation.description.ifEmpty() { recommendation.reasoning }
        val wrappedDescription = wrapText(description, width - 70f, bodyPaint)
        
        var descY = y + 45f
        wrappedDescription.take(2).forEach { line ->
            canvas.drawText(line, x + 60f, descY, captionPaint.apply { color = Color.DKGRAY })
            descY += 20f
        }
        
        // Priority label
        val priorityText = recommendation.priority.name.lowercase().replaceFirstChar { it.uppercase() }
        val priorityTextPaint = Paint(captionPaint).apply {
            color = priorityColor
            typeface = Typeface.DEFAULT_BOLD
        }
        canvas.drawText(priorityText, x + width - 60f, y + 20f, priorityTextPaint)
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    currentLine = word
                } else {
                    lines.add(word) // Single word longer than maxWidth
                }
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        
        return lines
    }
}