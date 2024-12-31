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

class DeadlinesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDeadlinesBinding
    private lateinit var adapter: DeadlineAdapter
    private var fetchJob: Job? = null
    private var deadlines = mutableListOf<Deadline>()

    companion object {
        private const val TAG = "DeadlinesActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting DeadlinesActivity")

        try {
            binding = ActivityDeadlinesBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupToolbar()
            setupRecyclerView()
            setupSortButton()
            loadDeadlines()
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Error initializing activity", e)
            Toast.makeText(this, "Error initializing deadlines", Toast.LENGTH_SHORT).show()
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
        Log.d(TAG, "setupRecyclerView: Setting up RecyclerView")
        try {
            adapter = DeadlineAdapter()
            binding.deadlinesRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@DeadlinesActivity)
                adapter = this@DeadlinesActivity.adapter
                addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
            }
        } catch (e: Exception) {
            Log.e(TAG, "setupRecyclerView: Failed to setup RecyclerView", e)
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
            "University Name (A-Z)"
        )

        MaterialAlertDialogBuilder(this)
            .setTitle("Sort Deadlines")
            .setItems(sortOptions) { _, which ->
                when (which) {
                    0 -> sortByUpcoming()
                    1 -> sortByOngoing()
                    2 -> sortByDate(ascending = false)
                    3 -> sortByDate(ascending = true)
                    4 -> sortByUniversityName()
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
    }

    private fun sortByDate(ascending: Boolean) {
        val sorted = if (ascending) {
            deadlines.sortedBy { it.date }
        } else {
            deadlines.sortedByDescending { it.date }
        }
        updateDeadlines(sorted)
    }

    private fun sortByUniversityName() {
        val sorted = deadlines.sortedBy { it.universityName }
        updateDeadlines(sorted)
    }

    private fun updateDeadlines(sorted: List<Deadline>) {
        deadlines.clear()
        deadlines.addAll(sorted)
        adapter.updateDeadlines(sorted)
    }

    private fun loadDeadlines() {
        Log.d(TAG, "loadDeadlines: Starting to fetch deadlines from Firebase")
        showLoading()

        fetchJob?.cancel()
        fetchJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                val db = FirebaseFirestore.getInstance()
                val universities = db.collection("universitylist")
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->
                        try {
                            val admissionInfo = doc.get("admissionInfo") as? Map<*, *>
                            val generalInfo = doc.get("generalInfo") as? Map<*, *>
                            val testDetails = (admissionInfo?.get("admissionTestDetails") as? Map<*, *>)

                            if (testDetails != null && generalInfo != null) {
                                Deadline(
                                    universityName = generalInfo["name"]?.toString() ?: "",
                                    eventType = "Admission Test",
                                    date = testDetails["date"]?.toString() ?: "",
                                    time = testDetails["time"]?.toString() ?: ""
                                )
                            } else null
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing document", e)
                            null
                        }
                    }

                if (!universities.isEmpty())  {
                    hideEmptyState()
                    val sortedDeadlines = universities.sortedWith(
                        compareBy<Deadline> {
                            when {
                                it.isOngoing() -> 0
                                it.isUpcoming() -> 1
                                else -> 2
                            }
                        }.thenBy { it.date }
                    )
                    adapter.updateDeadlines(sortedDeadlines)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading deadlines", e)
                Toast.makeText(
                    this@DeadlinesActivity,
                    "Error loading deadlines: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                hideLoading()
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
        try {
            binding.apply {
                emptyStateView.visibility = View.GONE
                deadlinesRecyclerView.visibility = View.VISIBLE
                sortButton.isEnabled = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding empty state", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fetchJob?.cancel()
    }
}
