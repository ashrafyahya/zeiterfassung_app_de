package com.example.zeiterfassungsapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TableLayout adminTableLayout;
    private Button loadDataButton;
    private EditText userIdEditText, newRoleEditText;
    private Button changeRoleButton;

    private EditText customUserIdEditText;
    private Button loadUserTimeEntriesButton;
    private boolean userTimesVisible = false; // Track visibility of user time entries

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        customUserIdEditText = findViewById(R.id.customUserIdEditText);
        loadUserTimeEntriesButton = findViewById(R.id.loadUserTimeEntriesButton);
        loadUserTimeEntriesButton.setOnClickListener(view -> toggleUserTimeEntries());

        db = FirebaseFirestore.getInstance();
        adminTableLayout = findViewById(R.id.adminTableLayout);
        loadDataButton = findViewById(R.id.loadDataButton);
        userIdEditText = findViewById(R.id.userIdEditText);
        newRoleEditText = findViewById(R.id.newRoleEditText);
        changeRoleButton = findViewById(R.id.changeRoleButton);

        customUserIdEditText = findViewById(R.id.customUserIdEditText);
        loadDataButton.setOnClickListener(view -> loadAllData());
        changeRoleButton.setOnClickListener(view -> changeUserRole());

        // Beispiel: Laden aller Benutzerdaten beim Start der Aktivität
        loadAllUsers();
    }

    @SuppressLint("SetTextI18n")
    private void toggleUserTimeEntries() {
        if (userTimesVisible) {
            // Benutzerzeiten ausblenden
            adminTableLayout.removeAllViews();
            loadUserTimeEntriesButton.setText("Load User Time Entries");
            userTimesVisible = false;

            // Benutzerliste wieder anzeigen
            loadAllUsers();
        } else {
            String customUserId = customUserIdEditText.getText().toString().trim();
            if (TextUtils.isEmpty(customUserId)) {
                Toast.makeText(this, "Please enter a custom user ID", Toast.LENGTH_SHORT).show();
                return;
            }
            loadUserTimeEntries(customUserId);
        }
    }

    @SuppressLint("SetTextI18n")
    private void loadUserTimeEntries(String customUserId) {
        db.collection("users")
                .whereEqualTo("customUserId", customUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            String userId = task.getResult().getDocuments().get(0).getId();
                            db.collection("timeEntries")
                                    .whereEqualTo("userId", userId)
                                    .orderBy("checkIn", Query.Direction.DESCENDING)
                                    .get()
                                    .addOnCompleteListener(timeTask -> {
                                        if (timeTask.isSuccessful()) {
                                            adminTableLayout.removeAllViews();
                                            // Header row
                                            TableRow headerRow = new TableRow(this);
                                            TextView checkInHeader = new TextView(this);
                                            checkInHeader.setText("Check In");
                                            checkInHeader.setPadding(8, 8, 8, 8);
                                            checkInHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                                            TextView checkOutHeader = new TextView(this);
                                            checkOutHeader.setText("Check Out");
                                            checkOutHeader.setPadding(8, 8, 8, 8);
                                            checkOutHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                                            TextView durationHeader = new TextView(this);
                                            durationHeader.setText("Duration");
                                            durationHeader.setPadding(8, 8, 8, 8);
                                            durationHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                                            headerRow.addView(checkInHeader);
                                            headerRow.addView(checkOutHeader);
                                            headerRow.addView(durationHeader);
                                            adminTableLayout.addView(headerRow);

                                            // Data rows
                                            for (QueryDocumentSnapshot document : timeTask.getResult()) {
                                                Long checkInTime = document.getLong("checkIn");
                                                Long checkOutTime = document.getLong("checkOut");

                                                TableRow row = new TableRow(this);
                                                TextView checkInView = new TextView(this);
                                                checkInView
                                                        .setText(checkInTime != null ? formatTime(checkInTime) : "N/A");
                                                checkInView.setPadding(8, 8, 8, 8);
                                                TextView checkOutView = new TextView(this);
                                                checkOutView.setText(
                                                        checkOutTime != null ? formatTime(checkOutTime) : "N/A");
                                                checkOutView.setPadding(8, 8, 8, 8);

                                                TextView durationView = new TextView(this);
                                                String durationText = "N/A";
                                                if (checkInTime != null && checkOutTime != null) {
                                                    long durationMillis = checkOutTime - checkInTime;
                                                    durationText = formatDuration(durationMillis);
                                                }
                                                durationView.setText(durationText);
                                                durationView.setPadding(8, 8, 8, 8);

                                                row.addView(checkInView);
                                                row.addView(checkOutView);
                                                row.addView(durationView);
                                                adminTableLayout.addView(row);
                                            }
                                            userTimesVisible = true;
                                            loadUserTimeEntriesButton.setText("Hide User Time Entries");
                                        } else {
                                            Toast.makeText(this,
                                                    "Failed to load time entries: "
                                                            + timeTask.getException().getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(this, "User ID does not exist", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Failed to retrieve user: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String formatDuration(long durationMillis) {
        long seconds = durationMillis / 1000 % 60;
        long minutes = durationMillis / (1000 * 60) % 60;
        long hours = durationMillis / (1000 * 60 * 60) % 24;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void loadAllUsers() {
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                adminTableLayout.removeAllViews();

                // Header row
                TableRow headerRow = new TableRow(AdminActivity.this);
                TextView emailHeader = new TextView(AdminActivity.this);
                emailHeader.setText("Email");
                emailHeader.setPadding(8, 8, 8, 8);
                emailHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                TextView roleHeader = new TextView(AdminActivity.this);
                roleHeader.setText("Role");
                roleHeader.setPadding(8, 8, 8, 8);
                roleHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                TextView idHeader = new TextView(AdminActivity.this);
                idHeader.setText("User ID");
                idHeader.setPadding(8, 8, 8, 8);
                idHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                headerRow.addView(emailHeader);
                headerRow.addView(roleHeader);
                headerRow.addView(idHeader);
                adminTableLayout.addView(headerRow);

                // Data rows
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String email = document.getString("email");
                    String role = document.getString("role");
                    String customUserId = document.getString("customUserId"); // Get User ID

                    TableRow row = new TableRow(AdminActivity.this);
                    TextView emailView = new TextView(AdminActivity.this);
                    emailView.setText(email != null ? email : "N/A");
                    emailView.setPadding(8, 8, 8, 8);
                    TextView roleView = new TextView(AdminActivity.this);
                    roleView.setText(role != null ? role : "N/A");
                    roleView.setPadding(8, 8, 8, 8);
                    TextView idView = new TextView(AdminActivity.this);
                    idView.setText(customUserId != null ? customUserId : "N/A"); // Display User ID
                    idView.setPadding(8, 8, 8, 8);
                    idView.setSingleLine(false); // Enable multi-line wrapping
                    idView.setEllipsize(null); // Disable ellipsize

                    row.addView(emailView);
                    row.addView(roleView);
                    row.addView(idView);
                    adminTableLayout.addView(row);
                }
            } else {
                Toast.makeText(AdminActivity.this, "Failed to load users: " + task.getException(), Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void loadAllData() {
        db.collection("timeEntries")
                .orderBy("checkIn", Query.Direction.DESCENDING) // Sort by checkIn time in descending order
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        adminTableLayout.removeAllViews();

                        // Header row
                        TableRow headerRow = new TableRow(this);
                        TextView checkInHeader = new TextView(this);
                        checkInHeader.setText("Check In");
                        checkInHeader.setPadding(8, 8, 8, 8);
                        checkInHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                        TextView checkOutHeader = new TextView(this);
                        checkOutHeader.setText("Check Out");
                        checkOutHeader.setPadding(8, 8, 8, 8);
                        checkOutHeader.setTypeface(null, android.graphics.Typeface.BOLD);
                        headerRow.addView(checkInHeader);
                        headerRow.addView(checkOutHeader);
                        adminTableLayout.addView(headerRow);

                        // Data rows
                        for (com.google.firebase.firestore.QueryDocumentSnapshot document : task.getResult()) {
                            Long checkInTime = document.getLong("checkIn");
                            Long checkOutTime = document.getLong("checkOut");

                            TableRow row = new TableRow(this);
                            TextView checkInView = new TextView(this);
                            checkInView.setText(checkInTime != null ? formatTime(checkInTime) : "N/A");
                            checkInView.setPadding(8, 8, 8, 8);
                            TextView checkOutView = new TextView(this);
                            checkOutView.setText(checkOutTime != null ? formatTime(checkOutTime) : "N/A");
                            checkOutView.setPadding(8, 8, 8, 8);

                            row.addView(checkInView);
                            row.addView(checkOutView);
                            adminTableLayout.addView(row);
                        }
                    } else {
                        Toast.makeText(this, "Failed to load time entries: " + task.getException(), Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        Date date = new Date(timestamp);
        return sdf.format(date);
    }

    private void changeUserRole() {
        String customUserId = userIdEditText.getText().toString().trim();
        String newRole = newRoleEditText.getText().toString().trim();

        if (TextUtils.isEmpty(customUserId) || TextUtils.isEmpty(newRole)) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users")
                .whereEqualTo("customUserId", customUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            db.collection("users").document(document.getId())
                                    .update("role", newRole)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(AdminActivity.this, "User role updated successfully",
                                                Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(AdminActivity.this,
                                                "Failed to update user role: " + e.getMessage(), Toast.LENGTH_SHORT)
                                                .show();
                                    });
                        }
                    } else {
                        Toast.makeText(this, "User ID does not exist", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminActivity.this, "Failed to update user role: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }
}
