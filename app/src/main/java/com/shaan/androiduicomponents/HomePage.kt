package com.shaan.androiduicomponents

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.shaan.androiduicomponents.fragments.AdmissionGuideFragment
import com.shaan.androiduicomponents.fragments.HomeFragment
import com.shaan.androiduicomponents.helpers.NotificationHelper
import android.content.Context
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shaan.androiduicomponents.helpers.FirebaseHelper
import com.shaan.androiduicomponents.models.University
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.view.View
import android.widget.TextView
import android.graphics.Rect
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestoreException
import com.shaan.androiduicomponents.UniversityListActivity
import java.io.IOException
import kotlinx.coroutines.Job
import com.shaan.androiduicomponents.models.AdditionalInfo
import com.shaan.androiduicomponents.models.AdmissionInfo
import com.shaan.androiduicomponents.models.AdmissionTestDetails
import com.shaan.androiduicomponents.models.GeneralInfo
import com.shaan.androiduicomponents.models.UniAcademicInfo
import com.google.android.material.imageview.ShapeableImageView
import com.shaan.androiduicomponents.ProfileViewActivity
import com.shaan.androiduicomponents.ProfileDashboardSheet
import com.shaan.androiduicomponents.adapters.PopularUniversityAdapter
import com.shaan.androiduicomponents.databinding.ActivityHomeBinding
import com.shaan.androiduicomponents.models.PopularUniversity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomePage : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var popularUniversityAdapter: PopularUniversityAdapter
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewAllButton()
        fetchPopularUniversities()
    }

    private fun setupViewAllButton() {
        binding.viewAllPopularUniversities.setOnClickListener {
            try {
                val intent = Intent(this, PopularUniversitiesActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error navigating to Popular Universities", e)
                Toast.makeText(
                    this,
                    "Could not open Popular Universities page",
                    Toast.LENGTH_SHORT
                ).show()
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
                        Log.e(TAG, "Error mapping document to PopularUniversity: ${e.message}", e)
                        null
                    }
                }.sortedByDescending { it.favoriteCount }.take(5)

                setupPopularUniversitiesRecyclerView(popularUniversities)
                hideLoading()
                updateEmptyState(popularUniversities.isEmpty())
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching popular universities", e)
                Toast.makeText(
                    this,
                    "Failed to load popular universities",
                    Toast.LENGTH_SHORT
                ).show()
                hideLoading()
                updateEmptyState(true)
            }
    }

    private fun setupPopularUniversitiesRecyclerView(universities: List<PopularUniversity>) {
        binding.popularUniversitiesRecyclerView.apply {
            layoutManager = LinearLayoutManager(
                this@HomePage,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = PopularUniversityAdapter(universities) { university ->
                try {
                    val intent = Intent(this@HomePage, UniversityDetailsActivity::class.java)
                    intent.putExtra("universityName", university.name)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error launching university details", e)
                    Toast.makeText(
                        this@HomePage,
                        "Could not open university details",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showLoading() {
        binding.popularUniversitiesProgressBar.visibility = View.VISIBLE
        binding.popularUniversitiesRecyclerView.visibility = View.GONE
        binding.popularUniversitiesEmptyState.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.popularUniversitiesProgressBar.visibility = View.GONE
        binding.popularUniversitiesRecyclerView.visibility = View.VISIBLE
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.popularUniversitiesEmptyState.visibility =
            if (isEmpty) View.VISIBLE else View.GONE
    }

    companion object {
        private const val TAG = "HomePage"
    }
}