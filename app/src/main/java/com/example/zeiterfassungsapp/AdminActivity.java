package com.example.zeiterfassungsapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
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

    // Firebase Firestore Instanz zur Kommunikation mit der Datenbank
    private FirebaseFirestore db;
    private TableLayout adminTableLayout; // Layout zur Anzeige von Tabellen
    private Button loadDataButton; // Button zum Laden aller Zeitbuchungen
    private EditText userIdEditText, newRoleEditText; // Eingabefelder für Benutzer-ID und neue Rolle
    private Button changeRoleButton; // Button zum Ändern der Benutzerrolle

    private EditText customUserIdEditText; // Eingabefeld für benutzerdefinierte Benutzer-ID
    private Button loadUserTimeEntriesButton; // Button zum Laden von Zeitbuchungen für einen bestimmten Benutzer
    private boolean userTimesVisible = false; // Flag zur Überprüfung, ob Benutzer-Zeitbuchungen sichtbar sind

    // onCreate(Bundle savedInstanceState): Initialisierung der UI-Komponenten und
    // Setzen der Event-Listener für die Buttons.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Initialisierung der UI-Komponenten
        // UI-Komponenten: Erklärt die verschiedenen UI-Elemente wie Buttons, EditTexts
        // und TableLayouts.
        customUserIdEditText = findViewById(R.id.customUserIdEditText);
        loadUserTimeEntriesButton = findViewById(R.id.loadUserTimeEntriesButton);
        loadDataButton = findViewById(R.id.loadDataButton);
        userIdEditText = findViewById(R.id.userIdEditText);
        newRoleEditText = findViewById(R.id.newRoleEditText);
        changeRoleButton = findViewById(R.id.changeRoleButton);
        adminTableLayout = findViewById(R.id.adminTableLayout);

        // Initialisierung der Firebase Firestore Instanz
        db = FirebaseFirestore.getInstance();

        // Setzen des Klick-Listeners für den Button zum Laden von Zeitbuchungen eines
        // bestimmten Benutzers
        loadUserTimeEntriesButton.setOnClickListener(view -> toggleUserTimeEntries());

        // Setzen des Klick-Listeners für den Button zum Laden aller Zeitbuchungen
        loadDataButton.setOnClickListener(view -> loadAllData());

        // Setzen des Klick-Listeners für den Button zum Ändern der Benutzerrolle
        changeRoleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userId = userIdEditText.getText().toString().trim();
                String newRole = newRoleEditText.getText().toString().trim();

                // Überprüfen, ob die Eingabefelder nicht leer sind
                if (userId.isEmpty() || newRole.isEmpty()) {
                    Toast.makeText(AdminActivity.this, "User ID and Role cannot be empty", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Überprüfen, ob die Rolle gültig ist
                if (!isValidRole(newRole)) {
                    Toast.makeText(AdminActivity.this, "Invalid role. Please enter 'user' or 'admin'",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                // Ändern der Benutzerrolle
                changeUserRole(userId, newRole);
            }
        });
        // Laden aller Benutzerdaten beim Start der Aktivität
        loadAllUsers();
    }

    // toggleUserTimeEntries(): Wechselt zwischen der Anzeige von
    // Benutzer-Zeitbuchungen und Benutzerlisten.
    @SuppressLint("SetTextI18n")
    private void toggleUserTimeEntries() {
        if (userTimesVisible) {
            // Benutzerzeiten ausblenden
            adminTableLayout.removeAllViews(); // Entfernt alle Views aus dem TableLayout
            loadUserTimeEntriesButton.setText("Load User Time Entries"); // Button-Text ändern
            userTimesVisible = false;

            // Benutzerliste wieder anzeigen
            loadAllUsers();
        } else {
            String customUserId = customUserIdEditText.getText().toString().trim();
            // Überprüfen, ob die benutzerdefinierte Benutzer-ID eingegeben wurde
            if (TextUtils.isEmpty(customUserId)) {
                Toast.makeText(this, "Please enter a custom user ID", Toast.LENGTH_SHORT).show();
                return;
            }
            // Laden der Zeitbuchungen für den angegebenen Benutzer
            loadUserTimeEntries(customUserId);
        }
    }

    // loadUserTimeEntries(String customUserId): Lädt und zeigt die Zeitbuchungen
    // eines bestimmten Benutzers an.
    @SuppressLint("SetTextI18n")
    private void loadUserTimeEntries(String customUserId) {
        db.collection("users")
                .whereEqualTo("customUserId", customUserId) // Suche nach Benutzer mit dieser benutzerdefinierten ID
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            // Wenn der Benutzer gefunden wurde, lade seine Zeitbuchungen
                            String userId = task.getResult().getDocuments().get(0).getId();
                            db.collection("timeEntries")
                                    .whereEqualTo("userId", userId)
                                    .orderBy("checkIn", Query.Direction.DESCENDING)
                                    .get()
                                    .addOnCompleteListener(timeTask -> {
                                        if (timeTask.isSuccessful()) {
                                            adminTableLayout.removeAllViews();
                                            // Header-Zeile hinzufügen
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

                                            // Daten-Zeilen hinzufügen
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

    // formatDuration(long durationMillis): Formatiert die Dauer von Millisekunden
    // in Stunden:Minuten
    private String formatDuration(long durationMillis) {
        // Formatierung der Dauer von Millisekunden in Stunden:Minuten:Sekunden
        long seconds = durationMillis / 1000 % 60;
        long minutes = durationMillis / (1000 * 60) % 60;
        long hours = durationMillis / (1000 * 60 * 60) % 24;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // loadAllUsers(): Lädt und zeigt alle Benutzer aus der Datenbank an.
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

    // loadAllData(): Lädt und zeigt alle Zeitbuchungen aus der Datenbank an.
    @SuppressLint("SetTextI18n")
    private void loadAllData() {
        try {
            // Lade alle Benutzerdaten
            loadAllUsers();
    
            // Zeige eine Toast-Nachricht an, wenn das Laden der Daten erfolgreich war
            Toast.makeText(this, "Daten erfolgreich geladen", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            // Fehlerbehandlung, wenn das Abrufen der Daten fehlgeschlagen ist
            Toast.makeText(this, "Fehler beim Laden der Daten: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    
    // formatTime(long timestamp): Formatiert einen Zeitstempel in Datum und
    // Uhrzeit.
    private String formatTime(long timestamp) {
        db.collection("timeEntries")
                .orderBy("checkIn", Query.Direction.DESCENDING); // Sort by checkIn time in descending order
        // Formatierung des Zeitstempels in Datum und Uhrzeit
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        Date date = new Date(timestamp);
        return sdf.format(date);
    }

    // changeUserRole(String customUserId, String newRole): Ändert die Rolle eines
    // Benutzers basierend auf der ID.
    private void changeUserRole(String customUserId, String newRole) {
        // String customUserId = userIdEditText.getText().toString().trim();
        // String newRole = newRoleEditText.getText().toString().trim();

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
                            // Wenn der Benutzer gefunden wurde, ändere seine Rolle
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

    // isValidRole(String role): Überprüft die Gültigkeit der Rolle ("user" oder
    // "admin").
    private boolean isValidRole(String role) {
        // Überprüfen, ob die eingegebene Rolle entweder "user" oder "admin" ist
        return role.equals("user") || role.equals("admin");
    }

}
