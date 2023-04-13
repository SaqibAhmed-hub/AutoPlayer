package com.example.autoplayer.adapter

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.autoplayer.R
import com.example.autoplayer.model.MediaObject

class VideoPlayerViewHolder(itemView: View):RecyclerView.ViewHolder(itemView) {

    var media_container: FrameLayout = itemView.findViewById(R.id.media_container)
    var title: TextView = itemView.findViewById(R.id.title);
    var thumbnail: ImageView = itemView.findViewById(R.id.thumbnail);
    var volumeControl: ImageView = itemView.findViewById(R.id.volume_control);
    var progressBar: ProgressBar = itemView.findViewById(R.id.progressBar);
    var parent: View = itemView
    lateinit var  requestManager: RequestManager

    fun bind(data: MediaObject, manager: RequestManager) {
        this.requestManager = manager
        parent.tag = this
        title.text = data.title;
        this.requestManager
            .load(data.thumbnail)
            .into(thumbnail);


    }
}