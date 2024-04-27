package com.rosetta.smsblocker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class PhraseAdapter(
    private var phrases: List<String>,
    private val onPhraseClicked: (String) -> Unit,
    private val onPhraseDeleted: (String) -> Unit
) : RecyclerView.Adapter<PhraseAdapter.PhraseViewHolder>() {

    class PhraseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val phraseTextView: TextView = itemView.findViewById(R.id.phraseTextView)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(phrase: String, onPhraseClicked: (String) -> Unit, onPhraseDeleted: (String) -> Unit) {
            phraseTextView.text = phrase
            itemView.setOnClickListener { onPhraseClicked(phrase) }
            deleteButton.setOnClickListener { onPhraseDeleted(phrase) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhraseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_phrase, parent, false)
        return PhraseViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhraseViewHolder, position: Int) {
        val phrase = phrases[position]
        holder.bind(phrase, onPhraseClicked, onPhraseDeleted)
    }

    override fun getItemCount(): Int {
        return phrases.size
    }

    fun updateData(newPhrases: List<String>) {
        phrases = newPhrases
        notifyDataSetChanged()
    }
}