package com.rohit.splitsmart.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rohit.splitsmart.Model.Member
import com.rohit.splitsmart.databinding.ItemAvailableUserBinding

class AvailableUsersAdapter(
    private val onUserClick: (Member) -> Unit
) : ListAdapter<Member, AvailableUsersAdapter.UserViewHolder>(SelectedMembersAdapter.MemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemAvailableUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(
        private val binding: ItemAvailableUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onUserClick(getItem(position))
                }
            }
        }

        fun bind(member: Member) {
            binding.tvUserName.text = member.name
            binding.tvUserEmail.text = member.email
        }
    }
}