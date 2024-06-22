package com.example.zeiterfassungsapp
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

class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var checkInButton: Button
    private lateinit var checkOutButton: Button
    private lateinit var viewTimesButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var tableLayout: TableLayout
    private var timesVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        checkInButton = findViewById(R.id.checkInButton)
        checkOutButton = findViewById(R.id.checkOutButton)
        viewTimesButton = findViewById(R.id.viewTimesButton)
        statusTextView = findViewById(R.id.statusTextView)
        tableLayout = findViewById(R.id.tableLayout)

        checkInButton.setOnClickListener { checkIn() }
        checkOutButton.setOnClickListener { checkOut() }
        viewTimesButton.setOnClickListener { viewTimes() }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        val date = Date(timestamp)
        return sdf.format(date)
    }

    private fun checkIn() {
        val user = mAuth.currentUser
        user?.let {
            val timeEntry = hashMapOf(
                "userId" to it.uid,
                "checkIn" to System.currentTimeMillis()
            )

            try {
                db.collection("timeEntries").add(timeEntry)
                    .addOnSuccessListener {
                        val formattedTime = formatTime(System.currentTimeMillis())
                        statusTextView.text = "Checked In at $formattedTime"
                    }
                    .addOnFailureListener { exception ->
                        statusTextView.text = "Check In failed: ${exception.message}"
                    }
            } catch (e: Exception) {
                statusTextView.text = "Check In failed: ${e.message}"
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

            try {
                db.collection("timeEntries").add(timeEntry)
                    .addOnSuccessListener {
                        val formattedTime = formatTime(System.currentTimeMillis())
                        statusTextView.text = "Checked Out at $formattedTime"
                    }
                    .addOnFailureListener { exception ->
                        statusTextView.text = "Check Out failed: ${exception.message}"
                    }
            } catch (e: Exception) {
                statusTextView.text = "Check Out failed: ${e.message}"
            }
        } ?: run {
            statusTextView.text = "User not signed in"
        }
    }

    private fun viewTimes() {
        if (timesVisible) {
            // Zeiten ausblenden
            tableLayout.visibility = View.GONE
            viewTimesButton.text = "View Times"
            timesVisible = false
        } else {
            // Zeiten anzeigen
            val user = mAuth.currentUser
            user?.let {
                try {
                    db.collection("timeEntries").whereEqualTo("userId", it.uid)
                        .get()
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                tableLayout.removeAllViews()

                                // Header row
                                val headerRow = TableRow(this)
                                val checkInHeader = TextView(this).apply {
                                    text = "Check In"
                                    setPadding(8, 8, 8, 8)
                                    setTypeface(null, Typeface.BOLD)
                                }
                                val checkOutHeader = TextView(this).apply {
                                    text = "Check Out"
                                    setPadding(8, 8, 8, 8)
                                    setTypeface(null, Typeface.BOLD)
                                }
                                headerRow.addView(checkInHeader)
                                headerRow.addView(checkOutHeader)
                                tableLayout.addView(headerRow)

                                // Data rows
                                for (document in task.result) {
                                    val checkInTime = document.getLong("checkIn")
                                    val checkOutTime = document.getLong("checkOut")
                                    val row = TableRow(this)
                                    val checkInView = TextView(this).apply {
                                        text = checkInTime?.let { formatTime(it) } ?: "N/A"
                                        setPadding(8, 8, 8, 8)
                                    }
                                    val checkOutView = TextView(this).apply {
                                        text = checkOutTime?.let { formatTime(it) } ?: "N/A"
                                        setPadding(8, 8, 8, 8)
                                    }
                                    row.addView(checkInView)
                                    row.addView(checkOutView)
                                    tableLayout.addView(row)
                                }
                                tableLayout.visibility = View.VISIBLE
                                viewTimesButton.text = "Hide Times"
                                timesVisible = true
                            } else {
                                statusTextView.text =
                                    "Failed to load times: ${task.exception?.message}"
                            }
                        }
                } catch (e: Exception) {
                    statusTextView.text = "Failed to load times: ${e.message}"
                }
            } ?: run {
                statusTextView.text = "User not signed in"
            }
        }
    }
}