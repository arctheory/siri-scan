package com.arcworks.cocoscan

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
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
            .addOnSuccessListener { labels ->
                Log.e("LABELS:", labels.toString())
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }
}