package com.gorkemoji.nfctoolkit

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gorkemoji.nfctoolkit.databinding.ActivityWriteBinding

class WriteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWriteBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}