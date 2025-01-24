package com.rohit.splitsmart.Activity

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.View
import kotlin.math.abs
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.rohit.splitsmart.Adapters.SplitMembersAdapter
import com.rohit.splitsmart.Model.Group
import com.rohit.splitsmart.Model.Member
import com.rohit.splitsmart.databinding.ActivityAddExpenseBinding
import com.rohit.splitsmart.Util.ReceiptOcrUtils
import com.rohit.splitsmart.utils.ReceiptUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

class AddExpenseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddExpenseBinding
    private lateinit var splitMembersAdapter: SplitMembersAdapter
    private val database = Firebase.database.reference
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var groupId: String? = null
    private var group: Group? = null
    private var selectedReceiptUri: Uri? = null
    private var currentPaidById: String? = null
    private var membersList: List<Member> = emptyList()

    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            selectedReceiptUri?.let { uri ->
                showSelectedReceipt(uri)
                processReceiptImage(uri)
            }
        }
    }

    private val selectImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedReceiptUri = it
            showSelectedReceipt(it)
            processReceiptImage(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        groupId = intent.getStringExtra("GROUP_ID")
        if (groupId == null) {
            Toast.makeText(this, "Invalid group ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        fetchGroupDetails()
        setupImageCapture()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add Expense"
    }

    private fun setupRecyclerView() {
        splitMembersAdapter = SplitMembersAdapter(
            onShareChanged = { position, amount ->
                updateManualSplitAmount(position, amount)
            }
        )
        binding.rvSplitMembers.apply {
            layoutManager = LinearLayoutManager(this@AddExpenseActivity)
            adapter = splitMembersAdapter
        }
    }

    private fun setupImageCapture() {
        binding.btnTakePhoto.setOnClickListener {
            // Create file for photo
            val photoFile = File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "receipt_${System.currentTimeMillis()}.jpg"
            )
            selectedReceiptUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                photoFile
            )
            takePhoto.launch(selectedReceiptUri)
        }

        binding.btnAttachReceipt.setOnClickListener {
            selectImage.launch("image/*")
        }
    }

    private fun processReceiptImage(uri: Uri) {
        lifecycleScope.launch {
            try {
                showLoading(true)
                binding.progressText.text = "Processing receipt..."

                val extractedData = ReceiptOcrUtils.extractReceiptData(this@AddExpenseActivity, uri)

                // Update UI with extracted data
                extractedData.merchantName?.let {
                    binding.titleInput.setText(it)
                }

                extractedData.totalAmount?.let { amount ->
                    binding.amountInput.setText(String.format("%.2f", amount))
                    if (binding.switchSplitEqually.isChecked) {
                        splitMembersAdapter.setEqualSplit(amount)
                    }
                }

                // Save extracted text to expense data
                binding.descriptionInput.setText(
                    buildString {
                        append(binding.descriptionInput.text)
                        if (isNotEmpty()) append("\n\n")
                        append("Extracted from receipt:\n")
                        append(extractedData.rawText)
                    }
                )

                showSnackbar("Receipt processed successfully")
            } catch (e: Exception) {
                showError("Failed to process receipt: ${e.message}")
            } finally {
                showLoading(false)
                binding.progressText.text = ""
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun updateManualSplitAmount(position: Int, amount: Double) {
        val currentAmount = binding.amountInput.text.toString().toDoubleOrNull() ?: 0.0
        var totalShares = 0.0

        // Calculate total shares excluding the current position
        splitMembersAdapter.getMemberShares().forEach { (_, share) ->
            totalShares += share
        }

        // Validate if total shares exceed the total amount
        if (totalShares > currentAmount) {
            showError("Total shares cannot exceed the total amount")
            // Reset the amount for this position
            splitMembersAdapter.notifyItemChanged(position)
        }
    }


    private fun setupPaidBySpinner(members: List<Member>) {
        val memberNames = members.map { it.name }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            memberNames
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.paidBySpinner.adapter = adapter
        binding.paidBySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentPaidById = members[position].id
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Set default selection to current user
        val currentUserPosition = members.indexOfFirst { it.id == currentUser?.uid }
        if (currentUserPosition != -1) {
            binding.paidBySpinner.setSelection(currentUserPosition)
        }
    }

    private fun setupListeners() {
        binding.saveButton.setOnClickListener {
            if (validateForm()) {
                lifecycleScope.launch {
                    try {
                        showLoading(true)
                        saveExpenseWithReceipt()
                    } finally {
                        showLoading(false)
                    }
                }
            }
        }

        binding.btnAttachReceipt.setOnClickListener {
            selectImage.launch("image/*")
        }

        binding.switchSplitEqually.setOnCheckedChangeListener { _, isChecked ->
            binding.rvSplitMembers.visibility = if (isChecked) View.GONE else View.VISIBLE
            if (isChecked) {
                val amount = binding.amountInput.text.toString().toDoubleOrNull() ?: 0.0
                splitMembersAdapter.setEqualSplit(amount)
            }
        }

        binding.amountInput.addTextChangedListener { text ->
            val amount = text.toString().toDoubleOrNull() ?: 0.0
            if (binding.switchSplitEqually.isChecked) {
                splitMembersAdapter.setEqualSplit(amount)
            }
        }
    }


    private fun showSelectedReceipt(uri: Uri) {
        binding.ivReceiptPreview.apply {
            visibility = View.VISIBLE
            Glide.with(this@AddExpenseActivity)
                .load(uri)
                .into(this)
        }
        binding.tvNoReceipt.visibility = View.GONE
    }

    private suspend fun saveExpenseWithReceipt() {
        try {
            var receiptUrl = ""
            var extractedText = ""

            // Handle receipt upload if present
            if (selectedReceiptUri != null) {
                try {
                    // Upload image
                    receiptUrl = ReceiptUtils.uploadReceipt(this@AddExpenseActivity, selectedReceiptUri!!)

                    // Extract text if not already processed
                    if (binding.descriptionInput.text?.contains("Extracted from receipt") != true) {
                        val extractedData = ReceiptOcrUtils.extractReceiptData(
                            this@AddExpenseActivity,
                            selectedReceiptUri!!
                        )
                        extractedText = extractedData.rawText
                    }
                } catch (e: Exception) {
                    showError("Failed to process receipt: ${e.message}. Continuing without receipt.")
                    e.printStackTrace()
                }
            }

            val title = binding.titleInput.text.toString()
            val amount = binding.amountInput.text.toString().toDoubleOrNull() ?: 0.0
            val description = binding.descriptionInput.text.toString()

            // Validate total split amount matches expense amount
            val splitMap = if (binding.switchSplitEqually.isChecked) {
                val memberCount = membersList.size
                val equalShare = amount / memberCount
                membersList.associate { it.id to equalShare }
            } else {
                val shares = splitMembersAdapter.getMemberShares()
                val totalShares = shares.values.sum()

                // Verify total shares match the expense amount
                if (abs(totalShares - amount) > 0.01) { // Using small delta for floating point comparison
                    throw IllegalStateException("Split amounts (${totalShares}) don't match total expense amount (${amount})")
                }
                shares
            }

            val expenseId = UUID.randomUUID().toString()
            val expenseData = hashMapOf(
                "id" to expenseId,
                "title" to title,
                "amount" to amount,
                "description" to description,
                "date" to System.currentTimeMillis(),
                "paidBy" to currentPaidById,
                "split" to splitMap,
                "receiptUrl" to receiptUrl,
                "extractedText" to extractedText
            )

            try {
                saveExpenseToDatabase(expenseId, expenseData, amount)
            } catch (e: Exception) {
                // If database save fails and we uploaded a receipt, try to clean it up
                if (receiptUrl.isNotEmpty()) {
                    try {
                        ReceiptUtils.deleteReceipt(receiptUrl)
                    } catch (cleanupError: Exception) {
                        // Log cleanup error but throw original exception
                        cleanupError.printStackTrace()
                    }
                }
                throw e
            }
        } catch (e: Exception) {
            showError("Failed to save expense: ${e.message}")
            throw e
        }
    }


    private suspend fun saveExpenseToDatabase(expenseId: String, expenseData: HashMap<String, Any?>, amount: Double) {
        try {
            database.child("Groups").child(groupId!!)
                .child("expenses").child(expenseId)
                .setValue(expenseData).await()

            database.child("Groups").child(groupId!!)
                .child("totalExpense")
                .setValue((group?.totalExpense ?: 0.0) + amount).await()

            showSuccessAndFinish("Expense added successfully")
        } catch (e: Exception) {
            showError("Failed to save to database: ${e.message}")
            throw e
        }
    }


    private fun fetchGroupDetails() {
        showLoading(true)
        database.child("Groups").child(groupId!!)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    group = snapshot.getValue(Group::class.java)?.apply {
                        id = groupId!!
                    }

                    if (group == null) {
                        showError("Group not found")
                        finish()
                        return
                    }

                    fetchMembersDetails()
                }

                override fun onCancelled(error: DatabaseError) {
                    showError("Failed to fetch group details: ${error.message}")
                    showLoading(false)
                }
            })
    }

    private fun fetchMembersDetails() {
        group?.let { group ->
            val memberIds = group.members.keys
            val membersList = mutableListOf<Member>()

            memberIds.forEach { memberId ->
                database.child("Users").child(memberId)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val name = snapshot.child("name").value as? String ?: "Unknown"
                        val email = snapshot.child("email").value as? String ?: ""

                        membersList.add(Member(memberId, name, email))

                        if (membersList.size == memberIds.size) {
                            this.membersList = membersList
                            setupPaidBySpinner(membersList)
                            splitMembersAdapter.submitList(membersList)
                            showLoading(false)
                        }
                    }
                    .addOnFailureListener { e ->
                        showError("Failed to fetch member details: ${e.message}")
                        showLoading(false)
                    }
            }
        }
    }

    private fun setupMembersList() {
        group?.let { group ->
            val members = group.members.mapNotNull { (id, data) ->
                val memberData = data as? Map<String, Any>
                val name = memberData?.get("name") as? String ?: return@mapNotNull null
                val email = memberData["email"] as? String ?: return@mapNotNull null
                Member(id, name, email)
            }

            setupPaidBySpinner(members)
            splitMembersAdapter.submitList(members)
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (binding.titleInput.text.isNullOrBlank()) {
            binding.titleLayout.error = "Title is required"
            isValid = false
        } else {
            binding.titleLayout.error = null
        }

        if (!validateAmount(binding.amountInput.text.toString())) {
            binding.amountLayout.error = "Enter a valid amount"
            isValid = false
        } else {
            binding.amountLayout.error = null
        }

        if (currentPaidById == null) {
            showError("Please select who paid")
            isValid = false
        }

        return isValid
    }

    private fun validateAmount(amount: String): Boolean {
        return try {
            val value = amount.toDouble()
            value > 0
        } catch (e: NumberFormatException) {
            false
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.saveButton.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccessAndFinish(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(snackbar: Snackbar, event: Int) {
                    finish()
                }
            }).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}