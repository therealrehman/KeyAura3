package com.example.animatedkeyboard.urdu

import android.content.Context
import com.example.animatedkeyboard.settings.KeyboardSettings

class RomanUrduEngine private constructor(context: Context) {

    private val settings = KeyboardSettings.getInstance(context.applicationContext)

    fun suggest(roman: String, maxResults: Int = 3): List<String> {
        val key = roman.lowercase().trim()
        if (key.isEmpty()) return emptyList()

        val results = LinkedHashSet<String>()

        settings.urduWordPreference(key)?.let { results.add(it) }

        DICT[key]?.let { results.add(it) }
        var prefixCount = 0
        for ((k, v) in DICT) {
            if (k != key && k.startsWith(key)) {
                results.add(v)
                prefixCount++
                if (prefixCount >= 2) break
            }
        }

        if (key.all { it.isLetter() }) {
            results.add(transliterate(key))
        }

        return results.filter { it.isNotBlank() }.take(maxResults)
    }

    fun learn(roman: String, chosenUrdu: String) {
        val key = roman.lowercase().trim()
        if (key.isNotEmpty() && chosenUrdu.isNotBlank()) {
            settings.setUrduWordPreference(key, chosenUrdu)
        }
    }

    private val digraphs = mapOf(
        "ch" to "چ", "sh" to "ش", "kh" to "خ", "gh" to "غ", "ph" to "پھ",
        "th" to "تھ", "bh" to "بھ", "dh" to "دھ", "jh" to "جھ", "lh" to "لھ",
        "mh" to "مھ", "nh" to "نھ", "rh" to "رھ", "aa" to "ا", "ee" to "ی",
        "oo" to "و", "ai" to "ے", "au" to "او", "ei" to "ے", "tt" to "ٹ",
        "dd" to "ڈ", "rr" to "ڑ", "ng" to "نگ"
    )

    private val singles = mapOf(
        'a' to "ا", 'b' to "ب", 'c' to "ک", 'd' to "د", 'e' to "ے",
        'f' to "ف", 'g' to "گ", 'h' to "ہ", 'i' to "ی", 'j' to "ج",
        'k' to "ک", 'l' to "ل", 'm' to "م", 'n' to "ن", 'o' to "و",
        'p' to "پ", 'q' to "ق", 'r' to "ر", 's' to "س", 't' to "ت",
        'u' to "و", 'v' to "و", 'w' to "و", 'x' to "کس", 'y' to "ی", 'z' to "ز"
    )

    fun transliterate(roman: String): String {
        val sb = StringBuilder()
        var i = 0
        val s = roman.lowercase()
        while (i < s.length) {
            var matched = false
            if (i + 1 < s.length) {
                val di = s.substring(i, i + 2)
                val mapped = digraphs[di]
                if (mapped != null) {
                    sb.append(mapped)
                    i += 2
                    matched = true
                }
            }
            if (!matched) {
                val mapped = singles[s[i]]
                if (mapped != null) sb.append(mapped) else sb.append(s[i])
                i++
            }
        }
        return sb.toString()
    }

