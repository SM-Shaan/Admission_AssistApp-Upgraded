package com.shaan.androiduicomponents.models

import java.io.Serializable
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class University(
    val generalInfo: GeneralInfo = GeneralInfo(),
    val academicInfo: UniAcademicInfo = UniAcademicInfo(),
    val admissionInfo: AdmissionInfo = AdmissionInfo(),
    val additionalInfo: AdditionalInfo = AdditionalInfo(),
    @ServerTimestamp
    val createdAt: Date? = null
) : Serializable

data class GeneralInfo(
    val name: String = "",
    val location: String = "",
    val established: Int = 0,
    val imageUrl: String = "",
    val websiteUrl: String = "",
    val description: String = "",
    val universityType: String = ""
) : Serializable

data class UniAcademicInfo(
    val departments: List<String> = emptyList(),
    val totalSeats: Int = 0,
    val programSpecificGpaRequirements: Map<String, Map<String, Double>> = emptyMap(),
    val quotaAdjustments: Map<String, Int> = emptyMap()
) : Serializable

data class AdmissionInfo(
    val requiredSSCGpa: Double = 0.0,
    val requiredHSCGpa: Double = 0.0,
    val admissionTestRequired: Boolean = false,
    val admissionTestSubjects: List<String> = emptyList(),
    val admissionTestMarksDistribution: Map<String, Int> = emptyMap(),
    val admissionTestSyllabus: String = "",
    val admissionTestDetails: AdmissionTestDetails = AdmissionTestDetails()
) : Serializable

data class AdmissionTestDetails(
    val date: String = "",
    val time: String = "",
    val venue: String = "",
    val duration: String = "",
    val totalMarks: Int = 0,
    val passMarks: Int = 0,
    val negativeMarking: Boolean = false
) : Serializable

data class AdditionalInfo(
    val seatAvailability: Map<String, Int> = emptyMap(),
    val additionalRequirements: Map<String, String> = emptyMap(),
    val applicationLink: String = ""
) : Serializable
