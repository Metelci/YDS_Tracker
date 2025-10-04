package com.mtlc.studyplan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mtlc.studyplan.monitoring.*
import com.mtlc.studyplan.performance.PerformanceMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Basic Monitoring Dashboard
 * Displays real-time performance metrics and crash reports
 */
@Composable
fun MonitoringDashboard(
    crashReporter: CrashReporter,
    performanceMonitor: PerformanceMonitor,
    realTimeMonitor: RealTimePerformanceMonitor,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    // State collections
    var crashReports by remember { mutableStateOf<List<CrashReport>>(emptyList()) }
    var errorStats by remember { mutableStateOf(com.mtlc.studyplan.monitoring.ErrorStatistics()) }
    var performanceMetrics by remember { mutableStateOf(com.mtlc.studyplan.monitoring.RealTimeMetrics()) }
    var performanceAlerts by remember { mutableStateOf<List<PerformanceAlert>>(emptyList()) }

    // Collect data flows
    LaunchedEffect(Unit) {
        launch {
            crashReporter.crashReports.collectLatest { reports ->
                crashReports = reports.take(10) // Show last 10 crashes
            }
        }

        launch {
            crashReporter.errorStats.collectLatest { stats ->
                errorStats = stats
            }
        }

        launch {
            realTimeMonitor.performanceMetrics.collectLatest { metrics ->
                performanceMetrics = metrics
            }
        }

        launch {
            realTimeMonitor.performanceAlerts.collectLatest { alerts ->
                performanceAlerts = alerts.take(5) // Show last 5 alerts
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                text = "Monitoring Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Real-time Performance Metrics
        item {
            PerformanceMetricsCard(performanceMetrics, dateFormat)
        }

        // Error Statistics
        item {
            ErrorStatisticsCard(errorStats)
        }

        // Active Performance Alerts
        if (performanceAlerts.isNotEmpty()) {
            item {
                PerformanceAlertsCard(performanceAlerts, onClearAlerts = {
                    realTimeMonitor.clearAlerts()
                })
            }
        }

        // Recent Crash Reports
        if (crashReports.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Crash Reports",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(crashReports) { crash ->
                CrashReportCard(crash, dateFormat)
            }
        }

        // Performance Report
        item {
            PerformanceReportCard(realTimeMonitor)
        }

        // Action Buttons
        item {
            MonitoringActionsCard(
                onGenerateReport = {
                    scope.launch {
                        val report = realTimeMonitor.generatePerformanceReport()
                        // In a real app, this would show the report or save it
                        println("Performance Report Generated: $report")
                    }
                },
                onClearCrashes = {
                    crashReporter.clearCrashReports()
                },
                onRefresh = {
                    // Force refresh of metrics
                    scope.launch {
                        delay(100) // Small delay to show refresh
                    }
                }
            )
        }
    }
}

@Composable
private fun PerformanceMetricsCard(
    metrics: RealTimeMetrics,
    dateFormat: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Real-time Performance Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Last Updated: ${dateFormat.format(Date(metrics.timestamp))}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            // Memory Usage
            MetricRow(
                label = "Memory Usage",
                value = "${metrics.memoryMetrics.usedMemoryMB} MB",
                status = when {
                    metrics.memoryMetrics.usedMemoryMB > 150 -> MetricStatus.CRITICAL
                    metrics.memoryMetrics.usedMemoryMB > 100 -> MetricStatus.WARNING
                    else -> MetricStatus.GOOD
                }
            )

            // FPS
            MetricRow(
                label = "Frame Rate",
                value = if (metrics.fps > 0) "${String.format("%.1f", metrics.fps)} FPS" else "N/A",
                status = when {
                    metrics.fps > 0 && metrics.fps < 50 -> MetricStatus.CRITICAL
                    metrics.fps > 0 && metrics.fps < 55 -> MetricStatus.WARNING
                    else -> MetricStatus.GOOD
                }
            )

            // CPU Usage
            MetricRow(
                label = "CPU Usage",
                value = "${String.format("%.1f", metrics.cpuUsagePercent)}%",
                status = when {
                    metrics.cpuUsagePercent > 80 -> MetricStatus.CRITICAL
                    metrics.cpuUsagePercent > 60 -> MetricStatus.WARNING
                    else -> MetricStatus.GOOD
                }
            )

            // Cache Hit Rate
            MetricRow(
                label = "Cache Hit Rate",
                value = "${String.format("%.1f", metrics.cacheHitRate * 100)}%",
                status = when {
                    metrics.cacheHitRate < 0.7 -> MetricStatus.WARNING
                    else -> MetricStatus.GOOD
                }
            )

            // Thread Count
            MetricRow(
                label = "Active Threads",
                value = "${metrics.threadCount}",
                status = when {
                    metrics.threadCount > 50 -> MetricStatus.CRITICAL
                    metrics.threadCount > 30 -> MetricStatus.WARNING
                    else -> MetricStatus.GOOD
                }
            )

            // Active Alerts
            if (metrics.activeAlerts > 0) {
                Text(
                    text = "Active Alerts: ${metrics.activeAlerts}",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ErrorStatisticsCard(stats: ErrorStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Error Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatisticItem("Total Errors", stats.totalErrors.toString())
                StatisticItem("Total ANRs", stats.totalANRs.toString())
            }

            if (stats.mostCommonException.isNotEmpty()) {
                Text(
                    text = "Most Common: ${stats.mostCommonException}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            stats.lastErrorTime?.let { lastError ->
                Text(
                    text = "Last Error: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastError))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun PerformanceAlertsCard(
    alerts: List<PerformanceAlert>,
    onClearAlerts: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Performance Alerts (${alerts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Button(onClick = onClearAlerts) {
                    Text("Clear All")
                }
            }

            alerts.forEach { alert ->
                AlertItem(alert)
            }
        }
    }
}

@Composable
private fun AlertItem(alert: PerformanceAlert) {
    val backgroundColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> Color.Red.copy(alpha = 0.1f)
        AlertSeverity.HIGH -> Color(0xFFFF6B35).copy(alpha = 0.1f)
        AlertSeverity.MEDIUM -> Color.Yellow.copy(alpha = 0.1f)
        AlertSeverity.LOW -> Color.Gray.copy(alpha = 0.1f)
    }

    val textColor = when (alert.severity) {
        AlertSeverity.CRITICAL -> Color.Red
        AlertSeverity.HIGH -> Color(0xFFFF6B35)
        AlertSeverity.MEDIUM -> Color(0xFF8B4513)
        AlertSeverity.LOW -> Color.Gray
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Text(
            text = alert.title,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
        Text(
            text = alert.message,
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
        Text(
            text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(alert.timestamp)),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun CrashReportCard(
    crash: CrashReport,
    dateFormat: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = crash.exceptionClass.substringAfterLast('.'),
                fontWeight = FontWeight.Bold,
                color = Color.Red
            )

            Text(
                text = crash.message,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "${crash.context} • ${dateFormat.format(Date(crash.timestamp))}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Text(
                text = "Thread: ${crash.threadName} (${crash.threadId})",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun PerformanceReportCard(realTimeMonitor: RealTimePerformanceMonitor) {
    var report by remember { mutableStateOf<PerformanceReport?>(null) }

    LaunchedEffect(Unit) {
        report = realTimeMonitor.generatePerformanceReport()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Performance Report",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            report?.let { perfReport ->
                Text("Uptime: ${perfReport.uptime / 1000}s")
                Text("Active Alerts: ${perfReport.activeAlerts}")
                Text("History Size: ${perfReport.historySize}")

                if (perfReport.recommendations.isNotEmpty()) {
                    Text(
                        text = "Recommendations:",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    perfReport.recommendations.forEach { recommendation ->
                        Text(
                            text = "• $recommendation",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8B4513)
                        )
                    }
                }
            } ?: run {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
private fun MonitoringActionsCard(
    onGenerateReport: () -> Unit,
    onClearCrashes: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onGenerateReport,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Generate Report")
                }

                OutlinedButton(
                    onClick = onClearCrashes,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear Crashes")
                }

                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Refresh")
                }
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    status: MetricStatus
) {
    val color = when (status) {
        MetricStatus.GOOD -> Color.Green
        MetricStatus.WARNING -> Color(0xFFFFA500) // Orange
        MetricStatus.CRITICAL -> Color.Red
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StatisticItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

private enum class MetricStatus {
    GOOD, WARNING, CRITICAL
}