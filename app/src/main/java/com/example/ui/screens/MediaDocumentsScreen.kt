package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.R
import com.example.core.camera.ProcessedImageAsset
import com.example.data.model.DocumentCaptureCategory
import com.example.data.model.EmployeeDocumentDto
import com.example.ui.CaptureTarget
import com.example.ui.CivicTrackUiState
import com.example.ui.components.PermissionBlockedBanner
import com.example.ui.components.StatusPill
import com.example.ui.theme.LocalCivicStatusPalette
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MediaDocumentsScreen(
  uiState: CivicTrackUiState,
  onStartCameraCapture: (CaptureTarget) -> Unit,
  onPickFromGallery: () -> Unit,
  onRawImageCaptured: (File) -> Unit,
  onRetake: () -> Unit,
  onCancelCapture: () -> Unit,
  onConfirmUpload: () -> Unit,
  onCategorySelected: (DocumentCaptureCategory) -> Unit,
  onTitleChanged: (String) -> Unit,
  onOpenSettings: () -> Unit,
  onDismissBlockedBanner: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalCivicStatusPalette.current

  if (uiState.activeCameraTarget != null) {
    BackHandler { onCancelCapture() }
    val pendingAsset = uiState.pendingUploadAsset
    if (pendingAsset != null) {
      CapturedPreviewScreen(
        target = uiState.activeCameraTarget,
        asset = pendingAsset,
        selectedCategory = uiState.selectedDocumentCategory,
        documentTitle = uiState.documentTitleInput,
        isUploading = uiState.isUploadingMedia,
        errorMessage = uiState.mediaErrorMessage,
        onCategorySelected = onCategorySelected,
        onTitleChanged = onTitleChanged,
        onRetake = onRetake,
        onCancel = onCancelCapture,
        onConfirmUpload = onConfirmUpload,
        modifier = modifier,
      )
    } else {
      CameraXCaptureView(
        target = uiState.activeCameraTarget,
        onImageCaptured = onRawImageCaptured,
        onCancel = onCancelCapture,
        modifier = modifier,
      )
    }
    return
  }

  val cameraAvailable = uiState.capabilities?.cameraAvailable ?: true

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      if (uiState.permissionBlockedMessage != null) {
        item {
          PermissionBlockedBanner(
            message = uiState.permissionBlockedMessage,
            requiresLocationSettings = false,
            onOpenSettings = onOpenSettings,
            onDismiss = onDismissBlockedBanner,
          )
        }
      }

      if (!uiState.mediaErrorMessage.isNullOrBlank()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = palette.errorContainer),
            modifier = Modifier.fillMaxWidth().testTag("media_error_card"),
          ) {
            Text(
              text = uiState.mediaErrorMessage,
              style = MaterialTheme.typography.bodyMedium,
              color = palette.error,
              modifier = Modifier.padding(14.dp),
            )
          }
        }
      }

      if (!uiState.mediaStatusMessage.isNullOrBlank()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = palette.verifiedContainer),
            modifier = Modifier.fillMaxWidth().testTag("media_success_card"),
          ) {
            Text(
              text = uiState.mediaStatusMessage,
              style = MaterialTheme.typography.bodyMedium,
              color = palette.verified,
              modifier = Modifier.padding(14.dp),
            )
          }
        }
      }

      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              val photoUrl = uiState.employee?.profilePhotoUrl
              if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                  model = photoUrl,
                  contentDescription = "Employee profile photo",
                  modifier = Modifier.size(64.dp).clip(CircleShape),
                  contentScale = ContentScale.Crop,
                )
              } else {
                Icon(
                  imageVector = Icons.Filled.AccountCircle,
                  contentDescription = "Profile avatar placeholder",
                  modifier = Modifier.size(64.dp),
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = "Profile Photo & Workforce Documents",
                  style = MaterialTheme.typography.titleLarge,
                )
                Text(
                  text =
                    "User-initiated capture only. Images are compressed to JPEG (max 512 KB profile / 2 MB documents) and never used as biometric templates.",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Button(
                onClick = { onStartCameraCapture(CaptureTarget.PROFILE_PHOTO) },
                enabled = cameraAvailable,
                modifier = Modifier.weight(1f).testTag("capture_profile_photo_button"),
              ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.capture_profile_photo))
              }
              OutlinedButton(
                onClick = { onStartCameraCapture(CaptureTarget.DOCUMENT) },
                enabled = cameraAvailable,
                modifier = Modifier.weight(1f).testTag("capture_document_button"),
              ) {
                Icon(Icons.Filled.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.capture_document))
              }
            }

            OutlinedButton(
              onClick = onPickFromGallery,
              modifier = Modifier.fillMaxWidth().testTag("pick_document_image_button"),
            ) {
              Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text(stringResource(R.string.upload_document_picker))
            }

            if (!cameraAvailable) {
              Text(
                text =
                  "Camera hardware unavailable on this device. Use the Photo Picker above to select an image without camera access.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.warning,
                modifier = Modifier.testTag("camera_unavailable_notice"),
              )
            }
          }
        }
      }

      item {
        Text(
          text = "Uploaded Employee Documents (${uiState.employeeDocuments.size})",
          style = MaterialTheme.typography.titleLarge,
        )
      }

      if (uiState.employeeDocuments.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors =
              CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
              ),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Filled.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              Text("No documents uploaded yet", style = MaterialTheme.typography.titleMedium)
              Text(
                "Capture an ID badge, site permit, or safety clearance form above to upload to Supabase Storage.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      } else {
        items(uiState.employeeDocuments, key = { it.id }) { doc ->
          DocumentItemCard(doc)
        }
      }
    }
  }
}

