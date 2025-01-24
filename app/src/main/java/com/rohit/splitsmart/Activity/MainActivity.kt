package com.rohit.splitsmart.Activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.rohit.splitsmart.Adapters.GroupsAdapter
import com.rohit.splitsmart.Model.Group
import com.rohit.splitsmart.databinding.ActivityMainBinding
import com.rohit.splitsmart.utils.NotificationUtils

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var groupsAdapter: GroupsAdapter
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val database = Firebase.database.reference

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Optionally show a message that notifications are disabled
            Toast.makeText(this, "Notifications are disabled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        NotificationUtils.createNotificationChannel(this)

        // Schedule background work
        NotificationUtils.schedulePaymentReminders(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setupUI()
        setupRecyclerView()
        fetchUserGroups()


        // You can add this temporarily to MainActivity to test

        setContentView(binding.root)
    }

    private fun setupUI() {
        // Setup FAB
        binding.createGroupFab.setOnClickListener {
            startActivity(Intent(this, CreateGroupsActivity::class.java))
        }

        // Setup profile avatar click if needed
        binding.profileAvatar.setOnClickListener {
            // Handle profile click
        }
    }

    private fun setupRecyclerView() {
        groupsAdapter = GroupsAdapter { group ->
            // Handle group click - navigate to group details
            navigateToGroupDetails(group)
        }

        binding.groupsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = groupsAdapter
        }
    }

    private fun fetchUserGroups() {
        showLoading(true)

        currentUser?.uid?.let { userId ->
            // First, get the groups this user is a member of
            database.child("Users").child(userId).child("Groups")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val groupIds = snapshot.children.mapNotNull { it.key }
                        if (groupIds.isEmpty()) {
                            showEmptyState(true)
                            showLoading(false)
                            return
                        }
                        fetchGroupDetails(groupIds)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        showError("Failed to fetch groups: ${error.message}")
                        showLoading(false)
                    }
                })
        } ?: run {
            showError("User not authenticated")
            showLoading(false)
        }
    }

    private fun fetchGroupDetails(groupIds: List<String>) {
        val groups = mutableListOf<Group>()
        var completedQueries = 0

        groupIds.forEach { groupId ->
            database.child("Groups").child(groupId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.getValue(Group::class.java)?.let { group ->
                            group.id = groupId
                            groups.add(group)
                        }

                        completedQueries++
                        if (completedQueries == groupIds.size) {
                            // All queries completed
                            updateUI(groups)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        completedQueries++
                        showError("Failed to fetch group details: ${error.message}")

                        if (completedQueries == groupIds.size) {
                            updateUI(groups)
                        }
                    }
                })
        }
    }

    private fun updateUI(groups: List<Group>) {
        showLoading(false)
        if (groups.isEmpty()) {
            showEmptyState(true)
        } else {
            showEmptyState(false)
            groupsAdapter.submitList(groups.sortedByDescending { it.createdAt })
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showEmptyState(show: Boolean) {
        binding.emptyStateContainer.visibility = if (show) View.VISIBLE else View.GONE
        binding.groupsRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToGroupDetails(group: Group) {
        // Implement navigation to group details
        val intent = Intent(this, GroupDetailActivity::class.java).apply {
            putExtra("GROUP_ID", group.id)
        }
        startActivity(intent)
    }
}