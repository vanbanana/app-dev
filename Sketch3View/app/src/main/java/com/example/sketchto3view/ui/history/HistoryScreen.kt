package com.example.sketchto3view.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.sketchto3view.domain.model.GenerationTask
import com.example.sketchto3view.domain.model.TaskStatus
import com.example.sketchto3view.ui.animation.floatingCard
import com.example.sketchto3view.ui.animation.pressClickEffect
import com.example.sketchto3view.ui.animation.slideUpFadeIn
import com.example.sketchto3view.ui.animation.slideDownFadeOut
import com.example.sketchto3view.ui.home.BottomNavBar
import com.example.sketchto3view.ui.theme.SketchTo3ViewTheme
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTaskDetail: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    HistoryScreenContent(
        uiState = uiState,
        onNavigateToHome = onNavigateToHome,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToTaskDetail = onNavigateToTaskDetail,
        onToggleViewMode = { viewModel.toggleViewMode() },
        onDownloadAll = { viewModel.downloadAllTasks() },
        onRetryTask = { taskId -> viewModel.retryTask(taskId) },
        onShareLastDownload = { viewModel.shareLastDownload() },
        onToggleTaskSelection = { taskId -> viewModel.toggleTaskSelection(taskId) },
        onClearSelection = { viewModel.clearSelection() },
        onDownloadSelected = { viewModel.downloadSelected() }
    )
}

@Composable
fun HistoryScreenContent(
    uiState: HistoryViewModel.UiState,
    onNavigateToHome: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTaskDetail: (String) -> Unit = {},
    onToggleViewMode: () -> Unit = {},
    onDownloadAll: () -> Unit = {},
    onRetryTask: (String) -> Unit = {},
    onShareLastDownload: () -> Unit = {},
    onToggleTaskSelection: (String) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onDownloadSelected: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Title + Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "历史记录",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )

            // Toggle button: 网格/列表
            Card(
                modifier = Modifier
                    .pressClickEffect()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleViewMode
                    ),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (uiState.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = if (uiState.isGridView) "切换列表" else "切换网格",
                        modifier = Modifier.size(18.dp),
                        tint = Color(0xFF1A1A1A)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.isGridView) "列表" else "网格",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1A1A1A)
                    )
                }
            }
        }

        // Active task status header
        if (uiState.activeTaskCount > 0 || uiState.queuedTaskCount > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7FF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF42A5F5), RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "当前: ${uiState.activeTaskCount}个生成中, ${uiState.queuedTaskCount}个排队中",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1976D2)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Download All button
        if (uiState.tasks.any { it.status == TaskStatus.COMPLETED }) {
            DownloadAllButton(
                isDownloading = uiState.isDownloadingAll,
                onClick = onDownloadAll,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Download message
            uiState.downloadMessage?.let { message ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message,
                        fontSize = 12.sp,
                        color = if (message.startsWith("下载失败")) Color(0xFFE53935) else Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState.showShareButton) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Card(
                            modifier = Modifier
                                .pressClickEffect()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onShareLastDownload
                                ),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "分享",
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "分享",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Content
        if (uiState.tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color(0xFFCCCCCC)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无历史记录",
                        fontSize = 15.sp,
                        color = Color(0xFF999999),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (uiState.isGridView) {
            // Grid View
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 20.dp, end = 20.dp, top = 8.dp,
                    bottom = if (uiState.isSelectionMode) 80.dp else 8.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(uiState.tasks) { index, task ->
                    GridTaskItem(
                        task = task,
                        index = index,
                        totalTasks = uiState.tasks.size,
                        isSelected = task.id in uiState.selectedTaskIds,
                        isSelectionMode = uiState.isSelectionMode,
                        onClick = {
                            if (uiState.isSelectionMode) {
                                onToggleTaskSelection(task.id)
                            } else {
                                onNavigateToTaskDetail(task.id)
                            }
                        },
                        onLongClick = { onToggleTaskSelection(task.id) },
                        onRetry = { onRetryTask(task.id) }
                    )
                }
            }
        } else {
            // List View
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 20.dp, end = 20.dp, top = 8.dp,
                    bottom = if (uiState.isSelectionMode) 80.dp else 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(uiState.tasks) { index, task ->
                    ListTaskItem(
                        task = task,
                        index = index,
                        totalTasks = uiState.tasks.size,
                        isSelected = task.id in uiState.selectedTaskIds,
                        isSelectionMode = uiState.isSelectionMode,
                        onClick = {
                            if (uiState.isSelectionMode) {
                                onToggleTaskSelection(task.id)
                            } else {
                                onNavigateToTaskDetail(task.id)
                            }
                        },
                        onLongClick = { onToggleTaskSelection(task.id) },
                        onRetry = { onRetryTask(task.id) }
                    )
                }
            }
        }

        // Bottom Navigation Bar
        BottomNavBar(
            currentRoute = "history",
            onHomeClick = onNavigateToHome,
            onHistoryClick = { /* Already on history */ },
            onSettingsClick = onNavigateToSettings
        )
    }

        // Selection mode floating action bar
        AnimatedVisibility(
            visible = uiState.isSelectionMode,
            enter = slideUpFadeIn(),
            exit = slideDownFadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) {
            SelectionActionBar(
                selectedCount = uiState.selectedTaskIds.size,
                isDownloading = uiState.isDownloadingAll,
                onDownload = onDownloadSelected,
                onClear = onClearSelection
            )
        }
    }
}

@Composable
private fun DownloadAllButton(
    isDownloading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressClickEffect()
            .floatingCard(
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = !isDownloading,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "全部打包下载",
                modifier = Modifier.size(20.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isDownloading) "打包中..." else "全部打包下载",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            if (isDownloading) {
                Spacer(modifier = Modifier.width(12.dp))
                LinearProgressIndicator(
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    trackColor = Color(0xFF444444)
                )
            }
        }
    }
}

