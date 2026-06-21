package com.example.sketchto3view.ui.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.sketchto3view.R
import com.example.sketchto3view.data.api.DefaultImageGenerationApi.Companion.PromptStyle
import com.example.sketchto3view.domain.model.GenerationTask
import com.example.sketchto3view.domain.model.TaskStatus
import com.example.sketchto3view.ui.animation.floatingCard
import com.example.sketchto3view.ui.animation.pressClickEffect
import com.example.sketchto3view.ui.animation.scaleFadeIn
import com.example.sketchto3view.ui.animation.slideUpEntrance
import com.example.sketchto3view.ui.animation.slideUpFadeIn
import com.example.sketchto3view.ui.animation.slideDownFadeOut
import com.example.sketchto3view.ui.theme.SketchTo3ViewTheme
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToImagePicker: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Completion banner state
    var completionBannerTaskId by remember { mutableStateOf<String?>(null) }

    // Observe completion events
    LaunchedEffect(Unit) {
        viewModel.completionEvent.collect { taskId ->
            completionBannerTaskId = taskId
            delay(3000)
            completionBannerTaskId = null
        }
    }

    // State for style selection dialog after gallery pick
    var pendingGalleryUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var showStyleDialog by remember { mutableStateOf(false) }

    // Gallery multi-select launcher for 批量上传
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            pendingGalleryUris = uris
            showStyleDialog = true
        }
    }

    // Style selection dialog
    if (showStyleDialog && pendingGalleryUris.isNotEmpty()) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showStyleDialog = false
                pendingGalleryUris = emptyList()
            },
            title = { Text("选择生成风格", fontWeight = FontWeight.SemiBold) },
            text = { Text("已选择 ${pendingGalleryUris.size} 张图片，请选择生成风格：") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.submitImages(pendingGalleryUris, PromptStyle.REALISTIC)
                    showStyleDialog = false
                    pendingGalleryUris = emptyList()
                }) {
                    Text("写实", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.submitImages(pendingGalleryUris, PromptStyle.CHIBI)
                    showStyleDialog = false
                    pendingGalleryUris = emptyList()
                }) {
                    Text("Q版")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeScreenContent(
            uiState = uiState,
            onNavigateToImagePicker = onNavigateToImagePicker,
            onNavigateToTaskDetail = onNavigateToTaskDetail,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToHistory = onNavigateToHistory,
            onBatchUploadClick = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        )

        // Completion banner overlay at top
        AnimatedVisibility(
            visible = completionBannerTaskId != null,
            enter = slideUpFadeIn(),
            exit = slideDownFadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            CompletionBanner(
                onClick = {
                    completionBannerTaskId?.let { taskId ->
                        onNavigateToTaskDetail(taskId)
                        completionBannerTaskId = null
                    }
                }
            )
        }
    }
}

@Composable
fun HomeScreenContent(
    uiState: HomeViewModel.UiState,
    onNavigateToImagePicker: () -> Unit = {},
    onNavigateToTaskDetail: (String) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onBatchUploadClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Brand Header - Dark card with white line art illustrations
            BrandHeader()

            Spacer(modifier = Modifier.height(20.dp))

            // 2 Action Buttons: 拍照 + 批量上传
            ActionButtons(
                onCameraClick = onNavigateToImagePicker,
                onBatchUploadClick = onBatchUploadClick
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Recent Task Pager Card
            RecentTaskPager(
                tasks = uiState.tasks,
                onTaskClick = onNavigateToTaskDetail
            )

            Spacer(modifier = Modifier.weight(1f))

            // Task Status Bar (for currently processing)
            AnimatedVisibility(
                visible = uiState.currentProcessingTask != null || uiState.activeTaskCount > 0,
                enter = slideUpFadeIn(),
                exit = slideDownFadeOut()
            ) {
                TaskStatusBar(
                    task = uiState.currentProcessingTask,
                    allTasks = uiState.tasks,
                    activeCount = uiState.activeTaskCount,
                    queuedCount = uiState.queuedTaskCount,
                    totalCount = uiState.totalTaskCount,
                    onClick = {
                        uiState.currentProcessingTask?.let { onNavigateToTaskDetail(it.id) }
                    }
                )
            }
        }

        // Bottom Navigation Bar - 3 tabs
        BottomNavBar(
            currentRoute = "home",
            onHomeClick = { /* Already on home */ },
            onHistoryClick = onNavigateToHistory,
            onSettingsClick = onNavigateToSettings
        )
    }
}

