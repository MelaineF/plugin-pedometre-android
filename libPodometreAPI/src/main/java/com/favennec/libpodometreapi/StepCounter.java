package com.favennec.libpodometreapi;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StepCounter implements SensorEventListener {

    private static final String TAG = "StepCounterLib"; // TAG pour filtrer les logs de la bibliothèque

    public interface StepListener {
        void onStepChanged(int steps);
    }

    private final SensorManager sensorManager;
    private final Sensor stepSensor;
    private final StepListener listener;
    private final SharedPreferences prefs;

    // Clés pour la sauvegarde
    private static final String PREFS_NAME = "StepCounterPrefs";
    private static final String KEY_INITIAL_STEPS = "initial_steps";
    private static final String KEY_LAST_SAVE_DATE = "last_save_date";
    private static final String KEY_STEPS_TODAY = "steps_today";


    public StepCounter(Context context, StepListener listener) {
        this.listener = listener;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Vérifie si le capteur de pas est disponible sur l'appareil.
     * @return true si le capteur existe, false sinon.
     */
    public boolean isSensorAvailable() {
        return stepSensor != null;
    }

    /**
     * Enregistre le listener pour commencer à écouter les événements du capteur.
     * Le batching (10s) permet au CPU de rester endormi entre les mises à jour.
     */
    public void start() {
        if (stepSensor != null) {
            // maxReportLatencyUs = 10 000 000 µs = 10 secondes de batching hardware
            // Le chip accumule les pas et ne réveille le CPU qu'une fois toutes les 10s
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL, 10_000_000);
        }
    }

    /**
     * Désenregistre le listener pour arrêter l'écoute et économiser la batterie.
     */
    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // La valeur totale depuis le dernier redémarrage de l'appareil
        int totalStepsFromBoot = (int) event.values[0];
        Log.d(TAG, "onSensorChanged: Valeur BRUTE du capteur = " + totalStepsFromBoot);

        // Récupérer la date du jour au format YYYY-MM-DD
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastSaveDate = prefs.getString(KEY_LAST_SAVE_DATE, "");

        // Récupérer la valeur initiale sauvegardée pour la journée. -1 si non trouvée.
        int initialSteps = prefs.getInt(KEY_INITIAL_STEPS, -1);
        Log.d(TAG, "onSensorChanged: Date du jour=" + todayDate + ", Dernière date sauvée=" + lastSaveDate + ", Pas initiaux sauvés=" + initialSteps);

        // Nouveau jour, première utilisation, ou reboot détecté (totalStepsFromBoot < initialSteps)
        boolean isNewDay = !todayDate.equals(lastSaveDate);
        boolean isFirstUse = initialSteps == -1;
        boolean isReboot = !isNewDay && !isFirstUse && totalStepsFromBoot < initialSteps;

        if (isNewDay || isFirstUse || isReboot) {
            if (isReboot) Log.i(TAG, "onSensorChanged: REBOOT DETECTE. Réinitialisation du compteur.");
            else Log.i(TAG, "onSensorChanged: NOUVEAU JOUR ou PREMIERE UTILISATION. Réinitialisation des pas pour la journée.");
            initialSteps = totalStepsFromBoot;

            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt(KEY_INITIAL_STEPS, initialSteps);
            editor.putString(KEY_LAST_SAVE_DATE, todayDate);
            editor.apply();

            Log.d(TAG, "onSensorChanged: Nouveaux pas initiaux sauvés: " + initialSteps + " pour la date " + todayDate);
        }

        // Le nombre de pas du jour est le total actuel moins la valeur de départ de la journée
        int stepsToday = totalStepsFromBoot - initialSteps;

        Log.d(TAG, "onSensorChanged: Calcul -> totalSteps(" + totalStepsFromBoot + ") - initialSteps(" + initialSteps + ") = " + stepsToday);

        // Persister les pas du jour pour pouvoir les lire sans attendre le prochain événement capteur
        prefs.edit().putInt(KEY_STEPS_TODAY, stepsToday).apply();

        if (listener != null) {
            listener.onStepChanged(stepsToday);
        }
    }

    /** Retourne les derniers pas du jour connus sans attendre un événement capteur. */
    public static int getLastKnownStepsToday(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String lastSaveDate = prefs.getString(KEY_LAST_SAVE_DATE, "");
        if (!todayDate.equals(lastSaveDate)) return 0; // nouveau jour, compteur remis à zéro
        return prefs.getInt(KEY_STEPS_TODAY, 0);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Rien à faire ici, mais on peut loguer si on veut
        Log.d(TAG, "onAccuracyChanged: Précision changée à " + accuracy);
    }
}
