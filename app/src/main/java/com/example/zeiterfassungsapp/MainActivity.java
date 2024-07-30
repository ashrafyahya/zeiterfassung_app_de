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

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
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

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extrahiere Daten
            String title = intent.getStringExtra("title");
            String body = intent.getStringExtra("body");
            
            Log.d("BroadcastReceiver", "Received message: " + title + " - " + body);


            // Zeige die In-App-Benachrichtigung
            showInAppNotification(title, body);
        }
    };

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        checkInButton = findViewById(R.id.checkInButton);
        checkOutButton = findViewById(R.id.checkOutButton);
        viewTimesButton = findViewById(R.id.viewTimesButton);
        logoutButton = findViewById(R.id.logoutButton);
        statusTextView = findViewById(R.id.statusTextView);
        tableLayout = findViewById(R.id.tableLayout);
        adminButton = findViewById(R.id.adminButton);
        totalDurationButton = findViewById(R.id.totalDurationButton);
        totalDurationTextView = findViewById(R.id.totalDurationTextView);

        checkInButton.setOnClickListener(v -> checkIn());
        checkOutButton.setOnClickListener(v -> checkOut());
        viewTimesButton.setOnClickListener(v -> viewTimes());
        logoutButton.setOnClickListener(v -> logout());

        // Registriere den Empfänger
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("MyFirebaseMessage"));

        totalDurationButton.setOnClickListener(v -> {
            if (totalDurationTextView.getVisibility() == View.GONE) {
                calculateAndShowTotalDuration();
            } else {
                totalDurationTextView.setVisibility(View.GONE);
                totalDurationButton.setText("Show Total Duration");
            }
        });

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
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
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
                        ((FirebaseFirestoreException) exception).getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                        Toast.makeText(MainActivity.this, "Failed to check in: user is already checked in", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Check In failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        } else {
            Toast.makeText(MainActivity.this, "User not signed in", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkOut() {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            db.collection("timeEntries")
               .whereEqualTo("userId", userId)
               .whereEqualTo("checkOut", null)
               .limit(1)
               .get()
               .addOnSuccessListener(documents -> {
                    if (!documents.isEmpty()) {
                        if (!documents.getDocuments().isEmpty()) {
                            com.google.firebase.firestore.DocumentSnapshot document = documents.getDocuments().get(0);
                            long checkOutTime = System.currentTimeMillis();
                            document.getReference().update("checkOut", checkOutTime)
                                .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Checked Out", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(exception -> Toast.makeText(MainActivity.this, "Check Out failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "No check-in found to check out", Toast.LENGTH_SHORT).show();
                    }
                })
               .addOnFailureListener(exception -> Toast.makeText(MainActivity.this, "Check Out failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show());
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
            if (mAuth.getCurrentUser() != null) {
                String userId = mAuth.getCurrentUser().getUid();
                db.collection("timeEntries")
                    .whereEqualTo("userId", userId)
                    .orderBy("checkIn", Query.Direction.DESCENDING)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            tableLayout.removeAllViews();
    
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
    
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Long checkInTime = document.getLong("checkIn");
                                Long checkOutTime = document.getLong("checkOut");
    
                                TableRow row = new TableRow(MainActivity.this);
                                row.setBackgroundColor(Color.parseColor("#FFFFFF"));
                                row.setPadding(16, 16, 16, 16);
    
                                TextView checkInView = new TextView(MainActivity.this);
                                checkInView.setText(checkInTime != null ? formatTime(checkInTime) : "N/A");
                                checkInView.setPadding(8, 8, 8, 8);
                                checkInView.setTextColor(Color.BLACK);
                                checkInView.setBackgroundResource(R.drawable.cell_border);
    
                                TextView checkOutView = new TextView(MainActivity.this);
                                checkOutView.setText(checkOutTime != null ? formatTime(checkOutTime) : "N/A");
                                checkOutView.setPadding(8, 8, 8, 8);
                                checkOutView.setTextColor(Color.BLACK);
                                checkOutView.setBackgroundResource(R.drawable.cell_border);
    
                                TextView durationView = new TextView(MainActivity.this);
                                durationView.setText(checkInTime != null && checkOutTime != null ? formatDuration(checkOutTime - checkInTime) : "N/A");
                                durationView.setPadding(8, 8, 8, 8);
                                durationView.setTextColor(Color.BLACK);
                                durationView.setBackgroundResource(R.drawable.cell_border);
    
                                row.addView(checkInView);
                                row.addView(checkOutView);
                                row.addView(durationView);
                                tableLayout.addView(row);
                            }
    
                            tableLayout.setVisibility(View.VISIBLE);
                            viewTimesButton.setText("Hide Times");
                            timesVisible = true;
                        } else {
                            Toast.makeText(MainActivity.this, "Failed to load times: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
            } else {
                Toast.makeText(MainActivity.this, "User not signed in", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @SuppressLint("DefaultLocale")
    private String formatDuration(long durationMillis) {
        long seconds = durationMillis / 1000 % 60;
        long minutes = durationMillis / (1000 * 60) % 60;
        long hours = durationMillis / (1000 * 60 * 60) % 24;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault());
        Date date = new Date(timestamp);
        return sdf.format(date);
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void isAdminUser(@NonNull final AdminCallback callback) {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String role = document.getString("role");
                        callback.onResult("admin".equals(role));
                    } else {
                        callback.onResult(false);
                    }
                })
                .addOnFailureListener(exception -> callback.onResult(false));
        } else {
            callback.onResult(false);
        }
    }

    private void saveTimeEntry(String userId, long checkInTime, Long checkOutTime) {
        HashMap<String, Object> timeEntry = new HashMap<String, Object>();
        timeEntry.put("userId", userId);
        timeEntry.put("checkIn", checkInTime);
        timeEntry.put("checkOut", checkOutTime);

        try {
            db.collection("timeEntries").add(timeEntry)
                .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Checked In", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Check In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Check In failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SetTextI18n")
    private void calculateAndShowTotalDuration() {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            db.collection("timeEntries")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(documents -> {
                    long totalDuration = 0L;
                    for (QueryDocumentSnapshot document : documents) {
                        Long checkInTime = document.getLong("checkIn");
                        Long checkOutTime = document.getLong("checkOut");
                        if (checkInTime != null && checkOutTime != null) {
                            totalDuration += (checkOutTime - checkInTime);
                        }
                    }
                    totalDurationTextView.setText("Total Duration: " + formatDuration(totalDuration));
                    totalDurationTextView.setVisibility(View.VISIBLE);
                    totalDurationButton.setText("Hide Total Duration");
                })
                .addOnFailureListener(exception -> Toast.makeText(MainActivity.this, "Failed to calculate total duration: " + exception.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(MainActivity.this, "User not signed in", Toast.LENGTH_SHORT).show();
        }
    }
    

    @Override
    protected void onDestroy() {
        // Unregister the receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onDestroy();
    }

    private void showInAppNotification(String title, String body) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(body)
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    private interface AdminCallback {
        void onResult(boolean isAdmin);
    }
}
