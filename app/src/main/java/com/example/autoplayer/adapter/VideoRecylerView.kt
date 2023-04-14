package com.example.autoplayer.adapter

import android.content.Context
import android.graphics.Point
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.Display
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.example.autoplayer.R
import com.example.autoplayer.model.MediaObject
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.*
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util


enum class VolumeState {
    ON, OFF
}

class VideoRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : RecyclerView(context, attrs) {

    companion object {
        private const val TAG = "VideoPlayerRecyclerView"
    }

    //ui
    private var thumbnail: ImageView? = null
    private var volumeControl: ImageView? = null
    private var progressBar: ProgressBar? = null
    private var viewHolderParent: View? = null
    private var frameLayout: FrameLayout? = null
    private var videoSurfaceView: PlayerView? = null
    private var videoPlayer: SimpleExoPlayer? = null

    private var volumeState: VolumeState? = null

    //var
    private var videoSurfaceDefaultHeight: Int = 0
    private var screenDefaultHeight: Int = 0
    private var playPosition = -1
    private var isVideoViewAdded = false
    private var requestManager: RequestManager? = null
    private var mediaObjects = ArrayList<MediaObject>()

    init {
        val context = context.applicationContext
        val display: Display =
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val point = Point()
        display.getSize(point)
        videoSurfaceDefaultHeight = point.x
        screenDefaultHeight = point.y

        videoSurfaceView = PlayerView(context)
        videoSurfaceView!!.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

        initializePlayer()

        addOnScrollListener(object : OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_IDLE) {
                    Log.d(TAG, "onScrollStateChanged: called.")
                    if (thumbnail != null) { // show the old thumbnail
                        thumbnail!!.visibility = VISIBLE
                    }

                    // There's a special case when the end of the list has been reached.
                    // Need to handle that with this bit of logic
                    if (!recyclerView.canScrollVertically(1)) {
                        playVideo(true)
                    } else {
                        playVideo(false)
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
            }
        })

