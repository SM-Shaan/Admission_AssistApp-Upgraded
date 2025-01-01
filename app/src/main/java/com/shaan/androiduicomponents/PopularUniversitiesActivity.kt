package com.shaan.androiduicomponents

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.firebase.firestore.FirebaseFirestore
import com.shaan.androiduicomponents.adapters.PopularUniversityAdapter
import com.shaan.androiduicomponents.databinding.ActivityPopularUniversitiesBinding
import com.shaan.androiduicomponents.models.PopularUniversity

class PopularUniversitiesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPopularUniversitiesBinding
    private lateinit var adapter: PopularUniversityAdapter
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "PopularUniversitiesActivity"
        private const val GRID_SPAN_COUNT = 2 // Number of columns
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPopularUniversitiesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        loadPopularUniversities()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Popular Universities"
        }
    }

    private fun setupRecyclerView() {
        adapter = PopularUniversityAdapter(emptyList()) { university ->
            navigateToUniversityDetails(university)
        }

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@PopularUniversitiesActivity, GRID_SPAN_COUNT)
            adapter = this@PopularUniversitiesActivity.adapter
            
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
                    val position = parent.getChildAdapterPosition(view)
                    val column = position % GRID_SPAN_COUNT

                    outRect.left = spacing - column * spacing / GRID_SPAN_COUNT
                    outRect.right = (column + 1) * spacing / GRID_SPAN_COUNT
                    outRect.bottom = spacing

                    if (position < GRID_SPAN_COUNT) {
                        outRect.top = spacing
                    }
                }
            })
        }
    }

    private fun loadPopularUniversities() {
        showLoading("Loading popular universities...")
        
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
                }.sortedByDescending { it.favoriteCount }

                adapter.updateList(popularUniversities)
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


    private fun navigateToUniversityDetails(university: PopularUniversity) {
        try {
            val intent = Intent(this, UniversityDetailsActivity::class.java)
            intent.putExtra("universityName", university.name)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching university details", e)
            showError("Could not open university details")
        }
    }

    private fun showLoading(message: String) {
        binding.apply {
            loadingState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            emptyState.visibility = View.GONE
            loadingMessage.text = message
        }
    }

    private fun hideLoading() {
        binding.apply {
            loadingState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 