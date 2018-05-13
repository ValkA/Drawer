package com.valka.drawer.Activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.valka.drawer.AppManager;
import com.valka.drawer.DataStructures.Vector;
import com.valka.drawer.DrawerDevice.BTDrawerDevice;
import com.valka.drawer.DrawerDevice.BaseDrawerDevice;
import com.valka.drawer.R;
import com.valka.drawer.Views.ManualControlView;

public class ManualControlActivity extends AppCompatActivity {
    ManualControlView manualControllerView;

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

                if(p.z!=_z) {
                    _z = p.z;
                    drawerDevice.sendGCodeCommand(String.format("G0 Z%.2f", p.z));//goto u
                }
                drawerDevice.sendGCodeCommand(String.format("G0 X%.2f Y%.2f", p.x, p.y));//goto u
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

}
