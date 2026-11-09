package de.connect2x.livekitnity.demo.shared.optimistic

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class OptimisticState<T>(
    private val source: State<T>,
) : MutableState<T> {
  class Request<T>(val value: T)

  var pending by mutableStateOf<Request<T>?>(null)
    private set

  override var value: T
    get() {
      val request = pending
      return if (request != null) request.value else source.value
    }
    set(value) {
      pending = Request(value)
    }

  override fun component1(): T {
    return value
  }

  override fun component2(): (T) -> Unit {
    return { value = it }
  }

  fun clear(request: Request<T>): Boolean {
    if (pending !== request) return false
    pending = null
    return true
  }
}
