package com.gorkemoji.nfctoolkit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gorkemoji.nfctoolkit.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.readButton.setOnClickListener { startActivity(Intent(this, ReadActivity::class.java)) }
        binding.writeButton.setOnClickListener { startActivity(Intent(this, WriteActivity::class.java)) }
    }
}