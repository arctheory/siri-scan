package com.arcworks.cocoscan

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class PreviewActivity : AppCompatActivity() {

    private val previewFrame: ImageView by lazy { findViewById(R.id.previewFrame) }
    private val closeBtn: Button by lazy { findViewById(R.id.closeBtn) }

    private lateinit var previewURI: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        previewFrame.clipToOutline = true
        previewURI = intent.data ?: return finish()

        previewFrame.setImageURI(previewURI)
        closeBtn.setOnClickListener {
            finish()
        }
    }
}