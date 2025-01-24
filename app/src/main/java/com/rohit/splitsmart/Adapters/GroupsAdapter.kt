package com.rohit.splitsmart.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rohit.splitsmart.Model.Group
import com.rohit.splitsmart.R
import com.rohit.splitsmart.databinding.GroupItemBinding

class GroupsAdapter(
    private val onGroupClick: (Group) -> Unit
) : androidx.recyclerview.widget.ListAdapter<Group, GroupsAdapter.GroupViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<Group>() {
        override fun areItemsTheSame(oldItem: Group, newItem: Group) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Group, newItem: Group) = oldItem == newItem
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = GroupItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class GroupViewHolder(
        private val binding: GroupItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onGroupClick(getItem(position))
                }
            }
        }

        fun bind(group: Group) {
            binding.apply {
                groupName.text = group.name
                membersCount.text = "${group.members.size} members"
                totalExpense.text = "₹%.2f".format(group.totalExpense)

                // Calculate settlement progress
                val totalMembers = group.members.size
                val settledMembers = group.members.values.count { it.settled }
                val progressPercent = if (totalMembers > 0) {
                    (settledMembers * 100f / totalMembers).toInt()
                } else {
                    0
                }

                settlementProgress.progress = progressPercent
                settlementStatus.text = "$progressPercent% settled"

                // Set group image
                groupImage.setImageResource(R.drawable.group)
            }
        }

    }
}