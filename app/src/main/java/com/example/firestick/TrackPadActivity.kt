package com.example.firestick

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.firestick.databinding.ActivityMainBinding
import com.example.firestick.databinding.ActivityTrackPadBinding
import com.example.firestick.viewmodel.MainViewModel

class TrackPadActivity : AppCompatActivity() {

    lateinit var binding: ActivityTrackPadBinding
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackPadBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        showSoftKeyboard(binding.edName)

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.edName.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                // If Enter is pressed, generate the ADB command for the whole text input
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    val text = binding.edName.text.toString()
                    val adbCommand = generateADBCommandFromText(text)
                    mainViewModel.runKeyBoardCommand(adbCommand)
                }
            }
            false
        }
    }

    // Generate ADB command from the full text
    private fun generateADBCommandFromText(text: String): String {
        return "shell input text \"$text\""
    }

    private fun showSoftKeyboard(view: View) {
        view.requestFocus()
        val imm = getSystemService(InputMethodManager::class.java)
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)


    }
}