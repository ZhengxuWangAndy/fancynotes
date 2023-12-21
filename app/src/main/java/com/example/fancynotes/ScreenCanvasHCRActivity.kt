package com.example.fancynotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import java.util.Locale

import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.annotation.VisibleForTesting
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.collect.ImmutableMap
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.collect.ImmutableSortedSet
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier

/**
 * Activity for handling the canvas screen and Handwriting Recognition (HCR) using Digital Ink Recognizer.
 */
class ScreenCanvasHCRActivity : ComponentActivity(), StrokeManager.DownloadedModelsChangedListener {

    // Stroke manager for handling the drawing and recognition process.
    @JvmField @VisibleForTesting
    val strokeManager = StrokeManager()

    // Adapter for the language selection spinner.
    private lateinit var languageAdapter: ArrayAdapter<ModelLanguageContainer>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.digital_ink)
        // HCR
        val languageSpinner = findViewById<Spinner>(R.id.languages_spinner)
        val drawingView = findViewById<DrawingView>(R.id.drawing_view)
        val statusTextView = findViewById<StatusTextView>(R.id.status_text_view)
        drawingView.setStrokeManager(strokeManager)
        statusTextView.setStrokeManager(strokeManager)
        strokeManager.setStatusChangedListener(statusTextView)
        strokeManager.setContentChangedListener(drawingView)
        strokeManager.setDownloadedModelsChangedListener(this)
        strokeManager.setClearCurrentInkAfterRecognition(true)
        strokeManager.setTriggerRecognitionAfterInput(false)
        languageAdapter = populateLanguageAdapter()
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = languageAdapter
        strokeManager.refreshDownloadedModelsStatus()

        languageSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    val languageCode =
                        (parent.adapter.getItem(position) as ModelLanguageContainer).languageTag ?: return
                    Log.i(TAG, "Selected language: $languageCode")
                    strokeManager.setActiveModel(languageCode)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    Log.i(TAG, "No language selected")
                }
            }
        strokeManager.reset()
    }

    /**
     * Initiates the download of the active model in the stroke manager.
     */
    fun downloadClick(v: View?) {
        strokeManager.download()
    }

    /**
     * Initiates recognition of the current ink strokes in the stroke manager.
     */
    fun recognizeClick(v: View?) {
        strokeManager.recognize()
    }

    /**
     * Clears the current ink strokes in the drawing view and resets the stroke manager.
     */
    fun clearClick(v: View?) {
        strokeManager.reset()
        val drawingView = findViewById<DrawingView>(R.id.drawing_view)
        drawingView.clear()
    }

    /**
     * Deletes the currently active model in the stroke manager.
     */
    fun deleteClick(v: View?) {
        strokeManager.deleteActiveModel()
    }

    /**
     * Nested class for handling language model containers.
     */
    private class ModelLanguageContainer
    private constructor(private val label: String, val languageTag: String?) :
        Comparable<ModelLanguageContainer> {

        var downloaded: Boolean = false

        override fun toString(): String {
            return when {
                languageTag == null -> label
                downloaded -> "   [D] $label"
                else -> "   $label"
            }
        }

        override fun compareTo(other: ModelLanguageContainer): Int {
            return label.compareTo(other.label)
        }

        companion object {
            /** Populates and returns a real model identifier, with label and language tag. */
            fun createModelContainer(label: String, languageTag: String?): ModelLanguageContainer {
                // Offset the actual language labels for better readability
                return ModelLanguageContainer(label, languageTag)
            }

            /** Populates and returns a label only, without a language tag. */
            fun createLabelOnly(label: String): ModelLanguageContainer {
                return ModelLanguageContainer(label, null)
            }
        }
    }

    /**
     * Populates and returns an adapter with language models for the spinner.
     */
    private fun populateLanguageAdapter(): ArrayAdapter<ModelLanguageContainer> {
        val languageAdapter =
            ArrayAdapter<ModelLanguageContainer>(this, android.R.layout.simple_spinner_item)
        languageAdapter.add(ModelLanguageContainer.createLabelOnly("Select language"))
        languageAdapter.add(ModelLanguageContainer.createLabelOnly("Non-text Models"))

        // Manually add non-text models first
        for (languageTag in NON_TEXT_MODELS.keys) {
            languageAdapter.add(
                ModelLanguageContainer.createModelContainer(NON_TEXT_MODELS[languageTag]!!, languageTag)
            )
        }
        languageAdapter.add(ModelLanguageContainer.createLabelOnly("Text Models"))
        val textModels = ImmutableSortedSet.naturalOrder<ModelLanguageContainer>()
        for (modelIdentifier in DigitalInkRecognitionModelIdentifier.allModelIdentifiers()) {
            if (NON_TEXT_MODELS.containsKey(modelIdentifier.languageTag)) {
                continue
            }
            if (modelIdentifier.languageTag.endsWith(Companion.GESTURE_EXTENSION)) {
                continue
            }
            textModels.add(buildModelContainer(modelIdentifier, "Script"))
        }
        languageAdapter.addAll(textModels.build())
        languageAdapter.add(ModelLanguageContainer.createLabelOnly("Gesture Models"))
        val gestureModels = ImmutableSortedSet.naturalOrder<ModelLanguageContainer>()
        for (modelIdentifier in DigitalInkRecognitionModelIdentifier.allModelIdentifiers()) {
            if (!modelIdentifier.languageTag.endsWith(Companion.GESTURE_EXTENSION)) {
                continue
            }
            gestureModels.add(buildModelContainer(modelIdentifier, "Script gesture classifier"))
        }
        languageAdapter.addAll(gestureModels.build())
        return languageAdapter
    }

    /**
     * Builds and returns a ModelLanguageContainer based on a DigitalInkRecognitionModelIdentifier.
     */
    private fun buildModelContainer(
        modelIdentifier: DigitalInkRecognitionModelIdentifier,
        labelSuffix: String
    ): ModelLanguageContainer {
        val label = StringBuilder()
        label.append(Locale(modelIdentifier.languageSubtag).displayName)
        if (modelIdentifier.regionSubtag != null) {
            label.append(" (").append(modelIdentifier.regionSubtag).append(")")
        }
        if (modelIdentifier.scriptSubtag != null) {
            label.append(", ").append(modelIdentifier.scriptSubtag).append(" ").append(labelSuffix)
        }
        return ModelLanguageContainer.createModelContainer(
            label.toString(),
            modelIdentifier.languageTag
        )
    }

    /**
     * Callback when downloaded models are changed.
     */
    override fun onDownloadedModelsChanged(downloadedLanguageTags: Set<String>) {
        for (i in 0 until languageAdapter.count) {
            val container = languageAdapter.getItem(i)!!
            container.downloaded = downloadedLanguageTags.contains(container.languageTag)
        }
        languageAdapter.notifyDataSetChanged()
    }

    companion object {
        private const val TAG = "MLKDI.Activity"

        // Map for non-text models.
        private val NON_TEXT_MODELS =
            ImmutableMap.of(
                "zxx-Zsym-x-autodraw",
                "Autodraw",
                "zxx-Zsye-x-emoji",
                "Emoji",
                "zxx-Zsym-x-shapes",
                "Shapes"
            )
        private const val GESTURE_EXTENSION = "-x-gesture"
    }
}
