package com.favennec.libpodometreapi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;

/**
 * Bridge entre le plugin Android et Unity.
 *
 * Depuis C# Unity, utiliser AndroidJavaClass :
 *
 *   AndroidJavaClass bridge = new AndroidJavaClass("com.favennec.libpodometreapi.PedometerBridge");
 *   bridge.CallStatic("Initialize", context, "PedometerManager", "OnStepUpdate");
 *   bool ok = bridge.CallStatic<bool>("IsSensorAvailable");
 *   bridge.CallStatic("StartCounting");
 *   int steps = bridge.CallStatic<int>("GetStepsToday");
 *   bridge.CallStatic("StopCounting");
 *
 * Le GameObject Unity nommé "PedometerManager" doit avoir une méthode publique "OnStepUpdate(string steps)".
 */
public class PedometerBridge {

    private static final String TAG = "PedometerBridge";
    private static final String PREFS_NAME = "PedometerBridgePrefs";
    private static final String KEY_GAME_OBJECT = "unity_game_object";
    private static final String KEY_METHOD = "unity_method";

    private static Context appContext;
    private static String unityGameObject = "PedometerManager";
    private static String unityMethod = "OnStepUpdate";

    /**
     * À appeler en premier depuis Unity pour initialiser le bridge.
     * @param context        Le contexte Android (passer UnityPlayer.currentActivity)
     * @param gameObjectName Nom du GameObject Unity qui recevra les mises à jour de pas
     * @param methodName     Nom de la méthode sur ce GameObject (signature : void Method(string steps))
     */
    public static void Initialize(Context context, String gameObjectName, String methodName) {
        appContext = context.getApplicationContext();

        if (gameObjectName != null && !gameObjectName.isEmpty()) unityGameObject = gameObjectName;
        if (methodName != null && !methodName.isEmpty()) unityMethod = methodName;

        // Persister pour que le service puisse les retrouver s'il redémarre sans Unity
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_GAME_OBJECT, unityGameObject)
                .putString(KEY_METHOD, unityMethod)
                .apply();

        Log.d(TAG, "Initialize: context=" + appContext.getPackageName()
                + " gameObject=" + unityGameObject + " method=" + unityMethod);
    }

    /**
     * Retourne true si le téléphone possède un capteur de pas hardware.
     * À appeler après Initialize(). Si false, ne pas appeler StartCounting().
     */
    public static boolean IsSensorAvailable() {
        if (appContext == null) return false;
        SensorManager sm = (SensorManager) appContext.getSystemService(Context.SENSOR_SERVICE);
        return sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null;
    }

    /**
     * Démarre le comptage de pas en arrière-plan (Foreground Service).
     * La permission ACTIVITY_RECOGNITION doit être accordée avant d'appeler cette méthode.
     */
    public static void StartCounting() {
        if (appContext == null) {
            Log.e(TAG, "StartCounting: Initialize() n'a pas été appelé.");
            return;
        }
        Intent intent = new Intent(appContext, StepCounterService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent);
        } else {
            appContext.startService(intent);
        }
        Log.d(TAG, "StartCounting: Service démarré.");
    }

    /**
     * Arrête le comptage de pas et le Foreground Service.
     */
    public static void StopCounting() {
        if (appContext == null) return;
        appContext.stopService(new Intent(appContext, StepCounterService.class));
        Log.d(TAG, "StopCounting: Service arrêté.");
    }

    /**
     * Retourne le nombre de pas du jour.
     * Lit depuis le cache SharedPreferences si le service n'est pas encore actif.
     */
    public static int GetStepsToday() {
        if (appContext != null) {
            return StepCounter.getLastKnownStepsToday(appContext);
        }
        return 0;
    }

    /**
     * Appelé par StepCounterService à chaque mise à jour du capteur.
     * Envoie la valeur au GameObject Unity via UnitySendMessage (par réflexion).
     * Charge les noms depuis SharedPreferences si les statiques sont vides (service redémarré seul).
     */
    static void notifyUnity(int steps) {
        loadPersistedNamesIfNeeded();
        Log.d(TAG, "notifyUnity: " + steps + " pas -> " + unityGameObject + "." + unityMethod);
        try {
            Class<?> unityPlayer = Class.forName("com.unity3d.player.UnityPlayer");
            java.lang.reflect.Method sendMessage = unityPlayer.getMethod(
                    "UnitySendMessage", String.class, String.class, String.class);
            sendMessage.invoke(null, unityGameObject, unityMethod, String.valueOf(steps));
        } catch (ClassNotFoundException e) {
            Log.d(TAG, "notifyUnity: UnityPlayer non disponible (hors Unity).");
        } catch (Exception e) {
            Log.e(TAG, "notifyUnity: Erreur lors de l'appel UnitySendMessage.", e);
        }
    }

    // Charge les noms persistés si le service a redémarré sans qu'Unity ait appelé Initialize()
    private static void loadPersistedNamesIfNeeded() {
        if (appContext == null) return;
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedObject = prefs.getString(KEY_GAME_OBJECT, null);
        String savedMethod = prefs.getString(KEY_METHOD, null);
        if (savedObject != null) unityGameObject = savedObject;
        if (savedMethod != null) unityMethod = savedMethod;
    }
}
