package com.gorkemoji.nfctoolkit

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gorkemoji.nfctoolkit.databinding.ActivityReadBinding
import androidx.core.view.isVisible

class ReadActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    private var nfcAdapter: NfcAdapter? = null
    private lateinit var binding: ActivityReadBinding
    private var blinkHandler: Handler? = null
    private var blinkRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available on this device", Toast.LENGTH_SHORT).show()
            finish()
            return
        } else if (nfcAdapter?.isEnabled == false) {
            Toast.makeText(this, "NFC is disabled. Enable it to use this app", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
        } else Toast.makeText(this, "NFC is enabled.", Toast.LENGTH_SHORT).show()

        binding.contentText.visibility = View.GONE
        binding.contentDesc.visibility = View.GONE

        blinkHandler = Handler(mainLooper)
        blinkRunnable = object : Runnable {
            override fun run() {
                binding.nfcIcon.visibility = if (binding.nfcIcon.isVisible) View.INVISIBLE else View.VISIBLE
                blinkHandler?.postDelayed(this, 500)
            }
        }
        blinkHandler?.post(blinkRunnable!!)


    }

    override fun onTagDiscovered(tag: Tag?) {
        runOnUiThread {
            binding.title.text = "Detected a tag!"
            binding.headerText.text = "Your device detected a tag. You can see the details below."
            binding.nfcIcon.visibility = View.GONE
            binding.contentText.visibility = View.VISIBLE
            binding.contentDesc.visibility = View.VISIBLE
            binding.contentDesc.text = tag.toString()
            blinkHandler?.removeCallbacks(blinkRunnable!!)
        }
    }

    override fun onResume() {
        super.onResume()
        if (nfcAdapter?.isEnabled == false) finish()

        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NFC_BARCODE or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
            null
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
        blinkHandler?.removeCallbacks(blinkRunnable!!)
    }
}