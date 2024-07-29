package com.payhere.mqtt

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.view.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.SocketException
import kotlin.system.exitProcess

object CommonFunction {
    fun getLocalIPAddress(): String? {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val enumIpAddress = en.nextElement().inetAddresses
                while (enumIpAddress.hasMoreElements()) {
                    val inetAddress = enumIpAddress.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        inetAddress.hostAddress?.let { return it }
                    }
                }
            }
        } catch (ex: SocketException) {
            ex.printStackTrace()
        }

        return null
    }

    fun getStorageUse(): Int {
        val freeSpace = Runtime.getRuntime().freeMemory()
        val totalSpace = Runtime.getRuntime().totalMemory()
        val usedSpace = totalSpace - freeSpace
        log.d("Storage", "Used Storage: $usedSpace")
        log.d("Storage", "Free Storage: $freeSpace")
        log.d("Storage", "Total Storage: $totalSpace")
        return freeSpace.toInt()
    }

    fun getBatteryPercentage(context: Context): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun getWifiSignalStrengthInDbm(context: Context): Int {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo = wifiManager.connectionInfo
        return wifiInfo.rssi // Returns the signal strength in dBm
    }

    fun systemExit() {
        exitProcess(0)
    }

    fun clearCache(context: Context) {
        try {
            val dir: File = context.cacheDir
            deleteDir(dir)
            systemExit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearStorage(context: Context) {
        try {
            // 캐시 디렉토리 삭제
            val cacheDir: File = context.cacheDir
            deleteDir(cacheDir)

            // 외부 캐시 디렉토리 삭제 (외부 저장소에 있는 앱 전용 데이터)
            val externalCacheDir: File? = context.externalCacheDir
            if (externalCacheDir != null) {
                deleteDir(externalCacheDir)
            }

            // 앱이 사용할 수 있는 외부 저장소의 파일 디렉토리 삭제
            val filesDir: File? = context.getExternalFilesDir(null)
            if (filesDir != null) {
                deleteDir(filesDir)
            }
            systemExit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children: Array<String> = dir.list()
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
            return dir.delete()
        } else if (dir != null && dir.isFile) {
            return dir.delete()
        } else {
            return false
        }
    }

    fun clearCacheWithFlow(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dir: File = context.cacheDir
                deleteDirFlow(dir).collect { success ->
                    if (success) {
                        withContext(Dispatchers.Main) {
                            exitProcess(0)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteDirFlow(dir: File?): Flow<Boolean> =
        flow {
            emit(dir != null && dir.deleteRecursively())
        }

    fun clearStorageWithFlow(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Clear SharedPreferences
                PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .edit()
                    .clear()
                    .apply()

                // Delete cache directory
                val cacheDir: File = context.cacheDir
                deleteDirFlow(cacheDir).collect { success ->
                    if (!success) log.e("Failed to delete cache directory")
                }

                // Delete external cache directory
                val externalCacheDir: File? = context.externalCacheDir
                externalCacheDir?.let {
                    deleteDirFlow(it).collect { success ->
                        if (!success) log.e("Failed to delete external cache directory")
                    }
                }

                // Delete files directory
                val filesDir: File? = context.getExternalFilesDir(null)
                filesDir?.let {
                    deleteDirFlow(it).collect { success ->
                        if (!success) log.e("Failed to delete files directory")
                    }
                }
                delay(2000)
                withContext(Dispatchers.Main) {
                    exitProcess(0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun keyCodeToNumber(keyCode: Int): String {
        return when (keyCode) {
            KeyEvent.KEYCODE_0 -> return "0"
            KeyEvent.KEYCODE_1 -> return "1"
            KeyEvent.KEYCODE_2 -> return "2"
            KeyEvent.KEYCODE_3 -> return "3"
            KeyEvent.KEYCODE_4 -> return "4"
            KeyEvent.KEYCODE_5 -> return "5"
            KeyEvent.KEYCODE_6 -> return "6"
            KeyEvent.KEYCODE_7 -> return "7"
            KeyEvent.KEYCODE_8 -> return "8"
            KeyEvent.KEYCODE_9 -> return "9"
            KeyEvent.KEYCODE_F5 -> return "00"
            KeyEvent.KEYCODE_F6 -> return "000"
            KeyEvent.KEYCODE_DEL -> return "DEL"
            else -> return ""
        }
    }

    fun initCpuUsageMonitor(context: Context) {
        activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 1000L // 1 second
    private var activityManager: ActivityManager? = null

    private val updateRunnable =
        object : Runnable {
            override fun run() {
                logCpuUsage()
                handler.postDelayed(this, updateInterval)
            }
        }

    fun startMonitoring() {
        handler.post(updateRunnable)
    }

    fun stopMonitoring() {
        handler.removeCallbacks(updateRunnable)
    }

    fun logCpuUsage(): String {
        val pid = android.os.Process.myPid()
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        val cpuUsage = Debug.threadCpuTimeNanos() / 1000000 // Convert to milliseconds
        return cpuUsage.toString()
    }

    fun getCpuUsage(context: Context): Float {
        val pid = android.os.Process.myPid()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val cpuTime1 = Debug.threadCpuTimeNanos() / 1000000 // Convert to milliseconds
        val uptime1 = System.currentTimeMillis()
        log.e("CPU Time 1: $cpuTime1")
        log.e("Uptime 1: $uptime1")

        // Sleep for a short interval to measure CPU usage over time
        Thread.sleep(360)

        val cpuTime2 = Debug.threadCpuTimeNanos() / 1000000 // Convert to milliseconds
        val uptime2 = System.currentTimeMillis()

        log.e("CPU Time 2: $cpuTime2")
        log.e("Uptime 2: $uptime2")

        val cpuDelta = cpuTime2 - cpuTime1
        val uptimeDelta = uptime2 - uptime1

        return if (uptimeDelta > 0) {
            (cpuDelta.toFloat() / uptimeDelta.toFloat()) * 100
        } else {
            0f
        }
    }

    private fun getTotalCpuTime(): Long {
        try {
            RandomAccessFile("/proc/stat", "r").use { reader ->
                val load = reader.readLine().split(" ")
                return load[2].toLong() + load[3].toLong() + load[4].toLong() + load[5].toLong() +
                    load[6].toLong() + load[7].toLong() + load[8].toLong()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    private fun getProcessCpuTime(pid: Int): Long {
        try {
            RandomAccessFile("/proc/$pid/stat", "r").use { reader ->
                val load = reader.readLine().split(" ")
                return load[13].toLong() + load[14].toLong() + load[15].toLong() + load[16].toLong()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    fun isAppRunning(context: Context, packageName: String): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningAppProcesses = activityManager.runningAppProcesses
        for (process in runningAppProcesses) {
            if (process.processName == packageName) {
                return true
            }
        }
        return false
    }
}