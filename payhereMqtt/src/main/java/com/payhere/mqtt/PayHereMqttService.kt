package com.payhere.mqtt

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Timer

@RequiresApi(Build.VERSION_CODES.N)
class PayHereMqttService : Service() {
    companion object {
        const val NOTICHANNEL_SERVICE_ID = "payhere_mqtt_service"
        const val NOTICHANNEL_SERVICE_NAME = "payhere_mqtt"
        const val PAYHEREMQTTSERVICEPREFS = "PayHereMqttServicePrefs"
        var appIdentifier: String = ""
        var isDebug: Boolean = false
        var access: String = ""
        var sid: String = ""
        var customerSN: String = ""
        var paxModel: String = ""
        var paxCsn: String = ""
        private var gson = Gson()
        private val coroutineExceptionHandler =
            CoroutineExceptionHandler { _, exception ->
//            eventLogUseCase.sendEventLog(
//                eventCategory = EventCategory.ERROR,
//                eventLog =
//                    mutableMapOf(
//                        "event" to "mqtt error",
//                        "message" to "${exception.message}",
//                    ),
//            )
            }
        val scope = CoroutineScope(Dispatchers.IO + coroutineExceptionHandler)

        fun isServiceRunning(
            serviceClass: Class<*>,
            context: Context,
        ): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val processes = manager.runningAppProcesses
            for (process in processes) {
                if (process.processName == serviceClass.name) {
                    return true
                }
            }
            return false
        }

