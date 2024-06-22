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
import android.content.Intent
import android.widget.Toast
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

        adminButton = findViewById(R.id.adminButton)
        adminButton.setOnClickListener {
            isAdminUser { isAdmin ->
                if (isAdmin) {
                val intent = Intent(this, AdminActivity::class.java)
                startActivity(intent)
                adminButton.visibility = View.VISIBLE // Beispiel: Sichtbarkeit eines Admin-Buttons ändern

            } else {
                // Hier können Sie eine Nachricht anzeigen oder andere Maßnahmen ergreifen,
                // wenn der Benutzer kein Administrator ist
                Toast.makeText(this, "You are not authorized as an admin", Toast.LENGTH_SHORT).show()
                adminButton.visibility = View.GONE // Beispiel: Admin-Button ausblenden

            }}
    }
    }

    private fun checkIn() {
        val user = mAuth.currentUser
        user?.let { currentUser ->
            val checkInTime = System.currentTimeMillis()
    
            // Speichern des Eintrags in Firestore
            saveTimeEntry(currentUser.uid, checkInTime, null)
        } ?: run {
            // Benutzer ist nicht angemeldet
            statusTextView.text = "Benutzer ist nicht angemeldet"
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
                        statusTextView.text = "Checked Out"
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
                                statusTextView.text = "Failed to load times: ${task.exception?.message}"
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

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
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
                            // Der Benutzer ist ein Administrator
                            callback(true)
                        } else {
                            // Standardmäßig kein Administrator
                            callback(false)
                        }
                    } else {
                        // Dokument existiert nicht
                        callback(false)
                    }
                }
                .addOnFailureListener { exception ->
                    // Fehler bei der Überprüfung der Berechtigung
                    callback(false)
                }
        } ?: run {
            // Benutzer ist nicht angemeldet
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
                .addOnSuccessListener { documentReference ->
                    statusTextView.text = "Erfolgreich eingecheckt"
                }
                .addOnFailureListener { e ->
                    statusTextView.text = "Einchecken fehlgeschlagen: ${e.message}"
                }
        } catch (e: Exception) {
            statusTextView.text = "Einchecken fehlgeschlagen: ${e.message}"
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