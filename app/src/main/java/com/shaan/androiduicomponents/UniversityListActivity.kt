package com.shaan.androiduicomponents

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.shaan.androiduicomponents.databinding.ActivityUniversityListBinding
import com.shaan.androiduicomponents.helpers.FirebaseHelper
import com.shaan.androiduicomponents.helpers.NotificationHelper
import com.shaan.androiduicomponents.models.University
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import com.google.firebase.firestore.FirebaseFirestore

class UniversityListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUniversityListBinding
    private lateinit var adapter: UniversityAdapter
    private lateinit var notificationHelper: NotificationHelper
    private var universities = mutableListOf<University>()
    private var fetchJob: Job? = null
    private var currentFilter: String = "All"
    private lateinit var db: FirebaseFirestore

    companion object {
        private const val TAG = "UniversityListActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUniversityListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        notificationHelper = NotificationHelper(this)
//        setupToolbar()
        setupRecyclerView()
        setupSearch()
        setupClearButton()
        checkNotificationPermission()
        loadUniversities()
        setupFilterButtons()
        getUniversityCount()
    }

    private fun setupRecyclerView() {
        adapter = UniversityAdapter(
            universities = universities,
            onItemClick = { university ->
                val intent = Intent(this, UniversityDetailsActivity::class.java)
                intent.putExtra("university", university)
                startActivity(intent)
            },
            onShortlistClick = { university, isShortlisted ->
                handleShortlistAction(university, isShortlisted)
            }
        )
        binding.universitiesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@UniversityListActivity)
            adapter = this@UniversityListActivity.adapter
        }
    }

    private fun handleShortlistAction(university: University, isShortlisted: Boolean) {
        try {
            if (isShortlisted) {
                ShortlistManager.addToShortlist(this, university.generalInfo.name)
                Toast.makeText(this, 
                    "${university.generalInfo.name} added to shortlist", 
                    Toast.LENGTH_SHORT).show()
            } else {
                ShortlistManager.removeFromShortlist(this, university.generalInfo.name)
                Toast.makeText(this, 
                    "${university.generalInfo.name} removed from shortlist", 
                    Toast.LENGTH_SHORT).show()
            }

            adapter.updateShortlistedUniversities(
                ShortlistManager.getShortlistedUniversities(this)
            )

            notificationHelper.showShortlistNotification(university, isShortlisted)

        } catch (e: Exception) {
            Log.e(TAG, "Error handling shortlist action", e)
            Toast.makeText(this, "Error updating shortlist", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupToolbar() {
        TODO()
//        setSupportActionBar(binding.toolbar)
//        supportActionBar?.apply {
//            setDisplayHomeAsUpEnabled(true)
//            title = "Universities"
//        }
//        binding.toolbar.setNavigationOnClickListener {
//            onBackPressedDispatcher.onBackPressed()
//        }
    }

    private fun loadUniversities() {
        showLoading()
        fetchJob?.cancel()
        fetchJob = lifecycleScope.launch(Dispatchers.Main) {
            try {
                val result = withContext(Dispatchers.IO) {
                    FirebaseHelper.fetchUniversities()
                }
                result.fold(
                    onSuccess = { universityList ->
                        if (universityList.isEmpty()) {
                            Log.d(TAG, "No universities found")
                            Toast.makeText(this@UniversityListActivity, 
                                "No universities found", 
                                Toast.LENGTH_LONG).show()
                        } else {
                            Log.d(TAG, "Loaded ${universityList.size} universities")
                            universities.clear()
                            universities.addAll(universityList)
                            adapter.updateList(universities)
                        }
                        hideLoading()
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error loading universities", exception)
                        Toast.makeText(this@UniversityListActivity, 
                            "Error: ${exception.localizedMessage}", 
                            Toast.LENGTH_LONG).show()
                        hideLoading()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error in coroutine", e)
                Toast.makeText(this@UniversityListActivity, 
                    "Error: ${e.localizedMessage}", 
                    Toast.LENGTH_LONG).show()
                hideLoading()
            }
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterUniversities(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupClearButton() {
//        binding.clearButton.setOnClickListener {
//            binding.searchEditText.text?.clear()
//            adapter.updateList(universities)
//        }
    }

    private fun filterUniversities(query: String) {
        val filteredList = if (query.isEmpty()) {
            if (currentFilter == "All") universities
            else filterUniversitiesByType(currentFilter)
        } else {
            universities.filter { university ->
                val matchesName = university.generalInfo.name.contains(query, ignoreCase = true)
                val matchesType = university.generalInfo.universityType.contains(query, ignoreCase = true)
                val matchesFilter = when (currentFilter) {
                    "All" -> true
                    "Public", "Private", "Medical", "Engineering" -> matchesTypeFilter(university, currentFilter)
                    else -> false
                }
                (matchesName || matchesType) && matchesFilter
            }
        }
        adapter.updateList(filteredList)
    }

    private fun matchesTypeFilter(university: University, filter: String): Boolean {
        return when (filter) {
            "Public" -> university.generalInfo.universityType.equals("Public", ignoreCase = true)
            "Private" -> university.generalInfo.universityType.equals("Private", ignoreCase = true)
//            "Medical" -> university.academicInfo.departments.any { dept ->
//                dept.contains("Medical", ignoreCase = true) ||
//                dept.contains("Medicine", ignoreCase = true) ||
//                dept.contains("MBBS", ignoreCase = true)
//            }
//            "Engineering" -> university.academicInfo.departments.any { dept ->
//                dept.contains("Engineering", ignoreCase = true) ||
//                dept.contains("CSE", ignoreCase = true) ||
//                dept.contains("EEE", ignoreCase = true)
//            }
            else -> false
        }
    }

    private fun filterUniversitiesByType(type: String): List<University> {
        return when (type) {
            "All" -> universities
            else -> universities.filter { university -> matchesTypeFilter(university, type) }
        }
    }

    private fun showLoading() {
        binding.loadingProgressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.loadingProgressBar.visibility = View.GONE
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.updateShortlistedUniversities(
            ShortlistManager.getShortlistedUniversities(this)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        fetchJob?.cancel()
        universities.clear()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.home_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_notifications -> {
                startActivity(Intent(this, NotificationListActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupFilterButtons() {
        binding.apply {
            filterAll.setOnClickListener {
                currentFilter = "All"
                applyFilter()
                updateFilterButtonStates()
            }

            filterPublic.setOnClickListener {
                currentFilter = "Public"
                applyFilter()
                updateFilterButtonStates()
            }

            filterPrivate.setOnClickListener {
                currentFilter = "Private"
                applyFilter()
                updateFilterButtonStates()
            }

            filterMedical.setOnClickListener {
                currentFilter = "Medical"
                applyFilter()
                updateFilterButtonStates()
            }

            filterEngineering.setOnClickListener {
                currentFilter = "Engineering"
                applyFilter()
                updateFilterButtonStates()
            }
        }
    }

    private fun applyFilter() {
        val filteredList = when (currentFilter) {
            "All" -> {
                // When showing all universities, get total count again
                getUniversityCount()
                universities
            }
            "Public" -> universities.filter { 
                it.generalInfo.universityType.equals("Public", ignoreCase = true) 
            }
            "Private" -> universities.filter { 
                it.generalInfo.universityType.equals("Private", ignoreCase = true) 
            }
            "Medical" -> universities.filter { university ->
                university.academicInfo.departments.any { dept ->
                    dept.contains("Medical", ignoreCase = true) ||
                    dept.contains("Medicine", ignoreCase = true) ||
                    dept.contains("MBBS", ignoreCase = true)
                }
            }
            "Engineering" -> universities.filter { university ->
                university.academicInfo.departments.any { dept ->
                    dept.contains("Engineering", ignoreCase = true) ||
                    dept.contains("CSE", ignoreCase = true) ||
                    dept.contains("EEE", ignoreCase = true)
                }
            }
            else -> universities
        }
        adapter.updateList(filteredList)
        
        // Update filtered count
        if (currentFilter != "All") {
            binding.totalUniversitiesTextView.text = filteredList.size.toString()
        }
    }

    private fun updateFilterButtonStates() {
        binding.apply {
            val defaultBgColor = getColor(R.color.filter_button_default)
            val selectedBgColor = getColor(R.color.filter_button_selected)
            val defaultTextColor = getColor(R.color.white)
            val selectedTextColor = getColor(R.color.white)

            filterAll.apply {
                setTextColor(if (currentFilter == "All") selectedTextColor else defaultTextColor)
                setBackgroundColor(if (currentFilter == "All") selectedBgColor else defaultBgColor)
            }
            filterPublic.apply {
                setTextColor(if (currentFilter == "Public") selectedTextColor else defaultTextColor)
                setBackgroundColor(if (currentFilter == "Public") selectedBgColor else defaultBgColor)
            }
            filterPrivate.apply {
                setTextColor(if (currentFilter == "Private") selectedTextColor else defaultTextColor)
                setBackgroundColor(if (currentFilter == "Private") selectedBgColor else defaultBgColor)
            }
            filterMedical.apply {
                setTextColor(if (currentFilter == "Medical") selectedTextColor else defaultTextColor)
                setBackgroundColor(if (currentFilter == "Medical") selectedBgColor else defaultBgColor)
            }
            filterEngineering.apply {
                setTextColor(if (currentFilter == "Engineering") selectedTextColor else defaultTextColor)
                setBackgroundColor(if (currentFilter == "Engineering") selectedBgColor else defaultBgColor)
            }
        }
    }

    private fun getUniversityCount() {
        db.collection("universitylist")
            .get()
            .addOnSuccessListener { documents ->
                val totalCount = documents.size()
                binding.totalUniversitiesTextView.text = totalCount.toString()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting university count", e)
                binding.totalUniversitiesTextView.text = "0"
            }
    }
}