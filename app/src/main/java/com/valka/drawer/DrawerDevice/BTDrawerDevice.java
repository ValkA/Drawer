package com.valka.drawer.DrawerDevice;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.valka.drawer.R;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

/**
 * Created by valentid on 28/08/2017.
 */

public class BTDrawerDevice extends BaseDrawerDevice {
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final int BLUETOOTH_TURN_ON_REQUEST_CODE = 1;

    BluetoothSocket socket = null;
    boolean isConnected = false;
    final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    HandlerThread ioThread;
    Handler ioHandler;

    public BTDrawerDevice(Activity activity){
        //make sure bluetooth exist
        if(bluetoothAdapter == null){
            Toast.makeText(activity, "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
        } else if (!bluetoothAdapter.isEnabled()) {
            //make sure bluetooth enabled
            askToTurnOnBluetooth(activity);
        }
    }

    @Override
    public void open(final Activity activity, final OnOpenListener onOpenListener) {
        //make sure bluetooth enabled
        if (!bluetoothAdapter.isEnabled()) {
            askToTurnOnBluetooth(activity);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onOpenListener.onOpen(false);
                }
            });
            return;
        }

        showPairedDevices(activity, onOpenListener, new OnBluetoothDeviceChosenListener() {
            @Override
            public void onBluetoothDeviceChosen(final BluetoothDevice chosenDevice) {
                AsyncTask<Void, Void, Boolean> connectionTask = new AsyncTask<Void, Void, Boolean>(){
                    private ProgressDialog progressDialog;
                    protected void onPreExecute() { progressDialog = ProgressDialog.show(activity, "Connecting...", "Please wait!!!"); }
                    protected Boolean doInBackground(Void... devices) {
                        if (socket != null) {
                            this.cancel(true);
                            Log.e(this.getClass().getSimpleName(), "BTDrawerDevice.open() was called while it is connected or already trying to connect a device");
                            return false;//still trying to connect or already connected
                        }
                        try {
                            socket = chosenDevice.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
                            bluetoothAdapter.cancelDiscovery();
                            socket.connect();
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                        ioThread = new HandlerThread(this.getClass().getSimpleName()+" - IO Thread");
                        ioThread.start();
                        ioHandler = new Handler(ioThread.getLooper());
                        return true;
                    }
                    @Override
                    protected void onPostExecute(Boolean success){
                        progressDialog.dismiss();
                        if(isCancelled()){
                            return;
                        }
                        if (!success) {
                            socket = null;
                            onOpenListener.onOpen(false);
                            return;
                        }
                        onOpenListener.onOpen(true);
                    }
                }.execute();
            }
        });

    }

    @Override
    public void close(Activity activity, final OnCloseListener onCloseListener) {
        if (socket == null) {
            Log.e(this.getClass().getSimpleName(), "BTDrawerDevice.close() was called on a closed device");
            return;
        }
        try{
            socket.close();
            socket = null;
        } catch (IOException e){
            e.printStackTrace();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onCloseListener.onClose(false);
                }
            });
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onCloseListener.onClose(true);
            }
        });
    }

    @Override
    public void sendGCodeCommandASync(final String gCodeCommand, final OnGCodeCommandResponse onGCodeCommandResponse) {
        final BluetoothSocket fSocket = socket;
        ioHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    fSocket.getOutputStream().write((gCodeCommand+"\n").getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                    onGCodeCommandResponse.onResponse(CommandResponse.Error);
                    return;
                }
                byte[] res = new byte[2];
                for(int i=0; i<3; ++i){
                    try {
                        if(i < 2) res[i] = (byte)fSocket.getInputStream().read();
                        else {
                            char endOfResponse = (char)fSocket.getInputStream().read();
                            if(endOfResponse == '\n'){
                                throw new IOException("end of response didn't end with \\n");
                            }
                        }
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                        onGCodeCommandResponse.onResponse(CommandResponse.Error);
                        return;
                    }
                }
                String response = new String(res);
                switch (response){
                    case "ok":
                        onGCodeCommandResponse.onResponse(CommandResponse.OK);
                        break;
                    case "er":
                    default:
                        onGCodeCommandResponse.onResponse(CommandResponse.OK);
                        break;

                }
            }
        });
    }

    @Override
    public CommandResponse sendGCodeCommand(final String gCodeCommand){
        try {
            socket.getOutputStream().write((gCodeCommand+"\n").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
            return CommandResponse.Error;
        }

        byte[] res = new byte[4];
        for(int i=0; i<4; ++i){
            try {
                res[i] = (byte)socket.getInputStream().read();
            } catch (IOException e) {
                e.printStackTrace();
                return CommandResponse.Error;
            }
        }

        if(res[2] != '\r' || res[3] != '\n'){
            return CommandResponse.Error;
        }

        String response = new String(res,0,2);
        switch (response){
            case "ok":
                return CommandResponse.OK;
            case "er":
            default:
                return CommandResponse.Error;
        }
    }

    private void askToTurnOnBluetooth(Activity activity){
        activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), BLUETOOTH_TURN_ON_REQUEST_CODE);
    }


    private interface OnBluetoothDeviceChosenListener{
        void onBluetoothDeviceChosen(BluetoothDevice chosenDevice);
    }

    private void showPairedDevices(Activity activity, final OnOpenListener onOpenListener, final OnBluetoothDeviceChosenListener onBluetoothDeviceChosenListener) {
        Set<BluetoothDevice> devicesSet = bluetoothAdapter.getBondedDevices();
        //TODO: filter irrelevant device (by their name or something...)
        final BluetoothDevice[] devices = devicesSet.toArray(new BluetoothDevice[devicesSet.size()]);
        Arrays.sort(devices, new Comparator<BluetoothDevice>() {
            @Override
            public int compare(BluetoothDevice d1, BluetoothDevice d2) { return d1.getName().compareTo(d2.getName()); }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Choose Drawer");
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                onOpenListener.onOpen(false);
            }
        });
        builder.setSingleChoiceItems(new PairedDevicesAdapter(devices, activity), 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                onBluetoothDeviceChosenListener.onBluetoothDeviceChosen(devices[i]);
                dialogInterface.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    private class PairedDevicesAdapter implements ListAdapter {
        final BluetoothDevice[] devices;
        final Context context;
        public PairedDevicesAdapter(final BluetoothDevice[] devices, Context context){
            this.devices = devices;
            this.context = context;
        }
        public boolean areAllItemsEnabled() { return true; }
        public boolean isEnabled(int i) { return true; }
        public void registerDataSetObserver(DataSetObserver dataSetObserver) {}
        public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {}
        public int getCount() {return devices.length;}
        public Object getItem(int i) {return devices[i];}
        public long getItemId(int i) {return i;}
        public boolean hasStableIds() {return true;}
        public View getView(int i, View view, ViewGroup viewGroup) {
            if(view == null){
                LayoutInflater layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = layoutInflater.inflate(R.layout.bluetooth_device_list_item, null);
            }
            ((TextView)view.findViewById(R.id.device_name_text_view)).setText(devices[i].getName());
            ((TextView)view.findViewById(R.id.device_address_text_view)).setText(devices[i].getAddress());
            return view;
        }
        public int getItemViewType(int i) { return 0; }
        public int getViewTypeCount() { return 1; }
        public boolean isEmpty() { return devices.length == 0; }
    }
}
