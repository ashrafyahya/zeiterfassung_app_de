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

    // Firebase Authentication instance
    private FirebaseAuth mAuth;

    // Firestore database instance
    private FirebaseFirestore db;

    // UI components
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

    // BroadcastReceiver to handle messages
    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data from the Intent
            String title = intent.getStringExtra("title");
            String body = intent.getStringExtra("body");

            Log.d("BroadcastReceiver", "Received message: " + title + " - " + body);

            // Show an in-app notification with the message content
            showInAppNotification(title, body);
        }
    };

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Authentication and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Find views by ID
        checkInButton = findViewById(R.id.checkInButton);
        checkOutButton = findViewById(R.id.checkOutButton);
        viewTimesButton = findViewById(R.id.viewTimesButton);
        logoutButton = findViewById(R.id.logoutButton);
        statusTextView = findViewById(R.id.statusTextView);
        tableLayout = findViewById(R.id.tableLayout);
        adminButton = findViewById(R.id.adminButton);
        totalDurationButton = findViewById(R.id.totalDurationButton);
        totalDurationTextView = findViewById(R.id.totalDurationTextView);

        // Set onClick listeners for the buttons
        checkInButton.setOnClickListener(v -> checkIn());
        checkOutButton.setOnClickListener(v -> checkOut());
        viewTimesButton.setOnClickListener(v -> viewTimes());
        logoutButton.setOnClickListener(v -> logout());

        // Register the BroadcastReceiver to receive messages
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("MyFirebaseMessage"));

        // Set onClick listener for total duration button
        totalDurationButton.setOnClickListener(v -> {
            if (totalDurationTextView.getVisibility() == View.GONE) {
                calculateAndShowTotalDuration();
            } else {
                totalDurationTextView.setVisibility(View.GONE);
                totalDurationButton.setText("Show Total Duration");
            }
        });

        // Set onClick listener for admin button
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
        // Check if a user is signed in
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            // Query Firestore to check if the user is already checked in
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
        // Check if a user is signed in
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            // Query Firestore to check if the user is currently checked in
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
            // Check if a user is signed in
            if (mAuth.getCurrentUser() != null) {
                String userId = mAuth.getCurrentUser().getUid();
                // Query Firestore to retrieve the user's time entries
                db.collection("timeEntries")
                        .whereEqualTo("userId", userId)
                        .orderBy("checkIn", Query.Direction.DESCENDING)
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                tableLayout.removeAllViews();

                                // Create and add the header row to the table layout
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

                                // Add each time entry to the table layout
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
        // Sign out the current user and navigate back to the login screen
        mAuth.signOut();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }

    private void saveTimeEntry(String userId, Long checkIn, Long checkOut) {
        // Create a map to hold time entry data
        HashMap<String, Object> timeEntry = new HashMap<>();
        timeEntry.put("userId", userId);
        timeEntry.put("checkIn", checkIn);
        timeEntry.put("checkOut", checkOut);

        // Add the new time entry to the Firestore database
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
        // Check if a user is signed in
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            // Query Firestore to check the user's current check-in status
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
        // Format the timestamp into a human-readable date and time string
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private String formatDuration(long durationMillis) {
        // Convert the duration from milliseconds to hours and minutes
        long minutes = (durationMillis / 1000) / 60;
        long hours = minutes / 60;
        minutes = minutes % 60;
        return hours + "h " + minutes + "m";
    }

    private void showInAppNotification(String title, String body) {
        // Create and display an in-app notification using an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(body);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void calculateAndShowTotalDuration() {
        // Check if a user is signed in
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            // Query Firestore to retrieve all of the user's time entries
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
        // Check if a user is signed in
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            // Query Firestore to determine if the user is an admin
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

    // Interface for admin check callback
    interface AdminCheckCallback {
        void onCheck(boolean isAdmin);
    }
}
