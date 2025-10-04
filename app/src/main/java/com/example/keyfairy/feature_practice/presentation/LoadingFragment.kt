package com.example.keyfairy.feature_practice.presentation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import coil.load
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.keyfairy.MainActivity
import com.example.keyfairy.R
import com.example.keyfairy.databinding.FragmentLoadingBinding
import com.example.keyfairy.feature_home.presentation.HomeFragment

class LoadingFragment : Fragment() {
    private var _binding: FragmentLoadingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()

        binding.imageViewLogo.load(R.drawable.loading_wheel) {
            crossfade(true)
            decoderFactory(
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    ImageDecoderDecoder.Factory()
                } else {
                    GifDecoder.Factory()
                }
            )
        }

        binding.buttonContinue.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(MusicalErrorFragment(), true)
        }
        binding.buttonHome.setOnClickListener {
            (activity as? MainActivity)?.replaceFragment(HomeFragment(), true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
