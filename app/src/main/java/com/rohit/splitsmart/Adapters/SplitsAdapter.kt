package com.rohit.splitsmart.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rohit.splitsmart.Model.SplitDetail
import com.rohit.splitsmart.R
import com.rohit.splitsmart.databinding.ItemSplitDetailBinding
import java.text.NumberFormat
import java.util.Locale

class SplitsAdapter : ListAdapter<SplitDetail, SplitsAdapter.ViewHolder>(SplitDetailDiffCallback()) {

    private var isExpenseSettled = false

    fun setExpenseSettled(settled: Boolean) {
        isExpenseSettled = settled
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSplitDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), isExpenseSettled)
    }

    class ViewHolder(
        private val binding: ItemSplitDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(splitDetail: SplitDetail, isExpenseSettled: Boolean) {
            binding.apply {
                tvMemberName.text = splitDetail.memberName

                // Format amount with currency
                val formatter = NumberFormat.getCurrencyInstance(Locale.US)
                tvAmount.text = formatter.format(splitDetail.amount)

                // Set status and adjust colors based on settled state
                when {
                    isExpenseSettled -> {
                        tvStatus.text = "Settled"
                        tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.deepOrange))
                        root.alpha = 0.8f // Slightly dim the entire item to indicate settled state

                        // Optional: Add a settled indicator
                        tvAmount.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray))
                    }
                    splitDetail.status == "Paid" -> {
                        tvStatus.text = "Paid"
                        tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.green))
                        root.alpha = 1.0f
                        tvAmount.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.black))
                    }
                    else -> {
                        tvStatus.text = "Owed"
                        tvStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.red))
                        root.alpha = 1.0f
                        tvAmount.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.black))
                    }
                }

                // Add visual feedback for different states
                val backgroundColor = when {
                    isExpenseSettled -> ContextCompat.getColor(itemView.context, R.color.settledBackground)
                    splitDetail.status == "Paid" -> ContextCompat.getColor(itemView.context, R.color.paidBackground)
                    else -> ContextCompat.getColor(itemView.context, R.color.owedBackground)
                }
                root.setBackgroundColor(backgroundColor)
            }
        }
    }

    private class SplitDetailDiffCallback : DiffUtil.ItemCallback<SplitDetail>() {
        override fun areItemsTheSame(oldItem: SplitDetail, newItem: SplitDetail): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: SplitDetail, newItem: SplitDetail): Boolean {
            return oldItem == newItem
        }
    }
}