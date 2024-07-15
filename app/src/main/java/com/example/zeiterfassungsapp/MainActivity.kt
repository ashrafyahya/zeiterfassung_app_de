package com.example.zeiterfassungsapp
import android.graphics.Color

import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import android.widget.TableLayout
import android.widget.TableRow
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Intent
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var checkInButton: Button
    private lateinit var checkOutButton: Button
    private lateinit var viewTimesButton: Button
    private lateinit var logoutButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var tableLayout: TableLayout
    private var timesVisible = false
    private lateinit var adminButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        checkInButton = findViewById(R.id.checkInButton)
        checkOutButton = findViewById(R.id.checkOutButton)
        viewTimesButton = findViewById(R.id.viewTimesButton)
        logoutButton = findViewById(R.id.logoutButton)
        statusTextView = findViewById(R.id.statusTextView)
        tableLayout = findViewById(R.id.tableLayout)
        adminButton = findViewById(R.id.adminButton)

        checkInButton.setOnClickListener { checkIn() }
        checkOutButton.setOnClickListener { checkOut() }
        viewTimesButton.setOnClickListener { viewTimes() }
        logoutButton.setOnClickListener { logout() }

        adminButton.setOnClickListener {
            isAdminUser { isAdmin ->
                if (isAdmin) {
                    val intent = Intent(this, AdminActivity::class.java)
                    startActivity(intent)
                    adminButton.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this, "You are not authorized as an admin", Toast.LENGTH_SHORT).show()
                    adminButton.visibility = View.GONE
                }
            }
        }
    }

    private fun checkIn() {
        val user = mAuth.currentUser
        user?.let { currentUser ->
            db.collection("timeEntries")
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("checkOut", null)
                .limit(1)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        Toast.makeText(this, "Already Checked In", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    } else {
                        saveTimeEntry(currentUser.uid, System.currentTimeMillis(), null)
                    }
                }
                .addOnFailureListener { exception ->
                    if (exception is FirebaseFirestoreException && exception.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                        Toast.makeText(this, "Failed to check in: user is already checked in", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Check In failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } ?: run {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show()
        }
    }
    
    

    private fun checkOut() {
        val user = mAuth.currentUser
        user?.let { currentUser ->
            db.collection("timeEntries")
               .whereEqualTo("userId", currentUser.uid)
               .whereEqualTo("checkOut", null)
               .limit(1)
               .get()
               .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val document = documents.documents[0]
                        val checkOutTime = System.currentTimeMillis()
                        document.reference.update("checkOut", checkOutTime)
                           .addOnSuccessListener {
                                Toast.makeText(this, "Checked Out", Toast.LENGTH_SHORT).show()
                            }
                           .addOnFailureListener { exception ->
                                Toast.makeText(this, "Check Out failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "No check-in found to check out", Toast.LENGTH_SHORT).show()
                    }
                }
               .addOnFailureListener { exception ->
                    Toast.makeText(this, "Check Out failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }?: run {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show()
        }
    }
    
    

    private fun viewTimes() {
    if (timesVisible) {
        tableLayout.visibility = View.GONE
        viewTimesButton.text = "View Times"
        timesVisible = false
    } else {
        val user = mAuth.currentUser
        user?.let {
            try {
                db.collection("timeEntries").whereEqualTo("userId", it.uid)
                    .get()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            tableLayout.removeAllViews()

                            val headerRow = TableRow(this).apply {
                                setBackgroundColor(Color.parseColor("#CCCCCC"))
                                setPadding(16, 16, 16, 16)
                            }

                            val checkInHeader = TextView(this).apply {
                                text = "Check In"
                                setPadding(8, 8, 8, 8)
                                setTypeface(null, Typeface.BOLD)
                                setTextColor(Color.BLACK)
                                setBackgroundColor(Color.parseColor("#DDDDDD"))
                            }
                            val checkOutHeader = TextView(this).apply {
                                text = "Check Out"
                                setPadding(8, 8, 8, 8)
                                setTypeface(null, Typeface.BOLD)
                                setTextColor(Color.BLACK)
                                setBackgroundColor(Color.parseColor("#DDDDDD"))
                            }
                            val durationHeader = TextView(this).apply {
                                text = "Duration"
                                setPadding(8, 8, 8, 8)
                                setTypeface(null, Typeface.BOLD)
                                setTextColor(Color.BLACK)
                                setBackgroundColor(Color.parseColor("#DDDDDD"))
                            }
                            headerRow.addView(checkInHeader)
                            headerRow.addView(checkOutHeader)
                            headerRow.addView(durationHeader)
                            tableLayout.addView(headerRow)

                            for (document in task.result) {
                                val checkInTime = document.getLong("checkIn")
                                val checkOutTime = document.getLong("checkOut")
                                val row = TableRow(this).apply {
                                    setBackgroundColor(Color.parseColor("#FFFFFF"))
                                    setPadding(16, 16, 16, 16)
                                }
                                val checkInView = TextView(this).apply {
                                    text = checkInTime?.let { formatTime(it) } ?: "N/A"
                                    setPadding(8, 8, 8, 8)
                                    setTextColor(Color.BLACK)
                                    setBackgroundResource(R.drawable.cell_border)
                                }
                                val checkOutView = TextView(this).apply {
                                    text = checkOutTime?.let { formatTime(it) } ?: "N/A"
                                    setPadding(8, 8, 8, 8)
                                    setTextColor(Color.BLACK)
                                    setBackgroundResource(R.drawable.cell_border)
                                }
                                val durationView = TextView(this).apply {
                                    text = if (checkInTime != null && checkOutTime != null) {
                                        formatDuration(checkOutTime - checkInTime)
                                    } else {
                                        "N/A"
                                    }
                                    setPadding(8, 8, 8, 8)
                                    setTextColor(Color.BLACK)
                                    setBackgroundResource(R.drawable.cell_border)
                                }
                                row.addView(checkInView)
                                row.addView(checkOutView)
                                row.addView(durationView)
                                tableLayout.addView(row)
                            }
                            tableLayout.visibility = View.VISIBLE
                            viewTimesButton.text = "Hide Times"
                            timesVisible = true
                        } else {
                            Toast.makeText(this, "Failed to load times: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to load times: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "User not signed in", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun formatDuration(durationMillis: Long): String {
    val seconds = durationMillis / 1000 % 60
    val minutes = durationMillis / (1000 * 60) % 60
    val hours = durationMillis / (1000 * 60 * 60) % 24
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

    

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        val date = Date(timestamp)
        return sdf.format(date)
    }

    private fun logout() {
        mAuth.signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun isAdminUser(callback: (Boolean) -> Unit) {
        val user = mAuth.currentUser

        user?.let {
            db.collection("users").document(it.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val role = document.getString("role")
                        if (role == "admin") {
                            callback(true)
                        } else {
                            callback(false)
                        }
                    } else {
                        callback(false)
                    }
                }
                .addOnFailureListener { exception ->
                    callback(false)
                }
        } ?: run {
            callback(false)
        }
    }

    private fun saveTimeEntry(userId: String, checkInTime: Long, checkOutTime: Long?) {
        val timeEntry = hashMapOf(
            "userId" to userId,
            "checkIn" to checkInTime,
            "checkOut" to checkOutTime
        )

        try {
            db.collection("timeEntries").add(timeEntry)
                .addOnSuccessListener {
                    Toast.makeText(this, "Checked In", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Check In failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Check In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getTimeEntriesForUser(userId: String) {
        db.collection("timeEntries")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val checkInTime = document.getLong("checkIn")
                    val checkOutTime = document.getLong("checkOut")
                    // Verarbeiten Sie die abgerufenen Zeiteinträge hier
                }
            }
            .addOnFailureListener { exception ->
                // Fehler beim Abrufen der Zeiteinträge
            }
    }
}
