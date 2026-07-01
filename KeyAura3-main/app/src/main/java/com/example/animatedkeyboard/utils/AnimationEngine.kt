package com.example.animatedkeyboard.utils

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlin.math.pow
import kotlin.random.Random

class AnimationEngine {
    private val activeAnimations = mutableListOf<GradientAnimation>()
    private val random = Random(System.currentTimeMillis())

    fun triggerAnimation(x: Float, y: Float, keyLabel: String) {
        val colors = getGradientColorsForKey(keyLabel)
        activeAnimations.add(GradientAnimation(x, y, colors))
    }

    fun update(elapsedTimeMs: Long) {
        activeAnimations.removeAll { anim ->
            anim.update(elapsedTimeMs)
            anim.isFinished
        }
    }

    fun draw(canvas: Canvas) {
        for (animation in activeAnimations) {
            animation.draw(canvas)
        }
    }

    fun hasActiveAnimations(): Boolean {
        return activeAnimations.isNotEmpty()
    }

    private fun getGradientColorsForKey(key: String): IntArray {
        return when(key.lowercase()) {
            "a" -> intArrayOf(Color.parseColor("#FF5050"), Color.parseColor("#FF6432"), Color.parseColor("#FF9600"), Color.parseColor("#FF6400"), Color.TRANSPARENT)
            "b" -> intArrayOf(Color.parseColor("#3296FF"), Color.parseColor("#32C8FF"), Color.parseColor("#0096FF"), Color.parseColor("#0064C8"), Color.TRANSPARENT)
            "c" -> intArrayOf(Color.parseColor("#FFDC00"), Color.parseColor("#FFF032"), Color.parseColor("#FFB400"), Color.parseColor("#C89600"), Color.TRANSPARENT)
            "d" -> intArrayOf(Color.parseColor("#00FF96"), Color.parseColor("#32FFB4"), Color.parseColor("#00C896"), Color.parseColor("#009664"), Color.TRANSPARENT)
            "e" -> intArrayOf(Color.parseColor("#FF00DC"), Color.parseColor("#FF32F0"), Color.parseColor("#C800C8"), Color.parseColor("#960096"), Color.TRANSPARENT)
            "f" -> intArrayOf(Color.parseColor("#FFA000"), Color.parseColor("#FFC832"), Color.parseColor("#FF7800"), Color.parseColor("#C86400"), Color.TRANSPARENT)
            "g" -> intArrayOf(Color.parseColor("#B432FF"), Color.parseColor("#C864FF"), Color.parseColor("#9600FF"), Color.parseColor("#6400C8"), Color.TRANSPARENT)
            "h" -> intArrayOf(Color.parseColor("#00FFFF"), Color.parseColor("#32FFFF"), Color.parseColor("#00C8C8"), Color.parseColor("#009696"), Color.TRANSPARENT)
            "i" -> intArrayOf(Color.parseColor("#FF6464"), Color.parseColor("#FF9696"), Color.parseColor("#C83232"), Color.parseColor("#963232"), Color.TRANSPARENT)
            "j" -> intArrayOf(Color.parseColor("#64FF64"), Color.parseColor("#96FF96"), Color.parseColor("#32C832"), Color.parseColor("#329632"), Color.TRANSPARENT)
            "k" -> intArrayOf(Color.parseColor("#FFFF32"), Color.parseColor("#FFFF78"), Color.parseColor("#C8C800"), Color.parseColor("#969600"), Color.TRANSPARENT)
            "l" -> intArrayOf(Color.parseColor("#FF64C8"), Color.parseColor("#FF96DC"), Color.parseColor("#C83296"), Color.parseColor("#963264"), Color.TRANSPARENT)
            "m" -> intArrayOf(Color.parseColor("#64C8FF"), Color.parseColor("#96DCFF"), Color.parseColor("#3296C8"), Color.parseColor("#326496"), Color.TRANSPARENT)
            "n" -> intArrayOf(Color.parseColor("#FFC832"), Color.parseColor("#FFDC64"), Color.parseColor("#C89632"), Color.parseColor("#966400"), Color.TRANSPARENT)
            "o" -> intArrayOf(Color.parseColor("#DC64FF"), Color.parseColor("#F096FF"), Color.parseColor("#B432DC"), Color.parseColor("#8200B4"), Color.TRANSPARENT)
            "p" -> intArrayOf(Color.parseColor("#32DCFF"), Color.parseColor("#64F0FF"), Color.parseColor("#00B4DC"), Color.parseColor("#0082B4"), Color.TRANSPARENT)
            "q" -> intArrayOf(Color.parseColor("#FF5050"), Color.parseColor("#FF8282"), Color.parseColor("#C83232"), Color.parseColor("#960000"), Color.TRANSPARENT)
            "r" -> intArrayOf(Color.parseColor("#50FF96"), Color.parseColor("#82FFBE"), Color.parseColor("#32C864"), Color.parseColor("#009650"), Color.TRANSPARENT)
            "s" -> intArrayOf(Color.parseColor("#FFF032"), Color.parseColor("#FFFF78"), Color.parseColor("#C8C800"), Color.parseColor("#968200"), Color.TRANSPARENT)
            "t" -> intArrayOf(Color.parseColor("#C864FF"), Color.parseColor("#DC96FF"), Color.parseColor("#A032DC"), Color.parseColor("#6E00AA"), Color.TRANSPARENT)
            "u" -> intArrayOf(Color.parseColor("#64FFC8"), Color.parseColor("#96FFE6"), Color.parseColor("#32C8A0"), Color.parseColor("#00966E"), Color.TRANSPARENT)
            "v" -> intArrayOf(Color.parseColor("#FFA064"), Color.parseColor("#FFC896"), Color.parseColor("#C86432"), Color.parseColor("#963C00"), Color.TRANSPARENT)
            "w" -> intArrayOf(Color.parseColor("#64A0FF"), Color.parseColor("#96C8FF"), Color.parseColor("#3278DC"), Color.parseColor("#0050B4"), Color.TRANSPARENT)
            "x" -> intArrayOf(Color.parseColor("#FFFF96"), Color.parseColor("#FFFFDC"), Color.parseColor("#DCDC32"), Color.parseColor("#AAAA00"), Color.TRANSPARENT)
            "y" -> intArrayOf(Color.parseColor("#FF5096"), Color.parseColor("#FF82BE"), Color.parseColor("#C81E78"), Color.parseColor("#960050"), Color.TRANSPARENT)
            "z" -> intArrayOf(Color.parseColor("#50FFDC"), Color.parseColor("#82FFF0"), Color.parseColor("#1EC8B4"), Color.parseColor("#009682"), Color.TRANSPARENT)
            "0" -> intArrayOf(Color.parseColor("#FFFFFF"), Color.parseColor("#DCE0FF"), Color.parseColor("#B4B4DC"), Color.parseColor("#8282B4"), Color.TRANSPARENT)
            "1" -> intArrayOf(Color.parseColor("#FF6432"), Color.parseColor("#FF9664"), Color.parseColor("#DC3200"), Color.parseColor("#AA0000"), Color.TRANSPARENT)
            "2" -> intArrayOf(Color.parseColor("#32DC64"), Color.parseColor("#64FF96"), Color.parseColor("#00B432"), Color.parseColor("#008200"), Color.TRANSPARENT)
            "3" -> intArrayOf(Color.parseColor("#FFDC32"), Color.parseColor("#FFF064"), Color.parseColor("#DCB400"), Color.parseColor("#AA8200"), Color.TRANSPARENT)
            "4" -> intArrayOf(Color.parseColor("#7878FF"), Color.parseColor("#AAAAFF"), Color.parseColor("#5050DC"), Color.parseColor("#1E1EB4"), Color.TRANSPARENT)
            "5" -> intArrayOf(Color.parseColor("#FFB4DC"), Color.parseColor("#FFDCF0"), Color.parseColor("#DC78B4"), Color.parseColor("#AA3C82"), Color.TRANSPARENT)
            "6" -> intArrayOf(Color.parseColor("#B4FF64"), Color.parseColor("#D2FF96"), Color.parseColor("#82DC32"), Color.parseColor("#50AA00"), Color.TRANSPARENT)
            "7" -> intArrayOf(Color.parseColor("#FFC896"), Color.parseColor("#FFE6C8"), Color.parseColor("#DCA064"), Color.parseColor("#AA6E32"), Color.TRANSPARENT)
            "8" -> intArrayOf(Color.parseColor("#64E6FF"), Color.parseColor("#96FAFF"), Color.parseColor("#32C8E6"), Color.parseColor("#0096B4"), Color.TRANSPARENT)
            "9" -> intArrayOf(Color.parseColor("#E664FF"), Color.parseColor("#FA96FF"), Color.parseColor("#C832E6"), Color.parseColor("#9600B4"), Color.TRANSPARENT)
            // Symbols get unique colors too
            "@" -> intArrayOf(Color.parseColor("#FF6B6B"), Color.parseColor("#FF8E8E"), Color.parseColor("#DC4C4C"), Color.parseColor("#AA2222"), Color.TRANSPARENT)
            "#" -> intArrayOf(Color.parseColor("#4ECDC4"), Color.parseColor("#7EDDD6"), Color.parseColor("#2EAAA0"), Color.parseColor("#1E8870"), Color.TRANSPARENT)
            "$" -> intArrayOf(Color.parseColor("#45B7D1"), Color.parseColor("#6BC5E0"), Color.parseColor("#2590B0"), Color.parseColor("#156888"), Color.TRANSPARENT)
            "_" -> intArrayOf(Color.parseColor("#96CEB4"), Color.parseColor("#B6DEC8"), Color.parseColor("#76AE94"), Color.parseColor("#568E74"), Color.TRANSPARENT)
            "&" -> intArrayOf(Color.parseColor("#FFEAA7"), Color.parseColor("#FFF0C7"), Color.parseColor("#DFCA87"), Color.parseColor("#BFAA67"), Color.TRANSPARENT)
            "-" -> intArrayOf(Color.parseColor("#DDA0DD"), Color.parseColor("#EDC0ED"), Color.parseColor("#BD80BD"), Color.parseColor("#9D609D"), Color.TRANSPARENT)
            "+" -> intArrayOf(Color.parseColor("#98D8C8"), Color.parseColor("#B8E8D8"), Color.parseColor("#78B8A8"), Color.parseColor("#589888"), Color.TRANSPARENT)
            "(" -> intArrayOf(Color.parseColor("#F7DC6F"), Color.parseColor("#F9E88F"), Color.parseColor("#D7BC4F"), Color.parseColor("#B79C2F"), Color.TRANSPARENT)
            ")" -> intArrayOf(Color.parseColor("#BB8FCE"), Color.parseColor("#CBAFDE"), Color.parseColor("#9B6FAE"), Color.parseColor("#7B4F8E"), Color.TRANSPARENT)
            "/" -> intArrayOf(Color.parseColor("#85C1E9"), Color.parseColor("#A5D1F9"), Color.parseColor("#65A1C9"), Color.parseColor("#4581A9"), Color.TRANSPARENT)
            "*" -> intArrayOf(Color.parseColor("#F8C471"), Color.parseColor("#FAD491"), Color.parseColor("#D8A451"), Color.parseColor("#B88431"), Color.TRANSPARENT)
            "\"" -> intArrayOf(Color.parseColor("#82E0AA"), Color.parseColor("#A2F0CA"), Color.parseColor("#62C08A"), Color.parseColor("#42A06A"), Color.TRANSPARENT)
            "'" -> intArrayOf(Color.parseColor("#F1948A"), Color.parseColor("#FBB4AA"), Color.parseColor("#D1746A"), Color.parseColor("#B1544A"), Color.TRANSPARENT)
            ":" -> intArrayOf(Color.parseColor("#85C1E9"), Color.parseColor("#A5D1F9"), Color.parseColor("#65A1C9"), Color.parseColor("#4581A9"), Color.TRANSPARENT)
            ";" -> intArrayOf(Color.parseColor("#D7BDE2"), Color.parseColor("#E7DDF2"), Color.parseColor("#B79DC2"), Color.parseColor("#977DA2"), Color.TRANSPARENT)
            "!" -> intArrayOf(Color.parseColor("#F5B7B1"), Color.parseColor("#F9D7D1"), Color.parseColor("#D59791"), Color.parseColor("#B57771"), Color.TRANSPARENT)
            "?" -> intArrayOf(Color.parseColor("#A9DFBF"), Color.parseColor("#C9EFDF"), Color.parseColor("#89BF9F"), Color.parseColor("#699F7F"), Color.TRANSPARENT)
            "=" -> intArrayOf(Color.parseColor("#AED6F1"), Color.parseColor("#CEE6FF"), Color.parseColor("#8EB6D1"), Color.parseColor("#6E96B1"), Color.TRANSPARENT)
            "%" -> intArrayOf(Color.parseColor("#F9E79F"), Color.parseColor("#FFF7BF"), Color.parseColor("#D9C77F"), Color.parseColor("#B9A75F"), Color.TRANSPARENT)
            "^" -> intArrayOf(Color.parseColor("#D5A6BD"), Color.parseColor("#E5C6DD"), Color.parseColor("#B5869D"), Color.parseColor("#95667D"), Color.TRANSPARENT)
            "[" -> intArrayOf(Color.parseColor("#A3E4D7"), Color.parseColor("#C3F4F7"), Color.parseColor("#83C4B7"), Color.parseColor("#63A497"), Color.TRANSPARENT)
            "]" -> intArrayOf(Color.parseColor("#F5CBA7"), Color.parseColor("#F5DBC7"), Color.parseColor("#D5AB87"), Color.parseColor("#B58B67"), Color.TRANSPARENT)
            "{" -> intArrayOf(Color.parseColor("#AEB6BF"), Color.parseColor("#CED6DF"), Color.parseColor("#8E969F"), Color.parseColor("#6E767F"), Color.TRANSPARENT)
            "}" -> intArrayOf(Color.parseColor("#D2B4DE"), Color.parseColor("#F2D4FE"), Color.parseColor("#B294BE"), Color.parseColor("#92749E"), Color.TRANSPARENT)
            "~" -> intArrayOf(Color.parseColor("#AED6F1"), Color.parseColor("#CEE6FF"), Color.parseColor("#8EB6D1"), Color.parseColor("#6E96B1"), Color.TRANSPARENT)
            "`" -> intArrayOf(Color.parseColor("#FAD7A0"), Color.parseColor("#F9E7C0"), Color.parseColor("#DAB780"), Color.parseColor("#BA9760"), Color.TRANSPARENT)
            "|" -> intArrayOf(Color.parseColor("#D5F5E3"), Color.parseColor("#F5FFF3"), Color.parseColor("#B5D5C3"), Color.parseColor("#95B5A3"), Color.TRANSPARENT)
            "•" -> intArrayOf(Color.parseColor("#FCF3CF"), Color.parseColor("#FFFFF0"), Color.parseColor("#DCD3AF"), Color.parseColor("#BCB38F"), Color.TRANSPARENT)
            "√" -> intArrayOf(Color.parseColor("#D1F2EB"), Color.parseColor("#F1FFFB"), Color.parseColor("#B1D2CB"), Color.parseColor("#91B2AB"), Color.TRANSPARENT)
            "π" -> intArrayOf(Color.parseColor("#D6EAF8"), Color.parseColor("#F6FAFF"), Color.parseColor("#B6CAD8"), Color.parseColor("#96AAB8"), Color.TRANSPARENT)
            "÷" -> intArrayOf(Color.parseColor("#FADBD8"), Color.parseColor("#FAEBE8"), Color.parseColor("#DABBB8"), Color.parseColor("#BA9B98"), Color.TRANSPARENT)
            "×" -> intArrayOf(Color.parseColor("#E8DAEF"), Color.parseColor("#F8EAFF"), Color.parseColor("#C8BACF"), Color.parseColor("#A89AAF"), Color.TRANSPARENT)
            "¶" -> intArrayOf(Color.parseColor("#D5DBDB"), Color.parseColor("#F5FBFB"), Color.parseColor("#B5BBBB"), Color.parseColor("#959B9B"), Color.TRANSPARENT)
            "Δ" -> intArrayOf(Color.parseColor("#FCF3CF"), Color.parseColor("#FFFFF0"), Color.parseColor("#DCD3AF"), Color.parseColor("#BCB38F"), Color.TRANSPARENT)
            "£" -> intArrayOf(Color.parseColor("#D4EFDF"), Color.parseColor("#F4FFFF"), Color.parseColor("#B4CFBF"), Color.parseColor("#94AF9F"), Color.TRANSPARENT)
            "¢" -> intArrayOf(Color.parseColor("#D6EAF8"), Color.parseColor("#F6FAFF"), Color.parseColor("#B6CAD8"), Color.parseColor("#96AAB8"), Color.TRANSPARENT)
            "€" -> intArrayOf(Color.parseColor("#FADBD8"), Color.parseColor("#FAEBE8"), Color.parseColor("#DABBB8"), Color.parseColor("#BA9B98"), Color.TRANSPARENT)
            "¥" -> intArrayOf(Color.parseColor("#E8DAEF"), Color.parseColor("#F8EAFF"), Color.parseColor("#C8BACF"), Color.parseColor("#A89AAF"), Color.TRANSPARENT)
            "°" -> intArrayOf(Color.parseColor("#D5F5E3"), Color.parseColor("#F5FFF3"), Color.parseColor("#B5D5C3"), Color.parseColor("#95B5A3"), Color.TRANSPARENT)
            "©" -> intArrayOf(Color.parseColor("#D1F2EB"), Color.parseColor("#F1FFFB"), Color.parseColor("#B1D2CB"), Color.parseColor("#91B2AB"), Color.TRANSPARENT)
            "®" -> intArrayOf(Color.parseColor("#FAD7A0"), Color.parseColor("#F9E7C0"), Color.parseColor("#DAB780"), Color.parseColor("#BA9760"), Color.TRANSPARENT)
            "™" -> intArrayOf(Color.parseColor("#AED6F1"), Color.parseColor("#CEE6FF"), Color.parseColor("#8EB6D1"), Color.parseColor("#6E96B1"), Color.TRANSPARENT)
            "✓" -> intArrayOf(Color.parseColor("#A9DFBF"), Color.parseColor("#C9EFDF"), Color.parseColor("#89BF9F"), Color.parseColor("#699F7F"), Color.TRANSPARENT)
            "<" -> intArrayOf(Color.parseColor("#F5B7B1"), Color.parseColor("#F9D7D1"), Color.parseColor("#D59791"), Color.parseColor("#B57771"), Color.TRANSPARENT)
            ">" -> intArrayOf(Color.parseColor("#D7BDE2"), Color.parseColor("#E7DDF2"), Color.parseColor("#B79DC2"), Color.parseColor("#977DA2"), Color.TRANSPARENT)
            "," -> intArrayOf(Color.parseColor("#C8C8C8"), Color.parseColor("#B4B4B4"), Color.parseColor("#969696"), Color.parseColor("#646464"), Color.TRANSPARENT)
            "." -> intArrayOf(Color.parseColor("#C8C8C8"), Color.parseColor("#B4B4B4"), Color.parseColor("#969696"), Color.parseColor("#646464"), Color.TRANSPARENT)
            "shift" -> intArrayOf(Color.parseColor("#FFFFFF"), Color.parseColor("#DCDCDC"), Color.parseColor("#B4B4B4"), Color.parseColor("#828282"), Color.TRANSPARENT)
            "backspace", "back", "del" -> intArrayOf(Color.parseColor("#FF6464"), Color.parseColor("#FF9696"), Color.parseColor("#DC3232"), Color.parseColor("#AA0000"), Color.TRANSPARENT)
            "enter", "go" -> intArrayOf(Color.parseColor("#64FF96"), Color.parseColor("#96FFBE"), Color.parseColor("#32DC64"), Color.parseColor("#00AA3C"), Color.TRANSPARENT)
            "space" -> intArrayOf(Color.parseColor("#FFC864"), Color.parseColor("#FFDC96"), Color.parseColor("#DCA032"), Color.parseColor("#AA6E00"), Color.TRANSPARENT)
            "symbols", "123" -> intArrayOf(Color.parseColor("#C8C8C8"), Color.parseColor("#B4B4B4"), Color.parseColor("#969696"), Color.parseColor("#646464"), Color.TRANSPARENT)
            else -> intArrayOf(Color.parseColor("#FFAA32"), Color.parseColor("#FFC864"), Color.parseColor("#DC9600"), Color.parseColor("#AA6400"), Color.TRANSPARENT)
        }
    }

    private class GradientAnimation(
        private val centerX: Float,
        private val centerY: Float,
        private val colors: IntArray
    ) {
        var radius = 0f
            private set
        var isFinished = false
            private set

        private val maxRadius = 800f
        private val durationMs = 800L
        private var startTime = System.currentTimeMillis()

        fun update(elapsedTimeMs: Long): Boolean {
            val progress = (System.currentTimeMillis() - startTime).toFloat() / durationMs.toFloat()
            if (progress >= 1.0f) {
                isFinished = true
                return false
            }
            radius = maxRadius * (1 - (1 - progress).toDouble().pow(2.0)).toFloat()
            return true
        }

        fun draw(canvas: Canvas) {
            if (radius <= 0) return
            val paint = Paint().apply {
                isAntiAlias = true
                shader = RadialGradient(
                    centerX, centerY, radius,
                    colors,
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(centerX, centerY, radius, paint)
        }
    }
}