package com.favennec.podometreapi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // <-- NOUVEL IMPORT

import android.Manifest; // <-- NOUVEL IMPORT
import android.content.pm.PackageManager; // <-- NOUVEL IMPORT
import android.os.Build; // <-- NOUVEL IMPORT
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.favennec.libpodometreapi.StepCounter;

public class MainActivity extends AppCompatActivity implements StepCounter.StepListener {

    private static final String TAG = "MainActivity";
    private static final int ACTIVITY_RECOGNITION_REQUEST_CODE = 100; // Code pour la demande

    private TextView tvSteps;
    private StepCounter stepCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "onCreate: L'activité est en cours de création.");

        tvSteps = findViewById(R.id.tv_steps);

        // Créez une instance de votre StepCounter
        stepCounter = new StepCounter(this, this);
        Log.d(TAG, "onCreate: StepCounter a été initialisé.");

        // **DÉBUT DE LA LOGIQUE DE PERMISSION**
        // La permission n'est requise qu'à partir d'Android 10 (API 29)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Vérifier si la permission n'est PAS encore accordée
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                // Demander la permission à l'utilisateur
                Log.d(TAG, "onCreate: Permission ACTIVITY_RECOGNITION non accordée, demande en cours...");
                requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, ACTIVITY_RECOGNITION_REQUEST_CODE);
            } else {
                Log.d(TAG, "onCreate: Permission ACTIVITY_RECOGNITION déjà accordée.");
            }
        }
        // **FIN DE LA LOGIQUE DE PERMISSION**
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: L'activité est visible, tentative de démarrage du capteur.");

        // On ne démarre le capteur que si la permission est accordée (ou si elle n'est pas nécessaire sur les anciennes versions)
        boolean hasPermission = (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) ||
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED);

        if (hasPermission) {
            if (stepCounter.isSensorAvailable()) {
                stepCounter.start();
                Log.d(TAG, "onResume: Le listener du capteur de pas a été enregistré.");
            } else {
                Toast.makeText(this, "Capteur de pas non disponible sur cet appareil.", Toast.LENGTH_LONG).show();
                Log.w(TAG, "onResume: Capteur de pas non disponible !");
            }
        } else {
            Log.w(TAG, "onResume: Impossible de démarrer le capteur, permission refusée.");
            Toast.makeText(this, "Permission de reconnaissance d'activité refusée.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: L'activité est mise en pause.");
        if (stepCounter.isSensorAvailable()) {
            stepCounter.stop();
            Log.d(TAG, "onPause: Le listener du capteur de pas a été désenregistré.");
        }
    }

    @Override
    public void onStepChanged(int steps) {
        Log.d(TAG, "onStepChanged: Nouvelle valeur de pas calculée -> " + steps);
        tvSteps.setText(String.valueOf(steps));
    }

    // Cette méthode est appelée après que l'utilisateur a répondu à la demande de permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACTIVITY_RECOGNITION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: Permission accordée !");
                // La permission a été accordée, on peut maintenant démarrer le compteur dans onResume.
                // onResume sera appelé automatiquement après cette méthode.
            } else {
                Log.w(TAG, "onRequestPermissionsResult: Permission refusée par l'utilisateur.");
                Toast.makeText(this, "La permission est nécessaire pour compter les pas.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
