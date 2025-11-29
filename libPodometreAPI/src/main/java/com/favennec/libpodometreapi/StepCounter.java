package com.favennec.libpodometreapi;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;

@androidx.annotation.RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
public class StepCounter implements SensorEventListener {

    public interface StepListener {
        void onStepChanged(int steps);
    }

    private final SensorManager sensorManager;
    private final Sensor stepSensor;
    private final StepListener listener;
    private int initialSteps = -1;

    @androidx.annotation.RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public StepCounter(Context context, StepListener listener) {
        this.listener = listener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
    }

    public boolean isSensorAvailable() {
        return stepSensor != null;
    }

    public void start() {
        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int totalSteps = (int) event.values[0];

        if (initialSteps == -1) {
            initialSteps = totalSteps;
        }

        int stepsToday = totalSteps - initialSteps;

        if (listener != null) {
            listener.onStepChanged(stepsToday);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Rien à faire
    }
}
