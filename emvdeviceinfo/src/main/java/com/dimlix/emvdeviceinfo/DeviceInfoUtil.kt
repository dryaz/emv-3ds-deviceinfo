package com.dimlix.emvdeviceinfo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.util.Locale


private val KEYS = listOf<String>(
    "C001", // Device Type
    "C002", // Device Name
    "C003", // Device OS
    "C004", // OS Version
    "C005", // Locale
    "C006", // Time Zone
    "C008", // Screen Resolution
    "C009", // Device Name
    "C010", // Ip Address
)

class DeviceInfoUtil {
    companion object {
        @JvmStatic
        fun getDeviceInfo(context: Context): String {
            val jsonData = fetchJsonData(context)
            val deviceData = jsonData.deviceData
            val dpna = jsonData.dpna
            val sw = jsonData.sw

            return buildJsonObject {
                // Add device version
                put("DV", jsonData.dataVersion)

                // Add device data if any
                if (deviceData.isNotEmpty()) {
                    putJsonObject("DD") {
                        deviceData.forEach {
                            put(it.key, it.value)
                        }
                    }
                }

                // Add not available device params if any
                if (dpna.isNotEmpty()) {
                    putJsonObject("DPNA") {
                        dpna.forEach {
                            put(it.key, it.value)
                        }
                    }
                }

                // Add sercutiry warnings if any
                if (sw.isNotEmpty()) {
                    putJsonObject("SW") {
                        sw.forEach {
                            put(it.key, it.value)
                        }
                    }
                }
            }.toString()
        }

        private fun fetchJsonData(context: Context): JsonData {
            val dataVersion = "1.0.0"
            val deviceData = mutableMapOf<String, String>()
            val dpna = mutableMapOf<String, String>()
            val sw = mutableMapOf<String, String>()

            KEYS.forEach {
                val value = getKeyValue(context, it)
                when (value) {
                    is FetchResult.Error -> dpna[it] = value.errorCode
                    is FetchResult.Success -> deviceData[it] = value.data
                }
            }

            return JsonData(dataVersion, deviceData, dpna, sw)
        }

        private fun getKeyValue(context: Context, key: String): FetchResult {
            return when (key) {
                "C001" -> FetchResult.Success("Android")
                "C002" -> FetchResult.Success("${Build.MANUFACTURER}||${Build.MODEL}")
                "C003" -> FetchResult.Success("Android ${getVersionName()} ${Build.VERSION.RELEASE} API ${Build.VERSION.SDK_INT}")
                "C004" -> if (Build.VERSION.RELEASE.isNullOrBlank()) FetchResult.Error(Error.Empty.code) else FetchResult.Success(
                    Build.VERSION.RELEASE
                )

                "C005" -> FetchResult.Success("${Locale.getDefault().language}-${Locale.getDefault().country}}")
                "C010" -> getIpAddress(context)
                else -> FetchResult.Error(Error.Empty.code)
                //else -> throw IllegalArgumentException("Unknown key: $key")
            }
        }

        private fun getIpAddress(context: Context): FetchResult {
            if (!isPermissionGranted(
                    context,
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_NETWORK_STATE
                )
            ) {
                return FetchResult.Error(Error.PermissionRequired.code)
            }

            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            if (connectivityManager is ConnectivityManager) {
                var link = connectivityManager.getLinkProperties(connectivityManager.activeNetwork)
                link?.linkAddresses?.forEach {
                    Log.d("Ip Address", it.address?.hostAddress ?: "")
                }
                val ip = link
                    ?.linkAddresses
                    ?.first { it.address?.hostAddress?.contains('.') == true }
                    ?.address
                    ?.hostAddress

                if (ip != null) {
                    return FetchResult.Success(ip)
                } else {
                    return FetchResult.Error(Error.Empty.code)
                }
            }

            return FetchResult.Error(Error.Restriction.code)
        }

        private fun isPermissionGranted(
            context: Context,
            vararg permissions: String,
        ): Boolean = permissions.none {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }


        private fun getVersionName(): String {
            val fields = Build.VERSION_CODES::class.java.fields
            return fields.firstOrNull { it.getInt(Build.VERSION_CODES::class) == Build.VERSION.SDK_INT }?.name
                ?: ""
        }
    }
}

enum class Error(val code: String) {
    Restriction("RE01"),
    Deprecated("RE02"),
    PermissionRequired("RE03"),
    Empty("RE04"),
}

private data class JsonData(
    val dataVersion: String,
    val deviceData: Map<String, String>,
    val dpna: Map<String, String>, // Device params not available
    val sw: Map<String, String>, // Security warnings
)

private sealed class FetchResult {
    data class Success(val data: String) : FetchResult()
    data class Error(val errorCode: String) : FetchResult()
}