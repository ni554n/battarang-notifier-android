package com.anissan.bpn.data

import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import com.anissan.bpn.BuildConfig
import com.anissan.bpn.utils.logE
import com.anissan.bpn.utils.logI
import com.anissan.bpn.utils.logV
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
  private lateinit var _onFail: () -> Unit
  private lateinit var _onSuccess: () -> Unit
  private lateinit var _finally: () -> Unit

  fun sendNotification(
    messageType: MessageType,
    batteryLevel: Int? = null,
    onFail: () -> Unit = {},
    onSuccess: () -> Unit = {},
    finally: () -> Unit = {},
  ) {
    _onFail = onFail
    _onSuccess = onSuccess
    _finally = finally

    val url: String = BuildConfig.RECEIVER_API_URL.toHttpUrl().newBuilder().apply {
      mapOf(
        "pairedService" to localKvStore.pairedService,
        "messageType" to messageType.name,
        "receiverToken" to localKvStore.receiverToken,
        "deviceName" to localKvStore.deviceName,
        "triggeredAt" to SimpleDateFormat("hh:mm a (EEEE)", Locale.getDefault()).format(Date()),
        "batteryLevel" to "$batteryLevel",
        "lastTgMessageId" to localKvStore.lastTelegramMessageId
      ).forEach { (parameter: String, value: String?) ->
        if (value.isNullOrBlank().not()) addQueryParameter(parameter, value)
      }
    }.build().toString()

    logV { "Constructed receiver API URL: $url" }

    val wakeLock: PowerManager.WakeLock = powerManager.run {
      newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "${javaClass.name}::notify").apply {
        acquire(10 * 1000L)
        logI { "Secured a partial wakelock of up to 10 seconds." }
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
    Handler(Looper.getMainLooper()).post {
      _onFail()
      _finally()
    }
  }

  override fun onResponse(call: Call, response: Response) {
    response.use {
      logV { "/api response: $response" }

      if (response.isSuccessful.not()) onFailure(call, IOException("Request failed with $response"))

      localKvStore.lastTelegramMessageId = response.headers["X-Tg-Message-Id"]

      Handler(Looper.getMainLooper()).post {
        _onSuccess()
        _finally()
      }
    }
  }
}
