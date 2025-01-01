package com.shaan.androiduicomponents.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.shaan.androiduicomponents.R
import com.shaan.androiduicomponents.models.PopularUniversity
import com.bumptech.glide.Glide

class PopularUniversityAdapter(
    private var universities: List<PopularUniversity>,
    private val onItemClick: (PopularUniversity) -> Unit
) : RecyclerView.Adapter<PopularUniversityAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val universityName: TextView = view.findViewById(R.id.universityName)
            ?: throw IllegalStateException("universityName view not found")
            ?: throw IllegalStateException("locationText view not found")
        val universityLogo: ImageView = view.findViewById(R.id.universityLogo)
            ?: throw IllegalStateException("universityLogo view not found")
        val favoriteCount: TextView = view.findViewById(R.id.favoriteCount)
            ?: throw IllegalStateException("favoriteCount view not found")
        val cardView: MaterialCardView = view as MaterialCardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_popular_university_home, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val university = universities[position]
        
        with(holder) {
            universityName.text = university.name
            favoriteCount.text = "${university.favoriteCount} favorites"
            
            // Load image using Glide
            Glide.with(itemView.context)
                .load(university.logoUrl)
                .placeholder(R.drawable.university_placeholder)
                .error(R.drawable.university_placeholder)
                .into(universityLogo)

            cardView.setOnClickListener { onItemClick(university) }
        }
    }

    override fun getItemCount() = universities.size

    fun updateList(newList: List<PopularUniversity>) {
        universities = newList
        notifyDataSetChanged()
    }
} 