@Composable
private fun DocumentItemCard(doc: EmployeeDocumentDto) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = Icons.Filled.Description,
        contentDescription = doc.title,
        tint = MaterialTheme.colorScheme.primary,
      )
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = doc.title, style = MaterialTheme.typography.titleMedium)
        Text(
          text = "${doc.documentType} • ${doc.mimeType} • ${doc.fileSizeBytes / 1024} KB",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = doc.storagePath,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun CameraXCaptureView(
  target: CaptureTarget,
  onImageCaptured: (File) -> Unit,
  onCancel: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var lensFacing by remember {
    mutableIntStateOf(
      if (target == CaptureTarget.PROFILE_PHOTO) CameraSelector.LENS_FACING_FRONT
      else CameraSelector.LENS_FACING_BACK
    )
  }
  var flashEnabled by remember { mutableStateOf(false) }
  var captureError by remember { mutableStateOf<String?>(null) }
  var isTakingPicture by remember { mutableStateOf(false) }

  val imageCapture =
    remember(flashEnabled) {
      ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .setFlashMode(
          if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        )
        .build()
    }

  val previewView = remember { PreviewView(context) }

  DisposableEffect(lensFacing, imageCapture, lifecycleOwner) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val executor = ContextCompat.getMainExecutor(context)
    var cameraProvider: ProcessCameraProvider? = null

    cameraProviderFuture.addListener(
      {
        try {
          val provider = cameraProviderFuture.get()
          cameraProvider = provider
          val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
          val selector =
            CameraSelector.Builder().requireLensFacing(lensFacing).build()
          val resolvedSelector =
            if (provider.hasCamera(selector)) selector else CameraSelector.DEFAULT_BACK_CAMERA
          provider.unbindAll()
          provider.bindToLifecycle(lifecycleOwner, resolvedSelector, preview, imageCapture)
          captureError = null
        } catch (e: Exception) {
          captureError = e.localizedMessage ?: "Unable to initialize device camera."
        }
      },
      executor,
    )

    onDispose {
      try {
        cameraProvider?.unbindAll()
      } catch (_: Exception) {
      }
    }
  }

  Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
    AndroidView(
      factory = { previewView },
      modifier = Modifier.fillMaxSize(),
    )

    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(
        onClick = onCancel,
        modifier =
          Modifier.clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .testTag("camera_cancel_button"),
      ) {
        Icon(Icons.Filled.Close, contentDescription = "Cancel capture", tint = Color.White)
      }

      Surface(
        color = Color.Black.copy(alpha = 0.6f),
        shape = RoundedCornerShape(16.dp),
      ) {
        Text(
          text =
            if (target == CaptureTarget.PROFILE_PHOTO) "Profile Photo" else "Workforce Document",
          color = Color.White,
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(
          onClick = { flashEnabled = !flashEnabled },
          modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)),
        ) {
          Icon(
            imageVector = if (flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
            contentDescription = "Toggle flash",
            tint = Color.White,
          )
        }
        IconButton(
          onClick = {
            lensFacing =
              if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT
              else CameraSelector.LENS_FACING_BACK
          },
          modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)),
        ) {
          Icon(
            imageVector = Icons.Filled.Cameraswitch,
            contentDescription = "Switch camera lens",
            tint = Color.White,
          )
        }
      }
    }

    if (captureError != null) {
      Card(
        modifier = Modifier.align(Alignment.Center).padding(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
      ) {
        Text(
          text = captureError ?: "",
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.padding(16.dp),
        )
      }
    }

    Box(
      modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp).align(Alignment.BottomCenter),
      contentAlignment = Alignment.Center,
    ) {
      FilledIconButton(
        onClick = {
          if (isTakingPicture) return@FilledIconButton
          isTakingPicture = true
          val outputFile = File(context.cacheDir, "raw_capture_${System.currentTimeMillis()}.jpg")
          val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
          imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
              override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                isTakingPicture = false
                onImageCaptured(outputFile)
              }

              override fun onError(exception: ImageCaptureException) {
                isTakingPicture = false
                captureError = exception.localizedMessage ?: "Photo capture failed."
              }
            },
          )
        },
        modifier = Modifier.size(72.dp).testTag("camera_shutter_button"),
      ) {
        if (isTakingPicture) {
          CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = MaterialTheme.colorScheme.onPrimary,
          )
        } else {
          Icon(
            imageVector = Icons.Filled.PhotoCamera,
            contentDescription = "Take photo",
            modifier = Modifier.size(32.dp),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CapturedPreviewScreen(
  target: CaptureTarget,
  asset: ProcessedImageAsset,
  selectedCategory: DocumentCaptureCategory,
  documentTitle: String,
  isUploading: Boolean,
  errorMessage: String?,
  onCategorySelected: (DocumentCaptureCategory) -> Unit,
  onTitleChanged: (String) -> Unit,
  onRetake: () -> Unit,
  onCancel: () -> Unit,
  onConfirmUpload: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Text(
          text =
            if (target == CaptureTarget.PROFILE_PHOTO) "Preview Profile Photo"
            else "Preview Workforce Document",
          style = MaterialTheme.typography.headlineMedium,
        )
      }

      item {
        Card(shape = RoundedCornerShape(18.dp)) {
          AsyncImage(
            model = asset.file,
            contentDescription = "Captured image preview",
            modifier = Modifier.fillMaxWidth().height(300.dp),
            contentScale = ContentScale.Fit,
          )
        }
      }

      item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          StatusPill(
            label = "${asset.width}x${asset.height}px",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          StatusPill(
            label = "${asset.sizeBytes / 1024} KB",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          )
          StatusPill(
            label = asset.mimeType,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
          )
        }
      }

      if (target == CaptureTarget.DOCUMENT) {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Document Category", style = MaterialTheme.typography.titleMedium)
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              DocumentCaptureCategory.entries
                .filter { it != DocumentCaptureCategory.PROFILE_PHOTO }
                .forEach { category ->
                  FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category.label) },
                  )
                }
            }
            OutlinedTextField(
              value = documentTitle,
              onValueChange = onTitleChanged,
              label = { Text("Document Title (Optional)") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("document_title_input"),
            )
          }
        }
      }

      if (!errorMessage.isNullOrBlank()) {
        item {
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = onRetake,
            enabled = !isUploading,
            modifier = Modifier.weight(1f).testTag("retake_photo_button"),
          ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.retake_photo))
          }
          OutlinedButton(
            onClick = onCancel,
            enabled = !isUploading,
            modifier = Modifier.weight(1f).testTag("cancel_upload_button"),
          ) {
            Text(stringResource(R.string.cancel_action))
          }
        }
      }

      item {
        Button(
          onClick = onConfirmUpload,
          enabled = !isUploading,
          modifier = Modifier.fillMaxWidth().height(52.dp).testTag("confirm_upload_button"),
        ) {
          if (isUploading) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary,
            )
          } else {
            Icon(Icons.Filled.CloudUpload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.confirm_upload))
          }
        }
      }
    }
  }
}
