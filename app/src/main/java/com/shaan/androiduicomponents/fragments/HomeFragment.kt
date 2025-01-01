package com.shaan.androiduicomponents.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import com.shaan.androiduicomponents.DeadlinesActivity
import com.shaan.androiduicomponents.EligibilityCheckActivity
import com.shaan.androiduicomponents.PopularUniversitiesActivity
import com.shaan.androiduicomponents.ShortlistActivity
import com.shaan.androiduicomponents.UniversityDetailsActivity
import com.shaan.androiduicomponents.UniversityListActivity
import com.shaan.androiduicomponents.adapters.PopularUniversityAdapter
import com.shaan.androiduicomponents.R
import com.shaan.androiduicomponents.databinding.ActivityHomeBinding
import com.shaan.androiduicomponents.models.PopularUniversity


class HomeFragment : Fragment() {
    private var _binding: ActivityHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var popularUniversityAdapter: PopularUniversityAdapter
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "HomeFragment"
        
        fun newInstance() = HomeFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewAllButton()
        fetchPopularUniversities()
        setupClickListeners(view)
    }

    private fun setupClickListeners(view: View) {
        view.findViewById<MaterialCardView>(R.id.universityFinderCard).setOnClickListener {
            startActivity(Intent(requireContext(), UniversityListActivity::class.java))
        }

        view.findViewById<MaterialCardView>(R.id.eligibilityCheckCard).setOnClickListener {
            startActivity(Intent(requireContext(), EligibilityCheckActivity::class.java))
        }

        view.findViewById<MaterialCardView>(R.id.deadlinesCard).setOnClickListener {
            startActivity(Intent(requireContext(), DeadlinesActivity::class.java))
        }

        view.findViewById<MaterialCardView>(R.id.shortlistCard).setOnClickListener {
            startActivity(Intent(requireContext(), ShortlistActivity::class.java))
        }
    }

    private fun setupViewAllButton() {
        binding.viewAllPopularUniversities.setOnClickListener {
            try {
                val intent = Intent(requireContext(), PopularUniversitiesActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error navigating to Popular Universities", e)
                showError("Could not open Popular Universities page")
            }
        }
    }

    private fun fetchPopularUniversities() {
        showLoading()
        
        firestore.collection("universitylist")
            .get()
            .addOnSuccessListener { documents ->
                val popularUniversities = documents.mapNotNull { document ->
                    try {
                        val generalInfo = document.get("generalInfo") as? Map<*, *>
                        if (generalInfo != null) {
                            PopularUniversity(
                                name = generalInfo["name"] as? String ?: "",
                                location = generalInfo["location"] as? String ?: "",
                                logoUrl = generalInfo["imageUrl"] as? String ?: "",
                                favoriteCount = (document.get("favoriteCount") as? Long)?.toInt() ?: 0
                            )
                        } else null
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping document to PopularUniversity", e)
                        null
                    }
                }.sortedByDescending { it.favoriteCount }.take(5)

                setupPopularUniversitiesRecyclerView(popularUniversities)
                hideLoading()
                updateEmptyState(popularUniversities.isEmpty())
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching popular universities", e)
                showError("Failed to load popular universities")
                hideLoading()
                updateEmptyState(true)
            }
    }

    private fun setupPopularUniversitiesRecyclerView(universities: List<PopularUniversity>) {
        binding.popularUniversitiesRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = PopularUniversityAdapter(universities) { university ->
                try {
                    val intent = Intent(requireContext(), UniversityDetailsActivity::class.java)
                    intent.putExtra("universityName", university.name)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error launching university details", e)
                    showError("Could not open university details")
                }
            }
            popularUniversityAdapter = adapter as PopularUniversityAdapter
        }
    }

    private fun showLoading() {
        binding.popularUniversitiesProgressBar.visibility = View.VISIBLE
        binding.popularUniversitiesRecyclerView.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.popularUniversitiesProgressBar.visibility = View.GONE
        binding.popularUniversitiesRecyclerView.visibility = View.VISIBLE
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.popularUniversitiesEmptyState.visibility = 
            if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 