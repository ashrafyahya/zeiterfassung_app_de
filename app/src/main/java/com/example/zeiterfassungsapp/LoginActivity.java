package com.example.zeiterfassungsapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth; // Firebase-Authentifizierungsinstanz
    private FirebaseFirestore db; // Firestore-Datenbankinstanz
    private EditText emailEditText, passwordEditText; // Eingabefelder für E-Mail und Passwort
    private Button loginButton, registerButton; // Buttons für Login und Registrierung
    private static final String KEY_EMAIL = "email"; // Schlüssel für gespeicherte E-Mail
    private static final String KEY_PASSWORD = "password"; // Schlüssel für gespeichertes Passwort

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance(); // Initialisieren der Firebase-Authentifizierung
        db = FirebaseFirestore.getInstance(); // Initialisieren der Firestore-Instanz
        emailEditText = findViewById(R.id.emailEditText); // E-Mail-Eingabefeld verknüpfen
        passwordEditText = findViewById(R.id.passwordEditText); // Passwort-Eingabefeld verknüpfen
        loginButton = findViewById(R.id.loginButton); // Login-Button verknüpfen
        registerButton = findViewById(R.id.registerButton); // Registrierungs-Button verknüpfen

        loginButton.setOnClickListener(view -> loginUser()); // Listener für Login-Button
        registerButton.setOnClickListener(view -> registerUser()); // Listener für Registrierungs-Button

        if (savedInstanceState != null) {
            // Wiederherstellen der gespeicherten Benutzereingaben
            String savedEmail = savedInstanceState.getString(KEY_EMAIL);
            String savedPassword = savedInstanceState.getString(KEY_PASSWORD);
            emailEditText.setText(savedEmail);
            passwordEditText.setText(savedPassword);
        }
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim(); // Eingegebene E-Mail abrufen
        String password = passwordEditText.getText().toString().trim(); // Eingegebenes Passwort abrufen

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show(); // Fehlermeldung bei fehlenden Eingaben
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser(); // Aktuellen Benutzer abrufen
                        updateUI(user); // UI mit Benutzerinformationen aktualisieren
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show(); // Fehlermeldung bei fehlgeschlagener Authentifizierung
                        updateUI(null); // UI aktualisieren, wenn Benutzer null ist
                    }
                });
    }

    private void registerUser() {
        String email = emailEditText.getText().toString().trim(); // Eingegebene E-Mail abrufen
        String password = passwordEditText.getText().toString().trim(); // Eingegebenes Passwort abrufen

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show(); // Fehlermeldung bei fehlenden Eingaben
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser(); // Aktuellen Benutzer abrufen
                        if (user != null) {
                            // Benutzerdefinierte ID erstellen oder automatisch generieren
                            String customUserId = generateCustomUserId(); // Funktion zum Generieren der ID
                            // Benutzerdaten in Firestore speichern
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("email", email);
                            userData.put("customUserId", customUserId); // Benutzerdefinierte ID speichern
                            userData.put("role", "user"); // Standardrolle für jeden Benutzer
                            db.collection("users").document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(LoginActivity.this, "Registration successful.",
                                                Toast.LENGTH_SHORT).show(); // Erfolgsnachricht bei erfolgreicher
                                                                            // Registrierung
                                        updateUI(user); // UI mit Benutzerinformationen aktualisieren
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(LoginActivity.this, "Registration failed.", Toast.LENGTH_SHORT)
                                                .show(); // Fehlermeldung bei fehlgeschlagener Registrierung
                                        updateUI(null); // UI aktualisieren, wenn Benutzer null ist
                                    });
                        } else {
                            Toast.makeText(LoginActivity.this, "User is null.", Toast.LENGTH_SHORT).show(); // Fehlermeldung, wenn Benutzer null ist
                            updateUI(null); // UI aktualisieren, wenn Benutzer null ist
                        }
                    } else {
                        handleRegistrationError(task.getException()); // Fehler bei Registrierung behandeln
                    }
                });
    }

    private void handleRegistrationError(Exception exception) {
        if (exception instanceof FirebaseAuthWeakPasswordException) {
            Toast.makeText(this, "Weak password. Password should be at least 6 characters long.", Toast.LENGTH_SHORT)
                    .show(); // Fehlermeldung bei schwachem Passwort
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            Toast.makeText(this, "Invalid email format.", Toast.LENGTH_SHORT).show(); // Fehlermeldung bei ungültigem
                                                                                      // E-Mail-Format
        } else if (exception instanceof FirebaseAuthUserCollisionException) {
            Toast.makeText(this, "User with this email already exists.", Toast.LENGTH_SHORT).show(); // Fehlermeldung bei bestehendem Benutzer
        } else {
            Toast.makeText(this, "Registration failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show(); // Allgemeine Fehlermeldung
        }
    }

    private String generateCustomUserId() {
        // Generierung einer sechsstelligen numerischen ID
        Random random = new Random();
        int number = random.nextInt(900000) + 100000; // Zufällige Zahl zwischen 100000 und 999999
        return String.valueOf(number);
    }

    private void saveUserToFirestore(FirebaseUser user) {
        String userId = user.getUid(); // Benutzer-ID abrufen
        String email = user.getEmail(); // E-Mail abrufen
        Map<String, Object> userDoc = new HashMap<>();
        userDoc.put("userId", userId); // Benutzer-ID speichern
        userDoc.put("email", email); // E-Mail speichern
        userDoc.put("role", "user"); // Standardmäßig als "user" festlegen
        userDoc.put("name", ""); // Sie können das Namensfeld optional später aktualisieren

        db.collection("users").document(userId).set(userDoc)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(LoginActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show(); // Erfolgsnachricht bei erfolgreicher Registrierung
                    updateUI(user); // UI mit Benutzerinformationen aktualisieren
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Failed to register user: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show(); // Fehlermeldung bei fehlgeschlagener Registrierung
                });
    }

    private void updateRole(String userId, String newRole) {
        db.collection("users").document(userId)
                .update("role", newRole)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(LoginActivity.this, "Role updated successfully", Toast.LENGTH_SHORT).show(); // Erfolgsnachricht bei erfolgreicher Rollenaktualisierung
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(LoginActivity.this, "Failed to update role: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show(); // Fehlermeldung bei fehlgeschlagener Rollenaktualisierung
                });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Benutzereingaben speichern
        outState.putString(KEY_EMAIL, emailEditText.getText().toString());
        outState.putString(KEY_PASSWORD, passwordEditText.getText().toString());
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Intent intent = new Intent(LoginActivity.this, MainActivity.class); // Intent zur MainActivity erstellen
            startActivity(intent); // MainActivity starten
            finish(); // LoginActivity beenden
        }
    }
}