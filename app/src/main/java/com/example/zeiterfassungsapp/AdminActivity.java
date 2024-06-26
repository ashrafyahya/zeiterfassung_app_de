package com.example.zeiterfassungsapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();
        adminTableLayout = findViewById(R.id.adminTableLayout);
        loadDataButton = findViewById(R.id.loadDataButton);
        userIdEditText = findViewById(R.id.userIdEditText);
        newRoleEditText = findViewById(R.id.newRoleEditText);
        changeRoleButton = findViewById(R.id.changeRoleButton);

        loadDataButton.setOnClickListener(view -> loadAllData());
        changeRoleButton.setOnClickListener(view -> changeUserRole());

        // Beispiel: Laden aller Benutzerdaten beim Start der Aktivität
        loadAllUsers();
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
                headerRow.addView(emailHeader);
                headerRow.addView(roleHeader);
                adminTableLayout.addView(headerRow);
    
                // Data rows
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String email = document.getString("email");
                    String role = document.getString("role");
    
                    TableRow row = new TableRow(AdminActivity.this);
                    TextView emailView = new TextView(AdminActivity.this);
                    emailView.setText(email != null ? email : "N/A");
                    emailView.setPadding(8, 8, 8, 8);
                    TextView roleView = new TextView(AdminActivity.this);
                    roleView.setText(role != null ? role : "N/A");
                    roleView.setPadding(8, 8, 8, 8);
    
                    row.addView(emailView);
                    row.addView(roleView);
                    adminTableLayout.addView(row);
                }
            } else {
                Toast.makeText(AdminActivity.this, "Failed to load users: " + task.getException(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadAllData() {
        db.collection("timeEntries").get().addOnCompleteListener(task -> {
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
                // Handle errors
            }
        });
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        Date date = new Date(timestamp);
        return sdf.format(date);
    }

    private void changeUserRole() {
        String userId = userIdEditText.getText().toString().trim();
        String newRole = newRoleEditText.getText().toString().trim();
    
        if (userId.isEmpty() || newRole.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }
    
        db.collection("users").document(userId)
                .update("role", newRole)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AdminActivity.this, "User role updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminActivity.this, "Failed to update user role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
}
