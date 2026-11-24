package com.example.ui.frontend

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.capabilities.AndroidDeviceCapabilityManager
import com.example.data.model.AttendanceCheckType
import com.example.data.model.OrgMemberRole
import com.example.ui.CaptureTarget
import com.example.ui.components.PermissionRationaleDialog
import com.example.ui.frontend.data.CivicRole
import com.example.ui.frontend.navigation.FrontendRoutes
import com.example.ui.frontend.screens.EmailVerificationInfoScreen
import com.example.ui.frontend.screens.EmployeeAttendanceDetailScreen
import com.example.ui.frontend.screens.EmployeeAttendanceHistoryScreen
import com.example.ui.frontend.screens.EmployeeBiometricConfirmationScreen
import com.example.ui.frontend.screens.EmployeeCheckInScreen
import com.example.ui.frontend.screens.EmployeeCheckOutScreen
import com.example.ui.frontend.screens.EmployeeDashboardFrontendScreen
import com.example.ui.frontend.screens.EmployeeDocumentsScreen
import com.example.ui.frontend.screens.EmployeeInvitationAcceptanceScreen
import com.example.ui.frontend.screens.EmployeeLeaveApplicationScreen
import com.example.ui.frontend.screens.EmployeeLeaveHistoryScreen
import com.example.ui.frontend.screens.EmployeeLocationVerificationScreen
import com.example.ui.frontend.screens.EmployeeNotificationsScreen
import com.example.ui.frontend.screens.EmployeeProfileScreen
import com.example.ui.frontend.screens.EmployeeSettingsScreen
import com.example.ui.frontend.screens.EmployeeWorkScheduleScreen
import com.example.ui.frontend.screens.ForgotPasswordScreen
import com.example.ui.frontend.screens.LoginScreen
import com.example.ui.frontend.screens.OwnerAddEmployeeScreen
import com.example.ui.frontend.screens.OwnerAttendanceDetailsScreen
import com.example.ui.frontend.screens.OwnerAttendanceOverviewScreen
import com.example.ui.frontend.screens.OwnerAuditActivityScreen
import com.example.ui.frontend.screens.OwnerBillingScreen
import com.example.ui.frontend.screens.OwnerDashboardFrontendScreen
import com.example.ui.frontend.screens.OwnerDepartmentManagementScreen
import com.example.ui.frontend.screens.OwnerEmployeeDetailsScreen
import com.example.ui.frontend.screens.OwnerEmployeeDirectoryScreen
import com.example.ui.frontend.screens.OwnerInviteSupervisorScreen
import com.example.ui.frontend.screens.OwnerLeaveManagementScreen
import com.example.ui.frontend.screens.OwnerNotificationCentreScreen
import com.example.ui.frontend.screens.OwnerOrgProfileFrontendScreen
import com.example.ui.frontend.screens.OwnerOrgSettingsScreen
import com.example.ui.frontend.screens.OwnerProfileScreen
import com.example.ui.frontend.screens.OwnerRegistrationScreen
import com.example.ui.frontend.screens.OwnerReportsAnalyticsScreen
import com.example.ui.frontend.screens.OwnerSupervisorDirectoryScreen
import com.example.ui.frontend.screens.OwnerWorkScheduleScreen
import com.example.ui.frontend.screens.OwnerWorkplaceManagementScreen
import com.example.ui.frontend.screens.PasswordResetScreen
import com.example.ui.frontend.screens.RoleSelectionScreen
import com.example.ui.frontend.screens.SplashScreen
import com.example.ui.frontend.screens.SupervisorAttendanceOverviewScreen
import com.example.ui.frontend.screens.SupervisorDashboardFrontendScreen
import com.example.ui.frontend.screens.SupervisorEmployeeDetailsScreen
import com.example.ui.frontend.screens.SupervisorExceptionsScreen
import com.example.ui.frontend.screens.SupervisorInvitationAcceptanceScreen
import com.example.ui.frontend.screens.SupervisorLeaveDetailScreen
import com.example.ui.frontend.screens.SupervisorLeaveRequestsScreen
import com.example.ui.frontend.screens.SupervisorNotificationsScreen
import com.example.ui.frontend.screens.SupervisorProfileSettingsScreen
import com.example.ui.frontend.screens.SupervisorReportsScreen
import com.example.ui.frontend.screens.SupervisorTeamSchedulesScreen
import com.example.ui.frontend.screens.SupervisorWorkforceScreen
import com.example.ui.frontend.screens.SupervisorWorkplaceInfoScreen
import com.example.ui.frontend.screens.WelcomeScreen
import com.example.ui.frontend.state.FrontendViewModel
import com.example.ui.screens.CameraXCaptureView
import com.example.ui.screens.CapturedPreviewScreen
import kotlinx.coroutines.launch

