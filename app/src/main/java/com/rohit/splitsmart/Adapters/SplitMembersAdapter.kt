package com.rohit.splitsmart.Adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rohit.splitsmart.Model.Member
import com.rohit.splitsmart.R
import java.text.NumberFormat
import java.util.Locale

class SplitMembersAdapter(
    private val onShareChanged: (position: Int, amount: Double) -> Unit
) : ListAdapter<Member, SplitMembersAdapter.ViewHolder>(MemberDiffCallback()) {

    private val memberShares = mutableMapOf<String, Double>()
    private var totalAmount: Double = 0.0

    fun setEqualSplit(amount: Double) {
        totalAmount = amount
        val equalShare = if (currentList.isNotEmpty()) amount / currentList.size else 0.0
        currentList.forEach { member ->
            memberShares[member.id] = equalShare
        }
        notifyDataSetChanged()
    }

    fun getMemberShares(): Map<String, Double> = memberShares.toMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_split_member, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMemberName: TextView = itemView.findViewById(R.id.tvMemberName)
        private val etAmount: EditText = itemView.findViewById(R.id.etAmount)
        private var textWatcher: TextWatcher? = null

        fun bind(member: Member) {
            tvMemberName.text = member.name

            // Remove previous TextWatcher if exists
            textWatcher?.let { etAmount.removeTextChangedListener(it) }

            // Set current amount
            val currentAmount = memberShares[member.id] ?: 0.0
            etAmount.setText(if (currentAmount > 0) currentAmount.toString() else "")

            // Create and set new TextWatcher
            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    try {
                        val amount = s.toString().toDoubleOrNull() ?: 0.0
                        memberShares[member.id] = amount
                        onShareChanged(adapterPosition, amount)
                    } catch (e: NumberFormatException) {
                        memberShares[member.id] = 0.0
                        onShareChanged(adapterPosition, 0.0)
                    }
                }
            }

            etAmount.addTextChangedListener(textWatcher)
        }
    }

    class MemberDiffCallback : DiffUtil.ItemCallback<Member>() {
        override fun areItemsTheSame(oldItem: Member, newItem: Member): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Member, newItem: Member): Boolean {
            return oldItem == newItem
        }
    }
}