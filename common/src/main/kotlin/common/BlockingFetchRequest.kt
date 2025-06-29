package common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KProperty

class BlockingFetchRequest<T>(
    initialScope: CoroutineScope,
    private val computation: suspend () -> T
) {
    private val computationJob = initialScope.async {
        computation()
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return runBlocking {
            computationJob.await()
        }
    }
}
