package com.example.fancynotes

import android.util.Log
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.RecognitionResult
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Task to run asynchronously to obtain recognition results from a Digital Ink Recognizer.
 * @param recognizer The Digital Ink Recognizer instance.
 * @param ink The ink data to be recognized.
 */
class RecognitionTask(private val recognizer: DigitalInkRecognizer?, private val ink: Ink) {
    private var currentResult: RecognizedInk? = null
    private val cancelled: AtomicBoolean
    private val done: AtomicBoolean

    /**
     * Cancels the recognition task.
     */
    fun cancel() {
        cancelled.set(true)
    }

    /**
     * Checks if the recognition task is done.
     * @return True if the task is completed, otherwise false.
     */
    fun done(): Boolean {
        return done.get()
    }

    /**
     * Retrieves the recognition result.
     * @return The recognized ink object or null if no result is available.
     */
    fun result(): RecognizedInk? {
        return currentResult
    }

    /**
     * Helper class that stores an ink along with the corresponding recognized text.
     */
    class RecognizedInk internal constructor(val ink: Ink, val text: String?)

    /**
     * Runs the recognition task asynchronously.
     * @return A task containing the recognition result string or null if no result.
     */
    fun run(): Task<String?> {
        Log.i(TAG, "RecoTask.run")
        return recognizer!!
            .recognize(ink)
            .onSuccessTask(
                SuccessContinuation { result: RecognitionResult? ->
                    if (cancelled.get() || result == null || result.candidates.isEmpty()
                    ) {
                        return@SuccessContinuation Tasks.forResult<String?>(null)
                    }
                    currentResult =
                        RecognizedInk(
                            ink,
                            result.candidates[0]
                                .text
                        )
                    Log.i(
                        TAG,
                        "result: " + currentResult!!.text
                    )
                    done.set(
                        true
                    )
                    return@SuccessContinuation Tasks.forResult<String?>(currentResult!!.text)
                }
            )
    }

    companion object {
        // Tag used for logging.
        private const val TAG = "MLKD.RecognitionTask"
    }

    init {
        // Initialize the cancellation and completion flags.
        cancelled = AtomicBoolean(false)
        done = AtomicBoolean(false)
    }
}