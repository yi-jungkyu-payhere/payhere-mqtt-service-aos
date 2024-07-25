package com.payhere.mqtt

import com.google.gson.annotations.SerializedName

data class MqttBaseRequest<T>(
    @SerializedName(value = "event_name", alternate = ["eventName"]) val eventName: String? = null,
    @SerializedName(value = "message") val message: T
)
