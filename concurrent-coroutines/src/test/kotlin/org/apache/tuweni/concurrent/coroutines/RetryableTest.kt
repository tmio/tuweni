// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.concurrent.coroutines

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicInteger

private val NOOP_EXCEPTION_HANDLER = CoroutineExceptionHandler { _, _ -> }

@ExperimentalCoroutinesApi
internal class RetryableTest {

  @Test
  fun shouldNotRetryIfFirstAttemptReturns() = runBlocking {
    var attempts = 0
    val result = retry(500) {
      attempts++
      "done"
    }
    assertEquals("done", result)
    assertEquals(1, attempts)
  }

  @Test
  fun shouldRetryUntilSuccess() = runBlocking {
    var attempts = 0
    val countAttempts = AtomicInteger(5)
    val result = retry(100) {
      attempts++
      val current = countAttempts.decrementAndGet()
      if (current == 0) "done" else null
    }
    assertEquals("done", result)
    assertEquals(5, attempts)
  }

  @Test
  fun shouldReturnAnySuccess() = runBlocking {
    var attempts = 0
    val countAttempts = AtomicInteger(5)
    val result = retry(25) { _ ->
      attempts++
      val current = countAttempts.decrementAndGet()
      if (current == 0) "done" else null
    }
    assertEquals("done", result)
    assertTrue(attempts > 4)
  }

  @Test
  fun shouldStopRetryingAfterMaxAttempts() = runBlocking {
    var attempts = 0
    val countAttempts = AtomicInteger(3)
    val result = retry(50, 3) { _ ->
      attempts++
      val current = countAttempts.decrementAndGet()
      if (current == 0) "done" else null
    }
    assertEquals("done", result)
    assertEquals(3, attempts)
  }

  @Test
  fun shouldReturnNullIfAllAttemptsFail() = runBlocking {
    var attempts = 0
    val result = retry(50, 3) {
      attempts++
      null
    }
    assertNull(result)
    assertEquals(3, attempts)
  }

  @Test
  fun shouldThrowIfAttemptThrows() {
    var attempts = 0
    val e = assertThrows<RuntimeException> {
      runBlocking(NOOP_EXCEPTION_HANDLER) {
        retry(25) { i ->
          attempts++
          if (i == 4) {
            throw RuntimeException("catch me")
          }
          delay(1000)
        }
      }
    }
    assertEquals("catch me", e.message)
    assertEquals(4, attempts)
  }
}
