# Zeiterfassung-App

## Überblick

Die Zeiterfassungs-App ist eine Android-Anwendung, die Benutzern die Möglichkeit bietet, ihre Arbeitszeiten zu erfassen, sich ein- und auszuchecken und ihre Zeitaufzeichnungen zu verwalten. Die App verwendet Firebase für Authentifizierung und Datenverwaltung.

## Technologien

- **Programmiersprachen**: Java
- **Entwicklungsumgebung**: Android Studio
- **Backend**: Firebase
  - **Firebase Authentication**: Benutzerverwaltung und Authentifizierung
  - **Firebase Firestore**: Speicherung und Abruf von Zeitaufzeichnungen
  - **Firebase FCM**: Firebase Cloud Messaging (FCM) Integration  
- **Datenbank**: Cloud Firestore
- **Layout**: XML für Android UI
- **Bibliotheken**: Firebase SDK

## Funktionen

### Benutzerauthentifizierung
- **Registrierung**: Benutzer können sich registrieren.
- **Login**: Vorhandene Benutzer können sich anmelden.

### Zeiterfassung
- **Check-In**: Benutzer können sich einchecken, um ihre Arbeitszeit zu beginnen.
- **Check-Out**: Benutzer können sich auschecken, um ihre Arbeitszeit zu beenden.
- **Zeiten anzeigen**: Benutzer können eine Übersicht ihrer Check-In- und Check-Out-Zeiten anzeigen lassen.

### Admin-Funktionen
- **Zeiten für einen bestimmten Benutzer anzeigen**: Admins können die Zeitaufzeichnungen eines spezifischen Benutzers einsehen.
- **Rollenverwaltung**: Admins können die Rollen von Benutzern ändern (z.B. Benutzer in Admin umwandeln).

## Installation

### Klonen des Repositories
```bash
git clone https://github.com/ashrafyahya/zeiterfassung_app_de

