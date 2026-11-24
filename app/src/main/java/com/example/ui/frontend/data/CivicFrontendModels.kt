package com.example.ui.frontend.data

enum class CivicRole(val label: String, val subtitle: String) {
  OWNER("Organisation Owner", "Full institutional governance & multi-site administration"),
  SUPERVISOR("Supervisor", "Scoped oversight for assigned department & workplace"),
  EMPLOYEE("Employee", "Personal attendance, schedule, leave & profile portal"),
}

enum class CivicThemeMode(val label: String, val storageKey: String) {
  SYSTEM("System Default", "system"),
  LIGHT("Light Mode", "light"),
  DARK("Dark Mode", "dark");

  companion object {
    fun fromStorage(raw: String?): CivicThemeMode =
      entries.firstOrNull { it.storageKey.equals(raw, ignoreCase = true) } ?: SYSTEM
  }
}

enum class CivicAttendanceStatus(val label: String) {
  ON_TIME("On Time"),
  LATE("Late Arrival"),
  INCOMPLETE("Missing Check-Out"),
  EXCEPTION("Geofence Exception"),
  ABSENT("Absent"),
}

enum class CivicLeaveStatus(val label: String) {
  PENDING("Pending"),
  APPROVED("Approved"),
  REJECTED("Rejected"),
}

data class CivicOrganization(
  val id: String,
  val name: String,
  val registrationNumber: String,
  val countryCode: String,
  val currencyCode: String,
  val industry: String,
  val description: String,
  val contactEmail: String,
  val contactPhone: String,
  val headquartersAddress: String,
  val logoUrl: String? = null,
)

data class CivicDepartment(
  val id: String,
  val name: String,
  val code: String,
  val headSupervisorName: String,
  val staffCount: Int,
  val description: String,
)

data class CivicWorkplace(
  val id: String,
  val name: String,
  val address: String,
  val latitude: Double,
  val longitude: Double,
  val geofenceRadiusMeters: Int,
  val maxAccuracyMeters: Int,
  val assignedStaffCount: Int,
  val activeShiftWindow: String,
)

data class CivicWorkSchedule(
  val id: String,
  val title: String,
  val workingDays: List<String>,
  val startTime: String,
  val endTime: String,
  val lateGraceMinutes: Int,
  val departmentName: String,
)

data class CivicEmployee(
  val id: String,
  val userId: String?,
  val fullName: String,
  val employeeCode: String,
  val email: String,
  val phone: String,
  val jobTitle: String,
  val departmentId: String,
  val departmentName: String,
  val workplaceId: String,
  val workplaceName: String,
  val status: String,
  val attendanceRatePercent: Int,
  val supervisorId: String?,
  val avatarUrl: String? = null,
)

data class CivicSupervisor(
  val id: String,
  val userId: String,
  val fullName: String,
  val email: String,
  val phone: String,
  val departmentId: String,
  val departmentName: String,
  val workplaceId: String,
  val workplaceName: String,
  val assignedStaffCount: Int,
  val status: String,
  val avatarUrl: String? = null,
)

data class CivicAttendanceRecord(
  val id: String,
  val employeeId: String,
  val employeeName: String,
  val employeeCode: String,
  val departmentName: String,
  val workplaceName: String,
  val dateLabel: String,
  val checkType: String,
  val checkInTime: String?,
  val checkOutTime: String?,
  val status: CivicAttendanceStatus,
  val locationNote: String,
  val biometricNote: String,
  val geofenceStatusRaw: String,
  val reviewStatusRaw: String,
  val distanceMeters: Double? = null,
  val accuracyMeters: Double? = null,
  val exceptionReason: String? = null,
  val reviewedBy: String? = null,
)

data class CivicLeaveRequest(
  val id: String,
  val employeeId: String,
  val employeeName: String,
  val departmentName: String,
  val leaveType: String,
  val startDate: String,
  val endDate: String,
  val daysCount: Int,
  val reason: String,
  val status: CivicLeaveStatus,
  val submittedDate: String,
  val reviewerNote: String? = null,
)

data class CivicNotification(
  val id: String,
  val targetRole: CivicRole,
  val title: String,
  val message: String,
  val timestampLabel: String,
  val category: String,
  val isRead: Boolean = false,
)

data class CivicAuditEvent(
  val id: String,
  val actorName: String,
  val actorRole: String,
  val action: String,
  val category: String,
  val summary: String,
  val timestampLabel: String,
)

data class CivicOrgSettings(
  val settingsId: String = "",
  val requireLocation: Boolean = true,
  val requireBiometric: Boolean = true,
  val allowOutsideGeofenceWithNote: Boolean = false,
  val workStartTime: String = "08:00",
  val workEndTime: String = "17:00",
  val lateGraceMinutes: Int = 15,
  val workingDays: List<String> = listOf("MON", "TUE", "WED", "THU", "FRI"),
)

data class CivicInvitationDetails(
  val invitationId: String,
  val organizationId: String,
  val tokenCode: String,
  val organizationName: String,
  val invitedEmail: String,
  val role: String,
  val departmentId: String?,
  val departmentName: String,
  val workplaceId: String?,
  val workplaceName: String,
  val expiresLabel: String,
  val status: String,
)

data class CivicChartBar(
  val label: String,
  val primaryValue: Float,
  val secondaryValue: Float = 0f,
)
