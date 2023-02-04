/*
 * ulog: simple, fast, and efficient logging facade for Kotlin
 * Source: https://github.com/kdrag0n/ulog
 * Compatibility Date: Aug 2, 2022
 *
 * - Fast: lazy message evaluation, no stack traces used for automatic tags (like square/logcat)
 * - Extensible with backends like Timber, but with a simpler API
 * - Easier to specify explicit tags than Timber
 * - Debug and verbose logs optimized out at compile time
 *   (completely removed from release builds)
 *
 * Licensed under the MIT License (MIT)
 *
 * Copyright (c) 2022 Danny Lin <oss@kdrag0n.dev>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

@file:Suppress("FunctionName") // _ indicates internal

package com.anissan.bpn.utils

import android.util.Log
import com.anissan.bpn.BuildConfig
import org.jetbrains.annotations.ApiStatus.Internal
import java.io.PrintWriter
import java.io.StringWriter

// APIs must be public for inline calls
object Ulog {
  // perf
  @Internal
  @JvmStatic
  var backends = emptyArray<LogBackend>()

  const val LOG_VERBOSE = true
  @JvmStatic
  val LOG_DEBUG = BuildConfig.DEBUG

  fun installBackend(backend: LogBackend) {
    backends += backend
  }

  @JvmStatic
  @Internal
  fun _formatException(e: Throwable) = e.let {
    val sw = StringWriter(256)
    val pw = PrintWriter(sw, false)
    e.printStackTrace(pw)
    pw.flush()
    '\n' + sw.toString()
  }

  @JvmStatic
  @Internal
  fun _print(tag: String, priority: Int, msg: String, exception: Throwable?) {
    backends.forEach {
      it.print(tag, priority, msg, exception)
    }
  }

  // Change this if you have more stringent min API requirements
  @JvmStatic
  @Internal
  fun _getDefaultTag(): String = BuildConfig.APPLICATION_ID
}

interface LogBackend {
  fun print(tag: String, priority: Int, message: String, exception: Throwable?)
}

class SystemLogBackend : LogBackend {
  override fun print(tag: String, priority: Int, message: String, exception: Throwable?) {
    val finalMsg = if (exception != null) message + Ulog._formatException(exception) else message
    Log.println(priority, tag, finalMsg)
  }
}

// Must be public for inline calls
@Internal
fun Any.__className_ulog_internal(): String {
  val clazz = this::class.java
  val name = clazz.simpleName
  // Slow path for anonymous classes
  return name.ifEmpty { clazz.name.split('.').last() }
}

/*
 * Generic (all levels)
 * Can't be optimized much as this needs to support all priorities.
 */
inline fun log(
  tag: String? = null,
  priority: Int = Log.DEBUG,
  exception: Throwable? = null,
  message: () -> String = { "" },
) {
  val msg = message()
  val finalTag = tag ?: Ulog._getDefaultTag()
  Ulog._print(finalTag, priority, msg, exception)
}

inline fun log(
  exception: Throwable,
  tag: String? = null,
  priority: Int = Log.DEBUG,
  message: () -> String = { "" },
) = log(tag, priority, exception, message)

inline fun log(
  priority: Int,
  tag: String? = null,
  exception: Throwable? = null,
  message: () -> String = { "" },
) = log(tag, priority, exception, message)

inline fun Any.log(
  tag: String? = null,
  priority: Int = Log.DEBUG,
  exception: Throwable? = null,
  message: () -> String = { "" },
) {
  val msg = message()
  val finalTag = tag ?: __className_ulog_internal()
  Ulog._print(finalTag, priority, msg, exception)
}

inline fun Any.log(
  exception: Throwable,
  tag: String? = null,
  priority: Int = Log.DEBUG,
  message: () -> String = { "" },
) = log(tag, priority, exception, message)

inline fun Any.log(
  priority: Int,
  tag: String? = null,
  exception: Throwable? = null,
  message: () -> String = { "" },
) = log(tag, priority, exception, message)

