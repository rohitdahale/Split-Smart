package com.rohit.splitsmart.Activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.rohit.splitsmart.Model.Expense
import com.rohit.splitsmart.databinding.ActivityExpenseDetailBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.rohit.splitsmart.Adapters.SplitsAdapter
import com.rohit.splitsmart.Model.SplitDetail
import com.rohit.splitsmart.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpenseDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExpenseDetailBinding
    private lateinit var splitDetailsAdapter: SplitsAdapter
    private val database = Firebase.database.reference
    private var groupId: String? = null
    private var expenseId: String? = null
    private var expense: Expense? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getStringExtra("GROUP_ID")
        expenseId = intent.getStringExtra("EXPENSE_ID")

        if (groupId == null || expenseId == null) {
            Toast.makeText(this, "Invalid expense details", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnSettlePayment.setOnClickListener {
            markAsSettled()
        }

        setupToolbar()
        setupRecyclerView()
        fetchExpenseDetails()

        setupSettleButton()

    }

    private fun setupSettleButton() {
        binding.btnSettlePayment.setOnClickListener {
            expense?.let { expense ->
                if (!expense.settled) {
                    // Mark the expense as settled in Firebase
                    database.child("Groups").child(groupId!!).child("expenses")
                        .child(expense.id)
                        .child("settled")
                        .setValue(true)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Expense marked as settled!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to settle expense: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        }
    }



    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecyclerView() {
        splitDetailsAdapter = SplitsAdapter()
        binding.rvSplitDetails.apply {
            layoutManager = LinearLayoutManager(this@ExpenseDetailActivity)
            adapter = splitDetailsAdapter
        }
    }

    private fun fetchExpenseDetails() {
        showLoading(true)

        database.child("Groups").child(groupId!!).child("expenses").child(expenseId!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    expense = snapshot.getValue(Expense::class.java)?.apply {
                        id = expenseId!!
                    }

                    if (expense == null) {
                        showError("Expense not found")
                        finish()
                        return
                    }

                    updateUI()
                    fetchMembersDetails()

                    // Check if all payments are made
                    if (isFullyPaid(expense!!)) {
                        binding.btnSettlePayment.visibility = View.VISIBLE
                    } else {
                        binding.btnSettlePayment.visibility = View.GONE
                    }

                    showLoading(false)
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Failed to fetch expense details: ${error.message}")
                    showLoading(false)
                }
            })
    }

    private fun isFullyPaid(expense: Expense): Boolean {
        return expense.split.all { (userId, amount) ->
            val paidAmount = expense.split[userId] ?: 0.0
            paidAmount >= amount
        }
    }



    private fun fetchMembersDetails() {
        expense?.let { expense ->
            val splitDetails = mutableListOf<SplitDetail>()
            var fetchedCount = 0

            expense.split.forEach { (userId, amount) ->
                database.child("Users").child(userId).child("name")
                    .get().addOnSuccessListener { nameSnapshot ->
                        val name = nameSnapshot.value as? String ?: "Unknown"

                        val status = if (userId == expense.paidBy) "Paid" else "Owed"
                        splitDetails.add(SplitDetail(userId, name, amount, status))

                        fetchedCount++
                        if (fetchedCount == expense.split.size) {
                            updateSplitDetailsList(splitDetails)
                            showLoading(false)
                        }
                    }
            }
        }
    }

    private fun markAsSettled() {
        expense?.let { expense ->
            database.child("Groups").child(groupId!!).child("expenses").child(expenseId!!)
                .child("settled")
                .setValue(true)
                .addOnSuccessListener {
                    Toast.makeText(this, "Expense marked as settled", Toast.LENGTH_SHORT).show()
                    binding.btnSettlePayment.visibility = View.GONE
                }
                .addOnFailureListener { error ->
                    showError("Failed to mark expense as settled: ${error.message}")
                }
        }
    }


    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        expense?.let { expense ->
            // Set toolbar title
            supportActionBar?.title = expense.title

            // Format and display amount
            val formatter = NumberFormat.getCurrencyInstance(Locale.US)
            binding.tvAmount.text = formatter.format(expense.amount)

            // Format and display date
            val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
            binding.tvDate.text = dateFormatter.format(Date(expense.date))

            // Set paid by text
            database.child("Users").child(expense.paidBy).child("name")
                .get().addOnSuccessListener { nameSnapshot ->
                    val name = nameSnapshot.value as? String ?: "Unknown"
                    binding.tvPaidBy.text = "Paid by $name"
                }

            // Update receipt image if available
            if (expense.receiptUrl.isNotEmpty()) {
                binding.ivReceipt.visibility = View.VISIBLE
                binding.tvNoReceipt.visibility = View.GONE
                Glide.with(this)
                    .load(expense.receiptUrl)
                    .into(binding.ivReceipt)
            } else {
                binding.ivReceipt.visibility = View.GONE
                binding.tvNoReceipt.visibility = View.VISIBLE
            }

            splitDetailsAdapter.setExpenseSettled(expense.settled)

            // Show "Settled" status
            if (expense.settled) {
                binding.tvStatus.visibility = View.VISIBLE
                binding.tvStatus.text = "Settled"
                binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.deepOrange))

                // Update button to show settled state
                binding.btnSettlePayment.isEnabled = false
                binding.btnSettlePayment.text = "Settled"
                binding.btnSettlePayment.setBackgroundColor(ContextCompat.getColor(this, R.color.gray2))
            } else {
                binding.tvStatus.visibility = View.GONE

                // Update button to allow settlement
                binding.btnSettlePayment.isEnabled = true
                binding.btnSettlePayment.text = "Settle Payment"
                binding.btnSettlePayment.setBackgroundColor(ContextCompat.getColor(this, R.color.indigo))
            }
        }
    }


    // ... (continuing from the previous implementation)

    private fun updateSplitDetailsList(splitDetails: List<SplitDetail>) {
        splitDetailsAdapter.submitList(splitDetails.sortedBy { it.memberName })
    }

    private fun showDeleteExpenseDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteExpense()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteExpense() {
        showLoading(true)

        database.child("Groups").child(groupId!!).child("expenses")
            .child(expenseId!!)
            .removeValue()
            .addOnSuccessListener {
                // Update group total expense
                updateGroupTotalExpense()
                Toast.makeText(this, "Expense deleted successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                showError("Failed to delete expense: ${e.message}")
                showLoading(false)
            }
    }

    private fun updateGroupTotalExpense() {
        expense?.let { expense ->
            database.child("Groups").child(groupId!!).child("totalExpense")
                .get()
                .addOnSuccessListener { snapshot ->
                    val currentTotal = snapshot.getValue(Double::class.java) ?: 0.0
                    val newTotal = currentTotal - expense.amount

                    database.child("Groups").child(groupId!!).child("totalExpense")
                        .setValue(newTotal)
                }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.GONE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.expense_detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_edit_expense -> {
                // Navigate to edit expense screen
                true
            }
            R.id.action_delete_expense -> {
                showDeleteExpenseDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}