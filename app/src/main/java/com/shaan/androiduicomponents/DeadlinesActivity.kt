package com.shaan.androiduicomponents
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.shaan.androiduicomponents.databinding.ActivityDeadlinesBinding
import com.shaan.androiduicomponents.models.Deadline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.withContext

class DeadlinesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeadlinesBinding
    private lateinit var adapter: DeadlineAdapter
    private var fetchJob: Job? = null
    private val deadlines = mutableListOf<Deadline>()

    companion object {
        private const val TAG = "DeadlinesActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Initialize binding first
            binding = ActivityDeadlinesBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Initialize adapter before setting up RecyclerView
            adapter = DeadlineAdapter()

            // Setup UI components
            setupToolbar()
            setupRecyclerView()
            setupSortButton()
            
            // Load data
            loadDeadlines()
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Error initializing activity", e)
            Toast.makeText(this, "Error initializing deadlines: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupToolbar() {
        Log.d(TAG, "setupToolbar: Setting up toolbar")
        try {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = "Admission Deadlines"
            }
            binding.toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
        } catch (e: Exception) {
            Log.e(TAG, "setupToolbar: Failed to setup toolbar", e)
            throw e
        }
    }

    private fun setupRecyclerView() {
        try {
            binding.deadlinesRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@DeadlinesActivity)
                adapter = this@DeadlinesActivity.adapter
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        } catch (e: Exception) {
            Log.e(TAG, "setupRecyclerView: Error", e)
            throw e
        }
    }

    private fun setupSortButton() {
        binding.sortButton.setOnClickListener {
            showSortDialog()
        }
    }

    private fun showSortDialog() {
        val sortOptions = arrayOf(
            "Upcoming First",
            "Ongoing First",
            "Date (Newest)",
            "Date (Oldest)",
            "University Name (A-Z)",
            "University Name (Z-A)",
            "Event Type"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Sort Deadlines")
            .setItems(sortOptions) { _, which ->
                when (which) {
                    0 -> sortByUpcoming()
                    1 -> sortByOngoing()
                    2 -> sortByDate(ascending = false)
                    3 -> sortByDate(ascending = true)
                    4 -> sortByUniversityName(ascending = true)
                    5 -> sortByUniversityName(ascending = false)
                    6 -> sortByEventType()
                }
            }
            .show()
    }

    private fun sortByUpcoming() {
        val sorted = deadlines.sortedWith(
            compareBy<Deadline> { deadline ->
                when {
                    deadline.isUpcoming() -> 0
                    deadline.isOngoing() -> 1
                    else -> 2
                }
            }.thenBy { it.date }
        )
        updateDeadlines(sorted)
        updateActiveSortChip("Upcoming First")
    }

    private fun sortByOngoing() {
        val sorted = deadlines.sortedWith(
            compareBy<Deadline> { deadline ->
                when {
                    deadline.isOngoing() -> 0
                    deadline.isUpcoming() -> 1
                    else -> 2
                }
            }.thenBy { it.date }
        )
        updateDeadlines(sorted)
        updateActiveSortChip("Ongoing First")
    }

    private fun sortByDate(ascending: Boolean) {
        val sorted = if (ascending) {
            deadlines.sortedBy { it.date }
        } else {
            deadlines.sortedByDescending { it.date }
        }
        updateDeadlines(sorted)
        updateActiveSortChip(if (ascending) "Date (Newest)" else "Date (Oldest)")
    }

    private fun sortByUniversityName(ascending: Boolean) {
        val sorted = if (ascending) {
            deadlines.sortedBy { it.universityName }
        } else {
            deadlines.sortedByDescending { it.universityName }
        }
        updateDeadlines(sorted)
        updateActiveSortChip(if (ascending) "University Name (A-Z)" else "University Name (Z-A)")
    }

    private fun sortByEventType() {
        val sorted = deadlines.sortedWith(
            compareBy<Deadline> { it.eventType }
                .thenBy { it.date }
        )
        updateDeadlines(sorted)
        updateActiveSortChip("Event Type")
    }

    private fun updateDeadlines(sorted: List<Deadline>) {
        deadlines.clear()
        deadlines.addAll(sorted)
        adapter.updateDeadlines(sorted)
    }

    private fun loadDeadlines() {
        showLoading()
        
        fetchJob?.cancel()
        fetchJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val documents = db.collection("universitylist")
                    .get()
                    .await()

                val newDeadlines = documents.mapNotNull { doc ->
                    try {
                        val admissionInfo = doc.get("admissionInfo") as? Map<*, *>
                        val generalInfo = doc.get("generalInfo") as? Map<*, *>
                        val testDetails = admissionInfo?.get("admissionTestDetails") as? Map<*, *>

                        if (testDetails != null && generalInfo != null) {
                            Deadline(
                                universityName = generalInfo["name"]?.toString() ?: "",
                                eventType = "Admission Test",
                                date = testDetails["date"]?.toString() ?: "",
                                time = testDetails["time"]?.toString() ?: ""
                            )
                        } else null
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing document: ${e.message}")
                        null
                    }
                }

                withContext(Dispatchers.Main) {
                    if (newDeadlines.isNotEmpty()) {
                        deadlines.clear()
                        deadlines.addAll(newDeadlines)
                        adapter.updateDeadlines(deadlines)
                        hideEmptyState()
                    } else {
                        showEmptyState()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading deadlines", e)
                withContext(Dispatchers.Main) {
                    showError("Failed to load deadlines: ${e.message}")
                    showEmptyState()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    hideLoading()
                }
            }
        }
    }

    private fun showLoading() {
        try {
            binding.apply {
                progressBar.visibility = View.VISIBLE
                deadlinesRecyclerView.visibility = View.GONE
                emptyStateView.visibility = View.GONE
                sortButton.isEnabled = false
                searchEditText.isEnabled = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing loading state", e)
        }
    }

    private fun hideLoading() {
        try {
            binding.apply {
                progressBar.visibility = View.GONE
                deadlinesRecyclerView.visibility = View.VISIBLE
                sortButton.isEnabled = true
                searchEditText.isEnabled = true

            }
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding loading state", e)
        }
    }


    private fun hideEmptyState() {
        binding.apply {
            emptyStateView.visibility = View.GONE
            deadlinesRecyclerView.visibility = View.VISIBLE
            sortButton.isEnabled = true
        }
    }

    private fun updateActiveSortChip(sortName: String) {
        binding.activeSortChip.apply {
            text = "Sorted by: $sortName"
            visibility = View.VISIBLE
        }
    }

    private fun showEmptyState() {
        binding.apply {
            emptyStateView.visibility = View.VISIBLE
            deadlinesRecyclerView.visibility = View.GONE
            sortButton.isEnabled = false
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        fetchJob?.cancel()
    }
}
