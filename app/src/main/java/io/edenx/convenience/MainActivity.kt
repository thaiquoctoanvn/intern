package io.edenx.convenience

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.edenx.convenience.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.bt.setOnClickListener {
            CommonFunc.openStoreListing(this, "io.lacanh.aiassistant")
        }
    }
}