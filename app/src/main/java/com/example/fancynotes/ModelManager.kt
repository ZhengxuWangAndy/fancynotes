package com.example.fancynotes

import android.util.Log
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.common.MlKitException
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import java.util.HashSet

/** Class to manage model downloading, deletion, and selection.  */
class ModelManager {
    private var model: DigitalInkRecognitionModel? = null
    var recognizer: DigitalInkRecognizer? = null
    val remoteModelManager = RemoteModelManager.getInstance()

    /**
     * Sets the model based on the provided language tag.
     * Clears any existing model and recognizer, and initializes new ones.
     * @param languageTag The language tag for the model.
     * @return A message indicating the outcome of setting the model.
     */
    fun setModel(languageTag: String): String {
        // Clear the old model and recognizer.
        model = null
        recognizer?.close()
        recognizer = null

        // Try to parse the languageTag and get a model from it.
        val modelIdentifier: DigitalInkRecognitionModelIdentifier?
        modelIdentifier = try {
            DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)
        } catch (e: MlKitException) {
            Log.e(
                TAG,
                "Failed to parse language '$languageTag'"
            )
            return ""
        } ?: return "No model for language: $languageTag"

        // Initialize the model and recognizer.
        model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
        recognizer = DigitalInkRecognition.getClient(
            DigitalInkRecognizerOptions.builder(model!!).build()
        )
        Log.i(
            TAG, "Model set for language '$languageTag' ('$modelIdentifier.languageTag')."
        )
        return "Model set for language: $languageTag"
    }

    /**
     * Checks if the current model has been downloaded.
     * @return A task indicating if the model is downloaded or not.
     */
    fun checkIsModelDownloaded(): Task<Boolean?> {
        return remoteModelManager.isModelDownloaded(model!!)
    }

    /**
     * Deletes the currently active model if it is downloaded.
     * @return A task with a message indicating the outcome of the deletion.
     */
    fun deleteActiveModel(): Task<String?> {
        if (model == null) {
            Log.i(TAG, "Model not set")
            return Tasks.forResult("Model not set")
        }
        return checkIsModelDownloaded()
            .onSuccessTask { result: Boolean? ->
                if (!result!!) {
                    return@onSuccessTask Tasks.forResult("Model not downloaded yet")
                }
                remoteModelManager
                    .deleteDownloadedModel(model!!)
                    .onSuccessTask { _: Void? ->
                        Log.i(
                            TAG,
                            "Model successfully deleted"
                        )
                        Tasks.forResult(
                            "Model successfully deleted"
                        )
                    }
            }
            .addOnFailureListener { e: Exception ->
                Log.e(
                    TAG,
                    "Error while model deletion: $e"
                )
            }
    }

    /**
     * Retrieves a set of languages for which models have been downloaded.
     * @return A task with a set of language tags of downloaded models.
     */
    val downloadedModelLanguages: Task<Set<String>>
        get() = remoteModelManager
            .getDownloadedModels(DigitalInkRecognitionModel::class.java)
            .onSuccessTask(
                SuccessContinuation { remoteModels: Set<DigitalInkRecognitionModel>? ->
                    val result: MutableSet<String> = HashSet()
                    for (model in remoteModels!!) {
                        result.add(model.modelIdentifier.languageTag)
                    }
                    Log.i(
                        TAG,
                        "Downloaded models for languages:$result"
                    )
                    Tasks.forResult<Set<String>>(result.toSet())
                }
            )

    /**
     * Downloads the currently selected model.
     * @return A task with a message indicating the outcome of the download.
     */
    fun download(): Task<String?> {
        return if (model == null) {
            Tasks.forResult("Model not selected.")
        } else remoteModelManager
            .download(model!!, DownloadConditions.Builder().build())
            .onSuccessTask { _: Void? ->
                Log.i(
                    TAG,
                    "Model download succeeded."
                )
                Tasks.forResult("Downloaded model successfully")
            }
            .addOnFailureListener { e: Exception ->
                Log.e(
                    TAG,
                    "Error while downloading the model: $e"
                )
            }
    }

    companion object {
        // Tag used for logging
        private const val TAG = "MLKD.ModelManager"
    }
}