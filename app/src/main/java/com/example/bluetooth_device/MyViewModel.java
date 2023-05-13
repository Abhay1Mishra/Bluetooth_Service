package com.example.bluetooth_device;

import static com.example.bluetooth_device.MainActivity.STATE_CONNECTED;
import static com.example.bluetooth_device.MainActivity.STATE_CONNECTING;
import static com.example.bluetooth_device.MainActivity.STATE_CONNECTION_FAILED;
import static com.example.bluetooth_device.MainActivity.STATE_LISTENING;
import static com.example.bluetooth_device.MainActivity.STATE_MESSAGE_RECEIVED;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Set;

public class MyViewModel extends ViewModel {
    private final BluetoothAdapter bluetoothAdapter;
    private final MutableLiveData<String[]> deviceListLiveData;
    public final MutableLiveData<BluetoothDevice[]> deviceArrayLiveData;

    private static final MutableLiveData<String> statusLiveData = new MutableLiveData<>();
    private static final MutableLiveData<String> messageLiveData = new MutableLiveData<>();

    public MyViewModel() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        deviceListLiveData = new MutableLiveData<>();
        deviceArrayLiveData = new MutableLiveData<>();
    }

    public LiveData<String[]> getDeviceListLiveData() {
        return deviceListLiveData;
    }

    public LiveData<BluetoothDevice[]> getDeviceArrayLiveData() {
        return deviceArrayLiveData;
    }

    public LiveData<String> getStatusLiveData() {
        return statusLiveData;
    }

    public LiveData<String> getMessageLiveData() {
        return messageLiveData;
    }

    public void startBluetoothDiscovery(Activity activity) {
        if (bluetoothAdapter == null) {
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        BluetoothDevice[] devices = new BluetoothDevice[bondedDevices.size()];
        String[] deviceNames = new String[bondedDevices.size()];
        int index = 0;

        for (BluetoothDevice device : bondedDevices) {
            devices[index] = device;
            deviceNames[index] = device.getName();
            index++;
        }

        deviceListLiveData.postValue(deviceNames);
        deviceArrayLiveData.postValue(devices);
    }
    static Handler handler = new Handler(new Handler.Callback() {
        @SuppressLint("SetTextI18n")
        @Override
        public boolean handleMessage(@NonNull Message message) {
            switch (message.what) {
                case STATE_LISTENING:
                    statusLiveData.setValue("Listening");
                    break;
                case STATE_CONNECTING:
                    statusLiveData.setValue("Connecting");
                    break;
                case STATE_CONNECTED:
                    statusLiveData.setValue("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    statusLiveData.setValue("Failed");
                    break;
                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuffer = (byte[]) message.obj;
                    String msg =new String(readBuffer,0,message.arg1);
                    messageLiveData .setValue(msg);
                    break;
            }
            return true;
        }
    });

}
