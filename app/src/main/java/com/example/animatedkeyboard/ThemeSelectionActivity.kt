package com.example.animatedkeyboard

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.animatedkeyboard.settings.KeyboardSettings
import com.example.animatedkeyboard.theme.KeyboardTheme
import com.example.animatedkeyboard.theme.ThemeRegistry

class ThemeSelectionActivity : AppCompatActivity() {

    private val settings by lazy { KeyboardSettings.getInstance(this) }
    private val themes = ThemeRegistry.themes

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ThemeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_selection)

        recyclerView = findViewById(R.id.themeRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        adapter = ThemeAdapter(themes, settings.selectedThemeIndex) { themeIndex ->
            settings.selectedThemeIndex = themeIndex
            adapter.setSelectedIndex(themeIndex)
            // Show a brief visual feedback
            findViewById<TextView>(R.id.themeStatus).apply {
                text = "Theme applied: ${themes[themeIndex].name}"
                visibility = View.VISIBLE
                postDelayed({ visibility = View.GONE }, 2000)
            }
        }
        recyclerView.adapter = adapter

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.setSelectedIndex(settings.selectedThemeIndex)
    }

    inner class ThemeAdapter(
        private val themes: List<KeyboardTheme>,
        private var selectedIndex: Int,
        private val onThemeSelected: (Int) -> Unit
    ) : RecyclerView.Adapter<ThemeAdapter.ViewHolder>() {

        fun setSelectedIndex(index: Int) {
            val old = selectedIndex
            selectedIndex = index
            notifyItemChanged(old)
            notifyItemChanged(index)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_theme, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val theme = themes[position]
            holder.bind(theme, position == selectedIndex)
            holder.itemView.setOnClickListener {
                onThemeSelected(position)
            }
        }

        override fun getItemCount(): Int = themes.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val preview: FrameLayout = itemView.findViewById(R.id.themePreview)
            private val nameText: TextView = itemView.findViewById(R.id.themeName)
            private val iconText: TextView = itemView.findViewById(R.id.themeIcon)
            private val checkmark: TextView = itemView.findViewById(R.id.themeCheckmark)

            fun bind(theme: KeyboardTheme, isSelected: Boolean) {
                nameText.text = theme.name
                iconText.text = theme.icon
                checkmark.visibility = if (isSelected) View.VISIBLE else View.GONE

                // Preview key colors
                val colors = theme.colors
                preview.setBackgroundColor(colors.bg)

                // Find the preview key views
                val key1 = preview.findViewById<View>(R.id.previewKey1)
                val key2 = preview.findViewById<View>(R.id.previewKey2)
                val key3 = preview.findViewById<View>(R.id.previewKey3)
                val key4 = preview.findViewById<View>(R.id.previewKey4)

                key1?.setBackgroundColor(colors.keyNormalBg)
                key2?.setBackgroundColor(colors.keyNormalBg)
                key3?.setBackgroundColor(colors.keyNormalBg)
                key4?.setBackgroundColor(colors.keyNormalBg)

                // Text colors on preview keys
                preview.findViewById<TextView>(R.id.previewText1)?.setTextColor(colors.keyNormalText)
                preview.findViewById<TextView>(R.id.previewText2)?.setTextColor(colors.keyNormalText)
                preview.findViewById<TextView>(R.id.previewText3)?.setTextColor(colors.keyNormalText)
                preview.findViewById<TextView>(R.id.previewText4)?.setTextColor(colors.keyNormalText)

                // Border
                itemView.setBackgroundResource(if (isSelected) R.drawable.bg_theme_item_selected else R.drawable.bg_theme_item)
            }
        }
    }
}