    companion object {
        @Volatile private var instance: RomanUrduEngine? = null
        fun getInstance(context: Context): RomanUrduEngine {
            return instance ?: synchronized(this) {
                instance ?: RomanUrduEngine(context.applicationContext).also { instance = it }
            }
        }

        private val DICT: LinkedHashMap<String, String> = linkedMapOf(
            "main" to "میں", "mein" to "میں", "aap" to "آپ", "ap" to "آپ",
            "tum" to "تم", "hum" to "ہم", "wo" to "وہ", "woh" to "وہ", "ye" to "یہ", "yeh" to "یہ",
            "kya" to "کیا", "kyun" to "کیوں", "kyu" to "کیوں", "kaise" to "کیسے", "kaisi" to "کیسی",
            "kab" to "کب", "kahan" to "کہاں", "kidhar" to "کدھر", "kaun" to "کون",
            "hai" to "ہے", "hain" to "ہیں", "tha" to "تھا", "thi" to "تھی", "the" to "تھے",
            "hoga" to "ہوگا", "hogi" to "ہوگی", "ho" to "ہو", "hua" to "ہوا", "hui" to "ہui",
            "nahi" to "نہیں", "nhi" to "نہیں", "na" to "نہ", "haan" to "ہاں", "han" to "ہاں", "ji" to "جی",
            "ka" to "کا", "ki" to "کی", "ke" to "کے", "ko" to "کو", "se" to "سے", "par" to "پر",
            "mein2" to "میں", "tak" to "تک", "saath" to "ساتھ", "liye" to "لئے", "wala" to "والا",
            "wali" to "والی", "wale" to "والے",
            "aur" to "اور", "ya" to "یا", "lekin" to "لیکن", "magar" to "مگر", "phir" to "پھر",
            "bhi" to "بھی", "hi" to "ہی", "sirf" to "صرف", "bas" to "بس", "bahut" to "بہت", "bohat" to "بہت",
            "thora" to "تھوڑا", "thori" to "تھوڑی", "zyada" to "زیادہ", "kam" to "کم",
            "acha" to "اچھا", "accha" to "اچھا", "achi" to "اچھی", "bura" to "برا", "buri" to "بری",
            "theek" to "ٹھیک", "thik" to "ٹھیک", "sahi" to "صحیح", "galat" to "غلط",
            "pyara" to "پیارا", "pyari" to "پیاری", "khoobsurat" to "خوبصورت", "zabardast" to "زبردست",
            "shukriya" to "شکریہ", "meherbani" to "مہربانی", "maaf" to "معاف",
            "bhai" to "بھائی", "behen" to "بہن", "dost" to "دوست", "yaar" to "یار",
            "ammi" to "امی", "abbu" to "ابو", "beta" to "بیٹا", "beti" to "بیٹی",
            "ghar" to "گھر", "kaam" to "کام", "paisa" to "پیسہ", "waqt" to "وقت", "din" to "دن",
            "raat" to "رات", "subah" to "صبح", "shaam" to "شام", "aaj" to "آج", "kal" to "کل",
            "abhi" to "ابھی", "ab" to "اب", "baad" to "بعد", "pehle" to "پہلے",
            "karo" to "کرو", "karna" to "کرنا", "kiya" to "کیا", "raha" to "رہا", "rahi" to "رہی",
            "rahe" to "رہے", "jao" to "جاؤ", "jana" to "جانا", "aao" to "آؤ", "aana" to "آنا",
            "bolo" to "بولو", "bolna" to "بولنا", "suno" to "سنو", "sunna" to "سننا",
            "dekho" to "دیکھو", "dekhna" to "دیکھنا", "chalo" to "چلو", "chalna" to "چلنا",
            "khao" to "کھاؤ", "khana" to "کھانا", "pio" to "پیو", "pina" to "پینا",
            "lo" to "لو", "lena" to "لینا", "do" to "دو", "dena" to "دینا", "diya" to "دیا",
            "milo" to "ملو", "milna" to "ملنا", "mila" to "ملا", "gaya" to "گیا", "gya" to "گیا",
            "dil" to "دل", "zindagi" to "زندگی", "mohabbat" to "محبت", "pyar" to "پیار",
            "khushi" to "خوشی", "gham" to "غم", "dua" to "دعا", "allah" to "اللہ",
            "inshallah" to "انشاءاللہ", "mashallah" to "ماشاءاللہ", "alhamdulillah" to "الحمدللہ",
            "salam" to "سلام", "khuda" to "خدا", "hafiz" to "حافظ",
            "pakistan" to "پاکستان", "pakistani" to "پاکستانی", "lahore" to "لاہور",
            "karachi" to "کراچی", "islamabad" to "اسلام آباد",
            "paani" to "پانی", "chai" to "چائے", "roti" to "روٹی", "meat" to "گوشت", "gosht" to "گوشت",
            "ek" to "ایک", "do2" to "دو", "teen" to "تین", "char" to "چار", "paanch" to "پانچ",
            "cheh" to "چھ", "saat" to "سات", "aath" to "آٹھ", "nau" to "نو", "das" to "دس",
            "sau" to "سو", "hazaar" to "ہزار", "lakh" to "لاکھ",
            "mera" to "میرا", "meri" to "میری", "mere" to "میرے",
            "tera" to "تیرا", "teri" to "تیری", "tere" to "تیرے",
            "tumhara" to "تمہارا", "tumhari" to "تمہاری", "hamara" to "ہمارا", "hamari" to "ہماری",
            "uska" to "اسکا", "uski" to "اسکی", "iska" to "اسکا", "inki" to "انکی",
            "acha2" to "اچھا", "jaldi" to "جلدی", "aahista" to "آہستہ", "ruk" to "رک", "ruko" to "رکو",
            "mazay" to "مزے", "maza" to "مزا", "scene" to "سین", "chahiye" to "چاہیے",
            "milega" to "ملےگا", "karega" to "کریگا", "jayega" to "جائےگا", "aayega" to "آئےگا",
            "samajh" to "سمجھ", "samjha" to "سمجھا", "pata" to "پتا", "maloom" to "معلوم",
            "yaad" to "یاد", "bhool" to "بھول", "soch" to "سوچ", "socho" to "سوچو"
        )
    }
}
