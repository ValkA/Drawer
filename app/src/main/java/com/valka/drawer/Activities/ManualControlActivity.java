package com.valka.drawer.Activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.valka.drawer.AppManager;
import com.valka.drawer.DataStructures.Vector;
import com.valka.drawer.DrawerDevice.BTDrawerDevice;
import com.valka.drawer.DrawerDevice.BaseDrawerDevice;
import com.valka.drawer.R;
import com.valka.drawer.Views.ManualControlView;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ManualControlActivity extends AppCompatActivity {
    private final static String TAG = "ManualControlActivity";
    ManualControlView manualControllerView;
    DrawerThread drawerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_control);

        manualControllerView = (ManualControlView)findViewById(R.id.manual_control_view);
        manualControllerView.setOnPositionListener(new ManualControlView.OnPositionListener() {
            double _z = -1;

            @Override
            public void onPosition(Vector p) {
                BaseDrawerDevice drawerDevice = AppManager.getInstance().getDrawerDevice();
                if(drawerDevice == null) return;
                if(!drawerDevice.isConnected()) return;

                try {
                    drawerThread.getPointsQueue().put(new Vector(p));
                    Log.d(TAG, "+ pointsQueue.size() = " + drawerThread.getPointsQueue().size());
                } catch (InterruptedException e) {}
            }
        });

        findViewById(R.id.choose_device_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AppManager.getInstance().setDrawerDevice(new BTDrawerDevice(ManualControlActivity.this));
                AppManager.getInstance().getDrawerDevice().open(ManualControlActivity.this, new BaseDrawerDevice.OnOpenListener() {
                    @Override
                    public void onOpen(boolean success) {
                        if(!success){
                            Toast.makeText(ManualControlActivity.this, "Can't connect to device", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(ManualControlActivity.this, "Connected successfully", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onResume(){
        super.onResume();
        drawerThread = new DrawerThread();
        drawerThread.start();
    }

    @Override
    public void onPause(){
        if(drawerThread != null){
            drawerThread.interrupt();
            drawerThread = null;
        }
        super.onPause();
    }

    private class DrawerThread extends Thread{
        private double _z = 0;
        private BlockingQueue<Vector> pointsQueue = new LinkedBlockingQueue<>();

        public BlockingQueue getPointsQueue(){
            return pointsQueue;
        }

        public void run() {
            while(!this.isInterrupted()){
                BaseDrawerDevice drawerDevice = AppManager.getInstance().getDrawerDevice();
                if(drawerDevice == null || !drawerDevice.isConnected()) {
                    try {
                        Thread.sleep(100);
                        continue;
                    } catch (InterruptedException e) {
                        return;
                    }

                }

                Vector p = null;
                try {
                    p = pointsQueue.take();
                } catch (InterruptedException e) {
                    return;
                }

                if(p.z>_z) drawerDevice.sendGCodeCommand(String.format("G0 Z%.2f", p.z));//up
                drawerDevice.sendGCodeCommand(String.format("G0 X%.2f Y%.2f", p.x, p.y));//x,y
                if(p.z<_z) drawerDevice.sendGCodeCommand(String.format("G0 Z%.2f", p.z));//down
                _z = p.z;
                manualControllerView.drawerCallback(p);
            }
        }
    }

}
