package com.example.ui.frontend.navigation

object FrontendRoutes {
  // 1. Application Entry & Authentication (10 screens)
  const val SPLASH = "entry/splash"
  const val WELCOME = "entry/welcome"
  const val ROLE_SELECTION = "entry/role_selection"
  const val OWNER_REGISTRATION = "entry/owner_registration"
  const val SUPERVISOR_INVITATION = "entry/supervisor_invitation"
  const val EMPLOYEE_INVITATION = "entry/employee_invitation"
  const val LOGIN = "entry/login"
  const val FORGOT_PASSWORD = "entry/forgot_password"
  const val PASSWORD_RESET = "entry/password_reset"
  const val EMAIL_VERIFICATION_INFO = "entry/email_verification_info"

  // 2. Organisation Owner Interface (19 screens)
  const val OWNER_DASHBOARD = "owner/dashboard"
  const val OWNER_ORG_PROFILE = "owner/org_profile"
  const val OWNER_EMPLOYEE_DIRECTORY = "owner/employees"
  const val OWNER_EMPLOYEE_DETAILS = "owner/employees/{employeeId}"
  const val OWNER_ADD_EMPLOYEE = "owner/add_employee"
  const val OWNER_SUPERVISOR_DIRECTORY = "owner/supervisors"
  const val OWNER_INVITE_SUPERVISOR = "owner/invite_supervisor"
  const val OWNER_DEPARTMENTS = "owner/departments"
  const val OWNER_WORKPLACES = "owner/workplaces"
  const val OWNER_WORK_SCHEDULES = "owner/schedules"
  const val OWNER_ATTENDANCE_OVERVIEW = "owner/attendance"
  const val OWNER_ATTENDANCE_DETAILS = "owner/attendance/{recordId}"
  const val OWNER_LEAVE_MANAGEMENT = "owner/leave"
  const val OWNER_REPORTS = "owner/reports"
  const val OWNER_NOTIFICATIONS = "owner/notifications"
  const val OWNER_AUDIT_ACTIVITY = "owner/audit"
  const val OWNER_SETTINGS = "owner/settings"
  const val OWNER_BILLING = "owner/billing"
  const val OWNER_PROFILE = "owner/profile"

  fun ownerEmployeeDetails(employeeId: String) = "owner/employees/$employeeId"
  fun ownerAttendanceDetails(recordId: String) = "owner/attendance/$recordId"

  // 3. Supervisor Interface (12 screens)
  const val SUPERVISOR_DASHBOARD = "supervisor/dashboard"
  const val SUPERVISOR_WORKFORCE = "supervisor/workforce"
  const val SUPERVISOR_EMPLOYEE_DETAILS = "supervisor/workforce/{employeeId}"
  const val SUPERVISOR_ATTENDANCE_OVERVIEW = "supervisor/attendance"
  const val SUPERVISOR_EXCEPTIONS = "supervisor/exceptions"
  const val SUPERVISOR_LEAVE_REQUESTS = "supervisor/leave"
  const val SUPERVISOR_LEAVE_DETAILS = "supervisor/leave/{leaveId}"
  const val SUPERVISOR_TEAM_SCHEDULES = "supervisor/schedules"
  const val SUPERVISOR_WORKPLACE_INFO = "supervisor/workplace"
  const val SUPERVISOR_REPORTS = "supervisor/reports"
  const val SUPERVISOR_NOTIFICATIONS = "supervisor/notifications"
  const val SUPERVISOR_PROFILE_SETTINGS = "supervisor/profile_settings"

  fun supervisorEmployeeDetails(employeeId: String) = "supervisor/workforce/$employeeId"
  fun supervisorLeaveDetails(leaveId: String) = "supervisor/leave/$leaveId"

  // 4. Employee Interface (14 screens)
  const val EMPLOYEE_DASHBOARD = "employee/dashboard"
  const val EMPLOYEE_CHECK_IN = "employee/check_in"
  const val EMPLOYEE_CHECK_OUT = "employee/check_out"
  const val EMPLOYEE_LOCATION_PRESENTATION = "employee/location_presentation"
  const val EMPLOYEE_BIOMETRIC_PRESENTATION = "employee/biometric_presentation"
  const val EMPLOYEE_ATTENDANCE_HISTORY = "employee/attendance_history"
  const val EMPLOYEE_ATTENDANCE_DETAILS = "employee/attendance_history/{recordId}"
  const val EMPLOYEE_WORK_SCHEDULE = "employee/schedule"
  const val EMPLOYEE_LEAVE_APPLICATION = "employee/leave_apply"
  const val EMPLOYEE_LEAVE_HISTORY = "employee/leave_history"
  const val EMPLOYEE_NOTIFICATIONS = "employee/notifications"
  const val EMPLOYEE_DOCUMENTS = "employee/documents"
  const val EMPLOYEE_PROFILE = "employee/profile"
  const val EMPLOYEE_SETTINGS = "employee/settings"

  fun employeeAttendanceDetails(recordId: String) = "employee/attendance_history/$recordId"
}
