package com.payhere.mqtt

import com.google.gson.annotations.SerializedName

data class ReqMqtt(
        @SerializedName("event_at") val eventAt: String? = null,
        @SerializedName("event_from") val eventFrom: String? = null,
        @SerializedName("sid") val sid: String? = null,
        @SerializedName("private_ip") val privateIp: String? = null,
        @SerializedName("device_model") val deviceModel: String? = null,
        @SerializedName("identifier") val identifier: String? = null,
        @SerializedName("device_serial_number") val deviceSerialnumber: String? = null,
        @SerializedName("terminal_client_external_key") val terminalClientExternalKey: String? = null,
        @SerializedName("extras") val extras: Map<String, Any>? = null,
        @SerializedName("wifi_signal_strength") val wifiSignalStrength: String? = null,
        @SerializedName("platform") val platform: String? = null,
        @SerializedName("alive") var alive: Boolean? = null,
        @SerializedName("event_name") var eventName: String? = null,
        @SerializedName("message") var message: ResMqttMessage? = null,
        @SerializedName("timestamp") var timestamp: String? = null,
        @SerializedName("result") var result: String? = null,

        //프린터 테스트
)

data class ResMqttMessage(
        @SerializedName("sid") val sid: String? = null,
        @SerializedName("pid") var pid: String? = null,
        @SerializedName("quota") var quota: String? = null,
        @SerializedName("auth_amount") var authAmount: String? = null,
        @SerializedName("refund_amount") var refundAmount: String? = null,
        @SerializedName("seller_payment_provider_external_key") var sellerPaymentProviderExternalKey: String? = null,
        @SerializedName("cooking_external_key") var cookingExternalKey: String? = null,
        @SerializedName("cooking_group_external_key") var cookingGroupExternalKey: String? = null,
        @SerializedName("data") var data: ResMqttData? = null,
        @SerializedName("result") var result: String? = null,
        @SerializedName("need_signature") var needSignature: Boolean? = null,
        @SerializedName("oversea_union_pay") var overseaUnionPay: Boolean? = null,
        @SerializedName("iden_type") var idenType: String? = null,

        @SerializedName("type") var type: String? = null,
        @SerializedName("content") var content: String? = null,
)

data class ResMqttData(
        @SerializedName("reason") val reason: String? = null,
        @SerializedName("reason_code") var reasonCode: String? = null,
        @SerializedName("ui_message") var uiMessage: String? = null,
)



//관제
data class ResMqttCycleData(
    @SerializedName("polling_cycle") val pollingCycle: String? = null,
    @SerializedName("event_at") var eventAt: String? = null,
)

data class ReqMqttStatusData(
    @SerializedName("app_status") val mqttAppStatus: MqttAppStatus? = null,
    @SerializedName("device_status") val mqttDeviceStatus: MqttDeviceStatus? = null,
    @SerializedName("version_info") val mqttVersionInfo: MqttVersionInfo? = null,
    @SerializedName("seller_integration_status") val mqttSellerIntegrationStatus: MqttSellerIntegrationStatus? = null,
    @SerializedName("event_at") val mqttEventAt: String? = null,
)

data class MqttAppStatus(
    @SerializedName("is_active") val isActive: Boolean? = null,
)
data class MqttDeviceStatus(
    @SerializedName("memory_usage") val memoryUsage: Int? = null,
    @SerializedName("storage_available") val storageAvailable: Int? = null,
    @SerializedName("battery_level") val batteryLevel: Int? = null,
    @SerializedName("wifi_signal_strength") val wifiSignalStrength: Int? = null,
    @SerializedName("cpu_status") val cpuStatus: String? = null,
)
data class MqttVersionInfo(
    @SerializedName("app_version") val appVersion: String? = null,
    @SerializedName("firmware_version") val firmwareVersion: String? = null,
)
data class MqttSellerIntegrationStatus(
    @SerializedName("is_integrated_websoket") val isIntegratedWebsoket: Boolean? = null,
    @SerializedName("is_integrated_mqtt") val isIntegratedMqtt: Boolean? = null,
)

