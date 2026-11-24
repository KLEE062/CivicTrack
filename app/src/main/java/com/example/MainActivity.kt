package com.example

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.capabilities.AndroidDeviceCapabilityManager
import com.example.core.security.InvitationTokenUtils
import com.example.data.model.EntryAuthDestination
import com.example.ui.ActiveWorkspaceMode
import com.example.ui.CivicTrackViewModel
import com.example.ui.OnboardingViewModel
import com.example.ui.components.PermissionRationaleDialog
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.DeviceSecuritySettingsScreen
import com.example.ui.screens.MediaDocumentsScreen
import com.example.ui.screens.OnboardingEntryScreen
import com.example.ui.screens.OwnerDashboardScreen
import com.example.ui.screens.OwnerOnboardingFlowScreen
import com.example.ui.screens.SupervisorDashboardScreen
import com.example.ui.screens.SupervisorOnboardingFlowScreen
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.ui.frontend.CivicTrackFrontendRoot
import com.example.ui.frontend.data.CivicThemeMode
import com.example.ui.frontend.state.FrontendViewModel
import com.example.ui.theme.CivicTrackTheme

object CivicRoutes {
  const val DASHBOARD = "dashboard"
  const val ATTENDANCE = "attendance"
  const val MEDIA = "media_documents"
  const val SETTINGS = "device_security_settings"
}

