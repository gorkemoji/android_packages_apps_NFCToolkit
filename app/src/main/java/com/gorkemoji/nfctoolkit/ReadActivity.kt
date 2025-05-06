package com.gorkemoji.nfctoolkit

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.os.Handler
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gorkemoji.nfctoolkit.databinding.ActivityReadBinding
import androidx.core.view.isVisible

class ReadActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    private lateinit var binding: ActivityReadBinding
    private var nfcAdapter: NfcAdapter? = null
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
        tag ?: return

        runOnUiThread {
            vibrateOnTagRead()
            binding.title.text = getString(R.string.detected_a_tag)
            binding.headerText.text = getString(R.string.tag_information_text)
            binding.nfcIcon.visibility = View.GONE
            binding.contentText.visibility = View.VISIBLE
            binding.contentDesc.visibility = View.VISIBLE
            blinkHandler?.removeCallbacks(blinkRunnable!!)
        }

        val tagId = tag.id?.joinToString(":") { String.format("%02X", it) } ?: getString(R.string.unknown_id)
        val techList = tag.techList.joinToString(", ")

        val info = getString(R.string.tag_id) + ": " + tagId + "\n" + getString(R.string.supported_tech) + ": " + techList

        runOnUiThread { binding.contentDesc.text = info }
    }

    private fun vibrateOnTagRead() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        else vibrator.vibrate(200)
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