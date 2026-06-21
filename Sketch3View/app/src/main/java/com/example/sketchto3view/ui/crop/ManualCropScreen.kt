package com.example.sketchto3view.ui.crop

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.sketchto3view.ui.animation.pressClickEffect
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualCropScreen(
    taskId: String,
    onNavigateBack: () -> Unit,
    viewModel: ManualCropViewModel = hiltViewModel()
) {
    val cropState by viewModel.cropState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ManualCropViewModel.UiEvent.CropSuccess -> onNavigateBack()
                is ManualCropViewModel.UiEvent.CropError -> { }
            }
        }
    }

    // Entrance animation
    val screenAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        screenAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(300, easing = EaseOutCubic)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .graphicsLayer { alpha = screenAlpha.value }
    ) {
        // Top toolbar: "取消" on left, "调整裁切线" center, "完成" on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onNavigateBack) {
                Text(
                    "取消",
                    color = Color.White,
                    fontSize = 15.sp
                )
            }
            Text(
                text = "调整裁切线",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            TextButton(
                onClick = { viewModel.confirmCrop() },
                enabled = !cropState.isProcessing
            ) {
                if (cropState.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "完成",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Image with white crop lines and drag handles
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            CropImageWithLines(
                imagePath = cropState.threeViewImagePath,
                cropLines = cropState.cropLines,
                onCropLineChanged = { index, position ->
                    viewModel.updateCropLine(index, position)
                }
            )
        }

        // "实时预览" section with 3 preview cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "实时预览",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            CropPreviewRow(
                imagePath = cropState.threeViewImagePath,
                cropLines = cropState.cropLines
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // "确认裁切" button at bottom
        Button(
            onClick = { viewModel.confirmCrop() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .pressClickEffect(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            enabled = !cropState.isProcessing
        ) {
            Text(
                "确认裁切",
                color = Color(0xFF1A1A1A),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun CropImageWithLines(
    imagePath: String?,
    cropLines: List<Float>,
    onCropLineChanged: (Int, Float) -> Unit
) {
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var activeLine by remember { mutableIntStateOf(-1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .onSizeChanged { imageSize = it }
    ) {
        // Background image
        imagePath?.let { path ->
            AsyncImage(
                model = java.io.File(path),
                contentDescription = "三视图",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // White crop lines overlay with drag handling
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cropLines) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val width = size.width.toFloat()
                            activeLine = cropLines.indices.minByOrNull { index ->
                                kotlin.math.abs(offset.x - cropLines[index] * width)
                            }?.takeIf { index ->
                                kotlin.math.abs(offset.x - cropLines[index] * width) < 40f
                            } ?: -1
                        },
                        onDrag = { change, _ ->
                            if (activeLine >= 0) {
                                val newPosition = change.position.x / size.width.toFloat()
                                onCropLineChanged(activeLine, newPosition)
                            }
                        },
                        onDragEnd = { activeLine = -1 }
                    )
                }
        ) {
            val width = size.width
            val height = size.height

            cropLines.forEachIndexed { index, position ->
                val animatedX = position * width

                // Draw white dashed line
                drawLine(
                    color = Color.White,
                    start = Offset(animatedX, 0f),
                    end = Offset(animatedX, height),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )

                // Draw white handle circle at center
                val handleRadius = if (index == activeLine) 18f else 14f
                val innerRadius = if (index == activeLine) 8f else 6f

                drawCircle(
                    color = Color.White,
                    radius = handleRadius,
                    center = Offset(animatedX, height / 2f)
                )
                drawCircle(
                    color = Color(0xFF1A1A1A),
                    radius = innerRadius,
                    center = Offset(animatedX, height / 2f)
                )
            }
        }
    }
}

@Composable
private fun CropPreviewRow(
    imagePath: String?,
    cropLines: List<Float>
) {
    val labels = listOf("正面图", "侧面图", "俯视图")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(3) { index ->
            Card(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.85f),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
            ) {
                Crossfade(
                    targetState = cropLines.hashCode(),
                    animationSpec = tween(200),
                    label = "cropPreview$index"
                ) { _ ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF3A3A3A)),
                            contentAlignment = Alignment.Center
                        ) {
                            imagePath?.let {
                                AsyncImage(
                                    model = java.io.File(it),
                                    contentDescription = labels[index],
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = labels[index],
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_5")
@Composable
private fun ManualCropScreenPreview() {
    com.example.sketchto3view.ui.theme.SketchTo3ViewTheme {
        ManualCropScreenContent(
            cropLines = listOf(0.33f, 0.66f),
            isProcessing = false,
            imagePath = null,
            onCancel = {},
            onConfirm = {},
            onCropLineChanged = { _, _ -> }
        )
    }
}

@Composable
private fun ManualCropScreenContent(
    cropLines: List<Float>,
    isProcessing: Boolean,
    imagePath: String?,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onCropLineChanged: (Int, Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
    ) {
        // Top toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("取消", color = Color.White, fontSize = 15.sp)
            }
            Text(
                text = "调整裁切线",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = onConfirm, enabled = !isProcessing) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "完成",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Image area with crop lines
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            CropImageWithLines(
                imagePath = imagePath,
                cropLines = cropLines,
                onCropLineChanged = onCropLineChanged
            )
        }

        // Preview section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "实时预览",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            CropPreviewRow(imagePath = imagePath, cropLines = cropLines)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm button
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            enabled = !isProcessing
        ) {
            Text(
                "确认裁切",
                color = Color(0xFF1A1A1A),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
