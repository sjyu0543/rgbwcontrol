package com.example.rgbwcontrol

import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import vendor.qti.hardware.rgbw.IVendorRgbwControl

class MainActivity : AppCompatActivity() {

    private var mService: IVendorRgbwControl? = null
    private var r = 0
    private var g = 0
    private var b = 0
    private var w = 0

    // Debounce 用：延遲 300ms 後才送出指令
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = Runnable { sendLedCommand() }
    private val DEBOUNCE_MS = 300L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initRgbwService()

        val sbRed   = findViewById<SeekBar>(R.id.sb_red)
        val sbGreen = findViewById<SeekBar>(R.id.sb_green)
        val sbBlue  = findViewById<SeekBar>(R.id.sb_blue)
        val sbWhite = findViewById<SeekBar>(R.id.sb_white)

        val etRed   = findViewById<EditText>(R.id.tv_red_value)
        val etGreen = findViewById<EditText>(R.id.tv_green_value)
        val etBlue  = findViewById<EditText>(R.id.tv_blue_value)
        val etWhite = findViewById<EditText>(R.id.tv_white_value)

        setupChannel(sbRed,   etRed)   { r = it }
        setupChannel(sbGreen, etGreen) { g = it }
        setupChannel(sbBlue,  etBlue)  { b = it }
        setupChannel(sbWhite, etWhite) { w = it }
    }

    private fun setupChannel(
        seekBar: SeekBar,
        editText: EditText,
        onValueChanged: (Int) -> Unit
    ) {
        seekBar.max = 350

        // SeekBar → EditText
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    onValueChanged(progress)
                    if (editText.text.toString() != progress.toString()) {
                        editText.setText(progress.toString())
                    }
                    scheduleUpdate()  // 取代直接呼叫 updateLeds()
                }
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            // 手指放開時立即送出，確保最終值一定會更新
            override fun onStopTrackingTouch(s: SeekBar?) {
                handler.removeCallbacks(updateRunnable)
                sendLedCommand()
            }
        })

        // EditText → SeekBar
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(e: Editable?) {
                val input = e?.toString()?.toIntOrNull() ?: return
                val clamped = input.coerceIn(0, 350)
                onValueChanged(clamped)
                if (seekBar.progress != clamped) {
                    seekBar.progress = clamped
                }
                scheduleUpdate()  // 取代直接呼叫 updateLeds()
            }
        })
    }

    /**
     * 重置計時器：每次有新變動就重新倒數 300ms
     * 若在 300ms 內又有變動，舊的排程會被取消
     */
    private fun scheduleUpdate() {
        handler.removeCallbacks(updateRunnable)
        handler.postDelayed(updateRunnable, DEBOUNCE_MS)
    }

    private fun sendLedCommand() {
        try {
            mService?.setLedColor(r, g, b, w)
            Log.d("RGBW_TEST", "sendLedColor r=$r g=$g b=$b w=$w")
        } catch (e: Exception) {
            Log.e("RGBW_TEST", "傳送指令失敗: ${e.message}")
        }
    }

    private fun initRgbwService() {
        try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val binder = getServiceMethod.invoke(
                null,
                "vendor.qti.hardware.rgbw.IVendorRgbwControl/default"
            ) as? IBinder

            if (binder != null) {
                mService = IVendorRgbwControl.Stub.asInterface(binder)
                Log.d("RGBW_TEST", "成功連線至硬體服務")
            } else {
                Log.e("RGBW_TEST", "找不到服務，請檢查 Service 是否啟動")
            }
        } catch (e: Exception) {
            Log.e("RGBW_TEST", "反射呼叫失敗: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)  // 避免 Activity 銷毀後還在執行
    }
}