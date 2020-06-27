package org.example.mediaindex

import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReferenceArray

suspend fun <T> Flow<T>.toChannel(): Channel<T> {
    val flow = this
    val channel = Channel<T>(Channel.UNLIMITED)
    coroutineScope {
        launch {
            flow.onCompletion { channel.close(it); println("Closed channel for flow") }.collect { channel.offer(it) }
        }
    }
    return channel
}

fun Collection<Job>.onAllCompleted(handler: (Collection<Throwable?>) -> Unit) {
    val latch = CountDownLatch(this.size)
    val causes = AtomicReferenceArray<Throwable?>(this.size)

    this.forEachIndexed { index, job ->
        job.invokeOnCompletion {
            causes.set(index, it)
            latch.countDown()

            if (latch.count == 0L) {
                handler((0 until causes.length()).map { i -> causes.get(i) })
            }
        }
    }


}