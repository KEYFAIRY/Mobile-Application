package com.example.keyfairy.feature_check_video.presentation

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import com.example.keyfairy.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CheckVideoFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CheckVideoFragment : Fragment() {
    private lateinit var videoView: VideoView
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button

    private var videoUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_check_video, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get video URI from arguments
        videoUri = arguments?.getParcelable(ARG_VIDEO_URI)

        if (videoUri == null) {
            Toast.makeText(requireContext(), "No video to display", Toast.LENGTH_SHORT).show()
            return
        }

        // Initialize views
        videoView = view.findViewById(R.id.videoView)
        btnSave = view.findViewById(R.id.btnSave)
        btnDelete = view.findViewById(R.id.btnDelete)

        setupVideoPlayer()
        setupButtons()
    }

    companion object {
        private const val ARG_VIDEO_URI = "video_uri"

        fun newInstance(videoUri: Uri): CheckVideoFragment {
            val fragment = CheckVideoFragment()
            val args = Bundle().apply {
                putParcelable(ARG_VIDEO_URI, videoUri)
            }
            fragment.arguments = args
            return fragment
        }
    }

    private fun setupVideoPlayer() {
        videoUri?.let { uri ->
            // Set up media controller for play/pause controls
            val mediaController = MediaController(requireContext())
            mediaController.setAnchorView(videoView)
            videoView.setMediaController(mediaController)

            // Set video URI and prepare
            videoView.setVideoURI(uri)

            videoView.setOnPreparedListener { mediaPlayer ->
                Log.d("VideoPlayback", "Video prepared and ready to play")
                videoView.start() // Auto-play when ready
            }

            videoView.setOnErrorListener { _, what, extra ->
                Log.e("VideoPlayback", "Error playing video: what=$what, extra=$extra")
                Toast.makeText(requireContext(), "Error playing video", Toast.LENGTH_SHORT).show()
                true
            }

            videoView.setOnCompletionListener {
                Log.d("VideoPlayback", "Video playback completed")
            }
        }
    }
    private fun setupButtons() {
        btnSave.setOnClickListener {
            // Handle save action
            videoUri?.let { uri ->
                Log.d("VideoPlayback", "Save button clicked for URI: $uri")
                Toast.makeText(requireContext(), "Video saved to gallery", Toast.LENGTH_SHORT).show()
                // Video is already saved to MediaStore, so this could navigate back
                // or show confirmation, or copy to different location
                parentFragmentManager.popBackStack()
            }
        }

        btnDelete.setOnClickListener {
            // Handle delete action
            videoUri?.let { uri ->
                Log.d("VideoPlayback", "Delete button clicked for URI: $uri")
                deleteVideo(uri)
            }
        }
    }
    private fun deleteVideo(uri: Uri) {
        try {
            val deleted = requireContext().contentResolver.delete(uri, null, null)
            if (deleted > 0) {
                Log.d("VideoPlayback", "Video deleted successfully")
                Toast.makeText(requireContext(), "Video deleted", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            } else {
                Log.w("VideoPlayback", "Failed to delete video")
                Toast.makeText(requireContext(), "Failed to delete video", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("VideoPlayback", "Error deleting video: ${e.message}")
            Toast.makeText(requireContext(), "Error deleting video", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        videoView.pause()
    }

    override fun onResume() {
        super.onResume()
        if (videoView.isPlaying.not()) {
            videoView.resume()
        }
    }
}