/*
 * Verbose
 * Optimized out at compile time when possible.
 */
inline fun logV(
  tag: String? = null,
  exception: Throwable? = null,
  message: () -> String = { "" },
) {
  if (!Ulog.LOG_VERBOSE) return

  val msg = message()
  val finalTag = tag ?: Ulog._getDefaultTag()
  Ulog._print(finalTag, Log.VERBOSE, msg, exception)
}

inline fun logV(
  exception: Throwable,
  tag: String? = null,
  message: () -> String = { "" },
) = logV(tag, exception, message)

inline fun Any.logV(
  tag: String? = null,
  exception: Throwable? = null,
  message: () -> String = { "" },
) {
  if (!Ulog.LOG_VERBOSE) return

  val msg = message()
  val finalTag = tag ?: __className_ulog_internal()
  Ulog._print(finalTag, Log.VERBOSE, msg, exception)
}

inline fun Any.logV(
  exception: Throwable,
  tag: String? = null,
  message: () -> String = { "" },
) = logV(tag, exception, message)

/*
 * Debug
 */
inline fun logD(
  tag: String? = null,
  exception: Throwable? = null,
  message: () -> String = { "" },
) {
  if (!Ulog.LOG_DEBUG) return

  val msg = message()
  val finalTag = tag ?: Ulog._getDefaultTag()
  Ulog._print(finalTag, Log.DEBUG, msg, exception)
}

inline fun logD(
  exception: Throwable,
  tag: String? = null,
  message: () -> String = { "" },
) = logD(tag, exception, message)

inline fun Any.logD(
  tag: String? = null,
  exception: Throwable? = null,
  message: () -> String = { "" },
) {
  if (!Ulog.LOG_DEBUG) return

  val msg = message()
  val finalTag = tag ?: __className_ulog_internal()
  Ulog._print(finalTag, Log.DEBUG, msg, exception)
}

inline fun Any.logD(
  exception: Throwable,
  tag: String? = null,
  message: () -> String = { "" },
) = logD(tag, exception, message)

/*
 * Other levels
 * (no special optimizations)
 */
inline fun logI(
  tag: String? = null,
  exception: Throwable? = null,
  message: () -> String = { "" },
) = log(tag, Log.INFO, exception, message)

inline fun logI(
  exception: Throwable,
  tag: String? = null,
  message: () -> String = { "" },
) = log(tag, Log.INFO, exception, message)

inline fun Any.logI(
  tag: String? = null,
  exception: Throwable? = null,
  message: () -> String = { "" },
) = log(tag, Log.INFO, exception, message)

inline fun Any.logI(
  exception: Throwable,
  tag: String? = null,
  message: () -> String = { "" },
) = log(tag, Log.INFO, exception, message)

inline fun logW(
  exception: Throwable? = null,
  tag: String? = null,
  message: () -> String = { "" },
) = log(tag, Log.WARN, exception, message)

inline fun logW(
  tag: String,
  exception: Throwable? = null,
  message: () -> String = { "" },
) = log(tag, Log.WARN, exception, message)

inline fun Any.logW(
  exception: Throwable? = null,
  tag: String? = null,
  message: () -> String = { "" },
) = log(tag, Log.WARN, exception, message)

inline fun Any.logW(
  tag: String,
  exception: Throwable? = null,
  message: () -> String = { "" },
) = log(tag, Log.WARN, exception, message)

inline fun logE(
  exception: Throwable? = null,
  tag: String? = null,
  message: () -> String = { "" },
) = log(tag, Log.ERROR, exception, message)

inline fun logE(
  tag: String,
  exception: Throwable? = null,
  message: () -> String = { "" },
) = log(tag, Log.ERROR, exception, message)

inline fun Any.logE(
  exception: Throwable? = null,
  tag: String? = null,
  message: () -> String = { "" },
) = log(tag, Log.ERROR, exception, message)

inline fun Any.logE(
  tag: String,
  exception: Throwable? = null,
  message: () -> String = { "" },
) = log(tag, Log.ERROR, exception, message)
