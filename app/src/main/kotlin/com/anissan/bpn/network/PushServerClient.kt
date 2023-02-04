package com.anissan.bpn.network

import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import com.anissan.bpn.BuildConfig
import com.anissan.bpn.utils.logE
import com.anissan.bpn.utils.logV
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class PushServerClient(
  private val powerManager: PowerManager,
  private val okHttpClient: OkHttpClient,
) {
  private val jsonMediaType: MediaType = "application/json; charset=utf-8".toMediaType()

  fun postNotification(
    token: String,
    title: String,
    body: String,
    onSuccessfulPost: () -> Unit = {},
  ) {
    val postBody: String = """
            |{
            |  "token": "$token",
            |  "title": "$title",
            |  "body": "$body"
            |}
            |""".trimMargin()

    logV { "Sending an API request with $postBody" }

    val apiRequest: Request = Request.Builder()
      .url(BuildConfig.PUSH_SERVER_URL)
      .post(postBody.toRequestBody(jsonMediaType))
      .build()

    // Invalidate token on 400
    // https://firebase.google.com/docs/cloud-messaging/manage-tokens
    okHttpClient.newCall(apiRequest).onAsyncResponse { response: Response ->
      response.use {
        if (response.isSuccessful.not()) {
          logE { "API request failed with $response.code: $response.message" }
        }

        logV { "Successful API response: $response" }

        Handler(Looper.getMainLooper()).post { onSuccessfulPost() }
      }
    }
  }

  /**
   * Extension function for providing a cleaner callback API over the OkHttp's Java API.
   * Callbacks run on a background thread managed by OkHttp.
   */
  private fun Call.onAsyncResponse(
    callback: (response: Response) -> Unit,
  ) {
    // Securing a partial wakelock for up to 10 seconds.
    val wakeLock: PowerManager.WakeLock = powerManager.run {
      newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "${javaClass.name}::notify")
        .apply { acquire(10 * 1000L) } // 10 seconds
    }

    enqueue(responseCallback = object : Callback {
      override fun onFailure(call: Call, e: IOException) {
        logE(e)

        wakeLock.release()
      }

      override fun onResponse(call: Call, response: Response) {
        callback(response)

        wakeLock.release()
      }
    })
  }
}

