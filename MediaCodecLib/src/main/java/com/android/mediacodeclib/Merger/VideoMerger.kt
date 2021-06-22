package com.android.mediacodeclib.Merger

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.android.mediacodeclib.FileChooser
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.ExecuteCallback
import com.arthenica.mobileffmpeg.FFmpeg
import com.googlecode.mp4parser.BasicContainer
import com.googlecode.mp4parser.authoring.Movie
import com.googlecode.mp4parser.authoring.Track
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator
import com.googlecode.mp4parser.authoring.tracks.AppendTrack
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.RandomAccessFile
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList

class VideoMerger constructor(private val context: Context, private val listener: ResultListener) {
    private val handler = Handler(Looper.getMainLooper())
    private val list = ArrayList<Uri>()
    fun addVideo(file: Uri) {
        list.add(file)
    }

    fun merge(type: String) {
        if (list.size < 2) {
            Toast.makeText(context, "Minumum 2 video required to merge", Toast.LENGTH_SHORT).show()
        } else {
            val thread = Thread {
                val fileList = mutableListOf<String>()
                for (l in list) {
                    fileList.add(FileChooser.getPath(context, l))
                }
                val finalFilename = appendVideos(fileList, type)
                handler.post { listener.getResult(finalFilename) }
            }
            thread.start()
        }
    }

    private fun appendVideos(fileList: MutableList<String>, type: String): String {
        try {
            val inMovies = mutableListOf<Movie>()
            for (file in fileList) {
                inMovies.add(MovieCreator.build(file))
            }
            val videoTracks: MutableList<Track> = LinkedList()
            val audioTracks: MutableList<Track> = LinkedList()
            for (m in inMovies) {
                for (t in m!!.tracks) {
                    if (t.handler == "soun") {
                        audioTracks.add(t)
                    }
                    if (type == "video" && t.handler == "vide") {
                        videoTracks.add(t)
                    }
                }
            }
            val result = Movie()
            if (audioTracks.size > 0) {
                result.addTrack(AppendTrack(*audioTracks
                        .toTypedArray()))
            }
            if (videoTracks.size > 0) {
                result.addTrack(AppendTrack(*videoTracks
                        .toTypedArray()))
            }
            val out = DefaultMp4Builder().build(result) as BasicContainer
            val fc = RandomAccessFile(Environment.getExternalStorageDirectory().toString() + "/output.mp4", "rw").channel
            out.writeContainer(fc)
            fc.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        var mFileName = Environment.getExternalStorageDirectory().absolutePath
        mFileName += "/output.mp4"
        return mFileName
    }

    private fun makeConcatFile(files: ArrayList<File>): File {
        var concatFile = Environment.getExternalStorageDirectory().absolutePath + "/concat.txt"
        val file = File(concatFile)
        if (file.exists() == false) {
            file.createNewFile()
        }
        val builder = StringBuilder()
        for (file in files) {
            builder.append(file.absolutePath)
            builder.append("\n")
        }
        return file
    }

    private fun append(context: Context, file: File) {
        val command = StringBuilder()
                .append("-i ")
                .append(file.absolutePath)
                .append(" -c")
                .append(" copy ")
                .append("output.mp4")
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