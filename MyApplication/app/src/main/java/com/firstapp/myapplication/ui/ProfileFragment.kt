package com.firstapp.myapplication.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.firstapp.myapplication.R
import com.firstapp.myapplication.auth.AuthManager
import com.firstapp.myapplication.network.Network
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class ProfileFragment : Fragment() {
    companion object { private const val TAG = "ProfileFragment" }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadImage(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AuthManager.init(requireContext())
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_edit_avatar)
        fab.setOnClickListener { pickImageLauncher.launch("image/*") }
        loadProfile(view)
    }

    private fun loadProfile(root: View) {
        val tvName = root.findViewById<TextView>(R.id.tvProfileName)
        val tvEmail = root.findViewById<TextView>(R.id.tvProfileEmail)
        val llHabits = root.findViewById<LinearLayout>(R.id.llHabits)
        val ivAvatar = root.findViewById<ShapeableImageView>(R.id.ivAvatar)

        lifecycleScope.launch {
            try {
                val resp = Network.api.getProfile()
                if (resp.isSuccessful) {
                    val body = resp.body()
                    tvName.text = body?.get("username")?.toString() ?: "-"
                    tvEmail.text = body?.get("email")?.toString() ?: "-"
                    // if backend returns a profile image URL, set it (simple)
                    val imageUrl = body?.get("profileImageUrl")?.toString()
                    if (!imageUrl.isNullOrEmpty()) {
                        // simple approach: let the ImageView load it if remote loader exists
                        // fallback: leave default icon; a proper implementation would use Glide/Picasso
                    }

                    val profileId = (body?.get("id") as? Double)?.toInt() ?: (body?.get("id") as? Int)
                    if (profileId != null) {
                        try {
                            val hresp = Network.api.getHabitsByUser(profileId)
                            if (hresp.isSuccessful) {
                                val list = hresp.body() ?: emptyList()
                                llHabits.removeAllViews()
                                for (h in list) {
                                    val name = h["name"]?.toString() ?: "(no name)"
                                    val tv = TextView(requireContext()).apply { text = name }
                                    val pb = ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal)
                                    pb.max = 100
                                    val percent = (h["progress"] as? Double)?.toInt() ?: 0
                                    pb.progress = percent
                                    llHabits.addView(tv)
                                    llHabits.addView(pb)
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "fetch user habits", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "profile fetch", e)
            }
        }
    }

    private fun uploadImage(uri: Uri) {
        val ivAvatar = view?.findViewById<ShapeableImageView>(R.id.ivAvatar)
        lifecycleScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes == null) {
                    Log.e(TAG, "unable to read image bytes")
                    return@launch
                }

                val requestFile: RequestBody = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", "avatar.jpg", requestFile)

                val resp = withContext(Dispatchers.IO) { Network.api.uploadProfileImage(part) }
                if (resp.isSuccessful) {
                    // show the selected image immediately
                    ivAvatar?.setImageURI(uri)
                } else {
                    Log.e(TAG, "upload failed: ${resp.code()} ${resp.errorBody()?.string()}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "uploadImage", e)
            }
        }
    }
}
