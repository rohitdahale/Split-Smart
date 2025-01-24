package com.rohit.splitsmart.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rohit.splitsmart.Model.Member
import com.rohit.splitsmart.databinding.ItemSelectedMemberBinding


class SelectedMembersAdapter(
    private val onRemoveClick: (Member) -> Unit
) : ListAdapter<Member, SelectedMembersAdapter.MemberViewHolder>(MemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemSelectedMemberBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MemberViewHolder(
        private val binding: ItemSelectedMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: Member) {
            binding.tvMemberName.text = member.name
            binding.tvMemberEmail.text = member.email
            binding.btnRemove.setOnClickListener { onRemoveClick(member) }
        }
    }

    class MemberDiffCallback : DiffUtil.ItemCallback<Member>() {
        override fun areItemsTheSame(oldItem: Member, newItem: Member) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Member, newItem: Member) =
            oldItem == newItem
    }
}