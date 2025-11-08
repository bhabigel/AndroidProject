package com.firstapp.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.firstapp.myapplication.R
import com.firstapp.myapplication.model.ScheduleResponseDto
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ScheduleAdapter : ListAdapter<ScheduleResponseDto, ScheduleAdapter.ViewHolder>(ScheduleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val schedule = getItem(position)
        holder.bind(schedule)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val habitIcon: ImageView = itemView.findViewById(R.id.ivHabitIcon)
        private val habitName: TextView = itemView.findViewById(R.id.tvHabitName)
        private val time: TextView = itemView.findViewById(R.id.tvTime)
        private val status: ImageView = itemView.findViewById(R.id.ivStatus)

        fun bind(schedule: ScheduleResponseDto) {
            // Set habit icon based on habit type
            habitIcon.setImageResource(getHabitIcon(schedule.habit.type))
            
            // Set habit name
            habitName.text = schedule.habit.name

            // Format and set time
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            time.text = LocalTime.parse(schedule.startTime).format(timeFormatter)

            // Set status icon based on completion status
            status.setImageResource(
                when (schedule.status) {
                    "Completed" -> R.drawable.ic_status_completed
                    else -> R.drawable.ic_status_pending
                }
            )
        }

        private fun getHabitIcon(habitType: String): Int {
            return when (habitType) {
                "EXERCISE" -> R.drawable.ic_habit_exercise
                "HYDRATION" -> R.drawable.ic_habit_water
                "WRITING" -> R.drawable.ic_habit_journal
                else -> R.drawable.ic_habit_default
            }
        }
    }
}

class ScheduleDiffCallback : DiffUtil.ItemCallback<ScheduleResponseDto>() {
    override fun areItemsTheSame(oldItem: ScheduleResponseDto, newItem: ScheduleResponseDto): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ScheduleResponseDto, newItem: ScheduleResponseDto): Boolean {
        return oldItem == newItem
    }
}