package com.android.medialib

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.mediacodeclib.Merger.Merger
import com.android.mediacodeclib.Merger.ResultListener
import java.io.File

class VideoMergerActivity : AppCompatActivity(), ResultListener {
    private lateinit var btn: Button
    private lateinit var merge: Button
    private lateinit var recyclerView: RecyclerView
    private var result: String? = null
    private var type: String? = null
    private val file: MutableList<Uri> = mutableListOf()
    private lateinit var adapter: Adapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_merger)

        type = intent.getStringExtra("type")
        btn = findViewById(R.id.addVideoBtn)
        recyclerView = findViewById(R.id.fileRecycler)
        merge = findViewById(R.id.merge)
        btn.setOnClickListener {
            addMediaFile()
        }
        merge.setOnClickListener {
            val merger = Merger(this, type!!)
            for (file in file) {
                merger.addFile(file)
            }
            merger.merge()
        }
        val linearLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = linearLayoutManager
        adapter = Adapter(file, this)
        recyclerView.adapter = adapter
    }

    private fun addMediaFile() {
        val intent = Intent()
        if (type.equals("video")) {
            intent.type = "video/*"
        } else {
            intent.type = "audio/*"
        }
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, 200)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val uri = data!!.data
            uri?.let {
                file.add(uri)
                Toast.makeText(this, "File added", Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class Adapter(list: MutableList<Uri>, context: Context) :
        RecyclerView.Adapter<Adapter.Viewholder>() {
        private val list = list
        private val context = context

        inner class Viewholder(view: View) : RecyclerView.ViewHolder(view) {
            val textView = view.findViewById<TextView>(R.id.text)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder {
            val view = LayoutInflater.from(context).inflate(R.layout.item, parent, false)
            return Viewholder(view)
        }

        override fun onBindViewHolder(holder: Viewholder, position: Int) {
            val file = File(list[position].path)
            holder.textView.text = file.name
        }

        override fun getItemCount(): Int {
            return list.size
        }
    }

    override fun getResult(name: String?) {
        Toast.makeText(this, "Merging done", Toast.LENGTH_LONG).show()
        val intent = Intent(this, VideoResultActivity::class.java)
        intent.putExtra("result", name)
        startActivity(intent)
    }
}