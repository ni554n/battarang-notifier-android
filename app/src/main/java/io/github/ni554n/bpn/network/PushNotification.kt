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
  private val okHttpClient: OkHttpClient,
) {
  private val jsonMediaType: MediaType = "application/json; charset=utf-8".toMediaType()

  fun notifyAsync(token: String, title: String, body: String, onSuccess: () -> Unit = {}) {
    val postBody: String = """
            |{
            |  "token": "$token",
            |  "title": "$title",
            |  "body": "$body"
            |}
            |""".trimMargin()

    logcat { "Sending an API request with $postBody" }

    val apiRequest: Request = Request.Builder()
      .url("https://qnibyq.deta.dev/notify")
      .post(postBody.toRequestBody(jsonMediaType))
      .build()

    // Invalidate token on 400
    // https://firebase.google.com/docs/cloud-messaging/manage-tokens
    okHttpClient.newCall(apiRequest).async { response: Response ->
      response.use {
        if (response.isSuccessful.not()) {
          logcat(LogPriority.ERROR) { "API request failed with $response.code: $response.message" }
        }

        logcat { "Successful API response: $response" }
        onSuccess()
      }
    }
  }

  /**
   * Extension function for providing cleaner Kotlin styled callbacks over OkHttp's Java API.
   */
  private fun Call.async(
    success: (response: Response) -> Unit,
  ) {
    // Securing a partial wakelock for up to 10 seconds.
    val wakeLock: PowerManager.WakeLock = powerManager.run {
      newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "${javaClass.name}::notify")
        .apply { acquire(10 * 1000L) } // 10 seconds
    }

    enqueue(responseCallback = object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        logcat(LogPriority.ERROR) { e.asLog() }

        wakeLock.release()
      }

      override fun onResponse(call: Call, response: Response) {
        success(response)

        wakeLock.release()
      }
    })
  }
}

