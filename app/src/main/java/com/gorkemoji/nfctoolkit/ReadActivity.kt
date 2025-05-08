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

class ReadActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    private lateinit var binding: ActivityReadBinding
    private var nfcAdapter: NfcAdapter? = null
    private var bottomSheet: NfcPromptBottomSheet? = null
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
        }

        binding.contentText.visibility = View.GONE
        binding.contentDesc.visibility = View.GONE

        showPrompt()

        binding.readButton.setOnClickListener { showPrompt() }
    }

    private fun enableReaderMode() {
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

    private fun showPrompt() {
        bottomSheet = NfcPromptBottomSheet()
        bottomSheet?.show(supportFragmentManager, "NFC_PROMPT")
        enableReaderMode()
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (bottomSheet?.isVisible != true) return

        if (tag == null) {
            runOnUiThread {
                bottomSheet?.changeDialog(getString(R.string.read_failed), R.drawable.ic_error_48) {
                    disableReaderMode()
                }
            }
            return
        }

        runOnUiThread {
            vibrateOnTagRead()
            binding.title.text = getString(R.string.detected_a_tag)
            binding.headerText.text = getString(R.string.tag_information_text)
            binding.contentText.visibility = View.VISIBLE
            binding.contentDesc.visibility = View.VISIBLE
        }

        val tagId = tag.id?.joinToString(":") { String.format("%02X", it) }
            ?: getString(R.string.unknown_id)
        val techList = tag.techList.joinToString(", ")
        val info =
            getString(R.string.tag_id) + ": " + tagId + "\n" + getString(R.string.supported_tech) + ": " + techList

        runOnUiThread {
            binding.contentDesc.text = info
            bottomSheet?.changeDialog(getString(R.string.read_success), R.drawable.ic_done_48) {
                disableReaderMode()
            }
        }
    }

    private fun disableReaderMode() { nfcAdapter?.disableReaderMode(this) }

    private fun vibrateOnTagRead() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        else vibrator.vibrate(200)
    }

    override fun onResume() {
        super.onResume()
        if (nfcAdapter?.isEnabled == false) finish()
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
        blinkHandler?.removeCallbacks(blinkRunnable!!)
    }
}