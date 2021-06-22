package com.android.mediacodeclib.Merger

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import com.android.mediacodeclib.FileChooser
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.ExecuteCallback
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStreamWriter

class Merger constructor(private val context: Context, val type: String) {
    private val list = ArrayList<String>()
    fun addFile(file: Uri) {
        val path = FileChooser.getPath(context, file)
        list.add(path)
    }

    private fun makeConcatFile(): File {
        var concatFile = Environment.getExternalStorageDirectory().absolutePath + "/concat.txt"
        val file = File(concatFile)
        if (file.exists() == false) {
            file.createNewFile()
        }
        val builder = StringBuilder()
        for (f in list) {
            builder.append("file " + f)
            builder.append("\n")
        }
        val fis = FileOutputStream(file)
        val writer =
            OutputStreamWriter(fis)
        writer.write(builder.toString())
        writer.close()
        return file
    }

    fun merge() {
        val file = makeConcatFile()
        val command = StringBuilder()
            .append("-f concat -safe 0 -i ")
            .append(file.absolutePath)
            .append(" -c copy ")
        if (type.equals("audio")) {
            command.append("/storage/emulated/0/output.mp3")
        } else {
            command.append("/storage/emulated/0/output.mp4")
        }
        val executionId = FFmpeg.executeAsync(command.toString(), object : ExecuteCallback {
            override fun apply(executionId: Long, returnCode: Int) {
                if (returnCode == Config.RETURN_CODE_SUCCESS) {
                    Toast.makeText(context, "SUCESS", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}