private data class WorkspaceNavItem(
  val route: String,
  val label: String,
  val icon: ImageVector,
  val testTag: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CivicTrackFrontendRoot(
  viewModel: FrontendViewModel,
  incomingDeepLink: String? = null,
  onConsumeDeepLink: () -> Unit = {},
  initialRoute: String = FrontendRoutes.SPLASH,
  activityOverride: FragmentActivity? = null,
) {
  val context = LocalContext.current
  val activity = activityOverride ?: (context as? FragmentActivity)
  val capabilityHelper = remember(context) { AndroidDeviceCapabilityManager(context) }

  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val navController = rememberNavController()
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route ?: initialRoute
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val scope = rememberCoroutineScope()
  var drawerSheetActive by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    viewModel.refreshCapabilities(activity)
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
      if (activity != null) {
        viewModel.onLocationPermissionResult(
          granted = fineGranted || coarseGranted,
          activity = activity,
        )
      }
    }

  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
      if (activity != null) {
        viewModel.onCameraPermissionResult(granted = granted, activity = activity)
      }
    }

  val documentPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
      if (uri != null) {
        viewModel.onDocumentImagePicked(context, uri)
      }
    }

  val orgLogoPickerLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
      if (uri != null) {
        viewModel.uploadOrganizationLogoFromUri(context, uri)
      }
    }

  if (state.activeRationale != null) {
    PermissionRationaleDialog(
      rationaleType = state.activeRationale!!,
      onAllow = {
        viewModel.confirmRationaleAndRequestPermission(
          onLaunchLocationPermissions = {
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
      onDismiss = viewModel::dismissRationale,
    )
  }

  LaunchedEffect(drawerState.currentValue, drawerState.isAnimationRunning) {
    if (drawerState.currentValue == DrawerValue.Closed && !drawerState.isAnimationRunning) {
      drawerSheetActive = false
    }
  }

  LaunchedEffect(drawerSheetActive) {
    if (drawerSheetActive) {
      drawerState.open()
    }
  }

  val isEntryRoute = currentRoute.startsWith("entry/")
  val activeRole = state.activeRole
  val workspaceRole = if (!isEntryRoute) activeRole else null
  val isCameraOpen = state.activeCameraTarget != null

  LaunchedEffect(workspaceRole) {
    drawerSheetActive = false
    if (drawerState.currentValue != DrawerValue.Closed) {
      drawerState.snapTo(DrawerValue.Closed)
    }
  }

  fun navigateToWorkspace(role: CivicRole) {
    val home = dashboardRouteForRole(role)
    navController.navigate(home) {
      popUpTo(navController.graph.id) { inclusive = true }
      launchSingleTop = true
    }
  }

  LaunchedEffect(incomingDeepLink) {
    if (!incomingDeepLink.isNullOrBlank()) {
      viewModel.handleDeepLink(incomingDeepLink) { role -> navigateToWorkspace(role) }
      onConsumeDeepLink()
    }
  }

  fun signOutToWelcome() {
    viewModel.logout()
    navController.navigate(FrontendRoutes.WELCOME) {
      popUpTo(navController.graph.id) { inclusive = true }
      launchSingleTop = true
    }
  }

  val primaryTabs =
    when (activeRole) {
      CivicRole.OWNER ->
        listOf(
          WorkspaceNavItem(FrontendRoutes.OWNER_DASHBOARD, "Home", Icons.Filled.Dashboard, "nav_owner_dashboard"),
          WorkspaceNavItem(FrontendRoutes.OWNER_EMPLOYEE_DIRECTORY, "Staff", Icons.Filled.People, "nav_owner_staff"),
          WorkspaceNavItem(FrontendRoutes.OWNER_ATTENDANCE_OVERVIEW, "Attendance", Icons.Filled.CalendarMonth, "nav_owner_attendance"),
          WorkspaceNavItem(FrontendRoutes.OWNER_LEAVE_MANAGEMENT, "Leave", Icons.Filled.EventBusy, "nav_owner_leave"),
          WorkspaceNavItem(FrontendRoutes.OWNER_SETTINGS, "Settings", Icons.Filled.Settings, "nav_owner_settings"),
        )
      CivicRole.SUPERVISOR ->
        listOf(
          WorkspaceNavItem(FrontendRoutes.SUPERVISOR_DASHBOARD, "Home", Icons.Filled.Dashboard, "nav_sup_dashboard"),
          WorkspaceNavItem(FrontendRoutes.SUPERVISOR_WORKFORCE, "Team", Icons.Filled.People, "nav_sup_team"),
          WorkspaceNavItem(FrontendRoutes.SUPERVISOR_ATTENDANCE_OVERVIEW, "Logs", Icons.Filled.CalendarMonth, "nav_sup_logs"),
          WorkspaceNavItem(FrontendRoutes.SUPERVISOR_LEAVE_REQUESTS, "Leave", Icons.Filled.EventBusy, "nav_sup_leave"),
          WorkspaceNavItem(FrontendRoutes.SUPERVISOR_PROFILE_SETTINGS, "Profile", Icons.Filled.Person, "nav_sup_profile"),
        )
      CivicRole.EMPLOYEE ->
        listOf(
          WorkspaceNavItem(FrontendRoutes.EMPLOYEE_DASHBOARD, "Home", Icons.Filled.Dashboard, "nav_emp_dashboard"),
          WorkspaceNavItem(FrontendRoutes.EMPLOYEE_CHECK_IN, "Check-In", Icons.AutoMirrored.Filled.Login, "nav_emp_checkin"),
          WorkspaceNavItem(FrontendRoutes.EMPLOYEE_ATTENDANCE_HISTORY, "History", Icons.Filled.History, "nav_emp_history"),
          WorkspaceNavItem(FrontendRoutes.EMPLOYEE_LEAVE_HISTORY, "Leave", Icons.Filled.EventBusy, "nav_emp_leave"),
          WorkspaceNavItem(FrontendRoutes.EMPLOYEE_PROFILE, "Profile", Icons.Filled.Person, "nav_emp_profile"),
        )
      null -> emptyList()
    }

  val drawerLinks =
    when (activeRole) {
      CivicRole.OWNER ->
        listOf(
          WorkspaceNavItem(FrontendRoutes.OWNER_DASHBOARD, "Owner Dashboard", Icons.Filled.Dashboard, "drawer_owner_dash"),
          WorkspaceNavItem(FrontendRoutes.OWNER_ORG_PROFILE, "Organisation Profile", Icons.Filled.Apartment, "drawer_owner_org"),
          WorkspaceNavItem(FrontendRoutes.OWNER_EMPLOYEE_DIRECTORY, "Employee Directory", Icons.Filled.People, "drawer_owner_emp"),
          WorkspaceNavItem(FrontendRoutes.OWNER_ADD_EMPLOYEE, "Invite Employee", Icons.Filled.GroupAdd, "drawer_owner_add_emp"),
          WorkspaceNavItem(FrontendRoutes.OWNER_SUPERVISOR_DIRECTORY, "Supervisor Directory", Icons.Filled.SupervisorAccount, "drawer_owner_sup"),
          WorkspaceNavItem(FrontendRoutes.OWNER_INVITE_SUPERVISOR, "Invite Supervisor", Icons.Filled.GroupAdd, "drawer_owner_inv_sup"),
          WorkspaceNavItem(FrontendRoutes.OWNER_DEPARTMENTS, "Departments", Icons.Filled.Apartment, "drawer_owner_dept"),
          WorkspaceNavItem(FrontendRoutes.OWNER_WORKPLACES, "Workplaces & Geofences", Icons.Filled.LocationCity, "drawer_owner_wp"),
          WorkspaceNavItem(FrontendRoutes.OWNER_WORK_SCHEDULES, "Work Schedules", Icons.Filled.Schedule, "drawer_owner_sch"),
          WorkspaceNavItem(FrontendRoutes.OWNER_ATTENDANCE_OVERVIEW, "Attendance Overview", Icons.Filled.CalendarMonth, "drawer_owner_att"),
          WorkspaceNavItem(FrontendRoutes.OWNER_LEAVE_MANAGEMENT, "Leave Management", Icons.Filled.EventBusy, "drawer_owner_leave"),
          WorkspaceNavItem(FrontendRoutes.OWNER_REPORTS, "Reports & Analytics", Icons.Filled.Assessment, "drawer_owner_rep"),
          WorkspaceNavItem(FrontendRoutes.OWNER_NOTIFICATIONS, "Notification Centre", Icons.Filled.Notifications, "drawer_owner_notif"),
          WorkspaceNavItem(FrontendRoutes.OWNER_AUDIT_ACTIVITY, "Audit Activity", Icons.Filled.History, "drawer_owner_audit"),
          WorkspaceNavItem(FrontendRoutes.OWNER_SETTINGS, "Organisation Settings", Icons.Filled.Settings, "drawer_owner_set"),
          WorkspaceNavItem(FrontendRoutes.OWNER_BILLING, "Workspace Status", Icons.Filled.Storage, "drawer_owner_bill"),
          WorkspaceNavItem(FrontendRoutes.OWNER_PROFILE, "Owner Profile", Icons.Filled.Person, "drawer_owner_prof"),
        )
      CivicRole.SUPERVISOR ->
        listOf(
          WorkspaceNavItem(FrontendRoutes.SUPERVISOR_DASHBOARD, "Supervisor Dashboard", Icons.Filled.Dashboard, "drawer_sup_dash"),
          WorkspaceNavItem(FrontendRoutes.SUPERVISOR_WORKFORCE, "Assigned Workforce", Icons.Filled.People, "drawer_sup_workforce"),
          WorkspaceNavItem(FrontendRoutes.SUPERVISOR_ATTENDANCE_OVERVIEW, "Team Attendance", Icons.Filled.CalendarMonth, "drawer_sup_att"),
          WorkspaceNavItem(FrontendRoutes.SUPERVISOR_EXCEPTIONS, "Late & Exception Review", Icons.Filled.ReportProblem, "drawer_sup_exc"),
          WorkspaceNavItem(FrontendRoutes.SUPERVISOR_LEAVE_REQUESTS, "Team Leave Requests", Icons.Filled.EventBusy, "drawer_sup_leave"),
          WorkspaceNavItem(FrontendRoutes.SUPERVISOR_TEAM_SCHEDULES, "Team Schedules", Icons.Filled.Schedule, "drawer_sup_sch"),
          WorkspaceNavItem(FrontendRoutes.SUPERVISOR_WORKPLACE_INFO, "Workplace Info", Icons.Filled.LocationOn, "drawer_sup_wp"),
          WorkspaceNavItem(FrontendRoutes.SUPERVISOR_REPORTS, "Unit Reports", Icons.Filled.Assessment, "drawer_sup_rep"),
          WorkspaceNavItem(FrontendRoutes.SUPERVISOR_NOTIFICATIONS, "Notifications", Icons.Filled.Notifications, "drawer_sup_notif"),
          WorkspaceNavItem(FrontendRoutes.SUPERVISOR_PROFILE_SETTINGS, "Profile & Settings", Icons.Filled.Person, "drawer_sup_prof"),
        )
      CivicRole.EMPLOYEE ->
        listOf(
          WorkspaceNavItem(FrontendRoutes.EMPLOYEE_DASHBOARD, "Employee Dashboard", Icons.Filled.Dashboard, "drawer_emp_dash"),
          WorkspaceNavItem(FrontendRoutes.EMPLOYEE_CHECK_IN, "Attendance Check-In", Icons.AutoMirrored.Filled.Login, "drawer_emp_in"),
          WorkspaceNavItem(FrontendRoutes.EMPLOYEE_CHECK_OUT, "Attendance Check-Out", Icons.AutoMirrored.Filled.Logout, "drawer_emp_out"),
          WorkspaceNavItem(FrontendRoutes.EMPLOYEE_LOCATION_PRESENTATION, "Location & Geofence", Icons.Filled.LocationOn, "drawer_emp_loc"),
          WorkspaceNavItem(FrontendRoutes.EMPLOYEE_BIOMETRIC_PRESENTATION, "Biometric Security", Icons.Filled.Fingerprint, "drawer_emp_bio"),
          WorkspaceNavItem(FrontendRoutes.EMPLOYEE_ATTENDANCE_HISTORY, "Attendance History", Icons.Filled.History, "drawer_emp_hist"),
          WorkspaceNavItem(FrontendRoutes.EMPLOYEE_WORK_SCHEDULE, "My Work Schedule", Icons.Filled.Schedule, "drawer_emp_sch"),
          WorkspaceNavItem(FrontendRoutes.EMPLOYEE_LEAVE_APPLICATION, "Apply for Leave", Icons.Filled.CalendarMonth, "drawer_emp_lapp"),
          WorkspaceNavItem(FrontendRoutes.EMPLOYEE_LEAVE_HISTORY, "Leave History", Icons.Filled.EventBusy, "drawer_emp_lhist"),
          WorkspaceNavItem(FrontendRoutes.EMPLOYEE_NOTIFICATIONS, "Notifications", Icons.Filled.Notifications, "drawer_emp_notif"),
          WorkspaceNavItem(FrontendRoutes.EMPLOYEE_DOCUMENTS, "My Documents", Icons.Filled.Description, "drawer_emp_docs"),
          WorkspaceNavItem(FrontendRoutes.EMPLOYEE_PROFILE, "Employee Profile", Icons.Filled.Badge, "drawer_emp_prof"),
          WorkspaceNavItem(FrontendRoutes.EMPLOYEE_SETTINGS, "App Settings", Icons.Filled.Settings, "drawer_emp_set"),
        )
      null -> emptyList()
    }

  val showWorkspaceChrome = workspaceRole != null && !isCameraOpen
  val isPrimaryTab = primaryTabs.any { it.route == currentRoute }
  val shouldRenderDrawerSheet =
    showWorkspaceChrome &&
      (drawerSheetActive || drawerState.currentValue == DrawerValue.Open || drawerState.targetValue == DrawerValue.Open)

  ModalNavigationDrawer(
    drawerState = drawerState,
    gesturesEnabled = showWorkspaceChrome,
    drawerContent = {
      if (shouldRenderDrawerSheet) {
        ModalDrawerSheet {
          Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(vertical = 12.dp)
          ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
              Text(
                text = state.organization?.name ?: "CivicTrack",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "${workspaceRole.label} Workspace",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            drawerLinks.forEach { item ->
              NavigationDrawerItem(
                label = { Text(item.label) },
                selected = currentRoute == item.route,
                onClick = {
                  drawerSheetActive = false
                  scope.launch { drawerState.close() }
                  navController.navigate(item.route) { launchSingleTop = true }
                },
                icon = { Icon(item.icon, contentDescription = null) },
                modifier = Modifier.padding(horizontal = 12.dp).testTag(item.testTag),
              )
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Spacer(Modifier.height(4.dp))
            NavigationDrawerItem(
              label = { Text("Sign Out") },
              selected = false,
              onClick = {
                drawerSheetActive = false
                scope.launch { drawerState.close() }
                signOutToWelcome()
              },
              icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
              modifier = Modifier.padding(horizontal = 12.dp).testTag("drawer_sign_out"),
            )
          }
        }
      } else {
        Box(modifier = Modifier.width(300.dp).fillMaxHeight())
      }
    },
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useRail = maxWidth >= 600.dp

      Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
          if (!isCameraOpen && currentRoute != FrontendRoutes.SPLASH) {
            CenterAlignedTopAppBar(
              title = {
                Text(
                  text = titleForRoute(currentRoute),
                  style = MaterialTheme.typography.titleLarge,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              navigationIcon = {
                if (workspaceRole != null && isPrimaryTab) {
                  IconButton(
                    onClick = { drawerSheetActive = true },
                    modifier = Modifier.testTag("workspace_drawer_button"),
                  ) {
                    Icon(Icons.Filled.Menu, contentDescription = "Open navigation menu")
                  }
                } else if (currentRoute != FrontendRoutes.WELCOME) {
                  IconButton(
                    onClick = {
                      if (!navController.popBackStack()) {
                        val fallback =
                          if (workspaceRole != null) dashboardRouteForRole(workspaceRole)
                          else FrontendRoutes.WELCOME
                        navController.navigate(fallback) { launchSingleTop = true }
                      }
                    },
                    modifier = Modifier.testTag("topbar_back_button"),
                  ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                  }
                }
              },
              actions = {
                if (workspaceRole != null) {
                  val unread = state.unreadNotificationsCountForActiveRole
                  val notifRoute =
                    when (workspaceRole) {
                      CivicRole.OWNER -> FrontendRoutes.OWNER_NOTIFICATIONS
                      CivicRole.SUPERVISOR -> FrontendRoutes.SUPERVISOR_NOTIFICATIONS
                      CivicRole.EMPLOYEE -> FrontendRoutes.EMPLOYEE_NOTIFICATIONS
                    }
                  IconButton(
                    onClick = { navController.navigate(notifRoute) { launchSingleTop = true } },
                    modifier = Modifier.testTag("topbar_notifications_button"),
                  ) {
                    BadgedBox(
                      badge = {
                        if (unread > 0) {
                          Badge { Text(unread.toString()) }
                        }
                      }
                    ) {
                      Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                    }
                  }
                }
              },
              colors =
                TopAppBarDefaults.centerAlignedTopAppBarColors(
                  containerColor = MaterialTheme.colorScheme.surface
                ),
            )
          }
        },
        bottomBar = {
          if (showWorkspaceChrome && !useRail) {
            NavigationBar(windowInsets = WindowInsets.navigationBars) {
              primaryTabs.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationBarItem(
                  selected = selected,
                  onClick = {
                    val home = dashboardRouteForRole(workspaceRole)
                    if (tab.route == home) {
                      if (currentRoute != home && !navController.popBackStack(home, inclusive = false)) {
                        navController.navigate(home) {
                          popUpTo(navController.graph.id) { inclusive = true }
                          launchSingleTop = true
                        }
                      }
                    } else {
                      navController.navigate(tab.route) {
                        popUpTo(home) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                      }
                    }
                  },
                  icon = { Icon(tab.icon, contentDescription = tab.label) },
                  label = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                  modifier = Modifier.testTag(tab.testTag),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          Row(modifier = Modifier.fillMaxSize()) {
            if (showWorkspaceChrome && useRail) {
              NavigationRail {
                primaryTabs.forEach { tab ->
                  val selected = currentRoute == tab.route
                  NavigationRailItem(
                    selected = selected,
                    onClick = {
                      val home = dashboardRouteForRole(workspaceRole)
                      if (tab.route == home) {
                        if (currentRoute != home && !navController.popBackStack(home, inclusive = false)) {
                          navController.navigate(home) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                          }
                        }
                      } else {
                        navController.navigate(tab.route) {
                          popUpTo(home) { saveState = true }
                          launchSingleTop = true
                          restoreState = true
                        }
                      }
                    },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label) },
                    modifier = Modifier.testTag(tab.testTag),
                  )
                }
              }
            }

            NavHost(
              navController = navController,
              startDestination = initialRoute,
              enterTransition = { EnterTransition.None },
              exitTransition = { ExitTransition.None },
              popEnterTransition = { EnterTransition.None },
              popExitTransition = { ExitTransition.None },
              modifier = Modifier.weight(1f).fillMaxSize(),
            ) {
              // --- 1. Entry & Auth (10 screens) ---
              composable(FrontendRoutes.SPLASH) {
                SplashScreen(
                  onContinue = {
                    navController.navigate(FrontendRoutes.WELCOME) {
                      popUpTo(FrontendRoutes.SPLASH) { inclusive = true }
                      launchSingleTop = true
                    }
                  }
                )
              }
              composable(FrontendRoutes.WELCOME) {
                WelcomeScreen(
                  state = state,
                  onRecheckConnection = viewModel::checkSupabaseConnection,
                  onGetStarted = { navController.navigate(FrontendRoutes.ROLE_SELECTION) },
                  onSignIn = { navController.navigate(FrontendRoutes.LOGIN) },
                  onBiometricUnlock = {
                    activity?.let { act ->
                      viewModel.unlockWithBiometrics(act) { role -> navigateToWorkspace(role) }
                    }
                  },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.ROLE_SELECTION) {
                RoleSelectionScreen(
                  state = state,
                  onSelectOwnerRegistration = { navController.navigate(FrontendRoutes.OWNER_REGISTRATION) },
                  onSelectSupervisorInvite = { navController.navigate(FrontendRoutes.SUPERVISOR_INVITATION) },
                  onSelectEmployeeInvite = { navController.navigate(FrontendRoutes.EMPLOYEE_INVITATION) },
                  onOpenLogin = { navController.navigate(FrontendRoutes.LOGIN) },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.OWNER_REGISTRATION) {
                OwnerRegistrationScreen(
                  state = state,
                  onSubmitRegistration = {
                    fullName,
                    email,
                    phone,
                    orgName,
                    regNumber,
                    countryCode,
                    currencyCode,
                    password,
                    confirmPassword ->
                    viewModel.registerOwner(
                      fullName = fullName,
                      email = email,
                      phone = phone,
                      orgName = orgName,
                      registrationNumber = regNumber,
                      countryCode = countryCode,
                      currencyCode = currencyCode,
                      password = password,
                      confirmPassword = confirmPassword,
                      onEmailVerificationNeeded = {
                        navController.navigate(FrontendRoutes.EMAIL_VERIFICATION_INFO)
                      },
                      onOwnerCreated = { navigateToWorkspace(CivicRole.OWNER) },
                    )
                  },
                  onGoToLogin = { navController.navigate(FrontendRoutes.LOGIN) },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.SUPERVISOR_INVITATION) {
                SupervisorInvitationAcceptanceScreen(
                  state = state,
                  onValidateCode = { token ->
                    viewModel.validateInvitationToken(token, OrgMemberRole.SUPERVISOR)
                  },
                  onAcceptInvite = { fullName, password, confirmPassword, isExisting ->
                    viewModel.acceptSupervisorInvitation(
                      fullName = fullName,
                      password = password,
                      confirmPassword = confirmPassword,
                      isExistingAccountSignIn = isExisting,
                      onAccepted = { navigateToWorkspace(CivicRole.SUPERVISOR) },
                    )
                  },
                  onDeclineInvite = {
                    viewModel.declineValidatedInvitation {
                      navController.navigate(FrontendRoutes.ROLE_SELECTION) {
                        popUpTo(FrontendRoutes.WELCOME)
                      }
                    }
                  },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.EMPLOYEE_INVITATION) {
                EmployeeInvitationAcceptanceScreen(
                  state = state,
                  onValidateCode = { token ->
                    viewModel.validateInvitationToken(token, OrgMemberRole.EMPLOYEE)
                  },
                  onAcceptInvite = {
                    fullName,
                    empCode,
                    jobTitle,
                    password,
                    confirmPassword,
                    isExisting ->
                    viewModel.acceptEmployeeInvitation(
                      fullName = fullName,
                      employeeCode = empCode,
                      jobTitle = jobTitle,
                      password = password,
                      confirmPassword = confirmPassword,
                      isExistingAccountSignIn = isExisting,
                      onAccepted = { navigateToWorkspace(CivicRole.EMPLOYEE) },
                    )
                  },
                  onDeclineInvite = {
                    viewModel.declineValidatedInvitation {
                      navController.navigate(FrontendRoutes.ROLE_SELECTION) {
                        popUpTo(FrontendRoutes.WELCOME)
                      }
                    }
                  },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.LOGIN) {
                LoginScreen(
                  state = state,
                  onSignIn = { email, password ->
                    viewModel.signIn(email, password) { role -> navigateToWorkspace(role) }
                  },
                  onBiometricUnlock = {
                    activity?.let { act ->
                      viewModel.unlockWithBiometrics(act) { role -> navigateToWorkspace(role) }
                    }
                  },
                  onForgotPassword = { navController.navigate(FrontendRoutes.FORGOT_PASSWORD) },
                  onRoleSelection = { navController.navigate(FrontendRoutes.ROLE_SELECTION) },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.FORGOT_PASSWORD) {
                ForgotPasswordScreen(
                  state = state,
                  onSubmitForgot = { email -> viewModel.requestPasswordReset(email) {} },
                  onOpenResetPassword = { navController.navigate(FrontendRoutes.PASSWORD_RESET) },
                  onBackToLogin = { navController.popBackStack() },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.PASSWORD_RESET) {
                PasswordResetScreen(
                  state = state,
                  onSubmitReset = { newPw, confirmPw ->
                    viewModel.submitPasswordReset(newPw, confirmPw) {
                      navController.navigate(FrontendRoutes.LOGIN) {
                        popUpTo(FrontendRoutes.WELCOME)
                      }
                    }
                  },
                  onBackToLogin = {
                    navController.navigate(FrontendRoutes.LOGIN) {
                      popUpTo(FrontendRoutes.WELCOME)
                    }
                  },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.EMAIL_VERIFICATION_INFO) {
                EmailVerificationInfoScreen(
                  state = state,
                  onCheckVerifiedAndContinue = {
                    viewModel.checkEmailVerifiedAndFinishOwnerSetup(
                      onOwnerCreated = { navigateToWorkspace(CivicRole.OWNER) },
                      onNeedsLogin = {
                        navController.navigate(FrontendRoutes.LOGIN) {
                          popUpTo(FrontendRoutes.WELCOME)
                        }
                      },
                    )
                  },
                  onGoToLogin = {
                    navController.navigate(FrontendRoutes.LOGIN) {
                      popUpTo(FrontendRoutes.WELCOME)
                    }
                  },
                  onDismissBanner = viewModel::clearBanners,
                )
              }

              // --- 2. Organisation Owner (19 screens) ---
              composable(FrontendRoutes.OWNER_DASHBOARD) {
                OwnerDashboardFrontendScreen(
                  state = state,
                  onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } },
                  onRefresh = viewModel::refreshActiveWorkspace,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.OWNER_ORG_PROFILE) {
                OwnerOrgProfileFrontendScreen(
                  state = state,
                  onSaveProfile = viewModel::updateOrganizationProfile,
                  onPickLogo = {
                    orgLogoPickerLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.OWNER_EMPLOYEE_DIRECTORY) {
                OwnerEmployeeDirectoryScreen(
                  state = state,
                  onOpenEmployeeDetails = { empId ->
                    navController.navigate(FrontendRoutes.ownerEmployeeDetails(empId))
                  },
                  onOpenAddEmployee = { navController.navigate(FrontendRoutes.OWNER_ADD_EMPLOYEE) },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(
                route = FrontendRoutes.OWNER_EMPLOYEE_DETAILS,
                arguments = listOf(navArgument("employeeId") { type = NavType.StringType }),
              ) { entry ->
                val employeeId = entry.arguments?.getString("employeeId").orEmpty()
                OwnerEmployeeDetailsScreen(
                  state = state,
                  employeeId = employeeId,
                  onOpenAttendanceDetails = { recId ->
                    navController.navigate(FrontendRoutes.ownerAttendanceDetails(recId))
                  },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.OWNER_ADD_EMPLOYEE) {
                OwnerAddEmployeeScreen(
                  state = state,
                  onInviteEmployee = viewModel::inviteEmployee,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.OWNER_SUPERVISOR_DIRECTORY) {
                OwnerSupervisorDirectoryScreen(
                  state = state,
                  onOpenInviteSupervisor = { navController.navigate(FrontendRoutes.OWNER_INVITE_SUPERVISOR) },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.OWNER_INVITE_SUPERVISOR) {
                OwnerInviteSupervisorScreen(
                  state = state,
                  onInviteSupervisor = viewModel::inviteSupervisor,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.OWNER_DEPARTMENTS) {
                OwnerDepartmentManagementScreen(
                  state = state,
                  onAddDepartment = viewModel::addDepartment,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.OWNER_WORKPLACES) {
                OwnerWorkplaceManagementScreen(
                  state = state,
                  onCaptureCurrentGps = {
                    activity?.let(viewModel::requestWorkplaceGpsCapture)
                  },
                  onAddWorkplace = viewModel::addWorkplace,
                  onOpenBlockedSettings = {
                    val intent =
                      if (state.blockedPermissionRequiresLocationSettings) {
                        capabilityHelper.buildLocationSettingsIntent()
                      } else {
                        capabilityHelper.buildAppSettingsIntent()
                      }
                    context.startActivity(intent)
                  },
                  onDismissBlockedBanner = viewModel::dismissPermissionBlockedBanner,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.OWNER_WORK_SCHEDULES) {
                OwnerWorkScheduleScreen(
                  state = state,
                  onSaveScheduleAndPolicies = viewModel::saveOrganizationSettingsAndSchedule,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.OWNER_ATTENDANCE_OVERVIEW) {
                OwnerAttendanceOverviewScreen(
                  state = state,
                  onOpenRecordDetails = { recId ->
                    navController.navigate(FrontendRoutes.ownerAttendanceDetails(recId))
                  },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(
                route = FrontendRoutes.OWNER_ATTENDANCE_DETAILS,
                arguments = listOf(navArgument("recordId") { type = NavType.StringType }),
              ) { entry ->
                val recordId = entry.arguments?.getString("recordId").orEmpty()
                OwnerAttendanceDetailsScreen(
                  state = state,
                  recordId = recordId,
                  onReviewRecord = viewModel::reviewAttendanceRecord,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.OWNER_LEAVE_MANAGEMENT) {
                OwnerLeaveManagementScreen(
                  state = state,
                  onDecideLeave = viewModel::decideLeaveRequest,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.OWNER_REPORTS) {
                OwnerReportsAnalyticsScreen(
                  state = state,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.OWNER_NOTIFICATIONS) {
                OwnerNotificationCentreScreen(
                  state = state,
                  onMarkRead = viewModel::markNotificationRead,
                  onMarkAllRead = viewModel::markAllNotificationsRead,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.OWNER_AUDIT_ACTIVITY) {
                OwnerAuditActivityScreen(
                  state = state,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.OWNER_SETTINGS) {
                OwnerOrgSettingsScreen(
                  state = state,
                  onSaveOrgSettings = viewModel::saveOrganizationSettingsAndSchedule,
                  onThemeChange = viewModel::setThemeMode,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.OWNER_BILLING) {
                OwnerBillingScreen(
                  state = state,
                  onRecheckConnection = viewModel::checkSupabaseConnection,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.OWNER_PROFILE) {
                OwnerProfileScreen(
                  state = state,
                  onSaveProfile = viewModel::updateCurrentProfile,
                  onSignOut = { signOutToWelcome() },
                  onDismissBanner = viewModel::clearBanners,
                )
              }

              // --- 3. Supervisor (12 screens) ---
              composable(FrontendRoutes.SUPERVISOR_DASHBOARD) {
                SupervisorDashboardFrontendScreen(
                  state = state,
                  onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } },
                  onRefresh = viewModel::refreshActiveWorkspace,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.SUPERVISOR_WORKFORCE) {
                SupervisorWorkforceScreen(
                  state = state,
                  onOpenEmployeeDetails = { empId ->
                    navController.navigate(FrontendRoutes.supervisorEmployeeDetails(empId))
                  },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(
                route = FrontendRoutes.SUPERVISOR_EMPLOYEE_DETAILS,
                arguments = listOf(navArgument("employeeId") { type = NavType.StringType }),
              ) { entry ->
                val employeeId = entry.arguments?.getString("employeeId").orEmpty()
                SupervisorEmployeeDetailsScreen(
                  state = state,
                  employeeId = employeeId,
                  onOpenExceptions = { navController.navigate(FrontendRoutes.SUPERVISOR_EXCEPTIONS) },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.SUPERVISOR_ATTENDANCE_OVERVIEW) {
                SupervisorAttendanceOverviewScreen(
                  state = state,
                  onOpenExceptions = { navController.navigate(FrontendRoutes.SUPERVISOR_EXCEPTIONS) },
                  onOpenEmployeeDetails = { empId ->
                    navController.navigate(FrontendRoutes.supervisorEmployeeDetails(empId))
                  },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.SUPERVISOR_EXCEPTIONS) {
                SupervisorExceptionsScreen(
                  state = state,
                  onReviewRecord = viewModel::reviewAttendanceRecord,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.SUPERVISOR_LEAVE_REQUESTS) {
                SupervisorLeaveRequestsScreen(
                  state = state,
                  onOpenLeaveDetail = { leaveId ->
                    navController.navigate(FrontendRoutes.supervisorLeaveDetails(leaveId))
                  },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(
                route = FrontendRoutes.SUPERVISOR_LEAVE_DETAILS,
                arguments = listOf(navArgument("leaveId") { type = NavType.StringType }),
              ) { entry ->
                val leaveId = entry.arguments?.getString("leaveId").orEmpty()
                SupervisorLeaveDetailScreen(
                  state = state,
                  leaveId = leaveId,
                  onDecideLeave = viewModel::decideLeaveRequest,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.SUPERVISOR_TEAM_SCHEDULES) {
                SupervisorTeamSchedulesScreen(
                  state = state,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.SUPERVISOR_WORKPLACE_INFO) {
                SupervisorWorkplaceInfoScreen(
                  state = state,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.SUPERVISOR_REPORTS) {
                SupervisorReportsScreen(
                  state = state,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.SUPERVISOR_NOTIFICATIONS) {
                SupervisorNotificationsScreen(
                  state = state,
                  onMarkRead = viewModel::markNotificationRead,
                  onMarkAllRead = viewModel::markAllNotificationsRead,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.SUPERVISOR_PROFILE_SETTINGS) {
                SupervisorProfileSettingsScreen(
                  state = state,
                  onSaveProfile = viewModel::updateCurrentProfile,
                  onToggleSupervisorPrefs = viewModel::toggleSupervisorPrefs,
                  onThemeChange = viewModel::setThemeMode,
                  onRecheckConnection = viewModel::checkSupabaseConnection,
                  onSignOut = { signOutToWelcome() },
                  onDismissBanner = viewModel::clearBanners,
                )
              }

              // --- 4. Employee (14 screens) ---
              composable(FrontendRoutes.EMPLOYEE_DASHBOARD) {
                EmployeeDashboardFrontendScreen(
                  state = state,
                  onNavigate = { route -> navController.navigate(route) { launchSingleTop = true } },
                  onRefresh = viewModel::refreshActiveWorkspace,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.EMPLOYEE_CHECK_IN) {
                EmployeeCheckInScreen(
                  state = state,
                  onStartCheckIn = {
                    activity?.let { act ->
                      viewModel.initiateAttendanceAction(AttendanceCheckType.CHECK_IN, act)
                    }
                  },
                  onRetrySync = viewModel::retryPendingAttendanceSubmission,
                  onOpenBlockedSettings = {
                    val intent =
                      if (state.blockedPermissionRequiresLocationSettings) {
                        capabilityHelper.buildLocationSettingsIntent()
                      } else {
                        capabilityHelper.buildAppSettingsIntent()
                      }
                    context.startActivity(intent)
                  },
                  onDismissBlockedBanner = viewModel::dismissPermissionBlockedBanner,
                  onOpenLocationDiagnostics = {
                    navController.navigate(FrontendRoutes.EMPLOYEE_LOCATION_PRESENTATION)
                  },
                  onOpenBiometricSettings = {
                    navController.navigate(FrontendRoutes.EMPLOYEE_BIOMETRIC_PRESENTATION)
                  },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.EMPLOYEE_CHECK_OUT) {
                EmployeeCheckOutScreen(
                  state = state,
                  onStartCheckOut = {
                    activity?.let { act ->
                      viewModel.initiateAttendanceAction(AttendanceCheckType.CHECK_OUT, act)
                    }
                  },
                  onRetrySync = viewModel::retryPendingAttendanceSubmission,
                  onOpenBlockedSettings = {
                    val intent =
                      if (state.blockedPermissionRequiresLocationSettings) {
                        capabilityHelper.buildLocationSettingsIntent()
                      } else {
                        capabilityHelper.buildAppSettingsIntent()
                      }
                    context.startActivity(intent)
                  },
                  onDismissBlockedBanner = viewModel::dismissPermissionBlockedBanner,
                  onOpenLocationDiagnostics = {
                    navController.navigate(FrontendRoutes.EMPLOYEE_LOCATION_PRESENTATION)
                  },
                  onOpenBiometricSettings = {
                    navController.navigate(FrontendRoutes.EMPLOYEE_BIOMETRIC_PRESENTATION)
                  },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.EMPLOYEE_LOCATION_PRESENTATION) {
                EmployeeLocationVerificationScreen(
                  state = state,
                  onRunLiveLocationCheck = {
                    activity?.let(viewModel::runLiveLocationCheck)
                  },
                  onOpenLocationSettings = {
                    context.startActivity(capabilityHelper.buildLocationSettingsIntent())
                  },
                  onOpenAppSettings = {
                    context.startActivity(capabilityHelper.buildAppSettingsIntent())
                  },
                  onDismissBlockedBanner = viewModel::dismissPermissionBlockedBanner,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.EMPLOYEE_BIOMETRIC_PRESENTATION) {
                EmployeeBiometricConfirmationScreen(
                  state = state,
                  onToggleBiometricLogin = viewModel::setBiometricLoginEnabled,
                  onTestBiometricPrompt = {
                    activity?.let(viewModel::testBiometricPrompt)
                  },
                  onOpenBiometricEnroll = {
                    context.startActivity(capabilityHelper.buildBiometricEnrollIntent())
                  },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.EMPLOYEE_ATTENDANCE_HISTORY) {
                EmployeeAttendanceHistoryScreen(
                  state = state,
                  onOpenRecordDetails = { recId ->
                    navController.navigate(FrontendRoutes.employeeAttendanceDetails(recId))
                  },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(
                route = FrontendRoutes.EMPLOYEE_ATTENDANCE_DETAILS,
                arguments = listOf(navArgument("recordId") { type = NavType.StringType }),
              ) { entry ->
                val recordId = entry.arguments?.getString("recordId").orEmpty()
                EmployeeAttendanceDetailScreen(
                  state = state,
                  recordId = recordId,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.EMPLOYEE_WORK_SCHEDULE) {
                EmployeeWorkScheduleScreen(
                  state = state,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.EMPLOYEE_LEAVE_APPLICATION) {
                EmployeeLeaveApplicationScreen(
                  state = state,
                  onSubmitLeave = viewModel::submitEmployeeLeave,
                  onSubmittedNavigateToHistory = {
                    navController.navigate(FrontendRoutes.EMPLOYEE_LEAVE_HISTORY) {
                      popUpTo(FrontendRoutes.EMPLOYEE_DASHBOARD)
                    }
                  },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.EMPLOYEE_LEAVE_HISTORY) {
                EmployeeLeaveHistoryScreen(
                  state = state,
                  onOpenApplyLeave = { navController.navigate(FrontendRoutes.EMPLOYEE_LEAVE_APPLICATION) },
                  onCancelLeave = viewModel::cancelEmployeeLeave,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.EMPLOYEE_NOTIFICATIONS) {
                EmployeeNotificationsScreen(
                  state = state,
                  onMarkRead = viewModel::markNotificationRead,
                  onMarkAllRead = viewModel::markAllNotificationsRead,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.EMPLOYEE_DOCUMENTS) {
                EmployeeDocumentsScreen(
                  state = state,
                  onStartProfilePhotoCapture = {
                    activity?.let { act ->
                      viewModel.requestCameraCapture(CaptureTarget.PROFILE_PHOTO, act)
                    }
                  },
                  onStartDocumentCapture = {
                    activity?.let { act ->
                      viewModel.requestCameraCapture(CaptureTarget.DOCUMENT, act)
                    }
                  },
                  onPickDocumentFromGallery = {
                    documentPickerLauncher.launch(
                      PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                  },
                  onOpenAppSettings = {
                    context.startActivity(capabilityHelper.buildAppSettingsIntent())
                  },
                  onDismissBlockedBanner = viewModel::dismissPermissionBlockedBanner,
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.EMPLOYEE_PROFILE) {
                EmployeeProfileScreen(
                  state = state,
                  onSaveProfile = viewModel::updateCurrentProfile,
                  onStartProfilePhotoCapture = {
                    activity?.let { act ->
                      viewModel.requestCameraCapture(CaptureTarget.PROFILE_PHOTO, act)
                    }
                  },
                  onOpenSettings = { navController.navigate(FrontendRoutes.EMPLOYEE_SETTINGS) },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
              composable(FrontendRoutes.EMPLOYEE_SETTINGS) {
                EmployeeSettingsScreen(
                  state = state,
                  onToggleEmployeeSettings = viewModel::toggleEmployeeAppSettings,
                  onToggleBiometricLogin = viewModel::setBiometricLoginEnabled,
                  onThemeChange = viewModel::setThemeMode,
                  onRecheckConnection = viewModel::checkSupabaseConnection,
                  onSignOut = { signOutToWelcome() },
                  onDismissBanner = viewModel::clearBanners,
                )
              }
            }
          }

          if (state.activeCameraTarget != null) {
            Surface(
              modifier = Modifier.fillMaxSize(),
              color = MaterialTheme.colorScheme.background,
            ) {
              val asset = state.pendingUploadAsset
              if (asset != null) {
                CapturedPreviewScreen(
                  target = state.activeCameraTarget!!,
                  asset = asset,
                  selectedCategory = state.selectedDocumentCategory,
                  documentTitle = state.documentTitleInput,
                  isUploading = state.isUploadingMedia,
                  errorMessage = state.errorBanner,
                  onCategorySelected = viewModel::updateDocumentCategory,
                  onTitleChanged = viewModel::updateDocumentTitle,
                  onRetake = viewModel::retakePhoto,
                  onCancel = viewModel::cancelCameraCapture,
                  onConfirmUpload = viewModel::confirmUploadPendingAsset,
                )
              } else {
                CameraXCaptureView(
                  target = state.activeCameraTarget!!,
                  onImageCaptured = { file ->
                    viewModel.onImageCapturedFromCamera(context, file)
                  },
                  onCancel = viewModel::cancelCameraCapture,
                )
              }
            }
          }
        }
      }
    }
  }
}

private fun dashboardRouteForRole(role: CivicRole): String =
  when (role) {
    CivicRole.OWNER -> FrontendRoutes.OWNER_DASHBOARD
    CivicRole.SUPERVISOR -> FrontendRoutes.SUPERVISOR_DASHBOARD
    CivicRole.EMPLOYEE -> FrontendRoutes.EMPLOYEE_DASHBOARD
  }

private fun titleForRoute(route: String): String =
  when (route) {
    FrontendRoutes.WELCOME -> "CivicTrack"
    FrontendRoutes.ROLE_SELECTION -> "Select Account Role"
    FrontendRoutes.OWNER_REGISTRATION -> "Owner Onboarding"
    FrontendRoutes.SUPERVISOR_INVITATION -> "Supervisor Invitation"
    FrontendRoutes.EMPLOYEE_INVITATION -> "Employee Invitation"
    FrontendRoutes.LOGIN -> "Sign In"
    FrontendRoutes.FORGOT_PASSWORD -> "Forgot Password"
    FrontendRoutes.PASSWORD_RESET -> "Reset Password"
    FrontendRoutes.EMAIL_VERIFICATION_INFO -> "Verify Email"
    FrontendRoutes.OWNER_DASHBOARD -> "Owner Dashboard"
    FrontendRoutes.OWNER_ORG_PROFILE -> "Organisation Profile"
    FrontendRoutes.OWNER_EMPLOYEE_DIRECTORY -> "Employee Directory"
    FrontendRoutes.OWNER_EMPLOYEE_DETAILS -> "Employee Details"
    FrontendRoutes.OWNER_ADD_EMPLOYEE -> "Invite Employee"
    FrontendRoutes.OWNER_SUPERVISOR_DIRECTORY -> "Supervisor Directory"
    FrontendRoutes.OWNER_INVITE_SUPERVISOR -> "Invite Supervisor"
    FrontendRoutes.OWNER_DEPARTMENTS -> "Departments"
    FrontendRoutes.OWNER_WORKPLACES -> "Workplaces"
    FrontendRoutes.OWNER_WORK_SCHEDULES -> "Work Schedules"
    FrontendRoutes.OWNER_ATTENDANCE_OVERVIEW -> "Attendance Overview"
    FrontendRoutes.OWNER_ATTENDANCE_DETAILS -> "Attendance Details"
    FrontendRoutes.OWNER_LEAVE_MANAGEMENT -> "Leave Management"
    FrontendRoutes.OWNER_REPORTS -> "Reports & Analytics"
    FrontendRoutes.OWNER_NOTIFICATIONS -> "Notifications"
    FrontendRoutes.OWNER_AUDIT_ACTIVITY -> "Audit Activity"
    FrontendRoutes.OWNER_SETTINGS -> "Organisation Settings"
    FrontendRoutes.OWNER_BILLING -> "Workspace Status"
    FrontendRoutes.OWNER_PROFILE -> "Owner Profile"
    FrontendRoutes.SUPERVISOR_DASHBOARD -> "Supervisor Dashboard"
    FrontendRoutes.SUPERVISOR_WORKFORCE -> "Assigned Workforce"
    FrontendRoutes.SUPERVISOR_EMPLOYEE_DETAILS -> "Staff Details"
    FrontendRoutes.SUPERVISOR_ATTENDANCE_OVERVIEW -> "Team Attendance"
    FrontendRoutes.SUPERVISOR_EXCEPTIONS -> "Exception Review"
    FrontendRoutes.SUPERVISOR_LEAVE_REQUESTS -> "Team Leave Queue"
    FrontendRoutes.SUPERVISOR_LEAVE_DETAILS -> "Leave Details"
    FrontendRoutes.SUPERVISOR_TEAM_SCHEDULES -> "Team Schedules"
    FrontendRoutes.SUPERVISOR_WORKPLACE_INFO -> "Workplace Info"
    FrontendRoutes.SUPERVISOR_REPORTS -> "Unit Reports"
    FrontendRoutes.SUPERVISOR_NOTIFICATIONS -> "Unit Notifications"
    FrontendRoutes.SUPERVISOR_PROFILE_SETTINGS -> "Supervisor Profile"
    FrontendRoutes.EMPLOYEE_DASHBOARD -> "Employee Portal"
    FrontendRoutes.EMPLOYEE_CHECK_IN -> "Attendance Check-In"
    FrontendRoutes.EMPLOYEE_CHECK_OUT -> "Attendance Check-Out"
    FrontendRoutes.EMPLOYEE_LOCATION_PRESENTATION -> "Location Verification"
    FrontendRoutes.EMPLOYEE_BIOMETRIC_PRESENTATION -> "Biometric Security"
    FrontendRoutes.EMPLOYEE_ATTENDANCE_HISTORY -> "Attendance History"
    FrontendRoutes.EMPLOYEE_ATTENDANCE_DETAILS -> "Attendance Record"
    FrontendRoutes.EMPLOYEE_WORK_SCHEDULE -> "My Work Schedule"
    FrontendRoutes.EMPLOYEE_LEAVE_APPLICATION -> "Apply for Leave"
    FrontendRoutes.EMPLOYEE_LEAVE_HISTORY -> "My Leave History"
    FrontendRoutes.EMPLOYEE_NOTIFICATIONS -> "Notifications"
    FrontendRoutes.EMPLOYEE_DOCUMENTS -> "My Documents"
    FrontendRoutes.EMPLOYEE_PROFILE -> "My Profile"
    FrontendRoutes.EMPLOYEE_SETTINGS -> "App Settings"
    else -> "CivicTrack"
  }
