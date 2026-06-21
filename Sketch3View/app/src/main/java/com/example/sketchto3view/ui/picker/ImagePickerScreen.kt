package com.example.sketchto3view.ui.picker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.sketchto3view.ui.animation.floatingCard
import com.example.sketchto3view.ui.animation.pressClickEffect
import com.example.sketchto3view.ui.animation.slideUpFadeIn
import com.example.sketchto3view.ui.animation.slideDownFadeOut
import com.example.sketchto3view.ui.home.HomeViewModel
import com.example.sketchto3view.data.api.DefaultImageGenerationApi.Companion.PromptStyle
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerScreen(
    onNavigateBack: () -> Unit,
    onTasksCreated: (List<String>) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedStyle by remember { mutableStateOf(PromptStyle.REALISTIC) }
    var showCropDialog by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var cropOutputUri by remember { mutableStateOf<Uri?>(null) }

    // Crop launcher
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            cropOutputUri?.let { uri ->
                selectedImages = selectedImages + uri
            }
        } else {
            // Crop cancelled or failed, use original photo
            pendingCameraUri?.let { uri ->
                selectedImages = selectedImages + uri
            }
        }
        pendingCameraUri = null
        cropOutputUri = null
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                pendingCameraUri = uri
                showCropDialog = true
            }
        }
    }

    // Gallery multi-select launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages = selectedImages + uris
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val photoFile = File.createTempFile(
                "sketch_${System.currentTimeMillis()}",
                ".jpg",
                context.cacheDir
            )
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            showPermissionDialog = true
        }
    }

    // Permission dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("需要权限") },
            text = { Text("需要相机权限来拍摄草图。请在设置中授予权限。") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) {
                    Text("打开设置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Crop dialog after camera capture
    if (showCropDialog && pendingCameraUri != null) {
        AlertDialog(
            onDismissRequest = {
                // Dismiss = use photo as-is
                pendingCameraUri?.let { uri ->
                    selectedImages = selectedImages + uri
                }
                pendingCameraUri = null
                showCropDialog = false
            },
            title = { Text("拍照完成") },
            text = { Text("是否需要裁切图片?") },
            confirmButton = {
                TextButton(onClick = {
                    showCropDialog = false
                    pendingCameraUri?.let { sourceUri ->
                        try {
                            val cropFile = File.createTempFile(
                                "crop_${System.currentTimeMillis()}",
                                ".png",
                                context.cacheDir
                            )
                            val outputUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                cropFile
                            )
                            cropOutputUri = outputUri
                            val cropIntent = Intent("com.android.camera.action.CROP").apply {
                                setDataAndType(sourceUri, "image/*")
                                putExtra("crop", "true")
                                putExtra("output", outputUri)
                                putExtra("outputFormat", "PNG")
                                putExtra("return-data", false)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            }
                            cropLauncher.launch(cropIntent)
                        } catch (e: Exception) {
                            // If crop intent not available, use original
                            selectedImages = selectedImages + sourceUri
                            pendingCameraUri = null
                            cropOutputUri = null
                        }
                    }
                }) {
                    Text("裁切")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCropDialog = false
                    pendingCameraUri?.let { uri ->
                        selectedImages = selectedImages + uri
                    }
                    pendingCameraUri = null
                }) {
                    Text("直接使用")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "选择图片",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
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

        // Selected count indicator
        if (selectedImages.isNotEmpty()) {
            Text(
                text = "已选择 ${selectedImages.size} 张图片",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
        }

        // Image Grid - 3x3
        if (selectedImages.isEmpty()) {
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
                        text = "暂无图片\n点击下方按钮添加图片",
                        fontSize = 14.sp,
                        color = Color(0xFF999999),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(selectedImages) { index, uri ->
                    AnimatedImageGridItem(
                        uri = uri,
                        index = index,
                        isSelected = true
                    )
                }
            }
        }

        // Bottom Buttons - pinned to bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Style selector toggle buttons
            AnimatedVisibility(
                visible = selectedImages.isNotEmpty(),
                enter = slideUpFadeIn(),
                exit = slideDownFadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // 写实 (Realistic) button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selectedStyle == PromptStyle.REALISTIC) Color(0xFF1A1A1A) else Color.Transparent
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { selectedStyle = PromptStyle.REALISTIC }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "写实",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedStyle == PromptStyle.REALISTIC) Color.White else Color(0xFF1A1A1A)
                        )
                    }
                    // Q版 (Chibi) button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selectedStyle == PromptStyle.CHIBI) Color(0xFF1A1A1A) else Color.Transparent
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { selectedStyle = PromptStyle.CHIBI }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Q版",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedStyle == PromptStyle.CHIBI) Color.White else Color(0xFF1A1A1A)
                        )
                    }
                }
            }

            // Primary button - Generate 3-view
            AnimatedVisibility(
                visible = selectedImages.isNotEmpty(),
                enter = slideUpFadeIn(),
                exit = slideDownFadeOut()
            ) {
                Button(
                    onClick = {
                        isSubmitting = true
                        viewModel.submitImages(selectedImages, selectedStyle)
                        onTasksCreated(emptyList())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressClickEffect()
                        .floatingCard(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1A1A1A)
                    ),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "生成三视图",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Secondary button - Open camera
            OutlinedButton(
                onClick = {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .pressClickEffect()
                    .floatingCard(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1A1A1A)),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "打开相机",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A1A)
                )
            }
        }
    }
}

