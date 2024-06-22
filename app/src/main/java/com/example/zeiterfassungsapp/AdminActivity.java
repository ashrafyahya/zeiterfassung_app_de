package com.example.zeiterfassungsapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TableLayout adminTableLayout;
    private Button loadDataButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();
        adminTableLayout = findViewById(R.id.adminTableLayout);
        loadDataButton = findViewById(R.id.loadDataButton);

        loadDataButton.setOnClickListener(view -> loadAllData());
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
}
