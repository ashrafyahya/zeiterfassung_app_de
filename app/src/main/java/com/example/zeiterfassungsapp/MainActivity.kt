package com.example.zeiterfassungsapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var checkInButton: Button
    private lateinit var checkOutButton: Button
    private lateinit var viewTimesButton: Button
    private lateinit var statusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        checkInButton = findViewById(R.id.checkInButton)
        checkOutButton = findViewById(R.id.checkOutButton)
        viewTimesButton = findViewById(R.id.viewTimesButton)
        statusTextView = findViewById(R.id.statusTextView)

        checkInButton.setOnClickListener { checkIn() }
        checkOutButton.setOnClickListener { checkOut() }
        viewTimesButton.setOnClickListener { viewTimes() }
    }

    private fun checkIn() {
        val user = mAuth.currentUser
        user?.let {
            val timeEntry = hashMapOf(
                "userId" to it.uid,
                "checkIn" to System.currentTimeMillis()
            )

            db.collection("timeEntries").add(timeEntry)
                .addOnSuccessListener {
                    statusTextView.text = "Checked In"
                }
                .addOnFailureListener { exception ->
                    statusTextView.text = "Check In failed: ${exception.message}"
                }
        } ?: run {
            statusTextView.text = "User not signed in"
        }
    }

    private fun checkOut() {
        val user = mAuth.currentUser
        user?.let {
            val timeEntry = hashMapOf(
                "userId" to it.uid,
                "checkOut" to System.currentTimeMillis()
            )

            db.collection("timeEntries").add(timeEntry)
                .addOnSuccessListener {
                    statusTextView.text = "Checked Out"
                }
                .addOnFailureListener { exception ->
                    statusTextView.text = "Check Out failed: ${exception.message}"
                }
        } ?: run {
            statusTextView.text = "User not signed in"
        }
    }

    private fun viewTimes() {
        val user = mAuth.currentUser
        user?.let {
            db.collection("timeEntries").whereEqualTo("userId", it.uid)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val times = StringBuilder()
                        for (document in task.result) {
                            val checkInTime = document.getLong("checkIn")
                            val checkOutTime = document.getLong("checkOut")
                            times.append("Check In: $checkInTime Check Out: $checkOutTime\n")
                        }
                        statusTextView.text = times.toString()
                    } else {
                        statusTextView.text = "Failed to load times: ${task.exception?.message}"
                    }
                }
        } ?: run {
            statusTextView.text = "User not signed in"
        }
    }
}
