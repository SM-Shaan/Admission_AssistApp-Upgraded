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

class HomePage : AppCompatActivity() {
    companion object {
        private const val TAG = "HomePage"
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var bottomNavigation: BottomNavigationView
    private var universities = mutableListOf<University>()
    private var fetchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Starting HomePage")
        try {
            setContentView(R.layout.activity_home)
            setupToolbar()
            setupClickListeners()
            loadUniversities()
//            setupProfileNavigation()
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Error initializing HomePage", e)
            Toast.makeText(this, "Error initializing app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupToolbar() {
        Log.d(TAG, "setupToolbar: Setting up toolbar")
        try {
            val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.title = "Home"
        } catch (e: Exception) {
            Log.e(TAG, "setupToolbar: Failed to setup toolbar", e)
            throw e
        }
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
//        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(TAG, "onCreateOptionsMenu: Creating options menu")
        try {
            menuInflater.inflate(R.menu.home_menu, menu)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "onCreateOptionsMenu: Failed to create menu", e)
            return false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "onOptionsItemSelected: Menu item selected: ${item.title}")
        return try {
            when (item.itemId) {
                R.id.action_notifications -> {
                    Log.d(TAG, "onOptionsItemSelected: Starting NotificationListActivity")
                    startActivity(Intent(this, NotificationListActivity::class.java))
                    true
                }
                R.id.action_profile -> {
                    Log.d(TAG, "onOptionsItemSelected: Showing profile dashboard")
                    val profileDashboardSheet = ProfileDashboardSheet()
                    profileDashboardSheet.show(supportFragmentManager, "ProfileDashboard")
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        } catch (e: Exception) {
            Log.e(TAG, "onOptionsItemSelected: Error handling menu item selection", e)
            false
        }
    }

    private fun setupClickListeners() {
        Log.d(TAG, "setupClickListeners: Setting up card click listeners")
        try {
            findViewById<MaterialCardView>(R.id.universityFinderCard).setOnClickListener {
                Log.d(TAG, "Card clicked: University Finder")
                navigateToActivity(UniversityListActivity::class.java)
            }
            findViewById<MaterialCardView>(R.id.eligibilityCheckCard).setOnClickListener {
                Log.d(TAG, "Card clicked: Eligibility Check")
                navigateToActivity(EligibilityCheckActivity::class.java)
            }
            findViewById<MaterialCardView>(R.id.deadlinesCard).setOnClickListener {
                Log.d(TAG, "Card clicked: Deadlines")
                navigateToActivity(DeadlinesActivity::class.java)
            }
            findViewById<MaterialCardView>(R.id.shortlistCard).setOnClickListener {
                Log.d(TAG, "Card clicked: Shortlist")
                navigateToActivity(ShortlistActivity::class.java)
            }
        } catch (e: Exception) {
            Log.e(TAG, "setupClickListeners: Failed to setup click listeners", e)
            throw e
        }
    }

    private fun showProfileDashboard() {
        Log.d(TAG, "showProfileDashboard: Showing profile dashboard")
        try {
            val profileDashboard = ProfileDashboardSheet().apply {
                onLogoutClick = {
                    Log.d(TAG, "Logout clicked, handling logout process")
                    try {
                        dismiss()
                        val sharedPreferences =
                            getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        sharedPreferences.edit().clear().apply()
                        Log.d(TAG, "User preferences cleared")

                        val intent = Intent(this@HomePage, Login::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(intent)
                        finish()
                        Log.d(TAG, "Successfully navigated to LoginActivity")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during logout process", e)
                        Toast.makeText(context, "Error logging out", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            profileDashboard.show(supportFragmentManager, ProfileDashboardSheet.TAG)
        } catch (e: Exception) {
            Log.e(TAG, "showProfileDashboard: Failed to show profile dashboard", e)
            Toast.makeText(this, "Error showing profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun <T : AppCompatActivity> navigateToActivity(activityClass: Class<T>) {
        Log.d(TAG, "navigateToActivity: Navigating to ${activityClass.simpleName}")
        try {
            startActivity(Intent(this, activityClass))
        } catch (e: Exception) {
            Log.e(TAG, "navigateToActivity: Failed to navigate to ${activityClass.simpleName}", e)
            Toast.makeText(this, "Error opening ${activityClass.simpleName}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun loadUniversities() {
        val loadingProgressBar = findViewById<ProgressBar>(R.id.loadingProgressBar)
        
        fetchJob?.cancel()
        fetchJob = lifecycleScope.launch(Dispatchers.Main) {
            try {
                loadingProgressBar.visibility = View.VISIBLE
                
                val result = withContext(Dispatchers.IO) {
                    FirebaseHelper.fetchUniversities()
                }
                
                result.fold(
                    onSuccess = { universityList ->
                        universities.clear()
                        universities.addAll(universityList)
                        loadingProgressBar.visibility = View.GONE
                    },
                    onFailure = { exception ->
                        Log.e("HomePage", "Error loading universities", exception)
                        Toast.makeText(
                            this@HomePage,
                            "Error loading universities",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadingProgressBar.visibility = View.GONE
                    }
                )
            } catch (e: Exception) {
                Log.e("HomePage", "Error in coroutine", e)
                loadingProgressBar.visibility = View.GONE
            }
        }
    }
//
//    private fun setupProfileNavigation() {
//        val profileImage = findViewById<ShapeableImageView>(R.id.profileImage)
//        profileImage.setOnClickListener {
//            Log.d(TAG, "Profile image clicked: Showing profile dashboard")
//            try {
//                val profileDashboard = ProfileDashboardSheet().apply {
//                    setOnViewProfileClickListener {
//                        startActivity(Intent(this@HomePage, ProfileViewActivity::class.java))
//                        dismiss()
//                    }
//                    setOnLogoutClickListener {
//                        onLogoutClick?.invoke()
//                    }
//                }
//                profileDashboard.show(supportFragmentManager, ProfileDashboardSheet.TAG)
//            } catch (e: Exception) {
//                Log.e(TAG, "Error showing profile dashboard", e)
//                Toast.makeText(this, "Error showing profile", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

    override fun onDestroy() {
        super.onDestroy()
        fetchJob?.cancel()
        universities.clear()
    }
}