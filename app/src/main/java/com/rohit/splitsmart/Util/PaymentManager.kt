package com.rohit.splitsmart.Util
//
//import com.google.firebase.database.DataSnapshot
//import com.google.firebase.database.DatabaseError
//import com.google.firebase.database.DatabaseReference
//import com.google.firebase.database.MutableData
//import com.google.firebase.database.Transaction
//import com.rohit.splitsmart.Model.Expense
//import com.rohit.splitsmart.Model.ExpenseStatus
//
//class PaymentManager(private val database: DatabaseReference) {
//
//    fun processPayment(
//        groupId: String,
//        expenseId: String,
//        payerId: String,
//        amount: Double,
//        onSuccess: () -> Unit,
//        onError: (Exception) -> Unit
//    ) {
//        val expenseRef = database.child("Groups").child(groupId).child("expenses").child(expenseId)
//
//        database.runTransaction(object : Transaction.Handler {
//            override fun doTransaction(mutableData: MutableData): Transaction.Result {
//                val expense = mutableData.getValue(Expense::class.java) ?: return Transaction.abort()
//
//                // Get current payment status
//                val currentStatus = expense.split[payerId] ?: return Transaction.abort()
//                val newPaidAmount = currentStatus.paidAmount + amount
//
//                // Update payment status
//                val updatedStatus = currentStatus.copy(
//                    paidAmount = newPaidAmount,
//                    lastPaymentDate = System.currentTimeMillis()
//                )
//
//                // Update split map
//                val updatedSplit = expense.split.toMutableMap()
//                updatedSplit[payerId] = updatedStatus
//
//                // Calculate overall expense status
//                val isFullySettled = updatedSplit.all { it.value.paidAmount >= it.value.totalAmount }
//                val hasPartialPayments = updatedSplit.any { it.value.paidAmount > 0 }
//
//                val newStatus = when {
//                    isFullySettled -> ExpenseStatus.SETTLED
//                    hasPartialPayments -> ExpenseStatus.PARTIAL
//                    else -> ExpenseStatus.UNPAID
//                }
//
//                // Update expense
//                mutableData.value = expense.copy(
//                    split = updatedSplit,
//                    status = newStatus
//                )
//
//                return Transaction.success(mutableData)
//            }
//
//            override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
//                if (error != null) {
//                    onError(error.toException())
//                } else {
//                    onSuccess()
//                }
//            }
//        })
//    }
//}