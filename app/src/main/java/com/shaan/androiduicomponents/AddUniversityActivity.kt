package com.shaan.androiduicomponents
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.shaan.androiduicomponents.databinding.ActivityAddUniversityBinding
import com.shaan.androiduicomponents.models.GeneralInfo
import com.shaan.androiduicomponents.models.UniAcademicInfo
import com.shaan.androiduicomponents.models.University
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.shaan.androiduicomponents.models.AdditionalInfo
import com.shaan.androiduicomponents.models.AdmissionInfo
import com.shaan.androiduicomponents.models.AdmissionTestDetails
import java.util.*
import android.widget.Button
import android.app.ProgressDialog

class AddUniversityActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddUniversityBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddUniversityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()
        
        // Initialize Progress Dialog
        progressDialog = ProgressDialog(this).apply {
            setMessage("Saving university data...")
            setCancelable(false)
        }

        setupUniversityTypeSpinner()
        setupAdmissionTestVisibility()
        setupSubmitButton()
    }

    private fun setupUniversityTypeSpinner() {
        val universityTypes = arrayOf("Public", "Private", "International")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, universityTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.universityTypeSpinner.adapter = adapter
    }

    private fun setupAdmissionTestVisibility() {
        binding.admissionTestCheckBox.setOnCheckedChangeListener { _, isChecked ->
            binding.admissionTestLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun setupSubmitButton() {
        binding.submitButton.setOnClickListener {
            if (validateInputs()) {
                saveUniversityToFirestore()
            }
        }
    }

    private fun saveUniversityToFirestore() {
        progressDialog.show()

        try {
            val university = createUniversity()
            
            db.collection("universitylist")
                .add(university)
                .addOnSuccessListener { documentReference ->
                    progressDialog.dismiss()
                    Toast.makeText(
                        this,
                        "University added successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                    setResult(RESULT_OK)
                    finish()
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(
                        this,
                        "Error saving to database: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        } catch (e: IllegalArgumentException) {
            progressDialog.dismiss()
            Toast.makeText(
                this,
                e.message,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun validateInputs(): Boolean {
        with(binding) {
            // General Info validation
            if (nameEditText.text.isNullOrBlank()) {
                nameEditText.error = "Name is required"
                return false
            }

            // Admission Info validation
            try {
                val sscGpa = sscGpaEditText.text.toString().toDouble()
                val hscGpa = hscGpaEditText.text.toString().toDouble()
                if (sscGpa < 0 || sscGpa > 5 || hscGpa < 0 || hscGpa > 5) {
                    Toast.makeText(this@AddUniversityActivity, 
                        "GPA must be between 0 and 5", 
                        Toast.LENGTH_SHORT).show()
                    return false
                }
            } catch (e: NumberFormatException) {
                Toast.makeText(this@AddUniversityActivity, 
                    "Please enter valid GPA values", 
                    Toast.LENGTH_SHORT).show()
                return false
            }

            return true
        }
    }

    private fun createUniversity(): University {
        try {
            with(binding) {
                // Validate required fields with specific error messages
                val name = nameEditText.text.toString().trim().also {
                    if (it.isEmpty()) throw IllegalArgumentException("University name is required")
                }
                
                val location = locationEditText.text.toString().trim().also {
                    if (it.isEmpty()) throw IllegalArgumentException("Location is required")
                }
                
                val established = try {
                    establishedEditText.text.toString().toInt()
                } catch (e: NumberFormatException) {
                    throw IllegalArgumentException("Please enter a valid establishment year")
                }

                val generalInfo = GeneralInfo(
                    name = name,
                    location = location,
                    established = established,
                    imageUrl = imageUrlEditText.text.toString().trim().ifEmpty { "default_url" },
                    websiteUrl = websiteUrlEditText.text.toString().trim().ifEmpty { "" },
                    description = descriptionEditText.text.toString().trim().ifEmpty { "" },
                    universityType = universityTypeSpinner.selectedItem?.toString() ?: "Public"
                )

                val departments = departmentsEditText.text.toString()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .also { 
                        if (it.isEmpty()) throw IllegalArgumentException("At least one department is required")
                    }

                val totalSeats = try {
                    totalSeatsEditText.text.toString().toInt().also {
                        if (it <= 0) throw IllegalArgumentException("Total seats must be greater than 0")
                    }
                } catch (e: NumberFormatException) {
                    throw IllegalArgumentException("Please enter a valid number for total seats")
                }

                val academicInfo = UniAcademicInfo(
                    departments = departments,
                    totalSeats = totalSeats,
                    programSpecificGpaRequirements = mapOf("default" to mapOf("minimum" to 0.0)),
                    quotaAdjustments = mapOf("general" to 100)
                )

                val sscGpa = try {
                    sscGpaEditText.text.toString().toDouble().also {
                        if (it < 0 || it > 5) throw IllegalArgumentException("SSC GPA must be between 0 and 5")
                    }
                } catch (e: NumberFormatException) {
                    throw IllegalArgumentException("Please enter a valid SSC GPA")
                }

                val hscGpa = try {
                    hscGpaEditText.text.toString().toDouble().also {
                        if (it < 0 || it > 5) throw IllegalArgumentException("HSC GPA must be between 0 and 5")
                    }
                } catch (e: NumberFormatException) {
                    throw IllegalArgumentException("Please enter a valid HSC GPA")
                }

                val admissionTestDetails = if (admissionTestCheckBox.isChecked) {
                    try {
                        AdmissionTestDetails(
                            date = testDateEditText.text.toString().trim().also {
                                if (it.isEmpty()) throw IllegalArgumentException("Test date is required")
                            },
                            time = testTimeEditText.text.toString().trim().also {
                                if (it.isEmpty()) throw IllegalArgumentException("Test time is required")
                            },
                            venue = testVenueEditText.text.toString().trim().also {
                                if (it.isEmpty()) throw IllegalArgumentException("Test venue is required")
                            },
                            duration = testDurationEditText.text.toString().trim().also {
                                if (it.isEmpty()) throw IllegalArgumentException("Test duration is required")
                            },
                            totalMarks = totalMarksEditText.text.toString().toInt().also {
                                if (it <= 0) throw IllegalArgumentException("Total marks must be greater than 0")
                            },
                            passMarks = passMarksEditText.text.toString().toInt().also {
                                if (it <= 0) throw IllegalArgumentException("Pass marks must be greater than 0")
                            },
                            negativeMarking = negativeMarkingCheckBox.isChecked
                        )
                    } catch (e: NumberFormatException) {
                        throw IllegalArgumentException("Please enter valid numbers for marks")
                    }
                } else {
                    AdmissionTestDetails(
                        date = "",
                        time = "",
                        venue = "",
                        duration = "",
                        totalMarks = 0,
                        passMarks = 0,
                        negativeMarking = false
                    )
                }

                val admissionInfo = AdmissionInfo(
                    requiredSSCGpa = sscGpa,
                    requiredHSCGpa = hscGpa,
                    admissionTestRequired = admissionTestCheckBox.isChecked,
                    admissionTestSubjects = listOf("General Knowledge", "Math", "English"),  // Default subjects
                    admissionTestMarksDistribution = mapOf("General Knowledge" to 30, "Math" to 40, "English" to 30),
                    admissionTestSyllabus = "Standard admission test syllabus",
                    admissionTestDetails = admissionTestDetails
                )

                val additionalInfo = AdditionalInfo(
                    seatAvailability = mapOf("Regular" to totalSeats),
                    additionalRequirements = mapOf("Age" to "17-21 years"),
                    applicationLink = applicationLinkEditText.text.toString().trim().ifEmpty { "Not available" }
                )

                return University(
                    generalInfo = generalInfo,
                    academicInfo = academicInfo,
                    admissionInfo = admissionInfo,
                    additionalInfo = additionalInfo
                )
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Error creating university data: ${e.message}")
        }
    }
} 