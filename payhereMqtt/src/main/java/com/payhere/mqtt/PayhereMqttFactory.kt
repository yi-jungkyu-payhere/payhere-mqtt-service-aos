package com.payhere.mqtt

import android.content.Context
import android.os.Build
import android.os.CountDownTimer
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import software.amazon.awssdk.crt.mqtt5.Mqtt5Client
import software.amazon.awssdk.crt.mqtt5.Mqtt5ClientOptions
import software.amazon.awssdk.crt.mqtt5.OnAttemptingConnectReturn
import software.amazon.awssdk.crt.mqtt5.OnConnectionFailureReturn
import software.amazon.awssdk.crt.mqtt5.OnConnectionSuccessReturn
import software.amazon.awssdk.crt.mqtt5.OnDisconnectionReturn
import software.amazon.awssdk.crt.mqtt5.OnStoppedReturn
import software.amazon.awssdk.crt.mqtt5.QOS
import software.amazon.awssdk.crt.mqtt5.packets.ConnectPacket
import software.amazon.awssdk.crt.mqtt5.packets.PublishPacket
import software.amazon.awssdk.crt.mqtt5.packets.SubscribePacket
import software.amazon.awssdk.crt.mqtt5.packets.UnsubscribePacket
import software.amazon.awssdk.iot.AwsIotMqtt5ClientBuilder

@RequiresApi(Build.VERSION_CODES.N)
object PayhereMqttFactory {

    private const val AUTHORIZER = "iot-authorizer"

    private var sid = ""
    private var csn = ""
    private var deviceModel = ""
    var access: String? = null
    private var clientEndpoint: String? = null

    var topicConnectionStatus = ""
    var topicConnectionStatusCsn = ""

//    var topicCancel = ""

    var baseTopicCommon = ""
    var baseTopicPayment = ""
    var baseTopicSeller = ""

    //    private var topicCycle = ""
    private var topicStatus = ""

    private var topicShutDown = ""
    private var topicReboot = ""
    var topicLogOut = ""
    private var topicClearCache = ""
    private var topicUpdate = ""
    private var topicReInstall = ""

    private var topicCommonTerminalAlive = ""
    private var topicCommonConnectionCheck = ""

    private var topicPaymentRequestConnect = ""
    private var topicPaymentResultConnect = ""

    private var topicSellerRequestConnect = ""
    private var topicSellerResultConnect = ""

    private var basicTopics = mutableSetOf<String>()
    private var topics = mutableSetOf<String>()
    private var isLogoutAction = false

    private val _messageArrived = MutableSharedFlow<Pair<String, String>>()
    val messageArrived: SharedFlow<Pair<String, String>> = _messageArrived

    private lateinit var cycleStatus: Job

    private var mqtt5Client: Mqtt5Client? = null

    private val coroutineExceptionHandler =
        CoroutineExceptionHandler { _, exception ->
//            eventLogUseCase.sendEventLog(
//                eventCategory = EventCategory.ERROR,
//                eventLog =
//                    mutableMapOf(
//                        "event" to "mqtt message error",
//                        "message" to "${exception.message}",
//                    ),
//            )
        }