private data class NavDestination(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

class MainActivity : FragmentActivity() {
  private var incomingDeepLinkState = mutableStateOf<String?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    incomingDeepLinkState.value = intent?.dataString
    enableEdgeToEdge()
    setContent {
      val frontendViewModel: FrontendViewModel =
        viewModel(factory = FrontendViewModel.Factory(applicationContext))
      val frontendState by frontendViewModel.uiState.collectAsStateWithLifecycle()
      val useDarkTheme =
        when (frontendState.themeMode) {
          CivicThemeMode.SYSTEM -> isSystemInDarkTheme()
          CivicThemeMode.LIGHT -> false
          CivicThemeMode.DARK -> true
        }
      CivicTrackTheme(darkTheme = useDarkTheme) {
        CivicTrackFrontendRoot(
          viewModel = frontendViewModel,
          incomingDeepLink = incomingDeepLinkState.value,
          onConsumeDeepLink = { incomingDeepLinkState.value = null },
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    intent.dataString?.let { uriString ->
      incomingDeepLinkState.value = uriString
    }
  }

  private fun extractInviteTokenFromIntent(intent: Intent?): String? {
    val dataString = intent?.dataString ?: return null
    return InvitationTokenUtils.extractInvitationToken(dataString).takeIf { it.isNotBlank() }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CivicTrackApp(
  activity: FragmentActivity,
  incomingInviteToken: String? = null,
  onConsumeInviteToken: () -> Unit = {},
) {
  val context = LocalContext.current
  val viewModel: CivicTrackViewModel = viewModel(factory = CivicTrackViewModel.Factory(context))
  val onboardingViewModel: OnboardingViewModel =
    viewModel(factory = OnboardingViewModel.Factory(context))
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val onboardingState by onboardingViewModel.uiState.collectAsStateWithLifecycle()
  val capabilityHelper = AndroidDeviceCapabilityManager(context)
  var showSecurityDuringOnboarding by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) { viewModel.refreshCapabilities(activity) }

  LaunchedEffect(uiState.session, onboardingState.session, onboardingState.isRestoringSession) {
    val activeEmpSession = uiState.session
    if (
      activeEmpSession != null &&
        onboardingState.session == null &&
        !onboardingState.isRestoringSession
    ) {
      onboardingViewModel.restoreAuthenticatedSession(activeEmpSession)
    }
  }

  LaunchedEffect(incomingInviteToken) {
    if (!incomingInviteToken.isNullOrBlank()) {
      onboardingViewModel.startSupervisorOnboardingFlow(incomingInviteToken)
      onConsumeInviteToken()
    }
  }

  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
    viewModel.refreshCapabilities(activity)
  }

  val locationPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultMap ->
      val fineGranted = resultMap[Manifest.permission.ACCESS_FINE_LOCATION] == true
      val coarseGranted = resultMap[Manifest.permission.ACCESS_COARSE_LOCATION] == true
      viewModel.onLocationPermissionResult(
        granted = fineGranted || coarseGranted,
        activity = activity,
      )
    }

  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
      viewModel.onCameraPermissionResult(granted = granted, activity = activity)
    }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
      if (uri != null) {
        viewModel.onImagePickedFromGallery(context, uri)
      }
    }

  val orgLogoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
      if (uri != null) {
        onboardingViewModel.uploadOrganizationLogoFromUri(context, uri)
      }
    }

  val supervisorPhotoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
      if (uri != null) {
        onboardingViewModel.uploadSupervisorProfilePhoto(context, uri)
      }
    }

  if (uiState.activeRationale != null) {
    PermissionRationaleDialog(
      rationaleType = uiState.activeRationale!!,
      onAllow = {
        viewModel.confirmRationaleAndRequestPermission(
          onLaunchLocationPermission = {
            locationPermissionLauncher.launch(
              arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
              )
            )
          },
          onLaunchCameraPermission = {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
          },
        )
      },
      onDismiss = { viewModel.dismissRationale() },
    )
  }

  val isInAuthenticatedWorkspace =
    onboardingState.workspaceMode != ActiveWorkspaceMode.UNAUTHENTICATED_OR_ONBOARDING

  if (!isInAuthenticatedWorkspace) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        CenterAlignedTopAppBar(
          title = {
            Text(
              text =
                if (showSecurityDuringOnboarding) "Device & Security Capabilities"
                else stringResource(R.string.app_name),
              style = MaterialTheme.typography.titleLarge,
            )
          },
          actions = {
            IconButton(
              onClick = { showSecurityDuringOnboarding = !showSecurityDuringOnboarding },
              modifier = Modifier.testTag("toggle_onboarding_security_button"),
            ) {
              Icon(
                imageVector =
                  if (showSecurityDuringOnboarding) Icons.Filled.Dashboard
                  else Icons.Outlined.Security,
                contentDescription = "Toggle device capability diagnostics",
              )
            }
          },
          colors =
            TopAppBarDefaults.centerAlignedTopAppBarColors(
              containerColor = MaterialTheme.colorScheme.surface
            ),
        )
      },
    ) { innerPadding ->
      if (showSecurityDuringOnboarding) {
        DeviceSecuritySettingsScreen(
          uiState = uiState,
          onRequestLocationPermission = {
            locationPermissionLauncher.launch(
              arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
              )
            )
          },
          onRequestCameraPermission = {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
          },
          onOpenAppSettings = { context.startActivity(capabilityHelper.buildAppSettingsIntent()) },
          onOpenLocationSettings = {
            context.startActivity(capabilityHelper.buildLocationSettingsIntent())
          },
          onOpenBiometricEnroll = {
            context.startActivity(capabilityHelper.buildBiometricEnrollIntent())
          },
          onToggleBiometricLogin = viewModel::setBiometricLoginEnabled,
          onTestBiometricPrompt = { viewModel.testBiometricPromptInSettings(activity) },
          modifier = Modifier.padding(innerPadding),
        )
      } else {
        when (onboardingState.entryDestination) {
          EntryAuthDestination.OWNER_ONBOARDING -> {
            OwnerOnboardingFlowScreen(
              uiState = onboardingState,
              onBackToEntry = {
                onboardingViewModel.selectEntryDestination(EntryAuthDestination.LANDING)
              },
              onSwitchToSignIn = {
                onboardingViewModel.selectEntryDestination(EntryAuthDestination.SIGN_IN)
              },
              onRegisterOwnerAccount = onboardingViewModel::registerOwnerAccount,
              onCreateOrganization = onboardingViewModel::createOrganization,
              onSaveOrgProfile = onboardingViewModel::saveOrganizationProfile,
              onPickOrgLogo = {
                orgLogoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              onCaptureCurrentGps = {
                onboardingViewModel.captureCurrentCoordinatesForWorkplace(context)
              },
              onAddWorkplace = onboardingViewModel::addWorkplace,
              onAddDepartment = onboardingViewModel::addDepartment,
              onSaveAttendanceSettings = onboardingViewModel::saveAttendanceSettings,
              onSendInvitation = onboardingViewModel::sendInvitation,
              onGoToDashboard = onboardingViewModel::goToOwnerDashboard,
              modifier = Modifier.padding(innerPadding),
            )
          }
          EntryAuthDestination.SUPERVISOR_ONBOARDING -> {
            SupervisorOnboardingFlowScreen(
              uiState = onboardingState,
              initialTokenInput = incomingInviteToken,
              onBackToEntry = {
                onboardingViewModel.selectEntryDestination(EntryAuthDestination.LANDING)
              },
              onValidateInvitation = onboardingViewModel::validateSupervisorInvitation,
              onToggleAuthMode = onboardingViewModel::setSupervisorAuthMode,
              onRegisterSupervisorAccount = onboardingViewModel::registerSupervisorAccount,
              onSignInSupervisorAccount = onboardingViewModel::signIn,
              onAcceptInvitation = onboardingViewModel::acceptValidatedSupervisorInvitation,
              onDeclineInvitation = onboardingViewModel::declineValidatedSupervisorInvitation,
              onSignOutToSwitchAccount = onboardingViewModel::signOut,
              onPickSupervisorPhoto = {
                supervisorPhotoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              onConfirmSupervisorProfile = onboardingViewModel::confirmSupervisorProfile,
              modifier = Modifier.padding(innerPadding),
            )
          }
          EntryAuthDestination.LANDING,
          EntryAuthDestination.SIGN_IN,
          EntryAuthDestination.FORGOT_PASSWORD -> {
            OnboardingEntryScreen(
              uiState = onboardingState,
              hasBiometricSavedSession = uiState.hasStoredSessionForBiometricUnlock,
              onStartOwnerOnboarding = { onboardingViewModel.startOwnerOnboardingFlow() },
              onStartSupervisorOnboarding = {
                onboardingViewModel.startSupervisorOnboardingFlow(incomingInviteToken)
              },
              onOpenSignIn = {
                onboardingViewModel.selectEntryDestination(EntryAuthDestination.SIGN_IN)
              },
              onOpenForgotPassword = {
                onboardingViewModel.selectEntryDestination(EntryAuthDestination.FORGOT_PASSWORD)
              },
              onBackToLanding = {
                onboardingViewModel.selectEntryDestination(EntryAuthDestination.LANDING)
              },
              onSignIn = onboardingViewModel::signIn,
              onBiometricUnlock = { viewModel.unlockSavedSessionWithBiometric(activity) },
              onSendPasswordReset = onboardingViewModel::requestPasswordReset,
              modifier = Modifier.padding(innerPadding),
            )
          }
        }
      }
    }
    return
  }

  val destinations =
    remember(onboardingState.workspaceMode) {
      buildList {
        if (
          onboardingState.workspaceMode == ActiveWorkspaceMode.OWNER_DASHBOARD ||
            onboardingState.workspaceMode == ActiveWorkspaceMode.SUPERVISOR_DASHBOARD
        ) {
          add(
            NavDestination(
              route = CivicRoutes.DASHBOARD,
              labelRes = R.string.nav_dashboard,
              selectedIcon = Icons.Filled.Dashboard,
              unselectedIcon = Icons.Outlined.Dashboard,
              testTag = "nav_dashboard",
            )
          )
        }
        add(
          NavDestination(
            route = CivicRoutes.ATTENDANCE,
            labelRes = R.string.nav_attendance,
            selectedIcon = Icons.Filled.LocationOn,
            unselectedIcon = Icons.Outlined.LocationOn,
            testTag = "nav_attendance",
          )
        )
        add(
          NavDestination(
            route = CivicRoutes.MEDIA,
            labelRes = R.string.nav_documents,
            selectedIcon = Icons.Filled.CameraAlt,
            unselectedIcon = Icons.Outlined.CameraAlt,
            testTag = "nav_documents",
          )
        )
        add(
          NavDestination(
            route = CivicRoutes.SETTINGS,
            labelRes = R.string.nav_settings,
            selectedIcon = Icons.Filled.Security,
            unselectedIcon = Icons.Outlined.Security,
            testTag = "nav_settings",
          )
        )
      }
    }

  val startRoute =
    if (
      onboardingState.workspaceMode == ActiveWorkspaceMode.OWNER_DASHBOARD ||
        onboardingState.workspaceMode == ActiveWorkspaceMode.SUPERVISOR_DASHBOARD
    ) {
      CivicRoutes.DASHBOARD
    } else {
      CivicRoutes.ATTENDANCE
    }

  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: startRoute

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val useNavigationRail = maxWidth >= 600.dp

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
        if (uiState.activeCameraTarget == null) {
          CenterAlignedTopAppBar(
            title = {
              Text(
                text =
                  onboardingState.activeOrganization?.name ?: stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
              )
            },
            colors =
              TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
              ),
          )
        }
      },
      bottomBar = {
        if (!useNavigationRail && uiState.activeCameraTarget == null) {
          NavigationBar(windowInsets = WindowInsets.navigationBars) {
            destinations.forEach { dest ->
              val selected = currentRoute == dest.route
              NavigationBarItem(
                selected = selected,
                onClick = {
                  navController.navigate(dest.route) {
                    popUpTo(startRoute) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = {
                  Icon(
                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = stringResource(dest.labelRes),
                  )
                },
                label = { Text(stringResource(dest.labelRes)) },
                modifier = Modifier.testTag(dest.testTag),
              )
            }
          }
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (useNavigationRail && uiState.activeCameraTarget == null) {
          NavigationRail {
            destinations.forEach { dest ->
              val selected = currentRoute == dest.route
              NavigationRailItem(
                selected = selected,
                onClick = {
                  navController.navigate(dest.route) {
                    popUpTo(startRoute) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                  }
                },
                icon = {
                  Icon(
                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = stringResource(dest.labelRes),
                  )
                },
                label = { Text(stringResource(dest.labelRes)) },
                modifier = Modifier.testTag(dest.testTag),
              )
            }
          }
        }

        NavHost(
          navController = navController,
          startDestination = startRoute,
          modifier = Modifier.weight(1f).fillMaxSize(),
        ) {
          composable(CivicRoutes.DASHBOARD) {
            if (onboardingState.workspaceMode == ActiveWorkspaceMode.OWNER_DASHBOARD) {
              OwnerDashboardScreen(
                uiState = onboardingState,
                onRefreshDashboard = { onboardingViewModel.refreshOwnerDashboard() },
                onOpenStructureWizard = onboardingViewModel::openOwnerStructureWizard,
                onOpenOrgProfile = onboardingViewModel::openOwnerOrgProfileStep,
                onSendInvitation = onboardingViewModel::sendInvitation,
                onRevokeInvitation = onboardingViewModel::revokeInvitation,
                onSignOut = {
                  onboardingViewModel.signOut()
                  viewModel.signOut()
                },
              )
            } else {
              SupervisorDashboardScreen(
                uiState = onboardingState,
                onRefreshDashboard = { onboardingViewModel.refreshSupervisorDashboard() },
                onSignOut = {
                  onboardingViewModel.signOut()
                  viewModel.signOut()
                },
              )
            }
          }

          composable(CivicRoutes.ATTENDANCE) {
            AttendanceScreen(
              uiState = uiState,
              onSelectAuthMode = viewModel::setAuthMode,
              onRegister = viewModel::registerEmployee,
              onSignIn = viewModel::signInWithPassword,
              onBiometricUnlock = { viewModel.unlockSavedSessionWithBiometric(activity) },
              onSignOut = {
                onboardingViewModel.signOut()
                viewModel.signOut()
              },
              onRefreshProfile = { viewModel.refreshEmployeeData() },
              onInitiateAttendance = { checkType ->
                viewModel.initiateAttendanceAction(
                  checkType = checkType,
                  activity = activity,
                  onRequestLocationPermissions = {
                    locationPermissionLauncher.launch(
                      arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                      )
                    )
                  },
                )
              },
              onRetrySync = viewModel::retryPendingAttendanceSubmission,
              onOpenBlockedSettings = {
                val intent =
                  if (uiState.blockedPermissionRequiresLocationSettings) {
                    capabilityHelper.buildLocationSettingsIntent()
                  } else {
                    capabilityHelper.buildAppSettingsIntent()
                  }
                context.startActivity(intent)
              },
              onDismissBlockedBanner = viewModel::dismissPermissionBlockedBanner,
            )
          }

          composable(CivicRoutes.MEDIA) {
            MediaDocumentsScreen(
              uiState = uiState,
              onStartCameraCapture = { target ->
                viewModel.requestCameraCapture(target, activity)
              },
              onPickFromGallery = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              onRawImageCaptured = { file ->
                viewModel.onImageCapturedFromCamera(context, file)
              },
              onRetake = viewModel::retakePhoto,
              onCancelCapture = viewModel::cancelCameraCapture,
              onConfirmUpload = viewModel::confirmUploadPendingAsset,
              onCategorySelected = viewModel::updateDocumentCategory,
              onTitleChanged = viewModel::updateDocumentTitle,
              onOpenSettings = {
                context.startActivity(capabilityHelper.buildAppSettingsIntent())
              },
              onDismissBlockedBanner = viewModel::dismissPermissionBlockedBanner,
            )
          }

          composable(CivicRoutes.SETTINGS) {
            DeviceSecuritySettingsScreen(
              uiState = uiState,
              onRequestLocationPermission = {
                locationPermissionLauncher.launch(
                  arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                  )
                )
              },
              onRequestCameraPermission = {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
              },
              onOpenAppSettings = {
                context.startActivity(capabilityHelper.buildAppSettingsIntent())
              },
              onOpenLocationSettings = {
                context.startActivity(capabilityHelper.buildLocationSettingsIntent())
              },
              onOpenBiometricEnroll = {
                context.startActivity(capabilityHelper.buildBiometricEnrollIntent())
              },
              onToggleBiometricLogin = viewModel::setBiometricLoginEnabled,
              onTestBiometricPrompt = { viewModel.testBiometricPromptInSettings(activity) },
            )
          }
        }
      }
    }
  }
}
