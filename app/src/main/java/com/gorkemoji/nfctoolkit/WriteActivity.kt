package com.gorkemoji.nfctoolkit

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NdefFormatable
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.gorkemoji.nfctoolkit.databinding.ActivityWriteBinding

class WriteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWriteBinding
    private lateinit var pendingIntent: PendingIntent
    private var nfcAdapter: NfcAdapter? = null
    private var bottomSheet: NfcPromptBottomSheet? = null
    private var writeEnabled = false
    private var formatEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Toast.makeText(this, getString(R.string.nfc_not_available), Toast.LENGTH_SHORT).show()
            finish()
            return
        } else if (nfcAdapter?.isEnabled == false) {
            Toast.makeText(this, getString(R.string.nfc_disabled), Toast.LENGTH_SHORT).show()
            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
        }

        pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE)

        binding.textChip.isChecked = true
        binding.writeButton.isEnabled = false

        binding.editText.addTextChangedListener {
            val input = it?.toString()?.trim()

            binding.writeButton.isEnabled = when {
                input.isNullOrEmpty() -> false
                binding.urlChip.isChecked -> isValidUrl(input)
                else -> true
            }
        }

        binding.formatButton.setOnClickListener {
            formatEnabled = true
            writeEnabled = false
            showPrompt()
        }

        binding.writeButton.setOnClickListener {
            formatEnabled = false
            writeEnabled = true
            showPrompt()
        }

        binding.chipGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == binding.urlChip.id) {
                binding.textChip.isChecked = false
                binding.editText.hint = getString(R.string.url)
                binding.editText.setText("")
            } else if (checkedId == binding.textChip.id) {
                binding.urlChip.isChecked = false
                binding.editText.hint = getString(R.string.data_content)
                binding.editText.setText("")
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
        tag?.let {
            if (writeEnabled) {
                runOnUiThread { vibrateOnTagRead() }
                writeToTag(it)
                writeEnabled = false
            }
            else if (formatEnabled) {
                runOnUiThread { vibrateOnTagRead() }
                formatTag(it)
                formatEnabled = false
            }
        }
    }

    private fun vibrateOnTagRead() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) vibrator.vibrate(
            VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        else vibrator.vibrate(200)
    }

    private fun showPrompt() {
        bottomSheet = NfcPromptBottomSheet()
        bottomSheet?.show(supportFragmentManager, "NFC_PROMPT")
    }

    private fun hidePrompt() { bottomSheet?.dismissSafely() }

    private fun formatTag(tag: Tag) {
        try {
            val formatable = NdefFormatable.get(tag)

            if (formatable != null) {
                formatable.connect()
                val empty = NdefMessage(arrayOf(NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)))
                formatable.format(empty)
                formatable.close()

                bottomSheet?.changeDialog(getString(R.string.format_success), R.drawable.ic_done_48) { hidePrompt() }
            } else bottomSheet?.changeDialog(getString(R.string.format_fail), R.drawable.ic_error_48) { hidePrompt() }
        } catch (e: Exception) { bottomSheet?.changeDialog(getString(R.string.format_fail) + e.message, R.drawable.ic_error_48) }
    }

    private fun isValidUrl(input: String): Boolean {
        val url = if (!input.startsWith("http://") && !input.startsWith("https://")) "https://$input"
        else input

        return android.util.Patterns.WEB_URL.matcher(url).matches()
    }

    private fun writeToTag(tag: Tag) {
        val rawText = binding.editText.text.toString().trim()
        val text = if (binding.urlChip.isChecked) {
            if (!isValidUrl(rawText)) {
                bottomSheet?.changeDialog(getString(R.string.invalid_url), R.drawable.ic_error_48) { hidePrompt() }
                return
            }
            if (!rawText.startsWith("http://") && !rawText.startsWith("https://")) "https://$rawText"
            else rawText
        } else rawText

        if (text.isNotEmpty()) {
            try {
                val record: NdefRecord = if (binding.urlChip.isChecked) NdefRecord.createUri(text)
                else NdefRecord.createTextRecord(null, text)

                val message = NdefMessage(arrayOf(record))
                val ndef = android.nfc.tech.Ndef.get(tag)

                if (ndef != null) {
                    ndef.connect()
                    if (ndef.isWritable) {
                        if (ndef.maxSize < message.toByteArray().size) {
                            bottomSheet?.changeDialog(getString(R.string.write_fail_too_large), R.drawable.ic_error_48) { hidePrompt() }
                            return
                        }

                        ndef.writeNdefMessage(message)
                        ndef.close()

                        bottomSheet?.changeDialog(getString(R.string.write_success), R.drawable.ic_done_48) { hidePrompt() }
                    } else bottomSheet?.changeDialog(getString(R.string.write_fail_not_writable), R.drawable.ic_error_48) { hidePrompt() }

                } else bottomSheet?.changeDialog(getString(R.string.write_fail), R.drawable.ic_error_48) { hidePrompt() }
            } catch (e: Exception) {
                bottomSheet?.changeDialog("${getString(R.string.write_fail)}: $e", R.drawable.ic_error_48) { hidePrompt() }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }
}