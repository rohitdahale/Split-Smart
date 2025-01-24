package com.rohit.splitsmart.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.rohit.splitsmart.Activity.GroupDetailActivity
import com.rohit.splitsmart.R
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

object NotificationUtils {
    const val CHANNEL_ID = "payment_reminders"
    private const val CHANNEL_NAME = "Payment Reminders"
    private const val NOTIFICATION_INTERVAL = 1L // 1 minute interval
    private const val WORK_NAME = "payment_reminder_work"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for pending bill payments"
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun schedulePaymentReminders(context: Context) {
        // Cancel any existing work
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)

        // Create the periodic work request
        val workRequest = PeriodicWorkRequestBuilder<PaymentReminderWorker>(
            NOTIFICATION_INTERVAL,
            TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()

        // Enqueue the work request
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )

        // Log for debugging
        Log.d("NotificationUtils", "Scheduled payment reminder work")

        // Schedule immediate work for first check
        val oneTimeWork = OneTimeWorkRequestBuilder<PaymentReminderWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueue(oneTimeWork)
    }
}

class PaymentReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val database = Firebase.database.reference
    private val currentUser = FirebaseAuth.getInstance().currentUser

    override suspend fun doWork(): Result {
        try {
            Log.d("PaymentReminderWorker", "Starting work check")
            checkAndSendReminders()
            return Result.success()
        } catch (e: Exception) {
            Log.e("PaymentReminderWorker", "Error in doWork", e)
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun checkAndSendReminders() {
        val currentUserId = currentUser?.uid ?: return
        Log.d("PaymentReminderWorker", "Checking reminders for user: $currentUserId")

        try {
            // Fetch all groups the user is part of
            val userGroups = database.child("Users").child(currentUserId)
                .child("Groups")
                .get()
                .await()

            userGroups.children.forEach { groupSnapshot ->
                val groupId = groupSnapshot.key ?: return@forEach
                Log.d("PaymentReminderWorker", "Checking group: $groupId")

                // Fetch group details
                val groupDetails = database.child("Groups").child(groupId).get().await()
                val paymentStatuses = groupDetails.child("paymentStatuses")
                    .child(currentUserId)
                    .getValue(Double::class.java) ?: 0.0

                if (paymentStatuses > 0) {
                    val groupName = groupDetails.child("name").getValue(String::class.java)
                        ?: "Unknown Group"
                    Log.d("PaymentReminderWorker", "Found pending payment in group: $groupName")
                    sendNotification(groupId, paymentStatuses, groupName)
                }
            }
        } catch (e: Exception) {
            Log.e("PaymentReminderWorker", "Error checking reminders", e)
        }
    }

    private fun sendNotification(
        groupId: String,
        amount: Double,
        groupName: String
    ) {
        try {
            val intent = Intent(applicationContext, GroupDetailActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("GROUP_ID", groupId)
            }

            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                groupId.hashCode(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(applicationContext, NotificationUtils.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Payment Reminder")
                .setContentText("You have a pending payment of $${String.format("%.2f", amount)} in $groupName")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val notificationManager = applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notificationId = "${currentUser?.uid}_${groupId}".hashCode()
            notificationManager.notify(notificationId, notification)

            Log.d("PaymentReminderWorker",
                "Successfully sent notification for group: $groupName, amount: $amount")
        } catch (e: Exception) {
            Log.e("PaymentReminderWorker", "Error sending notification", e)
        }
    }
}