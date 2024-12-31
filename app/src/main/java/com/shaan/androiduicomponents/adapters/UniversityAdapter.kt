package com.shaan.androiduicomponents
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.shaan.androiduicomponents.models.University
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView

class UniversityAdapter(
    private var universities: List<University>,
    private val onItemClick: (University) -> Unit,
    private val onShortlistClick: (University, Boolean) -> Unit,
    private val showShortlistButton: Boolean = true
) : RecyclerView.Adapter<UniversityAdapter.ViewHolder>() {

    private val shortlistedUniversities = mutableSetOf<String>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val logo: ShapeableImageView = view.findViewById(R.id.universityLogo)
        val nameText: TextView = view.findViewById(R.id.universityNameText)
        val locationText: TextView = view.findViewById(R.id.universityLocation)
        val shortlistButton: ImageButton = view.findViewById(R.id.shortlistButton)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val university = universities[position]
        
        // Load university logo
        val logoUrl = university.generalInfo.imageUrl
        if (!logoUrl.isNullOrEmpty()) {
            Glide.with(holder.logo.context)
                .load(logoUrl)
                .placeholder(R.drawable.ic_public_university)
                .error(R.drawable.ic_public_university)
                .centerCrop()
                .into(holder.logo)
        } else {
            holder.logo.setImageResource(R.drawable.ic_public_university)
        }

        holder.nameText.text = university.generalInfo.name
        holder.locationText.text = university.generalInfo.location

        holder.itemView.setOnClickListener {
            onItemClick(university)
        }

        holder.shortlistButton.visibility = if (showShortlistButton) View.VISIBLE else View.GONE
        if (showShortlistButton) {
            val isShortlisted = shortlistedUniversities.contains(university.generalInfo.name)
            updateShortlistButton(holder.shortlistButton, isShortlisted)

            holder.shortlistButton.setOnClickListener {
                val newState = !isShortlisted
                if (newState) {
                    shortlistedUniversities.add(university.generalInfo.name)
                } else {
                    shortlistedUniversities.remove(university.generalInfo.name)
                }
                updateShortlistButton(holder.shortlistButton, newState)
                onShortlistClick(university, newState)
            }
        }
    }

    private fun updateShortlistButton(button: ImageButton, isShortlisted: Boolean) {
        button.setImageResource(
            if (isShortlisted) R.drawable.ic_shortlist_filled
            else R.drawable.ic_shortlist_outline
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_university, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = universities.size

    fun updateList(newList: List<University>) {
        universities = newList
        notifyDataSetChanged()
    }

    fun updateShortlistedUniversities(shortlisted: Set<String>) {
        shortlistedUniversities.clear()
        shortlistedUniversities.addAll(shortlisted)
        notifyDataSetChanged()
    }
}