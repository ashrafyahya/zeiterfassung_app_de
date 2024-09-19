package com.example.zeiterfassungsapp;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Firebase-Authentifizierungsinstanz
    private FirebaseAuth mAuth;

    // Firestore-Datenbankinstanz
    private FirebaseFirestore db;

    // UI-Komponenten
    private Button checkInButton;
    private Button checkOutButton;
    private Button viewTimesButton;
    private Button logoutButton;
    private TextView statusTextView;
    private TableLayout tableLayout;
    private boolean timesVisible = false;
    private Button adminButton;
    private Button totalDurationButton;
    private TextView totalDurationTextView;

    // BroadcastReceiver zur Nachrichtenverarbeitung
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extrahieren der Daten von Intent
            String title = intent.getStringExtra("title");
            String body = intent.getStringExtra("body");

            Log.d("BroadcastReceiver", "Received message: " + title + " - " + body);

            // Anzeigen vom Inhalt von message in-app notification
            showInAppNotification(title, body);
        }
    };

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisierung von Firebase Authentication und Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Finden von views durch ID
        checkInButton = findViewById(R.id.checkInButton);
        checkOutButton = findViewById(R.id.checkOutButton);
        viewTimesButton = findViewById(R.id.viewTimesButton);
        logoutButton = findViewById(R.id.logoutButton);
        statusTextView = findViewById(R.id.statusTextView);
        tableLayout = findViewById(R.id.tableLayout);
        adminButton = findViewById(R.id.adminButton);
        totalDurationButton = findViewById(R.id.totalDurationButton);
        totalDurationTextView = findViewById(R.id.totalDurationTextView);

        // Setzung von onClick listeners für die Buttons
        checkInButton.setOnClickListener(v -> checkIn());
        checkOutButton.setOnClickListener(v -> checkOut());
        viewTimesButton.setOnClickListener(v -> viewTimes());
        logoutButton.setOnClickListener(v -> logout());

        // Registrierung des BroadcastReceivers für den Epmfang von messages
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("MyFirebaseMessage"));

        // Setzung von onClick listener für Total-Duration-Button
        totalDurationButton.setOnClickListener(v -> {
            if (totalDurationTextView.getVisibility() == View.GONE) {
                calculateAndShowTotalDuration();
            } else {
                totalDurationTextView.setVisibility(View.GONE);
                totalDurationButton.setText("Show Total Duration");
            }
        });

        // Setzung onClick listener für Admin-Button
        adminButton.setOnClickListener(v -> isAdminUser(isAdmin -> {
            if (isAdmin) {
                Intent intent = new Intent(MainActivity.this, AdminActivity.class);
                startActivity(intent);
                adminButton.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(MainActivity.this, "You are not authorized as an admin", Toast.LENGTH_SHORT).show();
                adminButton.setVisibility(View.GONE);
            }
        }));
    }

    private void checkIn() {
        // Überprüfung, ob ein Benutzer angemeldet ist
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            // Abfrage von Firestore, um zu prüfen, ob der Benutzer bereits eingecheckt ist
            db.collection("timeEntries")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("checkOut", null)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(documents -> {
                        if (!documents.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Already Checked In", Toast.LENGTH_SHORT).show();
                        } else {
                            saveTimeEntry(userId, System.currentTimeMillis(), null);
                        }
                    })
                    .addOnFailureListener(exception -> {
                        if (exception instanceof FirebaseFirestoreException &&
                                ((FirebaseFirestoreException) exception)
                                        .getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                            Toast.makeText(MainActivity.this, "Failed to check in: user is already checked in",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Check In failed: " + exception.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(MainActivity.this, "User not signed in", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkOut() {
        // Überprüfung, ob ein Benutzer angemeldet ist
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            // Abfrage von Firestore, um zu prüfen, ob der Benutzer aktuell eingecheckt ist
            db.collection("timeEntries")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("checkOut", null)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(documents -> {
                        if (!documents.isEmpty()) {
                            com.google.firebase.firestore.DocumentSnapshot document = documents.getDocuments().get(0);
                            long checkOutTime = System.currentTimeMillis();
                            document.getReference().update("checkOut", checkOutTime)
                                    .addOnSuccessListener(aVoid -> Toast
                                            .makeText(MainActivity.this, "Checked Out", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(exception -> Toast.makeText(MainActivity.this,
                                            "Check Out failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show());
                        } else {
                            Toast.makeText(MainActivity.this, "No check-in found to check out", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    })
                    .addOnFailureListener(exception -> Toast.makeText(MainActivity.this,
                            "Check Out failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(MainActivity.this, "User not signed in", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SetTextI18n")
    private void viewTimes() {
        if (timesVisible) {
            tableLayout.setVisibility(View.GONE);
            viewTimesButton.setText("View Times");
            timesVisible = false;
        } else {
            // Überprüfung, ob ein Benutzer angemeldet ist
            if (mAuth.getCurrentUser() != null) {
                String userId = mAuth.getCurrentUser().getUid();
                // Abfrage von Firestore zum Abrufen der Zeiteinträge des Benutzers
                db.collection("timeEntries")
                        .whereEqualTo("userId", userId)
                        .orderBy("checkIn", Query.Direction.DESCENDING)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                tableLayout.removeAllViews();

                                // Erstellen und Hinzufügen der Kopfzeile zum Tabellenlayout
                                TableRow headerRow = new TableRow(MainActivity.this);
                                headerRow.setBackgroundColor(Color.parseColor("#CCCCCC"));
                                headerRow.setPadding(16, 16, 16, 16);

                                TextView checkInHeader = new TextView(MainActivity.this);
                                checkInHeader.setText("Check In");
                                checkInHeader.setPadding(8, 8, 8, 8);
                                checkInHeader.setTypeface(null, Typeface.BOLD);
                                checkInHeader.setTextColor(Color.BLACK);
                                checkInHeader.setBackgroundColor(Color.parseColor("#DDDDDD"));

                                TextView checkOutHeader = new TextView(MainActivity.this);
                                checkOutHeader.setText("Check Out");
                                checkOutHeader.setPadding(8, 8, 8, 8);
                                checkOutHeader.setTypeface(null, Typeface.BOLD);
                                checkOutHeader.setTextColor(Color.BLACK);
                                checkOutHeader.setBackgroundColor(Color.parseColor("#DDDDDD"));

                                TextView durationHeader = new TextView(MainActivity.this);
                                durationHeader.setText("Duration");
                                durationHeader.setPadding(8, 8, 8, 8);
                                durationHeader.setTypeface(null, Typeface.BOLD);
                                durationHeader.setTextColor(Color.BLACK);
                                durationHeader.setBackgroundColor(Color.parseColor("#DDDDDD"));

                                headerRow.addView(checkInHeader);
                                headerRow.addView(checkOutHeader);
                                headerRow.addView(durationHeader);
                                tableLayout.addView(headerRow);

                                // Hinzufügung jedes Zeiteintrags zum Tabellenlayout
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Long checkInTime = document.getLong("checkIn");
                                    Long checkOutTime = document.getLong("checkOut");

                                    TableRow row = new TableRow(MainActivity.this);
                                    row.setPadding(16, 16, 16, 16);

                                    TextView checkInView = new TextView(MainActivity.this);
                                    checkInView.setText(checkInTime != null ? formatTime(checkInTime) : "N/A");
                                    checkInView.setPadding(8, 8, 8, 8);
                                    checkInView.setTextColor(Color.BLACK);

                                    TextView checkOutView = new TextView(MainActivity.this);
                                    checkOutView.setText(checkOutTime != null ? formatTime(checkOutTime) : "N/A");
                                    checkOutView.setPadding(8, 8, 8, 8);
                                    checkOutView.setTextColor(Color.BLACK);

                                    TextView durationView = new TextView(MainActivity.this);
                                    durationView.setText(checkInTime != null && checkOutTime != null
                                            ? formatDuration(checkOutTime - checkInTime)
                                            : "N/A");
                                    durationView.setPadding(8, 8, 8, 8);
                                    durationView.setTextColor(Color.BLACK);

                                    row.addView(checkInView);
                                    row.addView(checkOutView);
                                    row.addView(durationView);
                                    tableLayout.addView(row);
                                }

                                tableLayout.setVisibility(View.VISIBLE);
                                viewTimesButton.setText("Hide Times");
                                timesVisible = true;
                            } else {
                                Toast.makeText(MainActivity.this, "Failed to load times", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(MainActivity.this, "User not signed in", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void logout() {
        // Abmelden des aktuellen Benutzers und zurück navigieren zum Anmeldebildschirm
        mAuth.signOut();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    private void saveTimeEntry(String userId, Long checkIn, Long checkOut) {
        // Erstellen eine Karte zum Speichern von Zeiteintragsdaten
        HashMap<String, Object> timeEntry = new HashMap<>();
        timeEntry.put("userId", userId);
        timeEntry.put("checkIn", checkIn);
        timeEntry.put("checkOut", checkOut);

        // Hinzufügen vom neuen Zeiteintrag zur Firestore-Datenbank 
        db.collection("timeEntries")
                .add(timeEntry)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(MainActivity.this, "Check In successful", Toast.LENGTH_SHORT).show();
                    updateStatusText();
                })
                .addOnFailureListener(exception -> Toast
                        .makeText(MainActivity.this, "Check In failed: " + exception.getMessage(), Toast.LENGTH_SHORT)
                        .show());
    }

    private void updateStatusText() {
        // Überprüfung, ob ein Benutzer angemeldet ist
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            // Abfrage von Firestore, um den aktuellen Check-in-Status des Benutzers zu überprüfen
            db.collection("timeEntries")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("checkOut", null)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(documents -> {
                        if (!documents.isEmpty()) {
                            statusTextView.setText("Checked In");
                        } else {
                            statusTextView.setText("Checked Out");
                        }
                    })
                    .addOnFailureListener(exception -> Toast.makeText(MainActivity.this,
                            "Failed to update status: " + exception.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            statusTextView.setText("Not Signed In");
        }
    }

    private String formatTime(long timestamp) {
        // Formatierung dvomen Zeitstempel in eine für Menschen lesbare Datums- und Zeitzeichenfolge.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private String formatDuration(long durationMillis) {
        // Konvertierung die Dauer von Millisekunden in Stunden und Minuten
        long minutes = (durationMillis / 1000) / 60;
        long hours = minutes / 60;
        minutes = minutes % 60;
        return hours + "h " + minutes + "m";
    }

    private void showInAppNotification(String title, String body) {
        // Erstellung und Anzeigen einer In-App-Benachrichtigung mithilfe eines AlertDialogs
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(body);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void calculateAndShowTotalDuration() {
        // Überprüfung, ob ein Benutzer angemeldet ist
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            // Abfrage von Firestore zum Abrufen aller Zeiteinträge des Benutzers
            db.collection("timeEntries")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            long totalDuration = 0;
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Long checkInTime = document.getLong("checkIn");
                                Long checkOutTime = document.getLong("checkOut");
                                if (checkInTime != null && checkOutTime != null) {
                                    totalDuration += (checkOutTime - checkInTime);
                                }
                            }
                            totalDurationTextView.setText("Total Duration: " + formatDuration(totalDuration));
                            totalDurationTextView.setVisibility(View.VISIBLE);
                            totalDurationButton.setText("Hide Total Duration");
                        } else {
                            Toast.makeText(MainActivity.this, "Failed to calculate total duration", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
        } else {
            Toast.makeText(MainActivity.this, "User not signed in", Toast.LENGTH_SHORT).show();
        }
    }

    private void isAdminUser(AdminCheckCallback callback) {
        // Überprüfung, ob ein Benutzer angemeldet ist
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            // Firestore abfragen, um festzustellen, ob der Benutzer ein Administrator ist
            db.collection("admins")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(document -> callback.onCheck(document.exists()))
                    .addOnFailureListener(exception -> {
                        Toast.makeText(MainActivity.this, "Failed to check admin status: " + exception.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        callback.onCheck(false);
                    });
        } else {
            Toast.makeText(MainActivity.this, "User not signed in", Toast.LENGTH_SHORT).show();
            callback.onCheck(false);
        }
    }

    // Schnittstelle für Admin-Check-Callback
    interface AdminCheckCallback {
        void onCheck(boolean isAdmin);
    }
}
