package com.example.sketchto3view.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sketchto3view.ui.animation.floatingCard
import com.example.sketchto3view.ui.animation.pressClickEffect
import com.example.sketchto3view.ui.home.BottomNavBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var maxConcurrency by remember { mutableStateOf("3") }
    var scheduleDelay by remember { mutableStateOf("500") }
    var autoCrop by remember { mutableStateOf(true) }
    var serviceProvider by remember { mutableStateOf("OpenAI") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Title
        Text(
            text = "设置",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 16.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Section: API 配置
            SectionHeader("API 配置")
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .floatingCard(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // API Key
                    Text(
                        text = "API Key",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.apiKey,
                        onValueChange = { viewModel.updateApiKey(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("输入你的 API Key", color = Color(0xFFAAAAAA)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1A1A1A),
                            unfocusedBorderColor = Color(0xFFEEEEEE),
                            cursorColor = Color(0xFF1A1A1A)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 服务商
                    Text(
                        text = "服务商",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = serviceProvider,
                        onValueChange = { serviceProvider = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("选择服务商", color = Color(0xFFAAAAAA)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1A1A1A),
                            unfocusedBorderColor = Color(0xFFEEEEEE),
                            cursorColor = Color(0xFF1A1A1A)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: 生成设置
            SectionHeader("生成设置")
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .floatingCard(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 最大并发数
                    Text(
                        text = "最大并发数",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = maxConcurrency,
                        onValueChange = { maxConcurrency = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1A1A1A),
                            unfocusedBorderColor = Color(0xFFEEEEEE),
                            cursorColor = Color(0xFF1A1A1A)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 调度延迟
                    Text(
                        text = "调度延迟 (ms)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = scheduleDelay,
                        onValueChange = { scheduleDelay = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1A1A1A),
                            unfocusedBorderColor = Color(0xFFEEEEEE),
                            cursorColor = Color(0xFF1A1A1A)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 自动裁切 toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "自动裁切",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333)
                        )
                        Switch(
                            checked = autoCrop,
                            onCheckedChange = { autoCrop = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF1A1A1A),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFCCCCCC)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: 使用帮助
            SectionHeader("使用帮助")
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressClickEffect()
                    .floatingCard(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "使用帮助",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF1A1A1A)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "使用帮助",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "了解如何使用三视图生成功能",
                            fontSize = 12.sp,
                            color = Color(0xFF999999)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFCCCCCC)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: 存储
            SectionHeader("存储")
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .floatingCard(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "已使用空间",
                            fontSize = 14.sp,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = "128 MB / 1 GB",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { 0.128f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = Color(0xFF1A1A1A),
                        trackColor = Color(0xFFEEEEEE),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 清除所有数据 button
                    OutlinedButton(
                        onClick = { /* TODO: Clear data */ },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFE53935)
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.height(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "清除所有数据",
                            color = Color(0xFFE53935),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Bottom Navigation Bar (Settings tab active) - 3 tabs
        BottomNavBar(
            currentRoute = "settings",
            onHomeClick = onNavigateBack,
            onHistoryClick = onNavigateToHistory,
            onSettingsClick = { /* Already on settings */ }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF1A1A1A)
    )
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_5")
@Composable
private fun SettingsScreenPreview() {
    com.example.sketchto3view.ui.theme.SketchTo3ViewTheme {
        SettingsScreenContent(
            apiKey = "sk-xxxx...xxxx",
            serviceProvider = "OpenAI",
            maxConcurrency = "3",
            scheduleDelay = "500",
            autoCrop = true,
            onNavigateBack = {},
            onNavigateToHistory = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    apiKey: String,
    serviceProvider: String,
    maxConcurrency: String,
    scheduleDelay: String,
    autoCrop: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    var localApiKey by remember { mutableStateOf(apiKey) }
    var localServiceProvider by remember { mutableStateOf(serviceProvider) }
    var localMaxConcurrency by remember { mutableStateOf(maxConcurrency) }
    var localScheduleDelay by remember { mutableStateOf(scheduleDelay) }
    var localAutoCrop by remember { mutableStateOf(autoCrop) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Title
        Text(
            text = "设置",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 16.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Section: API 配置
            SectionHeader("API 配置")
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .floatingCard(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("API Key", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = localApiKey,
                        onValueChange = { localApiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("输入你的 API Key", color = Color(0xFFAAAAAA)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1A1A1A),
                            unfocusedBorderColor = Color(0xFFEEEEEE),
                            cursorColor = Color(0xFF1A1A1A)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("服务商", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = localServiceProvider,
                        onValueChange = { localServiceProvider = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1A1A1A),
                            unfocusedBorderColor = Color(0xFFEEEEEE),
                            cursorColor = Color(0xFF1A1A1A)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: 生成设置
            SectionHeader("生成设置")
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .floatingCard(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("最大并发数", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = localMaxConcurrency,
                        onValueChange = { localMaxConcurrency = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1A1A1A),
                            unfocusedBorderColor = Color(0xFFEEEEEE),
                            cursorColor = Color(0xFF1A1A1A)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("调度延迟 (ms)", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = localScheduleDelay,
                        onValueChange = { localScheduleDelay = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1A1A1A),
                            unfocusedBorderColor = Color(0xFFEEEEEE),
                            cursorColor = Color(0xFF1A1A1A)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("自动裁切", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                        Switch(
                            checked = localAutoCrop,
                            onCheckedChange = { localAutoCrop = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF1A1A1A),
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFFCCCCCC)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: 使用帮助
            SectionHeader("使用帮助")
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressClickEffect()
                    .floatingCard(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "使用帮助",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF1A1A1A)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "使用帮助",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "了解如何使用三视图生成功能",
                            fontSize = 12.sp,
                            color = Color(0xFF999999)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFCCCCCC)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: 存储
            SectionHeader("存储")
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .floatingCard(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("已使用空间", fontSize = 14.sp, color = Color(0xFF333333))
                        Text("128 MB / 1 GB", fontSize = 14.sp, color = Color(0xFF666666))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { 0.128f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = Color(0xFF1A1A1A),
                        trackColor = Color(0xFFEEEEEE),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE53935))
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.height(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("清除所有数据", color = Color(0xFFE53935), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Bottom Navigation Bar - 3 tabs
        BottomNavBar(
            currentRoute = "settings",
            onHomeClick = onNavigateBack,
            onHistoryClick = onNavigateToHistory,
            onSettingsClick = {}
        )
    }
}
