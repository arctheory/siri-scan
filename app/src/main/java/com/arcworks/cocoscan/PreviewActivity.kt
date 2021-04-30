package com.arcworks.cocoscan

import android.animation.ObjectAnimator
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions

class PreviewActivity : AppCompatActivity() {

    // previewFrame value to hold preview of the image which has been captured in MainActivity
    private val previewFrame: ImageView by lazy { findViewById(R.id.previewFrame) }
    // closeBtn value to hold button to go back to previous activity i.e, MainActivity
    private val closeBtn: Button by lazy { findViewById(R.id.closeBtn) }
    // previewURI variable to hold the link(file location path) to show image
    private lateinit var previewURI: Uri

    private val grainText: TextView by lazy { findViewById(R.id.grainFoundText) }
    private val noGrainText: TextView by lazy { findViewById(R.id.noGrainText) }

    private val confidenceView: ConstraintLayout by lazy { findViewById(R.id.confidenceView) }
    private val confidenceProgress: ProgressBar by lazy { findViewById(R.id.confidenceProgress) }
    private val confidenceValue: TextView by lazy { findViewById(R.id.confidenceValue) }

    // onCreate function to start the activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // default
        setContentView(R.layout.activity_preview) //default
        previewFrame.clipToOutline = true // not important

        previewURI = intent.data ?: return finish() // checks if the file is valid if not goes back to MainActivity

        // shows the captured image on screen
        previewFrame.setImageURI(previewURI)
        // closeBtn event goes back to MainActivity on click
        closeBtn.setOnClickListener {
            finish()
        }

        val localModel = LocalModel.Builder()
            .setAssetFilePath("model_quant.tflite")
            .build()

        val customImageLabelerOptions = CustomImageLabelerOptions.Builder(localModel)
            .setConfidenceThreshold(0.5f)
            .setMaxResultCount(5)
            .build()
        val labeler = ImageLabeling.getClient(customImageLabelerOptions)
        val image: InputImage = InputImage.fromFilePath(this, previewURI)

        labeler.process(image)
            .addOnSuccessListener { (imageLabel) ->
                when(imageLabel.text) {
                    "Grain" -> {
                        grainText.visibility = View.VISIBLE
                    }
                    "no_Grain" -> {
                        noGrainText.visibility = View.VISIBLE
                    }
                    else -> {}
                }
                ObjectAnimator.ofInt(
                    confidenceProgress, "progress", 0,
                    (imageLabel.confidence * 100).toInt()
                ).apply {
                    this.duration = 1000
                }.start()

                val value = "${(imageLabel.confidence * 100).toInt()}%"
                confidenceValue.text = value
            }
            .addOnFailureListener { _ ->
                Toast.makeText(baseContext, "Unable to process image", Toast.LENGTH_LONG).show()
            }
    }
}