package com.valka.drawer;

import com.valka.drawer.DrawerDevice.BTDrawerDevice;
import com.valka.drawer.DrawerDevice.BaseDrawerDevice;

/**
 * Created by valentid on 09/05/2018.
 */

public class AppManager {
    private static volatile AppManager instance;

    private AppManager(){
        if (instance != null) throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
    }

    public static AppManager getInstance() {
        if (instance == null) { //if there is no instance available... create new one
            synchronized (AppManager.class) {
                if (instance == null) instance = new AppManager();
            }
        }
        return instance;
    }

    private BaseDrawerDevice drawerDevice;

    public BaseDrawerDevice getDrawerDevice(){
        return drawerDevice;
    }

    public void setDrawerDevice(final BaseDrawerDevice drawerDevice){
        this.drawerDevice = drawerDevice;
    }

}
