package com.example.sketchto3view.ui.detail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.sketchto3view.ui.animation.floatingCard
import com.example.sketchto3view.ui.animation.pressClickEffect
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    onNavigateToManualCrop: () -> Unit,
    onNavigateToImagePreview: (Int) -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TaskDetailViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "sketch_${taskId.take(6)}.png",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "已完成 • 2分钟前",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F5F5),
                    titleContentColor = Color(0xFF1A1A1A)
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        val task = uiState.task

        if (task == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1A1A1A))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Large image preview card (single image)
            AnimatedSection(index = 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressClickEffect()
                        .floatingCard(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigateToImagePreview(-1) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
                ) {
                    task.threeViewImagePath?.let { imagePath ->
                        AsyncImage(
                            model = java.io.File(imagePath),
                            contentDescription = "三视图预览",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 3f)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } ?: Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("预览图片", color = Color(0xFF999999))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // "裁切视图" section with "调整" button
            AnimatedSection(index = 1) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "裁切视图",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A1A1A)
                        )
                        OutlinedButton(
                            onClick = onNavigateToManualCrop,
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = Color(0xFFEEEEEE)
                            ),
                            contentPadding = ButtonDefaults.ContentPadding
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFF1A1A1A)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "调整",
                                color = Color(0xFF1A1A1A),
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3 cropped view cards in a row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val viewLabels = listOf("正面图", "侧面图", "俯视图")
                        viewLabels.forEachIndexed { index, label ->
                            CroppedViewCard(
                                label = label,
                                imagePath = task.croppedImagePaths.getOrNull(index),
                                onClick = { onNavigateToImagePreview(index) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Download buttons
            AnimatedSection(index = 2) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 打包下载
                    Button(
                        onClick = { viewModel.downloadAsZip() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressClickEffect()
                            .floatingCard(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A1A1A)
                        ),
                        contentPadding = ButtonDefaults.ContentPadding,
                        enabled = !uiState.isDownloading
                    ) {
                        Icon(
                            Icons.Default.FolderZip,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("打包下载", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 全部下载
                        OutlinedButton(
                            onClick = { viewModel.downloadAll() },
                            modifier = Modifier
                                .weight(1f)
                                .pressClickEffect()
                                .floatingCard(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = Color(0xFFEEEEEE)
                            ),
                            enabled = !uiState.isDownloading
                        ) {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF1A1A1A)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("全部下载", fontSize = 13.sp, color = Color(0xFF1A1A1A))
                        }

                        // 单独保存
                        OutlinedButton(
                            onClick = { viewModel.downloadSingle(0) },
                            modifier = Modifier
                                .weight(1f)
                                .pressClickEffect()
                                .floatingCard(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = Color(0xFFEEEEEE)
                            ),
                            enabled = !uiState.isDownloading
                        ) {
                            Icon(
                                Icons.Default.SaveAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF1A1A1A)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("单独保存", fontSize = 13.sp, color = Color(0xFF1A1A1A))
                        }
                    }

                    if (uiState.isDownloading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF1A1A1A),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("下载中...", fontSize = 13.sp, color = Color(0xFF666666))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Share buttons
            AnimatedSection(index = 3) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 分享ZIP
                        OutlinedButton(
                            onClick = { viewModel.shareAsZip() },
                            modifier = Modifier
                                .weight(1f)
                                .pressClickEffect()
                                .floatingCard(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = Color(0xFFEEEEEE)
                            ),
                            enabled = !uiState.isDownloading
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("分享ZIP", fontSize = 13.sp, color = Color(0xFF1A1A1A))
                        }

                        // 分享图片
                        OutlinedButton(
                            onClick = { viewModel.shareImage(0) },
                            modifier = Modifier
                                .weight(1f)
                                .pressClickEffect()
                                .floatingCard(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = Color(0xFFEEEEEE)
                            ),
                            enabled = !uiState.isDownloading
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("分享图片", fontSize = 13.sp, color = Color(0xFF1A1A1A))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AnimatedSection(
    index: Int,
    content: @Composable () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(30f) }

    LaunchedEffect(Unit) {
        delay(index * 80L + 100L)
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(400, easing = EaseOutCubic)
        )
    }

    LaunchedEffect(Unit) {
        delay(index * 80L + 100L)
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 200f)
        )
    }

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = offsetY.value
        }
    ) {
        content()
    }
}

@Composable
private fun CroppedViewCard(
    label: String,
    imagePath: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(0.75f)
            .pressClickEffect()
            .floatingCard(
                elevation = 10.dp,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                imagePath?.let { path ->
                    AsyncImage(
                        model = java.io.File(path),
                        contentDescription = label,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_5")
@Composable
private fun TaskDetailScreenPreview() {
    com.example.sketchto3view.ui.theme.SketchTo3ViewTheme {
        TaskDetailScreenContent(
            taskId = "abc123",
            task = com.example.sketchto3view.domain.model.GenerationTask(
                id = "abc123",
                sourceImagePath = "/mock/source.png",
                status = com.example.sketchto3view.domain.model.TaskStatus.COMPLETED,
                threeViewImagePath = "/mock/three_view.png",
                croppedImagePaths = listOf("/mock/front.png", "/mock/side.png", "/mock/top.png"),
                createdAt = System.currentTimeMillis()
            ),
            isDownloading = false,
            onNavigateBack = {},
            onNavigateToManualCrop = {},
            onNavigateToImagePreview = {},
            onDownloadAsZip = {},
            onDownloadAll = {},
            onDownloadSingle = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskDetailScreenContent(
    taskId: String,
    task: com.example.sketchto3view.domain.model.GenerationTask?,
    isDownloading: Boolean,
    onNavigateBack: () -> Unit,
    onNavigateToManualCrop: () -> Unit,
    onNavigateToImagePreview: (Int) -> Unit,
    onDownloadAsZip: () -> Unit,
    onDownloadAll: () -> Unit,
    onDownloadSingle: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "sketch_${taskId.take(6)}.png",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "已完成 • 2分钟前",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F5F5),
                    titleContentColor = Color(0xFF1A1A1A)
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        if (task == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1A1A1A))
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Large image preview card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("预览图片", color = Color(0xFF999999))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // "裁切视图" section
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "裁切视图",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A1A)
                    )
                    OutlinedButton(
                        onClick = onNavigateToManualCrop,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = Color(0xFFEEEEEE)
                        )
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF1A1A1A)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("调整", color = Color(0xFF1A1A1A), fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("正面图", "侧面图", "俯视图").forEach { label ->
                        CroppedViewCard(
                            label = label,
                            imagePath = null,
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Download buttons
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onDownloadAsZip,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                    enabled = !isDownloading
                ) {
                    Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("打包下载", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDownloadAll,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
                        enabled = !isDownloading
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF1A1A1A))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("全部下载", fontSize = 13.sp, color = Color(0xFF1A1A1A))
                    }

                    OutlinedButton(
                        onClick = onDownloadSingle,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
                        enabled = !isDownloading
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF1A1A1A))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("单独保存", fontSize = 13.sp, color = Color(0xFF1A1A1A))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
