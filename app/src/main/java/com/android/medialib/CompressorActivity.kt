package com.android.medialib

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.mediacodeclib.VideoCompressor.CompressionListener
import com.android.mediacodeclib.VideoCompressor.VideoCompressor
import com.android.mediacodeclib.VideoCompressor.VideoHelper.getMediaPath
import com.android.mediacodeclib.VideoCompressor.VideoQuality
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.*

class CompressorActivity : AppCompatActivity() {
    companion object {
        const val REQUEST_SELECT_VIDEO = 0
        const val REQUEST_CAPTURE_VIDEO = 1
    }

    private lateinit var path: String
    private val compressor: VideoCompressor = VideoCompressor()
    private lateinit var progressDialog: ProgressBar
    private lateinit var progress: TextView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compressor)

        setReadStoragePermission()

        val data = intent.getStringExtra("KEY")
        val uri = Uri.parse(data)
        progressDialog = findViewById(R.id.progressBar)
        progress = findViewById(R.id.progress)
        processVideo(uri)
    }

    //Pick a video file from device
    private fun pickVideo() {
        val intent = Intent()
        intent.apply {
            type = "video/*"
            action = Intent.ACTION_PICK
        }
        startActivityForResult(Intent.createChooser(intent, "Select video"), REQUEST_SELECT_VIDEO)
    }

    private fun dispatchTakeVideoIntent() {
        Intent(MediaStore.ACTION_VIDEO_CAPTURE).also { takeVideoIntent ->
            takeVideoIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takeVideoIntent, REQUEST_CAPTURE_VIDEO)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode == Activity.RESULT_OK)
            if (requestCode == REQUEST_SELECT_VIDEO || requestCode == REQUEST_CAPTURE_VIDEO) {
                handleResult(data)
            }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun handleResult(data: Intent?) {
        if (data != null && data.data != null) {
            val str = data.getStringExtra("KEY")
            val uri = Uri.parse(str)
            processVideo(uri)
        }
    }

    private fun setReadStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) != PackageManager.PERMISSION_GRANTED
        ) {

            if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
            ) {
                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        1
                )
            }
        }
    }

    private fun processVideo(uri: Uri?) {
        val thread = Thread(Runnable {
            uri?.let {
                path = getMediaPath(applicationContext, uri)

                val desFile = saveVideoFile(path)

                desFile?.let {
                    var time = 0L
                    compressor.start(
                            context = applicationContext,
                            srcUri = uri,
                            srcPath = path,
                            destPath = desFile.path,
                            listener = object : CompressionListener {
                                override fun onProgress(percent: Float) {
                                    //Update UI
                                    if (percent <= 100 && percent.toInt() % 5 == 0) {
                                        handler.post(Runnable {
                                            progress.text = percent.toInt().toString() + "%"
                                        })
                                    }
                                }

                                override fun onStart() {
                                    time = System.currentTimeMillis()
                                }

                                override fun onSuccess() {
                                    handler.post(Runnable {
                                        progress.text = "100%"
                                        progressDialog.visibility = View.GONE
                                    })
                                    val newSizeValue = desFile.length()

                                    time = System.currentTimeMillis() - time
                                    path = desFile.path

                                    Looper.myLooper()?.let {
                                        Handler(it).postDelayed({}, 50)
                                    }
                                }

                                override fun onFailure(failureMessage: String) {
                                    Log.wtf("failureMessage", failureMessage)
                                }

                                override fun onCancelled() {
                                    Log.wtf("TAG", "compression has been cancelled")
                                    // make UI changes, cleanup, etc
                                }
                            },
                            quality = VideoQuality.MEDIUM,
                            isMinBitRateEnabled = true,
                            keepOriginalResolution = false,
                    )
                }
            }
        }).start()
    }

    private fun saveVideoFile(filePath: String?): File? {
        filePath?.let {
            val videoFile = File(filePath)
            val videoFileName = "${System.currentTimeMillis()}_${videoFile.name}"
            val folderName = Environment.DIRECTORY_MOVIES
            if (Build.VERSION.SDK_INT >= 30) {

                val values = ContentValues().apply {

                    put(
                            MediaStore.Images.Media.DISPLAY_NAME,
                            videoFileName
                    )
                    put(MediaStore.Images.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Images.Media.RELATIVE_PATH, folderName)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val collection =
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                val fileUri = applicationContext.contentResolver.insert(collection, values)

                fileUri?.let {
                    application.contentResolver.openFileDescriptor(fileUri, "rw")
                            .use { descriptor ->
                                descriptor?.let {
                                    FileOutputStream(descriptor.fileDescriptor).use { out ->
                                        FileInputStream(videoFile).use { inputStream ->
                                            val buf = ByteArray(4096)
                                            while (true) {
                                                val sz = inputStream.read(buf)
                                                if (sz <= 0) break
                                                out.write(buf, 0, sz)
                                            }
                                        }
                                    }
                                }
                            }

                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    applicationContext.contentResolver.update(fileUri, values, null, null)

                    return File(getMediaPath(applicationContext, fileUri))
                }
            } else {
                val downloadsPath =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val desFile = File(downloadsPath, videoFileName)

                if (desFile.exists())
                    desFile.delete()

                try {
                    desFile.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                return desFile
            }
        }
        return null
    }
}