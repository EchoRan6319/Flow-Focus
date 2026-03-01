package com.echoran.flowfocus.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.echoran.flowfocus.data.model.FocusSessionEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val stats by viewModel.dashboardStats.collectAsState()
    val selectedRange by viewModel.selectedRange.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("专注统计", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        Spacer(modifier = Modifier.height(16.dp))

        // Range Selector
        TabRow(selectedTabIndex = selectedRange.ordinal) {
            StatsTimeRange.values().forEach { range ->
                val label = when(range) {
                    StatsTimeRange.DAY -> "今日"
                    StatsTimeRange.WEEK -> "本周"
                    StatsTimeRange.MONTH -> "本月"
                    StatsTimeRange.YEAR -> "本年"
                }
                Tab(
                    selected = selectedRange == range,
                    onClick = { viewModel.setTimeRange(range) },
                    text = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                SummaryCard(stats)
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("分类分布", fontWeight = FontWeight.Bold)
                CategoryDistContent(stats.categoryDistribution)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("最近专注记录", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }

            items(stats.recentSessions) { session ->
                SessionItem(session)
            }
        }
    }
}

@Composable
fun SummaryCard(stats: DashboardStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("时长", "${stats.totalMinutes}m")
            StatItem("次数", "${stats.sessionCount}")
            StatItem("平均", "${stats.avgMinutesPerDay}m/d")
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CategoryDistContent(dist: Map<String, Int>) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (dist.isEmpty()) {
                Text("暂无数据", color = MaterialTheme.colorScheme.secondary)
            } else {
                dist.forEach { (cat, mins) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(cat)
                        Text("${mins} 分钟")
                    }
                }
            }
        }
    }
}

@Composable
fun SessionItem(session: FocusSessionEntity) {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    val dateStr = sdf.format(Date(session.startTime))

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(dateStr, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "${session.category} | ${if (session.isStrict) "严格" else "普通"}",
                    color = if (session.isStrict) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                )
            }
            Text("${session.durationMinutes} 分钟", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