@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .floatingCard(
                elevation = 12.dp,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clear selection button
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "取消选择",
                modifier = Modifier
                    .size(22.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClear
                    ),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "已选${selectedCount}个",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            // Download button
            Card(
                modifier = Modifier
                    .pressClickEffect()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !isDownloading,
                        onClick = onDownload
                    ),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "打包下载",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isDownloading) "打包中..." else "打包下载",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridTaskItem(
    task: GenerationTask,
    index: Int,
    totalTasks: Int,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onRetry: () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(30f) }

    LaunchedEffect(Unit) {
        delay(index * 50L)
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(350, easing = EaseOutCubic)
        )
    }

    LaunchedEffect(Unit) {
        delay(index * 50L)
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 200f)
        )
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val taskName = "任务${index + 1} · ${timeFormat.format(Date(task.createdAt))}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha.value
                translationY = offsetY.value
            }
            .pressClickEffect()
            .floatingCard(
                elevation = 8.dp,
                shape = RoundedCornerShape(14.dp)
            )
            .then(
                if (isSelected) Modifier.border(2.5.dp, Color(0xFF42A5F5), RoundedCornerShape(14.dp))
                else Modifier
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                if (task.threeViewImagePath != null) {
                    AsyncImage(
                        model = File(task.threeViewImagePath),
                        contentDescription = "三视图",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFFCCCCCC)
                    )
                }

                // Status badge
                if (task.status != TaskStatus.COMPLETED) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(
                                color = when (task.status) {
                                    TaskStatus.QUEUED -> Color(0xFFFFA726)
                                    TaskStatus.PROCESSING -> Color(0xFF42A5F5)
                                    TaskStatus.FAILED -> Color(0xFFE53935)
                                    else -> Color.Transparent
                                },
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when (task.status) {
                                TaskStatus.QUEUED -> "排队中"
                                TaskStatus.PROCESSING -> "处理中"
                                TaskStatus.FAILED -> "失败"
                                else -> ""
                            },
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Selection checkbox
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .size(24.dp)
                            .background(
                                color = if (isSelected) Color(0xFF42A5F5) else Color(0x80FFFFFF),
                                shape = CircleShape
                            )
                            .border(
                                width = if (isSelected) 0.dp else 1.5.dp,
                                color = if (isSelected) Color.Transparent else Color(0xFF999999),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "已选中",
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Task name + retry
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = taskName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A1A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (task.status == TaskStatus.FAILED) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "重新生成",
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onRetry
                            ),
                        tint = Color(0xFFE53935)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListTaskItem(
    task: GenerationTask,
    index: Int,
    totalTasks: Int,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onRetry: () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        delay(index * 40L)
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(300, easing = EaseOutCubic)
        )
    }

    LaunchedEffect(Unit) {
        delay(index * 40L)
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 200f)
        )
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val taskName = "任务${index + 1} · ${timeFormat.format(Date(task.createdAt))}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha.value
                translationY = offsetY.value
            }
            .pressClickEffect()
            .floatingCard(
                elevation = 6.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .then(
                if (isSelected) Modifier.border(2.5.dp, Color(0xFF42A5F5), RoundedCornerShape(12.dp))
                else Modifier
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection checkbox for list mode
            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = if (isSelected) Color(0xFF42A5F5) else Color.Transparent,
                            shape = CircleShape
                        )
                        .border(
                            width = if (isSelected) 0.dp else 1.5.dp,
                            color = if (isSelected) Color.Transparent else Color(0xFF999999),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "已选中",
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            // Thumbnail
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                if (task.threeViewImagePath != null) {
                    AsyncImage(
                        model = File(task.threeViewImagePath),
                        contentDescription = "三视图",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFFCCCCCC)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Task info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = taskName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A1A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = when (task.status) {
                                    TaskStatus.QUEUED -> Color(0xFFFFA726)
                                    TaskStatus.PROCESSING -> Color(0xFF42A5F5)
                                    TaskStatus.COMPLETED -> Color(0xFF4CAF50)
                                    TaskStatus.FAILED -> Color(0xFFE53935)
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (task.status) {
                            TaskStatus.QUEUED -> "排队中"
                            TaskStatus.PROCESSING -> "处理中..."
                            TaskStatus.COMPLETED -> "已完成"
                            TaskStatus.FAILED -> "失败"
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                }
            }

            // Retry or chevron
            if (task.status == TaskStatus.FAILED) {
                Card(
                    modifier = Modifier
                        .pressClickEffect()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onRetry
                        ),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "重新生成",
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFE53935)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "重试",
                            fontSize = 11.sp,
                            color = Color(0xFFE53935),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFCCCCCC)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_5")
@Composable
private fun HistoryScreenPreview() {
    SketchTo3ViewTheme {
        HistoryScreenContent(
            uiState = HistoryViewModel.UiState(
                tasks = listOf(
                    GenerationTask(
                        id = "abc123",
                        sourceImagePath = "/mock/path.png",
                        status = TaskStatus.COMPLETED,
                        threeViewImagePath = "/mock/three_view.png",
                        createdAt = System.currentTimeMillis()
                    ),
                    GenerationTask(
                        id = "def456",
                        sourceImagePath = "/mock/path2.png",
                        status = TaskStatus.PROCESSING,
                        createdAt = System.currentTimeMillis() - 60000
                    ),
                    GenerationTask(
                        id = "ghi789",
                        sourceImagePath = "/mock/path3.png",
                        status = TaskStatus.COMPLETED,
                        threeViewImagePath = "/mock/three_view2.png",
                        createdAt = System.currentTimeMillis() - 120000
                    )
                ),
                isGridView = true
            )
        )
    }
}