        addOnChildAttachStateChangeListener(object : OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {

            }

            override fun onChildViewDetachedFromWindow(view: View) {
                if (viewHolderParent != null && viewHolderParent == view) {
                    resetVideoView()
                }
            }
        })

        videoPlayer!!.addListener(object : Player.EventListener {
            override fun onTimelineChanged(timeline: Timeline, manifest: Any?, reason: Int) {

            }
            override fun onTracksChanged(
                trackGroups: TrackGroupArray,
                trackSelections: TrackSelectionArray
            ) {

            }

            override fun onLoadingChanged(isLoading: Boolean) {

            }
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        Log.e(TAG, "onPlayerStateChanged: Buffering video.")
                        if (progressBar != null) {
                            progressBar!!.visibility = VISIBLE
                        }
                    }
                    Player.STATE_ENDED -> {
                        Log.d(TAG, "onPlayerStateChanged: Video ended.")
                        videoPlayer!!.seekTo(0)
                    }
                    Player.STATE_IDLE -> {
                        if (progressBar != null) {
                            progressBar!!.visibility = GONE
                        }
                    }
                    Player.STATE_READY -> {
                        Log.e(TAG, "onPlayerStateChanged: Ready to play.")
                        if (progressBar != null) {
                            progressBar!!.visibility = GONE
                        }
                        if (!isVideoViewAdded) {
                            addVideoView()
                        }
                    }
                    else -> {}
                }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {}
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
            override fun onPlayerError(error: ExoPlaybackException) {}
            override fun onPositionDiscontinuity(reason: Int) {}
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {}
            override fun onSeekProcessed() {}
        })
    }

    private fun addVideoView() {
        frameLayout!!.addView(videoSurfaceView)
        isVideoViewAdded = true
        videoSurfaceView!!.requestFocus()
        videoSurfaceView!!.visibility = VISIBLE
        videoSurfaceView!!.alpha = 1f
        thumbnail!!.visibility = GONE
    }

    private fun resetVideoView() {
        if (isVideoViewAdded) {
            removeVideoView(videoSurfaceView!!)
            playPosition = -1
            videoSurfaceView!!.visibility = INVISIBLE
            thumbnail!!.visibility = VISIBLE
        }
    }

    private fun playVideo(isEndOfList: Boolean) {
        val targetPosition: Int
        if (!isEndOfList) {
            val startPosition =
                (layoutManager as LinearLayoutManager?)!!.findFirstVisibleItemPosition()
            var endPosition =
                (layoutManager as LinearLayoutManager?)!!.findLastVisibleItemPosition()

            // if there is more than 2 list-items on the screen, set the difference to be 1
            if (endPosition - startPosition > 1) {
                endPosition = startPosition + 1
            }

            // something is wrong. return.
            if (startPosition < 0 || endPosition < 0) {
                return
            }

            // if there is more than 1 list-item on the screen
            targetPosition = if (startPosition != endPosition) {
                val startPositionVideoHeight: Int = getVisibleVideoSurfaceHeight(startPosition)
                val endPositionVideoHeight: Int = getVisibleVideoSurfaceHeight(endPosition)
                if (startPositionVideoHeight > endPositionVideoHeight) startPosition else endPosition
            } else {
                startPosition
            }
        } else {
            targetPosition = mediaObjects.size - 1
        }

        Log.d(TAG, "playVideo: target position: $targetPosition")


        // video is already playing so return
        if (targetPosition == playPosition) {
            return
        }

        // set the position of the list-item that is to be played

        // set the position of the list-item that is to be played
        playPosition = targetPosition
        if (videoSurfaceView == null) {
            return
        }

        // remove any old surface views from previously playing videos

        // remove any old surface views from previously playing videos
        videoSurfaceView!!.visibility = INVISIBLE
        removeVideoView(videoSurfaceView!!)

        val currentPosition =
            targetPosition - (layoutManager as LinearLayoutManager?)!!.findFirstVisibleItemPosition()

        val child = getChildAt(currentPosition) ?: return

        val holder = child.tag as VideoPlayerViewHolder
        if (holder == null) {
            playPosition = -1
            return
        }
        thumbnail = holder.thumbnail
        progressBar = holder.progressBar
        volumeControl = holder.volumeControl
        viewHolderParent = holder.itemView
        requestManager = holder.requestManager
        frameLayout = holder.itemView.findViewById(R.id.media_container)

        videoSurfaceView!!.setPlayer(videoPlayer)

        viewHolderParent!!.setOnClickListener {
            toggleVolume()
        }

        val dataSourceFactory: DataSource.Factory = DefaultDataSourceFactory(
            context, Util.getUserAgent(context, "RecyclerView")
        )
        val mediaUrl: String = mediaObjects.get(targetPosition).media_url
        val videoSource: MediaSource = ExtractorMediaSource.Factory(dataSourceFactory)
            .createMediaSource(Uri.parse(mediaUrl))
        videoPlayer!!.prepare(videoSource)
        videoPlayer!!.playWhenReady = true
    }

    private fun toggleVolume() {
        if (videoPlayer != null) {
            if (volumeState == VolumeState.OFF) {
                Log.d(TAG, "togglePlaybackState: enabling volume.")
                setVolumeControl(VolumeState.ON)
            } else if (volumeState == VolumeState.ON) {
                Log.d(TAG, "togglePlaybackState: disabling volume.")
                setVolumeControl(VolumeState.OFF)
            }
        }
    }

    /**
     * Returns the visible region of the video surface on the screen.
     * if some is cut off, it will return less than the @videoSurfaceDefaultHeight
     * @param playPosition
     * @return
     */
    private fun getVisibleVideoSurfaceHeight(playPosition: Int): Int {
        val at =
            playPosition - (layoutManager as LinearLayoutManager?)!!.findFirstVisibleItemPosition()
        Log.d(TAG, "getVisibleVideoSurfaceHeight: at: $at")
        val child = getChildAt(at) ?: return 0
        val location = IntArray(2)
        child.getLocationInWindow(location)
        return if (location[1] < 0) {
            location[1] + videoSurfaceDefaultHeight
        } else {
            screenDefaultHeight - location[1]
        }
    }

    // Remove the old player
    private fun removeVideoView(videoView: PlayerView) {
        val parent = videoView.parent as? ViewGroup
        val index = parent?.indexOfChild(videoView)
        if (index != null) {
            if (index >= 0) {
                parent.removeViewAt(index)
                isVideoViewAdded = false
                viewHolderParent!!.setOnClickListener(null)
            }
        }
    }

    private fun initializePlayer() {
        val bandwidthMeter: BandwidthMeter = DefaultBandwidthMeter()
        val videoTrackSelectionFactory: TrackSelection.Factory =
            AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector: TrackSelector = DefaultTrackSelector(videoTrackSelectionFactory)

        videoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector)
        videoSurfaceView!!.useController = false
        videoSurfaceView!!.player = videoPlayer
        setVolumeControl(VolumeState.ON)
    }

    private fun setVolumeControl(state: VolumeState) {
        volumeState = state
        if (state == VolumeState.OFF) {
            videoPlayer!!.volume = 0f
            animateVolumeControl()
        } else if (state == VolumeState.ON) {
            videoPlayer!!.volume = 1f
            animateVolumeControl()
        }
    }

    private fun animateVolumeControl() {
        if (volumeControl != null) {
            volumeControl!!.bringToFront()
            if (volumeState == VolumeState.OFF) {
                requestManager?.load(R.drawable.ic_volume_off)
                    ?.into(volumeControl!!)
            } else if (volumeState == VolumeState.ON) {
                requestManager?.load(R.drawable.ic_volume_on)
                    ?.into(volumeControl!!)
            }
            volumeControl!!.animate().cancel()
            volumeControl!!.alpha = 1f
            volumeControl!!.animate()
                .alpha(0f)
                .setDuration(600)
                .startDelay = 1000
        }
    }

    fun setMediaObjects(mediaObject: ArrayList<MediaObject>) {
        mediaObjects = mediaObject
    }

    fun releasePlayer() {
        if (videoPlayer != null) {
            videoPlayer!!.release()
            videoPlayer = null
        }
        viewHolderParent = null
    }

}