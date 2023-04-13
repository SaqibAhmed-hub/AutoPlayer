package com.example.autoplayer.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.example.autoplayer.R
import com.example.autoplayer.adapter.VideoPlayerRecyclerAdapter
import com.example.autoplayer.databinding.ActivityMainBinding
import com.example.autoplayer.model.MediaObject
import com.example.autoplayer.model.Resources
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initRecyclerView()
    }

    private fun initRecyclerView() {
        binding.recyclerView.layoutManager =  LinearLayoutManager(this)
//        val itemDecorator = VerticalSpacingItemDecorator(10)
        binding.recyclerView.addItemDecoration(DividerItemDecoration(this,LinearLayoutManager.VERTICAL))

        val mediaObjects: ArrayList<MediaObject> = Resources.MEDIA_OBJECTS
        binding.recyclerView.setMediaObjects(mediaObjects)

        val adapter = VideoPlayerRecyclerAdapter(mediaObjects,initGlide())
        binding.recyclerView.adapter = adapter
    }

    private fun initGlide(): RequestManager {
        val options: RequestOptions = RequestOptions()
            .placeholder(R.drawable.white_background)
            .error(R.drawable.white_background)
        return Glide.with(this)
            .setDefaultRequestOptions(options)
    }

    override fun onDestroy() {
        binding.recyclerView.releasePlayer();
        super.onDestroy()
    }

}