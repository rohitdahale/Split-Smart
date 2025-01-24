package com.rohit.splitsmart.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.rohit.splitsmart.databinding.ActivityGroupDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.rohit.splitsmart.Adapters.ExpensesAdapter
import com.rohit.splitsmart.Adapters.MembersAdapter
import com.rohit.splitsmart.Model.Balance
import com.rohit.splitsmart.Model.Expense
import com.rohit.splitsmart.Model.Group
import com.rohit.splitsmart.Model.Member
import com.rohit.splitsmart.R


class GroupDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGroupDetailBinding
    private lateinit var membersAdapter: MembersAdapter
    private lateinit var expensesAdapter: ExpensesAdapter
    private val database = Firebase.database.reference
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var groupId: String? = null
    private var group: Group? = null

    private var unpaidExpenses = mutableMapOf<String, Double>()
    private var memberBalances = mutableMapOf<String, Double>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getStringExtra("GROUP_ID")
        if (groupId == null) {
            Toast.makeText(this, "Invalid group ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerViews()
        setupListeners()
        fetchGroupDetails()

    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecyclerViews() {
        // Setup members RecyclerView
        membersAdapter = MembersAdapter()
        binding.rvMembers.apply {
            layoutManager = LinearLayoutManager(this@GroupDetailActivity)
            adapter = membersAdapter
        }

        // Setup expenses RecyclerView
        expensesAdapter = ExpensesAdapter { expense ->
            navigateToExpenseDetails(expense)
        }
        binding.rvExpenses.apply {
            layoutManager = LinearLayoutManager(this@GroupDetailActivity)
            adapter = expensesAdapter
        }
    }

    private fun setupListeners() {
        binding.fabAddExpense.setOnClickListener {
            navigateToAddExpense()
        }
    }

    private fun fetchGroupDetails() {
        showLoading(true)

        database.child("Groups").child(groupId!!)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    group = snapshot.getValue(Group::class.java)?.apply {
                        id = groupId as String
                    }

                    if (group == null) {
                        showError("Group not found")
                        finish()
                        return
                    }

                    // Fetch members' names and expenses
                    fetchMembersNames()  // This fetches and sets the members' names
                    fetchExpenses()      // This fetches the expenses
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Failed to fetch group details: ${error.message}")
                    showLoading(false)
                }
            })
    }

    private fun fetchMembersNames() {
        group?.members?.let { members ->
            val memberIds = members.keys

            // Create a list to hold the member details
            val memberDetails = mutableListOf<Member>()

            // Iterate over the member IDs and fetch their names
            for (memberId in memberIds) {
                database.child("Users").child(memberId).child("name")
                    .get().addOnSuccessListener { nameSnapshot ->
                        val name = nameSnapshot.value as? String ?: "Unknown"
                        // Retrieve the email (or any other data) if needed

                        // Create the Member object and add it to the list
                        val member = Member(memberId, name)
                        memberDetails.add(member)

                        // Submit the list to the adapter when all names are fetched
                        if (memberDetails.size == memberIds.size) {
                            membersAdapter.submitList(memberDetails)
                            calculateAndDisplayBalance(memberDetails)  // Calculate balance after fetching members
                        }
                    }
            }
        }
    }


    private fun fetchExpenses() {
        database.child("Groups").child(groupId!!).child("expenses")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val expenses = mutableListOf<Expense>()
                    snapshot.children.forEach { expenseSnapshot ->
                        expenseSnapshot.getValue(Expense::class.java)?.let { expense ->
                            expense.id = expenseSnapshot.key.toString()
                            expenses.add(expense)
                        }
                    }
                    updateExpensesList(expenses)
                    showLoading(false)
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Failed to fetch expenses: ${error.message}")
                    showLoading(false)
                }
            })
    }

    private fun updateUI() {
//        group?.let { group ->
//            supportActionBar?.title = group.name
//
//            // Update total expense
//            val formatter = NumberFormat.getCurrencyInstance(Locale.US)
//            binding.tvTotalExpense.text = "Total: ${formatter.format(group.totalExpense)}"
//
//            // Update members list
//            val members = group.members.mapNotNull { (id, data) ->
//                // Ensure data is a Map<String, Any>
//                val memberData = data as? Map<String, Any>
//                val name = (memberData?.get("name") as? String)?.trim() ?: ""
//                val email = (memberData?.get("email") as? String)?.trim() ?: ""
//
//                // Ensure both name and email are non-empty before creating Member
//                if (name.isNotEmpty() && email.isNotEmpty()) {
//                    Member(id, name, email)
//                } else {
//                    null // Skip invalid members with missing data
//                }
//            }
//
//            // Submit list to the adapter
//            membersAdapter.submitList(members)
//
//            // Calculate and update balance
//            calculateAndDisplayBalance()
//        }
    }

    private fun calculateAndDisplayBalance(members: List<Member>) {
        group?.let { group ->
            memberBalances.clear()
            unpaidExpenses.clear()

            // Calculate who owes what
            database.child("Groups").child(groupId!!).child("expenses")
                .get()
                .addOnSuccessListener { snapshot ->
                    snapshot.children.forEach { expenseSnapshot ->
                        val expense = expenseSnapshot.getValue(Expense::class.java) ?: return@forEach

                        // Track who paid and who owes
                        expense.split.forEach { (userId, amount) ->
                            if (userId != expense.paidBy) {
                                memberBalances[userId] = (memberBalances[userId] ?: 0.0) + amount
                                unpaidExpenses[expense.id] = amount
                            }
                        }
                    }

                    // Update UI and trigger notifications if needed
                    updateBalanceDisplay()
                    checkAndUpdatePaymentStatus()
                }
        }
    }

    private fun updateBalanceDisplay() {
        val balanceText = StringBuilder()
        memberBalances.forEach { (userId, amount) ->
            val memberName = getMemberName(userId)
            if (amount > 0) {
                balanceText.append("$memberName owes: $${String.format("%.2f", amount)}\n")
            }
        }
        binding.tvBalanceOverview.text = balanceText.toString()
    }

    private fun checkAndUpdatePaymentStatus() {
        val currentTime = System.currentTimeMillis()

        database.child("Groups").child(groupId!!).child("paymentStatuses")
            .setValue(memberBalances)
            .addOnSuccessListener {
                // Update notification preferences if needed
                updateNotificationPreferences()
            }
    }

    private fun updateNotificationPreferences() {
        val currentTime = System.currentTimeMillis() - (16 * 60 * 1000) // Subtract 16 minutes to trigger immediate notification
        val notificationData = HashMap<String, Any>()
        memberBalances.forEach { (userId, amount) ->
            if (amount > 0) {
                notificationData[userId] = hashMapOf(
                    "lastNotified" to currentTime,
                    "amountDue" to amount
                )
            }
        }
        // ... rest of the function
    }

    private fun getMemberName(userId: String): String {
        return membersAdapter.currentList.find { it.id == userId }?.name ?: "Unknown Member"
    }


    private fun updateExpensesList(expenses: List<Expense>) {
        expensesAdapter.submitList(expenses.sortedByDescending { it.date })
    }

    private fun navigateToAddExpense() {
        val intent = Intent(this, AddExpenseActivity::class.java).apply {
            putExtra("GROUP_ID", groupId)
        }
        startActivity(intent)
    }

    private fun navigateToExpenseDetails(expense: Expense) {
        val intent = Intent(this, ExpenseDetailActivity::class.java).apply {
            putExtra("GROUP_ID", groupId)
            putExtra("EXPENSE_ID", expense.id)
        }
        startActivity(intent)
    }

    private fun showDeleteGroupDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Group")
            .setMessage("Are you sure you want to delete this group? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteGroup()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteGroup() {
        showLoading(true)

        group?.let { group ->
            // Remove group from all members' groups list
            group.members.keys.forEach { memberId ->
                database.child("Users").child(memberId)
                    .child("Groups").child(groupId!!)
                    .removeValue()
            }

            // Delete the group itself
            database.child("Groups").child(groupId!!)
                .removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Group deleted successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    showError("Failed to delete group: ${e.message}")
                    showLoading(false)
                }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.group_detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.action_edit_group -> {
                // Navigate to edit group screen
                true
            }
            R.id.action_delete_group -> {
                showDeleteGroupDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}