package com.firstapp.myapplication.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.firstapp.myapplication.R
import com.firstapp.myapplication.auth.AuthManager
import com.firstapp.myapplication.network.Network
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    companion object { private const val TAG = "HomeFragment" }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AuthManager.init(requireContext())
        fetchSchedules(view)
    }

    private fun fetchSchedules(root: View) {
        val container = root.findViewById<LinearLayout>(R.id.llSchedules)
        val statusTv = root.findViewById<TextView>(R.id.tvStatus)
        statusTv.text = "Loading..."
        lifecycleScope.launch {
            try {
                val resp = Network.api.getSchedulesByDay(null)
                if (resp.isSuccessful) {
                    val list = resp.body() ?: emptyList()
                    statusTv.text = "${list.size} schedules"
                    container.removeAllViews()
                    for (s in list) {
                        val tv = TextView(requireContext()).apply {
                            text = "#${s.id} — ${s.start_time ?: "?"} — ${s.status ?: "?"} — ${s.notes ?: ""}"
                            textSize = 16f
                            setPadding(8,8,8,8)
                            setOnClickListener {
                                try {
                                    val idInt = s.id.toInt()
                                    val intent = android.content.Intent(requireContext(), ScheduleDetailActivity::class.java)
                                    intent.putExtra(ScheduleDetailActivity.EXTRA_SCHEDULE_ID, idInt)
                                    startActivity(intent)
                                } catch (ex: Exception) {
                                    Log.e(TAG, "Invalid schedule id", ex)
                                }
                            }
                        }
                        container.addView(tv)
                    }
                } else {
                    statusTv.text = "Failed: ${resp.code()}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "fetch schedules", e)
                statusTv.text = "Error: ${e.localizedMessage}"
            }
        }
    }
}
