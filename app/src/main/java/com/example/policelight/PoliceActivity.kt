package com.example.policelight

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class PoliceActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = true
    private var phase = 0
    private var intervalMs = 280L
    private var currentMode = 0

    private lateinit var bulbs: Array<View>
    private lateinit var leftScreen: View
    private lateinit var rightScreen: View
    private lateinit var btns: Array<Button>

    private val RED   = 0xFFCC1111.toInt()
    private val BLUE  = 0xFF1144CC.toInt()
    private val BLACK = 0xFF000000.toInt()
    private val BTN_SEL = 0xFF555555.toInt()
    private val BTN_NOR = 0xFF333333.toInt()

    // 6种闪烁模式，每帧: [b0, b1, b2, b3, leftScreen, rightScreen]
    private val modes = arrayOf(
        // ① 红蓝交替
        arrayOf(
            intArrayOf(RED,RED,BLACK,BLACK,RED,BLACK),
            intArrayOf(BLACK,BLACK,BLUE,BLUE,BLACK,BLUE)
        ),
        // ② 同侧双闪
        arrayOf(
            intArrayOf(RED,RED,BLACK,BLACK,RED,BLACK),
            intArrayOf(BLACK,BLACK,BLACK,BLACK,BLACK,BLACK),
            intArrayOf(RED,RED,BLACK,BLACK,RED,BLACK),
            intArrayOf(BLACK,BLACK,BLACK,BLACK,BLACK,BLACK),
            intArrayOf(BLACK,BLACK,BLUE,BLUE,BLACK,BLUE),
            intArrayOf(BLACK,BLACK,BLACK,BLACK,BLACK,BLACK),
            intArrayOf(BLACK,BLACK,BLUE,BLUE,BLACK,BLUE),
            intArrayOf(BLACK,BLACK,BLACK,BLACK,BLACK,BLACK)
        ),
        // ③ 左右追逐
        arrayOf(
            intArrayOf(RED,BLACK,BLACK,BLACK,RED,BLACK),
            intArrayOf(BLACK,RED,BLACK,BLACK,RED,BLACK),
            intArrayOf(BLACK,BLACK,BLUE,BLACK,BLACK,BLUE),
            intArrayOf(BLACK,BLACK,BLACK,BLUE,BLACK,BLUE),
            intArrayOf(BLACK,BLACK,BLACK,BLACK,BLACK,BLACK)
        ),
        // ④ 内外扩散
        arrayOf(
            intArrayOf(BLACK,RED,BLUE,BLACK,RED,BLUE),
            intArrayOf(RED,BLACK,BLACK,BLUE,RED,BLUE),
            intArrayOf(BLACK,BLACK,BLACK,BLACK,BLACK,BLACK),
            intArrayOf(RED,BLACK,BLACK,BLUE,RED,BLUE),
            intArrayOf(BLACK,RED,BLUE,BLACK,RED,BLUE),
            intArrayOf(BLACK,BLACK,BLACK,BLACK,BLACK,BLACK)
        ),
        // ⑤ 单灯轮流
        arrayOf(
            intArrayOf(RED,BLACK,BLACK,BLACK,RED,BLACK),
            intArrayOf(BLACK,RED,BLACK,BLACK,RED,BLACK),
            intArrayOf(BLACK,BLACK,BLUE,BLACK,BLACK,BLUE),
            intArrayOf(BLACK,BLACK,BLACK,BLUE,BLACK,BLUE)
        ),
        // ⑥ 双色同亮
        arrayOf(
            intArrayOf(RED,RED,BLUE,BLUE,RED,BLUE),
            intArrayOf(BLACK,BLACK,BLACK,BLACK,BLACK,BLACK),
            intArrayOf(RED,RED,BLUE,BLUE,RED,BLUE),
            intArrayOf(BLACK,BLACK,BLACK,BLACK,BLACK,BLACK)
        )
    )

    private val flashRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            val frames = modes[currentMode]
            val f = frames[phase % frames.size]
            bulbs[0].setBackgroundColor(f[0])
            bulbs[1].setBackgroundColor(f[1])
            bulbs[2].setBackgroundColor(f[2])
            bulbs[3].setBackgroundColor(f[3])
            leftScreen.setBackgroundColor(f[4])
            rightScreen.setBackgroundColor(f[5])
            phase++
            handler.postDelayed(this, intervalMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.apply {
                hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }

        val attrs = window.attributes
        attrs.screenBrightness = 1.0f
        window.attributes = attrs

        setContentView(R.layout.activity_police)

        bulbs = arrayOf(
            findViewById(R.id.b0), findViewById(R.id.b1),
            findViewById(R.id.b2), findViewById(R.id.b3)
        )
        leftScreen  = findViewById(R.id.leftScreen)
        rightScreen = findViewById(R.id.rightScreen)

        btns = arrayOf(
            findViewById(R.id.btn0), findViewById(R.id.btn1),
            findViewById(R.id.btn2), findViewById(R.id.btn3),
            findViewById(R.id.btn4), findViewById(R.id.btn5)
        )
        for (i in btns.indices) {
            btns[i].setOnClickListener { switchMode(i) }
        }
        highlightBtn(0)

        startFlash()
    }

    private fun switchMode(mode: Int) {
        if (mode == currentMode) return
        currentMode = mode
        phase = 0
        highlightBtn(mode)
    }

    private fun highlightBtn(idx: Int) {
        for (i in btns.indices) {
            btns[i].setBackgroundColor(if (i == idx) BTN_SEL else BTN_NOR)
        }
    }

    private fun startFlash() {
        isRunning = true
        phase = 0
        handler.post(flashRunnable)
    }

    private fun stopFlash() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopFlash()
    }
}
