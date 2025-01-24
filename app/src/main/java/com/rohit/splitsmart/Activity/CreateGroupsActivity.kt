package com.rohit.splitsmart.Activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.rohit.splitsmart.Adapters.AvailableUsersAdapter
import com.rohit.splitsmart.Model.Member
import com.rohit.splitsmart.Adapters.SelectedMembersAdapter
import com.rohit.splitsmart.databinding.ActivityCreateGroupsBinding
import java.util.UUID

class CreateGroupsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateGroupsBinding
    private val selectedMembers = mutableListOf<Member>()
    private val allUsers = mutableListOf<Member>()
    private lateinit var membersAdapter: SelectedMembersAdapter
    private lateinit var availableUsersAdapter: AvailableUsersAdapter
    private val database = Firebase.database.reference
    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateGroupsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerViews()
        setupListeners()
        fetchUsers()
    }

    private fun setupRecyclerViews() {
        // Setup selected members RecyclerView
        membersAdapter = SelectedMembersAdapter { member ->
            removeMember(member)
        }
        binding.rvSelectedMembers.apply {
            layoutManager = LinearLayoutManager(this@CreateGroupsActivity)
            adapter = membersAdapter
        }

        // Setup available users RecyclerView
        availableUsersAdapter = AvailableUsersAdapter { member ->
            addMember(member)
        }
        binding.rvAvailableUsers.apply {
            layoutManager = LinearLayoutManager(this@CreateGroupsActivity)
            adapter = availableUsersAdapter
        }
    }

    private fun setupListeners() {
        // Add text change listener for group name
        binding.etGroupName.addTextChangedListener {
            binding.tilGroupName.error = null
            validateForm()
        }

        // Create group button click listener
        binding.btnCreateGroup.setOnClickListener {
            if (validateForm()) {
                createGroup()
            }
        }

        // Search functionality
        binding.etSearch.addTextChangedListener { text ->
            filterUsers(text?.toString() ?: "")
        }
    }

    private fun filterUsers(query: String) {
        val filteredList = if (query.isEmpty()) {
            allUsers
        } else {
            allUsers.filter { member ->
                member.name.contains(query, ignoreCase = true) ||
                        member.email.contains(query, ignoreCase = true)
            }
        }
        availableUsersAdapter.submitList(filteredList)
    }

    private fun fetchUsers() {
        binding.progressBar.visibility = View.VISIBLE
        database.child("Users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allUsers.clear()
                snapshot.children.forEach { userSnapshot ->
                    val userId = userSnapshot.key ?: return@forEach
                    // Skip current user
                    if (userId == currentUser?.uid) return@forEach

                    val name = userSnapshot.child("name").getValue(String::class.java) ?: return@forEach
                    val email = userSnapshot.child("email").getValue(String::class.java) ?: return@forEach

                    allUsers.add(Member(userId, name, email))
                }
                availableUsersAdapter.submitList(allUsers.toList())
                binding.progressBar.visibility = View.GONE
                binding.tvNoUsers.visibility = if (allUsers.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CreateGroupsActivity, "Failed to fetch users: ${error.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.tvNoUsers.visibility = View.VISIBLE
            }
        })
    }


    private fun addMember(member: Member) {
        if (!selectedMembers.any { it.id == member.id }) {
            selectedMembers.add(member)
            membersAdapter.submitList(selectedMembers.toList())
            updateMemberCount()
            validateForm()
        }
    }

    private fun removeMember(member: Member) {
        selectedMembers.remove(member)
        membersAdapter.submitList(selectedMembers.toList())
        updateMemberCount()
        validateForm()
    }

    @SuppressLint("SetTextI18n")
    private fun updateMemberCount() {
        binding.tvMemberCount.text = "${selectedMembers.size} members selected"
        binding.rvSelectedMembers.visibility = if (selectedMembers.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Validate group name
        val groupName = binding.etGroupName.text.toString().trim()
        if (groupName.isEmpty()) {
            binding.tilGroupName.error = "Group name required"
            isValid = false
        } else if (groupName.length < 3) {
            binding.tilGroupName.error = "Group name too short"
            isValid = false
        }

        // Validate member selection
        if (selectedMembers.isEmpty()) {
            Toast.makeText(this, "Add at least one member", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        binding.btnCreateGroup.isEnabled = isValid
        return isValid
    }


    private fun createGroup() {
        val groupName = binding.etGroupName.text.toString().trim()
        val groupId = UUID.randomUUID().toString()

        // Add current user to members
        val currentUserMember = Member(
            id = currentUser?.uid ?: return,
            name = currentUser.displayName ?: "Unknown",
            email = currentUser.email ?: ""
        )

        val allMembers = selectedMembers + currentUserMember

        val membersMap = allMembers.associate {
            it.id to mapOf(
                "name" to it.name,
                "email" to it.email
            )
        }

        val groupData = hashMapOf(
            "name" to groupName,
            "members" to membersMap,
            "totalExpense" to 0.0,
            "createdAt" to System.currentTimeMillis(),
            "createdBy" to currentUser.uid
        )

        binding.progressBar.visibility = View.VISIBLE
        binding.btnCreateGroup.isEnabled = false

        // Create group in groups node
        database.child("Groups").child(groupId).setValue(groupData)
            .addOnSuccessListener {
                // Add group reference to each member's groups
                allMembers.forEach { member ->
                    database.child("Users").child(member.id)
                        .child("Groups").child(groupId)
                        .setValue(true)
                }
                Toast.makeText(this, "Group created successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                binding.btnCreateGroup.isEnabled = true
                Toast.makeText(this, "Failed to create group: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