@Composable
private fun BrandHeader() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .scaleFadeIn(delay = 0L)
            .floatingCard(
                elevation = 16.dp,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Decorative white line art illustrations scattered across the card
            Image(
                painter = painterResource(id = R.drawable.slice_1),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = (-16).dp, y = 0.dp),
                contentScale = ContentScale.Fit,
                alpha = 0.9f
            )
            Image(
                painter = painterResource(id = R.drawable.slice_2),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.TopStart)
                    .offset(x = 20.dp, y = 20.dp),
                contentScale = ContentScale.Fit,
                alpha = 0.7f
            )
            Image(
                painter = painterResource(id = R.drawable.slice_4),
                contentDescription = null,
                modifier = Modifier
                    .size(70.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = 100.dp, y = (-20).dp),
                contentScale = ContentScale.Fit,
                alpha = 0.6f
            )
            Image(
                painter = painterResource(id = R.drawable.slice_5),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-20).dp, y = 16.dp),
                contentScale = ContentScale.Fit,
                alpha = 0.5f
            )
            Image(
                painter = painterResource(id = R.drawable.slice_6),
                contentDescription = null,
                modifier = Modifier
                    .size(35.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-80).dp, y = (-16).dp),
                contentScale = ContentScale.Fit,
                alpha = 0.5f
            )
            Image(
                painter = painterResource(id = R.drawable.slice_7),
                contentDescription = null,
                modifier = Modifier
                    .size(30.dp)
                    .align(Alignment.TopCenter)
                    .offset(x = 30.dp, y = 12.dp),
                contentScale = ContentScale.Fit,
                alpha = 0.4f
            )
        }
    }
}

@Composable
private fun ActionButtons(
    onCameraClick: () -> Unit,
    onBatchUploadClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ActionCard(
            title = "拍照",
            icon = Icons.Default.CameraAlt,
            onClick = onCameraClick,
            index = 0,
            modifier = Modifier.weight(1f)
        )
        ActionCard(
            title = "批量上传",
            icon = Icons.Default.Upload,
            onClick = onBatchUploadClick,
            index = 1,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    index: Int,
    modifier: Modifier = Modifier
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(40f) }

    LaunchedEffect(Unit) {
        delay(index * 60L + 100L)
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(400, easing = EaseOutCubic)
        )
    }

    LaunchedEffect(Unit) {
        delay(index * 60L + 100L)
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 200f)
        )
    }

    Card(
        modifier = modifier
            .height(72.dp)
            .graphicsLayer {
                this.alpha = alpha.value
                translationY = offsetY.value
            }
            .pressClickEffect()
            .floatingCard(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(28.dp),
                tint = Color(0xFF1A1A1A)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1A1A1A)
            )
        }
    }
}

