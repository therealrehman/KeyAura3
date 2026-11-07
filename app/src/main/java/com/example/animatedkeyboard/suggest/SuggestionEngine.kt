package com.example.animatedkeyboard.suggest

import android.content.Context
import com.example.animatedkeyboard.settings.KeyboardSettings

class SuggestionEngine private constructor(context: Context) {

    private val settings = KeyboardSettings.getInstance(context.applicationContext)

    fun suggest(prefix: String, maxResults: Int = 3): List<String> {
        val p = prefix.lowercase().trim()
        if (p.isEmpty()) return emptyList()

        val matches = ArrayList<Pair<String, Int>>()
        for (word in WORDS) {
            if (word.startsWith(p) && word != p) {
                val boost = settings.englishWordBoost(word)
                matches.add(word to boost)
            }
        }
        matches.sortWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first.length })

        val learned = settings.englishWordPreference(p)
        val result = matches.take(maxResults).map { it.first }.toMutableList()
        if (learned != null && learned.startsWith(p) && !result.contains(learned)) {
            result.add(0, learned)
        }
        return result.take(maxResults)
    }

    fun learn(prefix: String, chosenWord: String) {
        val p = prefix.lowercase().trim()
        val w = chosenWord.lowercase().trim()
        if (p.isEmpty() || w.isEmpty()) return
        settings.bumpEnglishWord(w)
        if (w.startsWith(p)) settings.setEnglishWordPreference(p, w)
    }

    companion object {
        @Volatile private var instance: SuggestionEngine? = null
        fun getInstance(context: Context): SuggestionEngine {
            return instance ?: synchronized(this) {
                instance ?: SuggestionEngine(context.applicationContext).also { instance = it }
            }
        }

        private val WORDS = arrayOf(
            "the","this","that","there","their","they","them","then","than","thanks","thank",
            "think","thing","things","time","today","tomorrow","tonight","together","too","tell",
            "take","talk","taking","try","trying","turn","two","text","team",
            "i","im","is","it","its","in","into","if","idea","important","inside","instead",
            "you","your","yours","yes","yeah","yet","yesterday","young",
            "we","will","would","with","what","when","where","which","who","why","work","working",
            "want","wanted","way","wait","waiting","well","went","were","week","weekend","welcome",
            "world","write","writing","watch","water","without","wonderful",
            "he","she","her","him","his","have","has","had","how","home","here","hello","hey","hi",
            "happy","happen","happened","hope","hoping","hour","house","hold","help","helping",
            "and","are","am","an","as","at","all","also","about","after","again","against","any",
            "anything","anyone","awesome","amazing","actually","already","always","another","ask",
            "asked","away","around","afternoon","agree",
            "be","been","being","because","before","but","by","back","bad","best","better","big",
            "brother","busy","beautiful","believe","bring","buy","book","birthday","both","break",
            "can","could","come","coming","came","call","called","car","care","check","class",
            "cool","complete","congratulations","create","currently",
            "do","does","did","done","doing","day","days","dear","dont","down","dinner","drink",
            "dear","definitely","different","difficult",
            "eat","eating","easy","even","evening","every","everything","everyone","enjoy","enough",
            "especially","exactly","excited","email","early",
            "for","from","friend","friends","family","fine","food","feel","feeling","first","find",
            "free","fun","fast","finally","finish","finished","follow","forget","forgot","forward",
            "get","getting","got","go","going","gone","good","great","give","given","game","gym",
            "glad","guess","goodnight","group","grow",
            "just","job","join","joined",
            "know","known","keep","kind","kids",
            "let","lets","like","liked","love","loved","look","looking","long","later","life","live",
            "little","last","late","leave","left","lunch","learn","list",
            "me","my","mine","make","making","made","many","much","more","most","morning","meet",
            "meeting","message","miss","missing","mom","money","movie","move","maybe","matter",
            "no","not","now","new","next","need","needed","never","nice","night","nothing","name",
            "number","near","note","normal",
            "of","on","one","ok","okay","or","our","out","over","off","old","only","open","office",
            "once","online","outside","order","own",
            "please","people","person","place","plan","planned","play","playing","phone","photo",
            "pretty","problem","put","party","perfect","possible","probably","project",
            "really","right","ready","remember","rest","run","running","room","reach","received",
            "see","seen","so","some","something","someone","say","said","same","soon","sorry","sure",
            "should","show","sleep","sleeping","send","sending","sent","start","started","still",
            "stop","school","sir","sister","sunday","saturday","sweet","super",
            "up","us","use","using","until","under","understand","urgent","update",
            "very","video","visit","voice",
            "ya","year","years","yet",
            "main","mera","meri","hai","hain","nahi","kya","ka","ki","ke","ko","se","aur","acha",
            "accha","theek","bhai","yaar","kal","aaj","abhi","bohat","bahut","karo","karna","hoga",
            "raha","rahi","gaya","gya","jao","aao","bolo","sun","suno","dekho","chalo","haan","han",
            "nhi","kr","kro","plz","pls","kahan","kidhar","kab","kaise","kaisi","kyun","kyu"
        )
    }
}
