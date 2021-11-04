package io.github.ni554n.bpn.network

import android.os.PowerManager
import logcat.LogPriority
import logcat.asLog
import logcat.logcat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object MyInfo {
  const val token =
    "cIViV4YZQCFTTI58ICM0uL:APA91bHhbeMI8XvkwcmZZT4huZBFipozTiQA6RzabLYBM0gznt5xyBu9cIMpBOwNzJFpjuIx21JbloHDRyyomvj7n1wwooJ5MXScba5s4joYuFnJ6_BB9sRIFxc-nlT1h9mD38HlFgF7"
}

val client: OkHttpClient = OkHttpClient()

fun getToken(otp: String): Call {
  val request: Request = Request.Builder()
    .url("https://notify.vercel.app/api/pair?otp=$otp")
    .build()

  return client.newCall(request)
}

class PushNotification(private val powerManager: PowerManager) {
  private val jsonMediaType: MediaType = "application/json; charset=utf-8".toMediaType()

  fun notify(token: String, title: String, body: String) {
    val wakeLock: PowerManager.WakeLock = powerManager.run {
      newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "${javaClass.name}::notify")
        .apply { acquire(10 * 1000L) } // 10 seconds
    }

    val postBody: String = """
            |{how
            |  "token": "$token",
            |  "title": "$title",
            |  "body": "$body"
            |}
            |""".trimMargin()

    logcat { postBody }

    val request: Request = Request.Builder()
//            .url("https://webhook.site/e6d74fc2-cae4-45f3-8877-e742521374b2")
      .url("https://notify.vercel.app/api/notify")
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
 * This extension function is for providing cleaner callbacks over OkHttp's Java API
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
  logcat(::logError::javaClass.name, LogPriority.ERROR) {
    error.asLog()
  }
}