@Composable
private fun RecentTaskPager(
    tasks: List<GenerationTask>,
    onTaskClick: (String) -> Unit
) {
    val completedTasks = tasks
        .filter { it.status == TaskStatus.COMPLETED }
        .sortedByDescending { it.createdAt }

    if (completedTasks.isEmpty()) {
        // Placeholder card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .slideUpEntrance(delay = 200L)
                .floatingCard(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFFCCCCCC)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "暂无生成记录",
                        fontSize = 15.sp,
                        color = Color(0xFF999999),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        val pagerState = rememberPagerState(pageCount = { completedTasks.size })

        Column {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .slideUpEntrance(delay = 200L)
                    .floatingCard(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(20.dp)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val task = completedTasks[page]
                    // Task index: sorted by createdAt DESC, so page 0 = newest = 任务1
                    val taskIndex = page
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val taskName = "任务${taskIndex + 1} · ${timeFormat.format(Date(task.createdAt))}"

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTaskClick(task.id) }
                    ) {
                        if (task.threeViewImagePath != null) {
                            AsyncImage(
                                model = File(task.threeViewImagePath),
                                contentDescription = "三视图",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(20.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Task name overlay at bottom
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .background(
                                    Color(0xCC1A1A1A),
                                    RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = taskName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Page indicator dots
            if (completedTasks.size > 1) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(completedTasks.size.coerceAtMost(5)) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                                .background(
                                    color = if (pagerState.currentPage == index) Color(0xFF1A1A1A) else Color(0xFFCCCCCC),
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskStatusBar(
    task: GenerationTask?,
    allTasks: List<GenerationTask>,
    activeCount: Int,
    queuedCount: Int,
    totalCount: Int,
    onClick: () -> Unit
) {
    val processingCount = allTasks.count { it.status == TaskStatus.PROCESSING }
    val queuedInDb = allTasks.count { it.status == TaskStatus.QUEUED }
    val displayActive = if (activeCount > 0) activeCount else processingCount
    val displayQueued = if (queuedCount > 0) queuedCount else queuedInDb
    val displayTotal = if (totalCount > 0) totalCount else (displayActive + displayQueued)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .slideUpEntrance(delay = 200L)
            .pressClickEffect()
            .floatingCard(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
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
            Column(modifier = Modifier.weight(1f)) {
                // Show "正在生成 N/M (X个排队中)" format
                val statusText = buildString {
                    append("正在生成 $displayActive/$displayTotal")
                    if (displayQueued > 0) {
                        append(" (${displayQueued}个排队中)")
                    }
                }
                Text(
                    text = statusText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Progress bar showing overall batch progress
                val completed = (displayTotal - displayActive - displayQueued).coerceAtLeast(0)
                val progress = if (displayTotal > 0) completed.toFloat() / displayTotal.toFloat() else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1A1A1A),
                    trackColor = Color(0xFFEEEEEE)
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(
    currentRoute: String,
    onHomeClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "首页") },
            label = {
                Text(
                    "首页",
                    fontWeight = if (currentRoute == "home") FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                )
            },
            selected = currentRoute == "home",
            onClick = onHomeClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF1A1A1A),
                selectedTextColor = Color(0xFF1A1A1A),
                unselectedIconColor = Color(0xFF999999),
                unselectedTextColor = Color(0xFF999999),
                indicatorColor = Color(0xFFF0F0F0)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.History, contentDescription = "历史") },
            label = {
                Text(
                    "历史",
                    fontWeight = if (currentRoute == "history") FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                )
            },
            selected = currentRoute == "history",
            onClick = onHistoryClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF1A1A1A),
                selectedTextColor = Color(0xFF1A1A1A),
                unselectedIconColor = Color(0xFF999999),
                unselectedTextColor = Color(0xFF999999),
                indicatorColor = Color(0xFFF0F0F0)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "设置") },
            label = {
                Text(
                    "设置",
                    fontWeight = if (currentRoute == "settings") FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                )
            },
            selected = currentRoute == "settings",
            onClick = onSettingsClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF1A1A1A),
                selectedTextColor = Color(0xFF1A1A1A),
                unselectedIconColor = Color(0xFF999999),
                unselectedTextColor = Color(0xFF999999),
                indicatorColor = Color(0xFFF0F0F0)
            )
        )
    }
}

@Composable
private fun CompletionBanner(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "✓ 生成完成! 点击查看",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_5")
@Composable
private fun HomeScreenPreview() {
    SketchTo3ViewTheme {
        HomeScreenContent(
            uiState = HomeViewModel.UiState(
                tasks = listOf(
                    GenerationTask(
                        id = "abc123",
                        sourceImagePath = "/mock/path.png",
                        status = TaskStatus.PROCESSING,
                        createdAt = System.currentTimeMillis()
                    ),
                    GenerationTask(
                        id = "def456",
                        sourceImagePath = "/mock/path2.png",
                        status = TaskStatus.COMPLETED,
                        threeViewImagePath = "/mock/three_view.png",
                        croppedImagePaths = listOf("/mock/front.png", "/mock/side.png", "/mock/top.png"),
                        createdAt = System.currentTimeMillis() - 60000
                    )
                ),
                isLoading = false,
                currentProcessingTask = GenerationTask(
                    id = "abc123",
                    sourceImagePath = "/mock/path.png",
                    status = TaskStatus.PROCESSING,
                    createdAt = System.currentTimeMillis()
                ),
                completedTaskCount = 1
            )
        )
    }
}