        fun startService(
            ctx: Context,
            access: String,
            appIdentifier: String,
            sid: String,
            customerSN: String,
            paxModel: String,
            paxCsn: String,
            isDebug: Boolean,
        ) {

            val sharedPreferences: SharedPreferences = ctx.getSharedPreferences(PAYHEREMQTTSERVICEPREFS, Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("access", access)
            editor.putString("appIdentifier", appIdentifier)
            editor.putString("sid", sid)
            editor.putString("customerSN", customerSN)
            editor.putString("paxModel", paxModel)
            editor.putString("paxCsn", paxCsn)
            editor.putBoolean("isDebug", isDebug)
            editor.apply()

            PayHereMqttService.access = access
            PayHereMqttService.appIdentifier = appIdentifier
            PayHereMqttService.sid = sid
            PayHereMqttService.customerSN = customerSN
            PayHereMqttService.paxModel = paxModel
            PayHereMqttService.paxCsn = paxCsn
            PayHereMqttService.isDebug = isDebug
            if (!isServiceRunning(PayHereMqttService::class.java, ctx)) {
                val service = Intent(ctx, PayHereMqttService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ctx.startForegroundService(service)
                } else {
                    ctx.startService(service)
                }
            }
        }

        fun restartService(ctx: Context) {
            val service = Intent(ctx, PayHereMqttService::class.java)
            ctx.stopService(service)
            if (Build.VERSION.SDK_INT >= 26) {
                ctx.startForegroundService(service)
            } else {
                ctx.startService(service)
            }
        }

        fun stopService(ctx: Context) {
            //        scope.cancel()
//        PayhereMqttFactory.clearMqtt()
            val service = Intent(ctx, PayHereMqttService::class.java)
            ctx.stopService(service)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log.e("onStartCommand")
        initService(
            access = access,
            sid = sid,
            customerSN = customerSN,
            paxModel = paxModel,
            paxCsn = paxCsn,
            isDebug = isDebug,
        )
//        return super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        PayhereMqttFactory.clearMqtt()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        log.e("bind service")
        TODO("Not yet implemented")
    }

    val timer = Timer()

    @SuppressLint("HardwareIds")
    fun getDeviceId(context: Context): String = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    private fun String.toHex(): String =
        this.toCharArray().joinToString(separator = "") {
            it.code.toString(16).padStart(2, '0')
        }

    private fun initService(
        access: String,
        sid: String,
        customerSN: String,
        paxModel: String,
        paxCsn: String,
        isDebug: Boolean,
    ) {
        val deviceIdLong = getDeviceId(this).hashCode()
        startForeground(deviceIdLong, createNotification(this))
        initRxMqtt(
            access = access,
            sid = sid,
            customerSN = customerSN,
            paxModel = paxModel,
            paxCsn = paxCsn,
            isDebug = isDebug,
        )
    }

    private fun initRxMqtt(
        access: String,
        sid: String,
        customerSN: String,
        paxModel: String,
        paxCsn: String,
        isDebug: Boolean,
    ) {
        log.e("sid: $sid")
        var finalAccess = access
        var finalSid = sid
        var finalCustomerSN = customerSN
        var finalPaxModel = paxModel
        var finalPaxCsn = paxCsn
        var finalIsDebug = isDebug
        if (finalSid.isEmpty()){
            val sharedPreferences: SharedPreferences = getSharedPreferences(PAYHEREMQTTSERVICEPREFS, Context.MODE_PRIVATE)
            finalSid = sharedPreferences.getString("sid", "")?:""
            finalAccess = sharedPreferences.getString("access", "")?:""
            finalCustomerSN = sharedPreferences.getString("customerSN", "")?:""
            finalPaxModel = sharedPreferences.getString("paxModel", "")?:""
            finalPaxCsn = sharedPreferences.getString("paxCsn", "")?:""
            finalIsDebug = sharedPreferences.getBoolean("isDebug", false)
            if (finalAccess.isEmpty()) {
                return
            }
            if (finalSid.isEmpty()) {
                return
            }
        }
//        val mqttMessage =
//            ReqMqtt(
//                eventAt = System.currentTimeMillis().toString(),
//                eventFrom = customerSN ?: "",
//                sid = sid,
//                privateIp = getLocalIPAddress() ?: "",
//                deviceModel = paxModel ?: "",
//                deviceSerialnumber = paxCsn ?: "",
//                terminalClientExternalKey = preferenceLogin.getTerminalClientExternalKey() ?: "",
//                extras = it,
//                wifiSignalStrength = CommonFunction.getWifiSignalStrength(this).toString(),
//                alive = true,
//            )

        if (PayhereMqttFactory.setMqttCallBackConnect(
                context = this@PayHereMqttService,
                access = finalAccess,
                sid = finalSid,
//                reqMqtt = mqttMessage,
                csn = finalPaxCsn,
                model = finalPaxModel ?: "",
                clientEndpoint = if (finalIsDebug) "a3khqefygzmvss-ats.iot.ap-northeast-2.amazonaws.com" else "a39oosdvor8dzt-ats.iot.ap-northeast-2.amazonaws.com",
            )
        ) {
            scope.launch {
                PayhereMqttFactory.messageArrived.collect { (topic, data) ->
                    if (data.isEmpty()) return@collect
                    val baseRequest = gson.fromJson(data, MqttBaseRequest::class.java)
                    if (baseRequest.eventName.isNullOrEmpty()) return@collect
                    val eventName = baseRequest.eventName.toString()
                    if (!topic.contains(eventName)) return@collect

                    when (eventName) {
//                        else -> {
//                            val requestPaymentDTO: RequestPaymentDTO =
//                                when (eventName) {
//                                    MqttEvent.Request.CAPTURE_CARD -> parseDto<RequestCaptureDTO>(data)
//                                    MqttEvent.Request.REFUND_CARD -> parseDto<RequestRefundDTO>(data)
//                                    MqttEvent.Request.CAPTURE_QR -> parseDto<RequestCaptureQrDTO>(data)
//                                    MqttEvent.Request.REFUND_QR -> parseDto<RequestRefundQrDTO>(data)
//                                    MqttEvent.Request.CAPTURE_CASH -> parseDto<RequestCaptureCashDTO>(data)
//                                    MqttEvent.Request.REFUND_CASH -> parseDto<RequestRefundCashDTO>(data)
//                                    MqttEvent.Request.CAPTURE_CASH_RECEIPT ->
//                                        parseDto<RequestCaptureReceiptDTO>(
//                                            data,
//                                        )
//
//                                    MqttEvent.Request.REFUND_CASH_RECEIPT ->
//                                        parseDto<RequestRefundReceiptDTO>(
//                                            data,
//                                        )
//
//                                    else -> return@collect
//                                }
//
//                            requestPaymentDTO.setResponseIsApi(topic.contains("payments"))
//                            requestPaymentDTO.reqTopic = topic
//                            triggerExternalPaymentDialogMqtt.onNext(requestPaymentDTO)
//                        }
                    }
                }
            }
        }
    }

    private fun createNotification(ctx: Context): Notification {
        log.e("application is running: $appIdentifier")
        val notificationIntent = Intent("android.intent.action.MAIN")
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent =
            PendingIntent.getActivity(
                ctx,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT,
            )
        return if (Build.VERSION.SDK_INT >= 26) {
            val channelID = NOTICHANNEL_SERVICE_ID
            val channel =
                NotificationChannel(
                    channelID,
                    NOTICHANNEL_SERVICE_NAME,
                    NotificationManager.IMPORTANCE_LOW,
                )

            channel.enableVibration(false)
            channel.enableLights(false)
            channel.vibrationPattern = longArrayOf(0) // 진동 무음
            (ctx.getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
            NotificationCompat.Builder(ctx, channelID)
        } else {
            NotificationCompat.Builder(ctx)
        }.setContentText("터미널 실행")
            .setSmallIcon(R.drawable.payhere_logo_small)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
