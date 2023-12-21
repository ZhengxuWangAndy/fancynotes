package com.example.fancynotes
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * A custom TextView class designed to display the status updates from the StrokeManager.
 * This class implements the StatusChangedListener interface to listen for status changes.
 */
class StatusTextView : AppCompatTextView, StrokeManager.StatusChangedListener {
    private var strokeManager: StrokeManager? = null

    /**
     * Primary constructor for creating StatusTextView with context.
     * @param context The context in which the view is running.
     */
    constructor(context: Context) : super(context) {}

    /**
     * Secondary constructor for creating StatusTextView with context and attributes.
     * @param context The context in which the view is running.
     * @param attributeSet The attributes set from XML layout.
     */
    constructor(context: Context?, attributeSet: AttributeSet?) : super(
        context!!,
        attributeSet
    ) {
    }

    /**
     * Called when there is a status change event from the StrokeManager.
     * Updates the text of the TextView to reflect the current status.
     */
    override fun onStatusChanged() {
        this.text = strokeManager!!.status
    }

    /**
     * Sets the StrokeManager instance for this view.
     * @param strokeManager The StrokeManager instance to listen to for status updates.
     */
    fun setStrokeManager(strokeManager: StrokeManager?) {
        this.strokeManager = strokeManager
    }
}