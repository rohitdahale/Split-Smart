package com.rohit.splitsmart.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rohit.splitsmart.Model.Member
import com.rohit.splitsmart.databinding.ItemMemberBinding
import java.text.NumberFormat
import java.util.Locale

class MembersAdapter : ListAdapter<Member, MembersAdapter.MemberViewHolder>(MemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemMemberBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MemberViewHolder(
        private val binding: ItemMemberBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

        fun bind(member: Member) {
            binding.apply {
                tvMemberName.text = member.name
                tvMemberEmail.text = member.email
                tvMemberBalance.text = currencyFormatter.format(member.balance)
                tvMemberBalance.setTextColor(
                    if (member.balance >= 0)
                        itemView.context.getColor(android.R.color.holo_green_dark)
                    else
                        itemView.context.getColor(android.R.color.holo_red_dark)
                )
            }
        }
    }

    private class MemberDiffCallback : DiffUtil.ItemCallback<Member>() {
        override fun areItemsTheSame(oldItem: Member, newItem: Member): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Member, newItem: Member): Boolean {
            return oldItem == newItem
        }
    }
}