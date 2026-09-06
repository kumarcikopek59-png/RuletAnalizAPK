package com.serkan.ruletanaliz

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private val wheel = intArrayOf(
        0,32,15,19,4,21,2,25,17,34,6,27,13,36,11,30,8,23,10,
        5,24,16,33,1,20,14,31,9,22,18,29,7,28,12,35,3,26
    )
    private val seeded = listOf(
        23,33,12,15,7,18,30,3,12,19,32,24,30,2,
        33,24,20,18,12,11,14,13,22,3,9,4,
        12,19,29,1,0,34,19,2,2,12,21,6,
        20,27,20,30,23,23,16,31,23,13,4,16,
        5,8,25,31,24,11,25,0,10,31,32,2,
        14,32,18,2,6,17,22,9,20,27,4,2,
        19,29,31,22,26,28,6,36,0,20,15,7,
        8,5,36,18,21,25,1,35,3,21,26,3,
        30,9,31,2,30,12,29,27,19,2,22,29,
        12,36,13,26,1,1,25,36,17,33,6,19,
        18,21,14,10,0,16,3,3,22,35,18,1,
        6,33,14,20,9,6,4,35,2,35,30,7,
        29,1,28,13,35,6,10,19,8,27,19,33,
        32,15,35,11,27,23,8,19,20,33,4,12,
        24,18,31,33,1,20,36,0,14,23,25,2,
        26,32,8,11,18,2,19,7,31,10,7,33,
        23,3,11,15,3,28,34,9,9,15,1,20,
        28,6,27,4,26,28,21,2,14,30,30,27,
        16,27,13,12,2,19,14,1,1,5,31,35,
        9,10,29,8,30,5,4,5,22,31,14,24,
        17,35,23,4,1,31,4,0,9,31,6,4,
        11,8,10,9,30,1,35,16,31,27,23,9,
        1,34,35,17,15,20,11,5,2,17,5,20,
        17,7,36,6,5,20,14,23
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 35, 28, 24)
            setBackgroundColor(Color.rgb(17,17,17))
        }

        val title = TextView(this).apply {
            text = "🎯 Rulet Analiz"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        root.addView(title, LinearLayout.LayoutParams(-1, 70))

        val info = TextView(this).apply {
            text = "Son gelen sayıyı gir. Uygulama geçmiş verideki komşuluk + frekans ağırlığına göre 3 bölge çıkarır."
            textSize = 15f
            setTextColor(Color.LTGRAY)
            setPadding(0, 10, 0, 20)
        }
        root.addView(info)

        val input = EditText(this).apply {
            hint = "Son sayı (0–36)"
            inputType = InputType.TYPE_CLASS_NUMBER
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            setSingleLine(true)
        }
        root.addView(input, LinearLayout.LayoutParams(-1, 60))

        val button = Button(this).apply {
            text = "3 BÖLGEYİ HESAPLA"
        }
        root.addView(button, LinearLayout.LayoutParams(-1, 65))

        val result = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, 30, 0, 0)
        }
        root.addView(result)

        val note = TextView(this).apply {
            text = "Not: Bu bir istatistiksel analiz aracıdır; rulette sonraki sonucu garanti etmez."
            textSize = 13f
            setTextColor(Color.GRAY)
            setPadding(0, 30, 0, 0)
        }
        root.addView(note)

        button.setOnClickListener {
            val n = input.text.toString().toIntOrNull()
            if (n == null || n !in 0..36) {
                input.error = "0 ile 36 arasında bir sayı gir"
                return@setOnClickListener
            }
            result.text = calculate(n)
        }
        setContentView(root)
    }

    private fun calculate(last: Int): String {
        val idx = wheel.indexOf(last)
        val freq = IntArray(37)
        seeded.forEach { if (it in 0..36) freq[it]++ }

        // Score each wheel sector by local historical frequency plus
        // how often numbers near the current number appeared in the seed.
        data class Candidate(val number:Int, val score:Double)
        val candidates = mutableListOf<Candidate>()
        for (offset in -8..8) {
            if (offset == 0) continue
            val n = wheel[(idx + offset + wheel.size) % wheel.size]
            val local = (freq[n] + 1).toDouble()
            val neighborFreq = (-2..2).sumOf { d ->
                val x = wheel[(idx + offset + d + wheel.size) % wheel.size]
                freq[x]
            }.toDouble()
            val distanceWeight = 1.0 / (1.0 + abs(offset) * 0.18)
            candidates.add(Candidate(n, local * 0.7 + neighborFreq * 0.3 * distanceWeight))
        }

        val ranked = candidates.sortedByDescending { it.score }
        val used = mutableSetOf<Int>()
        val zones = mutableListOf<List<Int>>()

        for (c in ranked) {
            if (zones.size == 3) break
            val ci = wheel.indexOf(c.number)
            val zone = (-2..2).map { wheel[(ci + it + wheel.size) % wheel.size] }
            if (zone.any { used.contains(it) }) continue
            zones.add(zone)
            used.addAll(zone)
        }

        while (zones.size < 3) {
            val start = (idx + zones.size * 5 + 1) % wheel.size
            zones.add((-2..2).map { wheel[(start + it + wheel.size) % wheel.size] })
        }

        return buildString {
            append("SON SAYI: $last\n\n")
            zones.forEachIndexed { i, z ->
                append("${i+1}. BÖLGE → ${z.joinToString(" - ")}\n")
            }
            append("\nMerkez adayları: ")
            append(zones.map { it[2] }.joinToString(", "))
        }
    }
}
