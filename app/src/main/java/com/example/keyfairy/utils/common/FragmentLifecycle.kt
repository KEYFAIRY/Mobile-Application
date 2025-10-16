package com.example.keyfairy.utils.common

import androidx.fragment.app.Fragment
import android.util.Log

abstract class BaseFragment : Fragment() {

    protected var isFragmentActive = false
        private set

    protected var hasNavigatedAway = false
        private set

    override fun onResume() {
        super.onResume()
        isFragmentActive = true
        hasNavigatedAway = false
        Log.d(this::class.java.simpleName, "Fragment resumed - Active: $isFragmentActive")
    }

    override fun onPause() {
        super.onPause()
        isFragmentActive = false
        Log.d(this::class.java.simpleName, "Fragment paused - Active: $isFragmentActive")
    }

    override fun onStop() {
        super.onStop()
        isFragmentActive = false
        hasNavigatedAway = true
        Log.d(this::class.java.simpleName, "Fragment stopped - Navigated away: $hasNavigatedAway")
    }

    protected fun safeNavigate(action: () -> Unit) {
        if (isAdded && !isDetached && !hasNavigatedAway) {
            action()
        } else {
            Log.w(this::class.java.simpleName, "Attempted navigation on inactive fragment")
        }
    }
}