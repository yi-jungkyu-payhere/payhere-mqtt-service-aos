package com.payhere.mqtt

import com.google.gson.annotations.SerializedName

data class ReqMonitorStatus(
    @SerializedName("status") val status: ReqStatusData? = null,
)

data class ReqStatusData(
    @SerializedName("app_info") val appInfo: AppInfo? = null,
    @SerializedName("device_info") val deviceInfo: DeviceInfo? = null,
    @SerializedName("version_info") val versionInfo: VersionInfo? = null,
    @SerializedName("seller_integration_status") val sellerIntegrationStatus: SellerIntegrationStatus? = null,
)

data class AppInfo(
    @SerializedName("app_identifier") val appIdentifier: String? = null,
    @SerializedName("is_active") val isActive: Boolean? = null,
    @SerializedName("is_frozen") val isFrozen: Boolean? = null,
    @SerializedName("is_kiosk_mode") val isKioskMode: Boolean? = null,
    @SerializedName("is_emergency_mode") val isEmergencyMode: Boolean? = null,
)

data class DeviceInfo(
    @SerializedName("memory_usage") val memoryUsage: String? = null,
    @SerializedName("storage_available") val storageAvailable: Int? = null,
    @SerializedName("wifi_name") val wifiName: String? = null,
    @SerializedName("ip_address") val ipAddress: String? = null,
    @SerializedName("wifi_signal_strength") val wifiSignalStrength: Int? = null,
    @SerializedName("battery_level") val batteryLevel: Int? = null,
    @SerializedName("battery_status") val batteryStatus: String? = null,
)

data class VersionInfo(
    @SerializedName("app_version") val appVersion: String? = null,
    @SerializedName("os_version") val osVersion: String? = null,
    @SerializedName("firmware_version") val firmwareVersion: String? = null,
)

data class SellerIntegrationStatus(
    @SerializedName("is_integrated_websoket") val isIntegratedWebsoket: Boolean? = null,
    @SerializedName("is_integrated_mqtt") val isIntegratedMqtt: Boolean? = null,
)
