package com.example.autoplayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.autoplayer.R
import com.example.autoplayer.model.MediaObject


class VideoPlayerRecyclerAdapter(
    private val dataset: ArrayList<MediaObject>,
    private val initGlide: RequestManager
) : RecyclerView.Adapter<VideoPlayerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoPlayerViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.list_item_video, parent, false)
        return VideoPlayerViewHolder(view)
    }

    override fun getItemCount(): Int = dataset.size

    override fun onBindViewHolder(holder: VideoPlayerViewHolder, position: Int) {
        val data = dataset[position]
        holder.bind(data, initGlide)
    }

}