    fun initTopic() {
//        connectionJob =
//            CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
//                delay(1000 * 10) // 10초 대기
//                val myData =
//                    Gson().fromJson(
//                        Gson().toJson(
//                            ReqMqtt(deviceModel = deviceModel, deviceSerialnumber = csn),
//                        ),
//                        JsonObject::class.java,
//                    )
//                val jsonArray = JsonArray()
//                jsonArray.add(myData)
//                if (mqtt5Client?.isConnected == false) return@launch
//                val message =
//                    PublishPacket.PublishPacketBuilder()
//                        .withTopic(topicCommonConnectionCheck)
//                        .withQOS(QOS.AT_LEAST_ONCE)
//                        .withPayload(Gson().toJson(jsonArray).toByteArray())
//                        .withRetain(true)
//                        .build()
//                mqtt5Client?.publish(message)
//            }

        // 기존 사용
        topicConnectionStatus = "sellers/$sid/terminal_client/connection-status"
        topicConnectionStatusCsn = "sellers/$sid/terminal_client/connection-status/$csn"

        baseTopicCommon = "${MqttEvent.BASE_TOPIC_COMMON}/$sid/"
        baseTopicPayment = "${MqttEvent.BASE_TOPIC_PAYMENT}/$sid/$csn/"
        baseTopicSeller = "${MqttEvent.BASE_TOPIC_SELLERS}/$sid/terminal/$csn/"

        // 공통
        topicCommonConnectionCheck = "${baseTopicCommon}${MqttEvent.CONNECTION_CHECK}"
        topicCommonTerminalAlive = "${baseTopicCommon}${MqttEvent.ALIVE}"

        // 외부연동
        topicPaymentRequestConnect = "${baseTopicPayment}${MqttEvent.Request.CONNECT}"
        topicPaymentResultConnect = "${baseTopicPayment}${MqttEvent.Result.CONNECT}"
        // 셀러연동
        topicSellerRequestConnect = "${baseTopicSeller}${MqttEvent.Request.CONNECT}"
        topicSellerResultConnect = "${baseTopicSeller}${MqttEvent.Result.CONNECT}"

        // 관제 토픽
//        topicCycle = "${baseTopicSeller}${MqttEvent.CYCLE}"
        topicStatus = "${baseTopicSeller}${MqttEvent.STATUS}"

        // 제어 토픽
        topicShutDown = "${baseTopicSeller}${MqttEvent.Request.SHUTDOWN}"
        topicReboot = "${baseTopicSeller}${MqttEvent.Request.REBOOT}"
        topicLogOut = "${baseTopicSeller}${MqttEvent.Request.LOGOUT}"
        topicClearCache = "${baseTopicSeller}${MqttEvent.Request.CLEAR_CACHE}"
        topicUpdate = "${baseTopicSeller}${MqttEvent.Request.UPDATE}"
        topicReInstall = "${baseTopicSeller}${MqttEvent.Request.REINSTALL}"

        basicTopics.run {
            add(topicCommonConnectionCheck)
            add(topicPaymentRequestConnect)
            // 관제
//            add(topicCycle)
//            add(topicStatus)
            // 제어
            add(topicShutDown)
            add(topicReboot)
            add(topicLogOut)
            add(topicClearCache)
            add(topicUpdate)
            add(topicReInstall)
        }
    }

