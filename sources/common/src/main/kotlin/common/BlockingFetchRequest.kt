package common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

val fireAndForgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
