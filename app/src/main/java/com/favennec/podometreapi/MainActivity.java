package com.favennec.podometreapi;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.favennec.libpodometreapi.StepCounter;


public class MainActivity extends AppCompatActivity {

    StepCounter stepCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1) INITIALISER AVANT DE L'UTILISER
        stepCounter = new StepCounter(this, steps -> {
            TextView txt = findViewById(R.id.textViewSteps);
            txt.setText("Pas : " + steps);
        });

        // 2) ENSUITE vérifier si le capteur existe
        if (!stepCounter.isSensorAvailable()) {
            Toast.makeText(this, "Capteur de pas NON disponible ❌", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Capteur de pas OK ✔️", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        stepCounter.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stepCounter.stop();
    }
}
