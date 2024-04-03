import kotlinx.coroutines.*

import kotlin.coroutines.CoroutineContext

fun main(args: Array<String>) {
    //create custom scope
    // (include SupervisorJob to not cancel all coroutines started on this scope if one fails)
    val scope = CustomScope(
        SupervisorJob() +
                createCoroutineDispatcher() +
                createCoroutineExceptionHandler()
    )
    scope.onStart {
        downloadFile(1)
        downloadFile(2)
        downloadFile(3)
        downloadFile(4)
        downloadLongFile()
    }
}

suspend fun downloadFile(num: Int) {
    println("downloadFile $num")
    delay(1000L)
}

suspend fun downloadLongFile() {
    delay(5000L)
}

@OptIn(ExperimentalCoroutinesApi::class)
fun createCoroutineDispatcher(): CoroutineDispatcher =
    Dispatchers.IO.limitedParallelism(5) //create dispatcher with limit of threads

fun createCoroutineExceptionHandler() =
    CoroutineExceptionHandler { _, throwable ->
        println("Unhandled exception: $throwable")
    } //create coroutine exception handler with logging of exceptions

//custom scope which can start and cancel coroutine
class CustomScope(override val coroutineContext: CoroutineContext) : CoroutineScope {

    fun onStart(action: suspend () -> Unit) {
        launch {
            //while current Job has not completed and was not cancelled yet
            while (isActive) {
                action.invoke()
            }
        }
    }

    fun onCancel(cancelAll: Boolean, exception: CancellationException) {
        if (!cancelAll)
            coroutineContext.cancel(exception)
        else {
            val parentJob = checkNotNull(coroutineContext[Job]) //get parent Job
            parentJob.cancel(exception)
        }
    }

}