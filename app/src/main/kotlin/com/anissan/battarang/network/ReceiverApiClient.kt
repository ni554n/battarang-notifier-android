package com.anissan.battarang.network

import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import com.anissan.battarang.BuildConfig
import com.anissan.battarang.data.LocalKvStore
import com.anissan.battarang.utils.logE
import com.anissan.battarang.utils.logI
import com.anissan.battarang.utils.logV
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MessageType {
  TEST, PAIRED, LOW, FULL
}

enum class SupportedService(val serviceName: String) {
  FCM("Receiver"), TG("Telegram")
}

class ReceiverApiClient(
  private val powerManager: PowerManager,
  private val okHttpClient: OkHttpClient,
  private val localKvStore: LocalKvStore,
) : Callback {
  private var _onResponseResult: ((String?) -> Unit)? = null

  fun sendNotification(
    messageType: MessageType,
    batteryLevel: Int? = null,
    onResponseResult: ((responseBody: String?) -> Unit)? = null,
  ) {
    _onResponseResult = onResponseResult

    val url: String = BuildConfig.RECEIVER_API_URL.toHttpUrl().newBuilder().apply {
      mapOf(
        "pairedService" to localKvStore.pairedServiceTag,
        "messageType" to messageType.name,
        "receiverToken" to localKvStore.receiverToken,
        "deviceName" to localKvStore.deviceName,
        "triggeredAt" to SimpleDateFormat("hh:mm a (EEEE)", Locale.getDefault()).format(Date()),
        "batteryLevel" to "$batteryLevel",
        "lastTgMessageId" to localKvStore.lastTelegramMessageId,
      ).forEach { (parameter: String, value: String?) ->
        if (value.isNullOrBlank().not()) addQueryParameter(parameter, value)
      }
    }.build().toString()

    val wakeLock: PowerManager.WakeLock = powerManager.run {
      newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "${javaClass.name}::notify").apply {
        acquire(10 * 1000L)
        logI { "Secured a partial wakelock for up to 10 seconds." }
      }
    }

    try {
      okHttpClient.newCall(Request.Builder().url(url).get().build())
        .enqueue(responseCallback = this)
    } finally {
      wakeLock.release()
      logI { "Wakelock released." }
    }
  }

  override fun onFailure(call: Call, e: IOException) {
    logE(e)

    if (_onResponseResult == null) return

    Handler(Looper.getMainLooper()).post {
      _onResponseResult?.invoke(null)
    }
  }

  override fun onResponse(call: Call, response: Response) {
    response.use {
      logV { "/api response: $response" }

      localKvStore.lastTelegramMessageId = response.headers["X-Tg-Message-Id"]

      if (_onResponseResult == null) return@use

      // Success or Error message should be sent from the server as a response body text.
      val bodyText = try {
        response.body.string()
      } catch (e: Exception) {
        logE(e)

        "Server sent an invalid response body."
      }

      Handler(Looper.getMainLooper()).post {
        _onResponseResult?.invoke(bodyText)
      }
    }
  }
}
