package com.example.firestick

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.firestick.connectionandcommand.DataStoreManager
import com.example.firestick.connectionandcommand.KeyEventCodes
import com.example.firestick.databinding.ActivityAirMouseBinding
import com.example.firestick.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class AirMouseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAirMouseBinding
    private val mainViewModel: MainViewModel by viewModels()
    private var previousX = 0f
    private var previousY = 0f
    private var isTouching = false
    private var isDragging = false
    private var isFirstMove = true
    private var touchStartTime: Long = 0
    private lateinit var vibrator: Vibrator
    private var lastCommandTime: Long = 0 // Track the time of the last command
    private val commandDelay = 200L // Minimum delay between commands (in milliseconds)

    private val threshold = 30f // Threshold for detecting substantial movement
    private val tapThreshold = 100 // Maximum time for tap detection in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAirMouseBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        val dataStoreManager = DataStoreManager.getInstance(application)
        lifecycleScope.launch {
            dataStoreManager.deviceIP.collect { deviceIP ->
                mainViewModel.deviceIP = deviceIP
            }
        }

        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


        binding.btnPlayback.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_MEDIA_PREVIOUS) }
        binding.btnPlayforward.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_MEDIA_NEXT) }

        binding.imgForward.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_MEDIA_FAST_FORWARD) }
        binding.imgBackward.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_MEDIA_REWIND) }

        binding.play.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_MEDIA_PLAY) }
        binding.playPause.setOnClickListener { runCommandWithDebounce(KeyEventCodes.KEYCODE_MEDIA_PLAY_PAUSE) }


        binding.cursorView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isTouching = true
                    touchStartTime = System.currentTimeMillis()
                    previousX = event.x
                    previousY = event.y
                    isFirstMove = true
                    isDragging = false
                    vibrateDevice() // Vibrate on touch down
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isTouching) {
                        val deltaX = Math.abs(event.x - previousX)
                        val deltaY = Math.abs(event.y - previousY)

                        // If the movement exceeds the threshold and it's the first move, we start dragging
                        if ((deltaX > threshold || deltaY > threshold) && !isDragging) {
                            isDragging = true // Mark the start of dragging
                            handleMouseDrag(event.x, event.y) // Call handleMouseDrag() once
                        }

                        // Update previous position for next comparison
                        previousX = event.x
                        previousY = event.y
                    }
                }

                MotionEvent.ACTION_UP -> {
                    isTouching = false
                    val touchEndTime = System.currentTimeMillis()
                    val touchDuration = touchEndTime - touchStartTime

                    // If it's a short touch and movement is within the threshold, consider it a tap
                    if (touchDuration < tapThreshold && isTap(event.x, event.y)) {
                        handleTap()
                    }

                    isDragging = false // Reset dragging flag after release
                    vibrateDevice() // Vibrate on touch up
                }
            }
            true
        }
    }

    // Detect drag direction based on previous and current touch position
    private fun getDirection(x: Float, y: Float): String {
        var direction = ""
        val deltaX = x - previousX
        val deltaY = y - previousY

        // Horizontal movement detection
        if (deltaX > threshold) {
            direction += "right"
        } else if (deltaX < -threshold) {
            direction += "left"
        }

        // Vertical movement detection
        if (deltaY > threshold) {
            direction += if (direction.isNotEmpty()) "down" else "down"
        } else if (deltaY < -threshold) {
            direction += if (direction.isNotEmpty()) "up" else "up"
        }

        return direction
    }

    // Handle the drag event and send mouse move command to the TV
    private fun handleMouseDrag(x: Float, y: Float) {
        // Only process drag if it is substantial movement (i.e., direction is not empty)
        val direction = getDirection(x, y)
        if (direction.isNotEmpty()) {
            sendMouseMovementToTV(direction)
        }
    }

    private fun sendMouseMovementToTV(direction: String) {
        // Check if enough time has passed since the last command was executed
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCommandTime < commandDelay) {
            // If the command is executed too fast, we ignore this input
            return
        }
        // Update the time of the last executed command
        lastCommandTime = currentTime

        when (direction) {
            "right" -> {
                // Handle moving to the right
                mainViewModel.runCommand(KeyEventCodes.KEYCODE_DPAD_RIGHT)
            }

            "left" -> {
                mainViewModel.runCommand(KeyEventCodes.KEYCODE_DPAD_LEFT)
            }

            "up" -> {
                // Handle moving upwards
                mainViewModel.runCommand(KeyEventCodes.KEYCODE_DPAD_UP)
            }

            "down" -> {
                // Handle moving downwards
                mainViewModel.runCommand(KeyEventCodes.KEYCODE_DPAD_DOWN)
            }

            "leftup" -> {
                // Handle moving left and up diagonally
                mainViewModel.runCommand(KeyEventCodes.KEYCODE_DPAD_UP)
            }

            "leftdown" -> {
                // Handle moving left and down diagonally
                mainViewModel.runCommand(KeyEventCodes.KEYCODE_DPAD_DOWN)
            }

            "rightup" -> {
                // Handle moving right and up diagonally
                mainViewModel.runCommand(KeyEventCodes.KEYCODE_DPAD_UP)
            }

            "rightdown" -> {
                // Handle moving right and down diagonally
                mainViewModel.runCommand(KeyEventCodes.KEYCODE_DPAD_DOWN)
            }
        }
    }

    // Provide haptic feedback for touch events
    private fun vibrateDevice() {
        if (vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(100)
            }
        }
    }

    // Check if the touch event is a tap (small movement within threshold)
    private fun isTap(x: Float, y: Float): Boolean {
        val deltaX = Math.abs(x - previousX)
        val deltaY = Math.abs(y - previousY)
        return deltaX < threshold && deltaY < threshold
    }

    // Handle tap event
    private fun handleTap() {
        mainViewModel.runCommand(KeyEventCodes.KEYCODE_BUTTON_START)
    }

    private fun runCommandWithDebounce(command: Int) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastCommandTime >= commandDelay) {
            lastCommandTime = currentTime
            mainViewModel.runCommand(command)
        }
    }
}
