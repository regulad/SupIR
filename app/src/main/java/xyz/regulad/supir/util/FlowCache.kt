package xyz.regulad.supir.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

suspend fun <T> Flow<T>.collectUntil(predicate: (T) -> Boolean): T? {
    var result: T? = null
    collect { value ->
        if (predicate(value)) {
            result = value
            return@collect
        }
    }
    return result
}

object FlowCache {
    private val cache = mutableMapOf<Flow<*>, List<*>>()
    private val unfinalizedCache = mutableMapOf<Flow<*>, List<*>>()
    // ignore memory leaks: this is only used once in all code
    private val backingMap = mutableMapOf<Flow<*>, Flow<*>>()

    private val mutex = Mutex()

    fun <T : Any> Flow<T>.cached(): Flow<T> {
        val frontendFlow = flow {
            mutex.withLock {
                @Suppress("UNCHECKED_CAST")
                if (this@cached in cache) {
                    (cache[this@cached] as? List<T>)?.forEach { emit(it) }
                } else {
                    val collected = Collections.synchronizedList(mutableListOf<T>())
                    unfinalizedCache[this@cached] = collected
                    collect { value ->
                        collected.add(value)
                        emit(value)
                    }
                    unfinalizedCache.remove(this@cached)
                    cache[this@cached] = collected
                }
            }
        }

        backingMap[frontendFlow] = this
        return frontendFlow
    }

    suspend fun <T> Flow<T>.cachedCollectUntil(predicate: (T) -> Boolean): T? {
        if (this !in backingMap.keys) {
            return collectUntil(predicate)
        }

        val backingFlow = backingMap[this]!! as Flow<T> // type will always be correct

        if (backingFlow in cache) {
            @Suppress("UNCHECKED_CAST")
            (cache[backingFlow] as? List<T>)?.forEach { value ->
                if (predicate(value)) {
                    return@cachedCollectUntil value
                }
            }
        }

        if (backingFlow in unfinalizedCache) {
            @Suppress("UNCHECKED_CAST")
            (unfinalizedCache[backingFlow] as? List<T>)?.forEach { value ->
                if (predicate(value)) {
                    return@cachedCollectUntil value
                }
            }
        }

        return backingFlow.collectUntil(predicate)
    }

    suspend fun clear() {
        mutex.withLock {
            cache.clear()
        }
    }

    suspend fun remove(flow: Flow<*>) {
        mutex.withLock {
            cache.remove(flow)
        }
    }
}
