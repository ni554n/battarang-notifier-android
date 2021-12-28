package io.github.ni554n.bpn.network

import android.os.PowerManager
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class PushNotification(
  private val powerManager: PowerManager,
  private val client: OkHttpClient,
) {
  private val jsonMediaType: MediaType = "application/json; charset=utf-8".toMediaType()

  fun notify(token: String, title: String, body: String) {
    // Securing Wakelock for 10 seconds.
    val wakeLock: PowerManager.WakeLock = powerManager.run {
      newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "${javaClass.name}::notify")
        .apply { acquire(10 * 1000L) } // 10 seconds
    }

    val postBody: String = """
            |{
            |  "token": "$token",
            |  "title": "$title",
            |  "body": "$body"
            |}
            |""".trimMargin()

    logcat { postBody }

    val request: Request = Request.Builder()
      .url("https://qnibyq.deta.dev/notify")
      .post(postBody.toRequestBody(jsonMediaType))
      .build()

    // Invalidate token on 400
    // https://firebase.google.com/docs/cloud-messaging/manage-tokens
    client.newCall(request).enqueue(
      always = { wakeLock.release() },
    ) { response: Response ->
      response.use {
        if (response.isSuccessful.not()) {
          logError(Exception("${response.code}: ${response.message}"))
        }

        logcat(javaClass.name) { response.toString() }
      }
    }
  }
}

/**
 * Extension function for providing cleaner Kotlin styled callbacks over OkHttp's Java API.
 */
fun Call.enqueue(
  always: () -> Unit = {},
  failure: (e: IOException) -> Unit = ::logError,
  success: (response: Response) -> Unit,
) {
  enqueue(responseCallback = object : Callback {
    override fun onFailure(call: Call, e: IOException) {
      failure(e)

      always()
    }

    override fun onResponse(call: Call, response: Response) {
      success(response)

      always()
    }
  })
}

fun logError(error: Exception) {
  logcat("OKHttp: ${::logError::javaClass.name}", LogPriority.ERROR) {
    error.asLog()
  }
}