@Composable
private fun AnimatedImageGridItem(
    uri: Uri,
    index: Int,
    isSelected: Boolean
) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.8f) }

    LaunchedEffect(uri) {
        delay(index * 50L)
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(300, easing = EaseOutCubic)
        )
    }

    LaunchedEffect(uri) {
        delay(index * 50L)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.75f,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .graphicsLayer {
                this.alpha = alpha.value
                scaleX = scale.value
                scaleY = scale.value
            }
            .then(
                if (isSelected) Modifier.floatingCard(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(8.dp)
                ) else Modifier
            )
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) Color(0xFF1A1A1A) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "选中的图片",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        // Checkmark overlay for selected items
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .background(Color(0xFF1A1A1A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_5")
@Composable
private fun ImagePickerScreenEmptyPreview() {
    com.example.sketchto3view.ui.theme.SketchTo3ViewTheme {
        ImagePickerScreenContent(
            selectedImages = emptyList(),
            isSubmitting = false,
            onNavigateBack = {},
            onSubmit = {},
            onOpenCamera = {},
            onOpenGallery = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, device = "id:pixel_5")
@Composable
private fun ImagePickerScreenWithImagesPreview() {
    com.example.sketchto3view.ui.theme.SketchTo3ViewTheme {
        ImagePickerScreenContent(
            selectedImages = listOf(
                Uri.parse("content://mock/image1"),
                Uri.parse("content://mock/image2"),
                Uri.parse("content://mock/image3")
            ),
            isSubmitting = false,
            onNavigateBack = {},
            onSubmit = {},
            onOpenCamera = {},
            onOpenGallery = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImagePickerScreenContent(
    selectedImages: List<Uri>,
    isSubmitting: Boolean,
    onNavigateBack: () -> Unit,
    onSubmit: () -> Unit,
    onOpenCamera: () -> Unit,
    onOpenGallery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "选择图片",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
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

        // Selected count indicator
        if (selectedImages.isNotEmpty()) {
            Text(
                text = "已选择 ${selectedImages.size} 张图片",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
        }

        // Image Grid or empty state
        if (selectedImages.isEmpty()) {
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
                        text = "暂无图片\n点击下方按钮添加图片",
                        fontSize = 14.sp,
                        color = Color(0xFF999999),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(selectedImages) { index, uri ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEEEEEE))
                            .border(3.dp, Color(0xFF1A1A1A), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", fontSize = 16.sp, color = Color(0xFF666666))
                    }
                }
            }
        }

        // Bottom Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(visible = selectedImages.isNotEmpty()) {
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    enabled = !isSubmitting
                ) {
                    Text(
                        text = "生成三视图",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            OutlinedButton(
                onClick = onOpenCamera,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1A1A1A)),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF1A1A1A)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "打开相机",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A1A)
                )
            }
        }
    }
}
