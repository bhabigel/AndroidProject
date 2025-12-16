package com.firstapp.myapplication.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.firstapp.myapplication.R
import com.firstapp.myapplication.models.ProfileResponseDto
import com.firstapp.myapplication.models.UpdateProfileDto
import com.firstapp.myapplication.network.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class EditProfileActivity : AppCompatActivity() {

    private var currentProfile: ProfileResponseDto? = null
    
    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadProfileImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        setupViews()
        loadProfile()
    }

    private fun setupViews() {
        // Back button
        findViewById<TextView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Save button
        findViewById<TextView>(R.id.btnSave).setOnClickListener {
            saveProfile()
        }
        
        // Profile photo change button
        findViewById<TextView>(R.id.btnChangePhoto)?.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
    }

    private fun loadProfile() {
        lifecycleScope.launch {
            try {
                val response = Network.api.getProfile()
                if (response.isSuccessful) {
                    val profile = response.body()
                    if (profile != null) {
                        currentProfile = profile
                        populateFields(profile)
                    } else {
                        showError("Profile not found")
                    }
                } else {
                    showError("Failed to load profile: ${response.code()}")
                }
            } catch (e: Exception) {
                showError("Error loading profile: ${e.localizedMessage}")
            }
        }
    }

    private fun populateFields(profile: ProfileResponseDto) {
        findViewById<EditText>(R.id.etUsername).setText(profile.username ?: "")
        findViewById<EditText>(R.id.etEmail).setText(profile.email ?: "")
        findViewById<EditText>(R.id.etDescription).setText(profile.description ?: "")
        
        // Load profile image if available
        // For now, just show the default placeholder
        // In a production app, you would load the image from profile.profileImageUrl using an image loading library like Glide or Picasso
    }

    private fun uploadProfileImage(uri: Uri) {
        lifecycleScope.launch {
            try {
                // Show loading state
                findViewById<TextView>(R.id.btnChangePhoto)?.text = "Uploading..."
                findViewById<TextView>(R.id.btnChangePhoto)?.isEnabled = false
                
                // Read image bytes
                val bytes = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                
                if (bytes == null) {
                    Toast.makeText(this@EditProfileActivity, "Failed to read image", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Create multipart body
                val requestFile = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("profileImage", "profile.jpg", requestFile)

                // Upload to server
                val response = Network.api.uploadProfileImage(part)
                if (response.isSuccessful) {
                    Toast.makeText(this@EditProfileActivity, "Profile image updated successfully", Toast.LENGTH_SHORT).show()
                    // Update the UI to show the new image
                    findViewById<ImageView>(R.id.ivProfilePhoto)?.setImageURI(uri)
                    // Refresh the profile data
                    loadProfile()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@EditProfileActivity, "Failed to upload image: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivity, "Error uploading image: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                // Restore button state
                findViewById<TextView>(R.id.btnChangePhoto)?.text = "Change Photo"
                findViewById<TextView>(R.id.btnChangePhoto)?.isEnabled = true
            }
        }
    }

    private fun saveProfile() {
        val username = findViewById<EditText>(R.id.etUsername).text.toString().trim()
        val description = findViewById<EditText>(R.id.etDescription).text.toString().trim()

        lifecycleScope.launch {
            try {
                findViewById<TextView>(R.id.btnSave).text = "Saving..."
                findViewById<TextView>(R.id.btnSave).isEnabled = false

                // Create UpdateProfileDto with only non-empty fields
                val updateData = UpdateProfileDto(
                    username = if (username.isNotEmpty()) username else null,
                    description = if (description.isNotEmpty()) description else null
                )

                val response = Network.api.updateProfile(updateData)
                if (response.isSuccessful) {
                    Toast.makeText(this@EditProfileActivity, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(this@EditProfileActivity, "Failed to update: ${response.code()}\n$errorBody", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditProfileActivity, "Error updating profile: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                findViewById<TextView>(R.id.btnSave).text = "Save"
                findViewById<TextView>(R.id.btnSave).isEnabled = true
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}