    fun startCycleStatus(
        delay: Long,
        context: Context
    ) {
        log.e("delay: $delay")
        log.e("topicStatus: ${topicStatus}")
        if (delay == 0L) return

        cycleStatus = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
            while (true) { // 현재 코루틴이 활성 상태인지 확인
                // 여기에 반복할 작업을 넣습니다.
                delay(delay * 1000)
                val cpu = runBlocking {
                    CommonFunction.getCpuUsage(context)
                }
                log.e("cpu: $cpu")
                val reqMqttStatusData = ReqMqttStatusData(
//                    mqttAppStatus =
                    MqttAppStatus(
                        isActive = CommonFunction.isAppRunning(context,""),
                    ),
                    mqttDeviceStatus =
                    MqttDeviceStatus(
                        memoryUsage = CommonFunction.getBatteryPercentage(context),
                        storageAvailable = CommonFunction.getStorageUse(),
                        batteryLevel = CommonFunction.getBatteryPercentage(context),
                        wifiSignalStrength = CommonFunction.getWifiSignalStrengthInDbm(context),
                        cpuStatus = "${cpu}",
                    ),
                    mqttVersionInfo =
                    MqttVersionInfo(
                        appVersion = "test",
                        firmwareVersion = "testtest",
                    ),
                    mqttSellerIntegrationStatus =
                    MqttSellerIntegrationStatus(
                        isIntegratedWebsoket = null, // TODO : 웹소켓 연동 여부
                        isIntegratedMqtt = null, // TODO : mqtt 연동 여부
                    ),
                    mqttEventAt = System.currentTimeMillis().toString(),
                )
                val message =
                    PublishPacket.PublishPacketBuilder()
                        .withTopic(topicStatus)
                        .withQOS(QOS.AT_LEAST_ONCE)
                        .withPayload(Gson().toJson(
                            reqMqttStatusData
                        ).toByteArray())
                        .withRetain(false)
                        .build()
                mqtt5Client?.publish(message)?.whenComplete { publishComplete, throwable ->
                    if (throwable != null) {
                        log.e("Publish failed: ${throwable.message}")
                    } else {
                        log.e("MQTT-Publish: $topicStatus")
                        log.dMqtt("MQTT-Publish", "--> Publish: $topicStatus", String(Gson().toJson(reqMqttStatusData).toByteArray()), false)
                    }
                }
            }
        }
    }

    fun stopCycleStatus() {
        if (this::cycleStatus.isInitialized) {
            cycleStatus.cancel()
        }
    }

    fun setMqttCallBackConnect(
        context: Context,
        access: String,
        sid: String,
        csn: String,
        model: String,
        clientEndpoint: String = "a3khqefygzmvss-ats.iot.ap-northeast-2.amazonaws.com",
        reqMqtt: ReqMqtt? = null,
    ): Boolean {
        if (mqtt5Client != null) {
            return false
        }
        this.access = access
        this.clientEndpoint = clientEndpoint
        this.sid = sid
        this.csn = csn
        this.deviceModel = model
        try {
            initTopic()
            val customAuthConfig =
                AwsIotMqtt5ClientBuilder.MqttConnectCustomAuthConfig().apply {
                    authorizerName = AUTHORIZER
                    username = "${csn}_payherService"
                    password = access.toByteArray()
                }

            val builder =
                AwsIotMqtt5ClientBuilder.newDirectMqttBuilderWithCustomAuth(clientEndpoint, customAuthConfig).apply {
                    withConnectProperties(
                        ConnectPacket.ConnectPacketBuilder()
                            .withClientId("${csn}_payherService")
                            .withKeepAliveIntervalSeconds(60L)
                            .withWillDelayIntervalSeconds(0)
                            .withSessionExpiryIntervalSeconds(60L * 60 * 12)
                            .withWill(
                                PublishPacket.PublishPacketBuilder()
                                    .withTopic(topicCommonTerminalAlive)
                                    .withQOS(QOS.AT_LEAST_ONCE)
                                    .withPayload(
                                        Gson().toJson(
                                            ReqMqtt(
                                                deviceSerialnumber = csn,
                                                alive = false,
                                            ),
                                        ).toByteArray(),
                                    )
                                    .withRetain(true)
                                    .build(),
                            ),
                    )
                    withSessionBehavior(Mqtt5ClientOptions.ClientSessionBehavior.REJOIN_ALWAYS)
                    withMaxReconnectDelayMs(1000 * 10)
                    withPublishEvents { mqtt5Client, publishReturn ->
                        try {
                            val topic = publishReturn.publishPacket.topic ?: ""
                            val qos = publishReturn.publishPacket.qos ?: ""
                            val messageByte = publishReturn.publishPacket.payload ?: byteArrayOf()
                            val isRetained = publishReturn.publishPacket.retain ?: false
                            log.dMqtt(
                                "MQTT-Arrived",
                                "<-- Arrived: $qos $topic",
                                String(messageByte),
                                isRetained,
                            )
//                            CoroutineScope(Dispatchers.IO + coroutineExceptionHandler).launch {
//                                when (topic) {
//                                    topicCommonConnectionCheck -> {
//                                        connectionJob.cancel()
//                                        val messageData = String(messageByte)
//                                        val list =
//                                            if (messageData.isEmpty()) {
//                                                JsonArray()
//                                            } else {
//                                                Gson().fromJson(messageData, JsonArray::class.java)
//                                            }
//                                        if (isLogoutAction) {
//                                            // 로그아웃 상태
//                                            isLogoutAction = false
//                                            if (list.toString().contains(csn)) {
//                                                // 해당 디바이스 CSN이 검새되면 제거합니다.
//                                                val removeItem =
//                                                    Gson().toJsonTree(
//                                                        ReqMqtt(
//                                                            deviceModel = deviceModel,
//                                                            deviceSerialnumber = csn,
//                                                        ),
//                                                        ReqMqtt::class.java,
//                                                    )
//                                                list.remove(removeItem)
//                                            }
//                                            // 제거하여 최신화된 디바이스 정보를 다시 발행
//                                            if (mqtt5Client?.isConnected == false) return@launch
//                                            val message =
//                                                PublishPacket.PublishPacketBuilder()
//                                                    .withTopic(topic)
//                                                    .withQOS(QOS.AT_LEAST_ONCE)
//                                                    .withPayload(if (list.isEmpty) ByteArray(0) else Gson().toJson(list).toByteArray())
//                                                    .withRetain(true)
//                                                    .build()
//                                            MqttFactory.mqtt5Client?.publish(message)
//                                            clearMqtt()
//                                        } else {
//                                            // 로그인 상태
//                                            if (!list.toString().contains(csn)) {
//                                                // 지정 메시지에 내 csn이 없으면 추가합니다.
//                                                list.add(
//                                                    Gson().fromJson(
//                                                        Gson().toJson(
//                                                            ReqMqtt(deviceModel = deviceModel, deviceSerialnumber = MqttFactory.csn),
//                                                        ),
//                                                        JsonObject::class.java,
//                                                    ),
//                                                )
//                                                // 디바이스 추가하여 발행
//                                                if (mqtt5Client?.isConnected == false) return@launch
//
//                                                val message =
//                                                    PublishPacket.PublishPacketBuilder()
//                                                        .withTopic(topic)
//                                                        .withQOS(QOS.AT_LEAST_ONCE)
//                                                        .withPayload(Gson().toJson(list).toByteArray())
//                                                        .withRetain(true)
//                                                        .build()
//                                                MqttFactory.mqtt5Client?.publish(message)
//                                            }
//                                        }
//                                    }
//
//                                    topicPaymentRequestConnect -> {
//                                        val connectionJson =
//                                            Gson().fromJson(
//                                                Gson().fromJson(
//                                                    String(messageByte),
//                                                    JsonObject::class.java,
//                                                ),
//                                                JsonObject::class.java,
//                                            )
//                                        if (connectionJson != null && connectionJson.has("message")) {
//                                            (connectionJson.get("message") as JsonObject).get("identifier")?.let {
//                                                val identifier = it.asString
//                                                val copyOfTopics = topics.toSet()
//                                                copyOfTopics.forEach {
//                                                    log.d("MQTT-Unsubscribe: $it")
//                                                    val unsubAckPacket =
//                                                        UnsubscribePacket.UnsubscribePacketBuilder()
//                                                            .withSubscription(it)
//                                                            .build()
//                                                    mqtt5Client?.unsubscribe(unsubAckPacket)
//                                                }
//                                                topics.clear()
//                                                val topicCancel = "${baseTopicPayment}$identifier/${MqttEvent.Request.CANCEL}"
//                                                topics.run {
//                                                    add("${baseTopicPayment}$identifier/${MqttEvent.Request.CAPTURE_CARD}")
//                                                    add("${baseTopicPayment}$identifier/${MqttEvent.Request.REFUND_CARD}")
//                                                    add("${baseTopicPayment}$identifier/${MqttEvent.Request.CAPTURE_QR}")
//                                                    add("${baseTopicPayment}$identifier/${MqttEvent.Request.REFUND_QR}")
//                                                    add("${baseTopicPayment}$identifier/${MqttEvent.Request.CAPTURE_CASH}")
//                                                    add("${baseTopicPayment}$identifier/${MqttEvent.Request.REFUND_CASH}")
//                                                    add("${baseTopicPayment}$identifier/${MqttEvent.Request.CAPTURE_CASH_RECEIPT}")
//                                                    add("${baseTopicPayment}$identifier/${MqttEvent.Request.REFUND_CASH_RECEIPT}")
//                                                    add("${baseTopicPayment}$identifier/${MqttEvent.Request.PRINT}")
//                                                    add(topicCancel)
//                                                }
//                                                subscribeTopics(topics)
//                                                sendMqttMessage(
//                                                    topic = topicPaymentResultConnect,
//                                                    reqMqtt =
//                                                        ReqMqtt(
//                                                            deviceSerialnumber = csn,
//                                                        ),
//                                                    retained = true,
//                                                )
//                                            }
//                                        } else {
//                                            sendMqttMessage(
//                                                topic = topicPaymentResultConnect,
//                                                reqMqtt =
//                                                    ReqMqtt(
//                                                        deviceSerialnumber = csn,
//                                                    ),
//                                                retained = true,
//                                                init = true,
//                                            )
//                                        }
//                                    }
//
//                                    else ->{
//                                        _messageArrived.emit(Pair(topic, String(messageByte)))
//                                    }
//                                }
//                            }
                        } catch (e: Exception) {
                            log.e("exception: ${e.message}")
                        }
                    }
                }

            builder.withLifeCycleEvents(
                object : Mqtt5ClientOptions.LifecycleEvents {
                    override fun onAttemptingConnect(
                        mqtt5Client: Mqtt5Client?,
                        onAttemptingConnectReturn: OnAttemptingConnectReturn?,
                    ) {
                        log.e(
                            "onAttemptingConnect\n" +
                                "${mqtt5Client?.clientOptions?.connectOptions?.clientId}\n" +
                                "${mqtt5Client?.clientOptions?.connectOptions?.userProperties}\n" +
                                "${mqtt5Client?.clientOptions?.connectOptions?.will?.topic}\n" +
                                "${mqtt5Client?.clientOptions?.connectOptions?.username}\n" +
                                "${String(mqtt5Client?.clientOptions?.connectOptions?.password ?: ByteArray(0))}\n" +
                                "${mqtt5Client?.clientOptions?.connectOptions?.willDelayIntervalSeconds}\n" +
                                "${mqtt5Client?.clientOptions?.connectOptions?.requestProblemInformation}\n" +
                                "${mqtt5Client?.clientOptions?.connectOptions?.requestResponseInformation}\n" +
                                "${mqtt5Client?.clientOptions?.connectOptions?.sessionExpiryIntervalSeconds}\n" +
                                "${mqtt5Client?.resourceLogDescription}\n" +
                                "${mqtt5Client?.operationStatistics}",
                        )
                    }

                    override fun onConnectionSuccess(
                        mqtt5Client: Mqtt5Client?,
                        onConnectionSuccessReturn: OnConnectionSuccessReturn?,
                    ) {
                        log.e(
                            "mqtt connectComplete: \n" +
                                "sessionExpiryIntervalSeconds : ${onConnectionSuccessReturn?.connAckPacket?.sessionExpiryIntervalSeconds}" +
                                "\nserverKeepAliveSeconds : ${onConnectionSuccessReturn?.connAckPacket?.serverKeepAliveSeconds}" +
                                "\n${onConnectionSuccessReturn?.connAckPacket}",
                        )
                        startCycleStatus(
                            delay = 10,
                            context = context,
                        )

//                        subscribeTopics(basicTopics)
//                        sendMqttMessage(topic = topicConnectionStatus, reqMqtt = reqMqtt, retained = true)
//                        sendMqttMessage(topic = topicConnectionStatusCsn, reqMqtt = reqMqtt, retained = true)
//                        sendMqttMessage(
//                            topic = topicCommonTerminalAlive,
//                            reqMqtt =
//                                ReqMqtt(
//                                    deviceSerialnumber = csn,
//                                    alive = true,
//                                ),
//                            retained = true,
//                        )
//                        connectionJob.start()
                    }

                    override fun onConnectionFailure(
                        mqtt5Client: Mqtt5Client?,
                        onConnectionFailureReturn: OnConnectionFailureReturn?,
                    ) {
                        log.e(
                            "onConnectionFailure: ${onConnectionFailureReturn?.errorCode}\n${onConnectionFailureReturn?.connAckPacket?.reasonCode} :  ${onConnectionFailureReturn?.connAckPacket?.reasonString} :  ${onConnectionFailureReturn?.connAckPacket?.sessionPresent} \n ${onConnectionFailureReturn?.connAckPacket?.assignedClientIdentifier} \n ${onConnectionFailureReturn?.connAckPacket?.serverKeepAliveSeconds} \n ${onConnectionFailureReturn?.connAckPacket?.userProperties}",
                        )
                    }

                    override fun onDisconnection(
                        mqtt5Client: Mqtt5Client?,
                        onDisconnectionReturn: OnDisconnectionReturn?,
                    ) {
                        log.d("onDisconnection: ${onDisconnectionReturn?.disconnectPacket?.reasonString}")
//                        connectionJob.cancel()
                    }

                    override fun onStopped(
                        mqtt5Client: Mqtt5Client?,
                        onStoppedReturn: OnStoppedReturn?,
                    ) {
                        log.d("onStoppedReturn: $onStoppedReturn")
                    }
                },
            )
            mqtt5Client = builder.build()
            mqtt5Client?.start()
        } catch (e: Exception) {
            log.e("exception: ${e.message}")
        }
        return true
    }

    fun subscribeTopics(topics: MutableSet<String>) {
        try {
            val copyOfTopics = topics.toSet()
            copyOfTopics.forEach {
                val mqtt5qos = QOS.AT_LEAST_ONCE
                val subscribePacket =
                    SubscribePacket.SubscribePacketBuilder()
                        .withSubscription(it, mqtt5qos)
                        .build()
                mqtt5Client?.subscribe(subscribePacket)?.whenComplete { subAckPacket, throwable ->
                    if (throwable != null) {
                        // Handle subscription failure
                        log.e("Subscription failed: ${throwable.message}")
                    } else {
                        // Handle subscription success
                        log.d("MQTT-Subscribe: $it")
                    }
                }
            }
        } catch (e: Exception) {
            log.e("exception: ${e.message}")
        }
    }

    fun sendMqttMessage(
        topic: String,
        reqMqtt: ReqMqtt? = null,
        retained: Boolean? = false,
        init: Boolean? = false,
    ) {
        try {
            if (mqtt5Client?.isConnected == false) return
            if (reqMqtt?.sid?.isEmpty() == true) return
            val message =
                PublishPacket.PublishPacketBuilder()
                    .withTopic(topic)
                    .withQOS(QOS.AT_LEAST_ONCE)
                    .withPayload(if (init == true) ByteArray(0) else Gson().toJson(reqMqtt).toByteArray())
                    .withRetain(retained ?: false)
                    .build()
            mqtt5Client?.publish(message)?.whenComplete { publishComplete, throwable ->
                if (throwable != null) {
                    log.e("Publish failed: ${throwable.message}")
                } else {
                    log.d("MQTT-Publish: $topic")
                    log.dMqtt("MQTT-Publish", "--> Publish: $topic", String(Gson().toJson(reqMqtt).toByteArray()), retained ?: false)
                }
            }
        } catch (e: Exception) {
            log.e("exception: ${e.message}")
        }
    }

    fun clearMqtt() {
        try {
            val copyOfTopics = topics.toSet()
            copyOfTopics.forEach {
                log.d("MQTT-Unsubscribe: $it")
                val unsubAckPacket =
                    UnsubscribePacket.UnsubscribePacketBuilder()
                        .withSubscription(it)
                        .build()
                mqtt5Client?.unsubscribe(unsubAckPacket)
            }
            topics.clear()
            if (mqtt5Client?.isConnected == true) {
                sendMqttMessage(
                    topic = topicCommonTerminalAlive,
                    reqMqtt =
                        ReqMqtt(
                            deviceSerialnumber = csn,
                            alive = false,
                        ),
                    retained = true,
                )
            }
            if (mqtt5Client?.isConnected == true) mqtt5Client?.stop()
            mqtt5Client = null
        } catch (e: Exception) {
            log.e("exception: ${e.message}")
        }
    }

    fun logOutMqtt() {
        if (mqtt5Client?.isConnected == true) {
            isLogoutAction = true
            val mqtt5qos = QOS.AT_LEAST_ONCE
            val subscribePacket =
                SubscribePacket.SubscribePacketBuilder()
                    .withSubscription(topicCommonConnectionCheck, mqtt5qos)
                    .build()
            mqtt5Client?.subscribe(subscribePacket)
        }
    }

    fun unavailable(
        eventName: String,
        reqTopic: String,
    ) {
        val unavailableEvent =
            when (eventName) {
                MqttEvent.Request.CAPTURE_CARD -> MqttEvent.Result.CAPTURE_CARD
                MqttEvent.Request.REFUND_CARD -> MqttEvent.Result.REFUND_CARD
                MqttEvent.Request.CAPTURE_QR -> MqttEvent.Result.CAPTURE_QR
                MqttEvent.Request.REFUND_QR -> MqttEvent.Result.REFUND_QR
                MqttEvent.Request.CAPTURE_CASH -> MqttEvent.Result.CAPTURE_CASH
                MqttEvent.Request.REFUND_CASH -> MqttEvent.Result.REFUND_CASH
                MqttEvent.Request.CAPTURE_CASH_RECEIPT -> MqttEvent.Result.CAPTURE_CASH_RECEIPT
                MqttEvent.Request.REFUND_CASH_RECEIPT -> MqttEvent.Result.REFUND_CASH_RECEIPT
                MqttEvent.Request.CANCEL -> MqttEvent.Result.CANCEL
                else -> ""
            }
        val topic = "${processTopic(reqTopic)}$unavailableEvent"
        val reqMqtt =
            ReqMqtt(
                eventName = unavailableEvent,
                message =
                    ResMqttMessage(
                        pid = "",
                        data =
                            ResMqttData(
                                reason = "",
                                reasonCode = "",
                            ),
                        result = "UNAVAILABLE",
                    ),
            )
        sendMqttMessage(
            topic = topic,
            reqMqtt = reqMqtt,
            retained = false,
        )
    }

    // 카드 승인 결과
    fun cardCaptureResult(
        isSuccess: Boolean,
        pid: String,
        reqTopic: String,
    ) {
        responseResult(
            MqttEventResult = MqttEvent.Result.CAPTURE_CARD,
            pid = pid,
            reason = "결제 생성",
            isSuccess = isSuccess,
            reqTopic = reqTopic,
        )
    }

    // 카드 환불 결과
    fun cardRefundResult(
        isSuccess: Boolean,
        pid: String,
        reqTopic: String,
    ) {
        responseResult(
            MqttEvent.Result.REFUND_CARD,
            pid = pid,
            reason = "결제 취소",
            isSuccess = isSuccess,
            reqTopic = reqTopic,
        )
    }

    // 카드 승인 결과
    fun qrCaptureResult(
        isSuccess: Boolean,
        pid: String,
        reqTopic: String,
    ) {
        responseResult(
            MqttEventResult = MqttEvent.Result.CAPTURE_QR,
            pid = pid,
            reason = "결제 생성",
            isSuccess = isSuccess,
            reqTopic = reqTopic,
        )
    }

    // 카드 환불 결과
    fun qrRefundResult(
        isSuccess: Boolean,
        pid: String,
        reqTopic: String,
    ) {
        responseResult(
            MqttEventResult = MqttEvent.Result.REFUND_QR,
            pid = pid,
            reason = "결제 취소",
            isSuccess = isSuccess,
            reqTopic = reqTopic,
        )
    }

    // 현금 승인 결과
    fun cashCaptureResult(
        isSuccess: Boolean,
        pid: String,
        reqTopic: String,
    ) {
        responseResult(
            MqttEventResult = MqttEvent.Result.CAPTURE_CASH,
            pid = pid,
            reason = "결제 생성",
            isSuccess = isSuccess,
            reqTopic = reqTopic,
        )
    }

    // 현금 환불 결과
    fun cashRefundResult(
        isSuccess: Boolean,
        pid: String,
        reqTopic: String,
    ) {
        responseResult(
            MqttEventResult = MqttEvent.Result.REFUND_CASH,
            pid = pid,
            reason = "결제 취소",
            isSuccess = isSuccess,
            reqTopic = reqTopic,
        )
    }

    // 현금영수증 발행 결과
    fun cashReceiptPublishResult(
        isSuccess: Boolean,
        pid: String,
        reqTopic: String,
    ) {
        responseResult(
            MqttEventResult = MqttEvent.Result.CAPTURE_CASH_RECEIPT,
            pid = pid,
            reason = "결제 생성",
            isSuccess = isSuccess,
            reqTopic = reqTopic,
        )
    }

    // 현금영수증 발행취소 결과
    fun cashReceiptCancelResult(
        isSuccess: Boolean,
        pid: String,
        reqTopic: String,
    ) {
        responseResult(
            MqttEventResult = MqttEvent.Result.REFUND_CASH_RECEIPT,
            pid = pid,
            reason = "결제 취소",
            isSuccess = isSuccess,
            reqTopic = reqTopic,
        )
    }

    fun responseResult(
        MqttEventResult: String? = null,
        pid: String? = null,
        reason: String? = null,
        reqMqtt: ReqMqtt? = null,
        isSuccess: Boolean? = null,
        reqTopic: String,
        retained: Boolean? = false,
        init: Boolean? = false,
    ) {
        val topic = "${processTopic(reqTopic)}$MqttEventResult"
        val mqtt =
            reqMqtt
                ?: ReqMqtt(
                    eventName = MqttEventResult,
                    message =
                        ResMqttMessage(
                            pid = pid,
                            data =
                                ResMqttData(
                                    reason = reason,
                                ),
                            result = if (isSuccess == true) "SUCCESS" else "FAILURE",
                        ),
                )

        sendMqttMessage(
            topic = topic,
            reqMqtt = mqtt,
            retained = retained,
            init = init,
        )
    }

    fun processTopic(input: String): String {
        // Split the input string by "/"
        val parts = input.split("/")

        // Remove the last element (action part)
        val filteredParts = parts.dropLast(1)

        // Join the remaining parts back together, separated by "/", and add a trailing "/"
        return filteredParts.joinToString(separator = "/", postfix = "/")
    }
}
