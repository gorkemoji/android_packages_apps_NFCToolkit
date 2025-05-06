package com.gorkemoji.nfctoolkit

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.gorkemoji.nfctoolkit.databinding.BottomSheetNfcPromptBinding

class NfcPromptBottomSheet : BottomSheetDialogFragment() {
    private lateinit var binding: BottomSheetNfcPromptBinding
    private var blinkHandler: Handler? = null
    private var blinkRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetNfcPromptBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun startBlinking() {
        blinkHandler = Handler(Looper.getMainLooper())
        blinkRunnable = object : Runnable {
            private var visible = true
            override fun run() {
                binding.nfcIcon.visibility = if (visible) View.INVISIBLE else View.VISIBLE
                visible = !visible
                blinkHandler?.postDelayed(this, 500)
            }
        }
        blinkHandler?.post(blinkRunnable!!)
    }

    private fun stopBlinking() {
        blinkHandler?.removeCallbacks(blinkRunnable!!)
        blinkHandler = null
        blinkRunnable = null
        binding.nfcIcon.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        startBlinking()
    }

    override fun onStop() {
        super.onStop()
        stopBlinking()
    }

    fun dismissSafely() { if (isAdded) dismiss() }
}
