package com.example.bluetooth_device;

import static com.example.bluetooth_device.MyViewModel.handler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    Button listen;
    Button show;
    Button send;
    ListView listview;
    @SuppressLint("StaticFieldLeak")
    static EditText messageText;
    @SuppressLint("StaticFieldLeak")
    static TextView textMess;
    @SuppressLint("StaticFieldLeak")
    static TextView status;

    SendReceive sendReceive;

    BluetoothAdapter Bluetoothadapter;
    BluetoothDevice[] device;
    static final int STATE_LISTENING = 1;
    static final int STATE_CONNECTING = 2;
    static final int STATE_CONNECTED = 3;
    static final int STATE_CONNECTION_FAILED = 4;
    static final int STATE_MESSAGE_RECEIVED = 5;
    int REQUEST_ENABLE = 1;
    private MyViewModel myViewModel;

    private ArrayAdapter<String> arrayAdapter;
    private static final String APP_NAME = "Device";
    private static final UUID My_UUID = UUID.fromString("c3af2228-e388-11ed-b5ea-0242ac120002");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FindViewById();
        Bluetoothadapter = BluetoothAdapter.getDefaultAdapter();
        myViewModel = new ViewModelProvider(this).get(MyViewModel.class);

        arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1);
        listview.setAdapter(arrayAdapter);

        myViewModel.getDeviceListLiveData().observe(this, strings -> {
            arrayAdapter.clear();
            arrayAdapter.addAll(strings);
        });
        myViewModel.getStatusLiveData().observe(this, s -> {
            setStatus(s);
        });
        myViewModel.getMessageLiveData().observe(this, message -> {
            setMessage(message);
        });
        myViewModel.getDeviceArrayLiveData().observe(this, devices -> {
            if (devices != null && devices.length > 0) {
                arrayAdapter.clear();
                String[] strings = new String[devices.length];
                for (int i = 0; i < devices.length; i++) {
                    if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        strings[i] = devices[i].getName();
                    }

                }
                arrayAdapter.addAll(strings);
            }
        });

        if (!Bluetoothadapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(intent, REQUEST_ENABLE);
            }

        }
        implementListener();


    }

    private void setStatus(String s) {
        status.setText(s);
    }

    private void setMessage(String message) {
        textMess.setText(message);
    }

    private void implementListener() {
        show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myViewModel.startBluetoothDiscovery(MainActivity.this);

            }
        });
        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Server_Class server_class = new Server_Class();
                server_class.start();
            }
        });
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BluetoothDevice[] devices = myViewModel.getDeviceArrayLiveData().getValue();
                if (devices != null && i < devices.length) {
                    Client_Class client_class = new Client_Class(devices[i]);
                    client_class.start();
                    status.setText("Connecting");
                }
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (sendReceive == null || sendReceive.bluetoothSocket == null || !sendReceive.bluetoothSocket.isConnected()) {
                    Toast.makeText(MainActivity.this, "Devices are not connected", Toast.LENGTH_SHORT).show();
                } else {
                    String string = String.valueOf(messageText.getText());
                    sendReceive.write(string.getBytes());
                }
            }

        });
    }


    private void FindViewById() {
        status = findViewById(R.id.status);
        listen = findViewById(R.id.button);
        textMess = findViewById(R.id.textMess);
        send = findViewById(R.id.send);
        messageText = findViewById(R.id.message);
        listview = findViewById(R.id.listview);
        show = findViewById(R.id.Show);
    }

    private class Server_Class extends Thread {
        private BluetoothServerSocket serverSocket;

        public Server_Class() {
            try {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    serverSocket = Bluetoothadapter.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, My_UUID);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            BluetoothSocket socket = null;
            while (true) {
                try {
                    Message message1 = Message.obtain();
                    message1.what = STATE_CONNECTED;
                    handler.sendMessage(message1);
                    socket = serverSocket.accept();

                } catch (IOException e) {
                    Message message1 = Message.obtain();
                    message1.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message1);
                }
                if (socket != null) {
                    Message message1 = Message.obtain();
                    message1.what = STATE_CONNECTED;
                    handler.sendMessage(message1);
                    sendReceive = new SendReceive(socket);
                    sendReceive.start();
                    break;
                }
            }

        }

    }

    private class Client_Class extends Thread {
        private BluetoothSocket socket;
        private BluetoothDevice device;

        public Client_Class(BluetoothDevice device) {
            this.device = device;
            try {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    socket = device.createInsecureRfcommSocketToServiceRecord(My_UUID);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                try {
                    socket.connect();
                    Message message1 = Message.obtain();
                    message1.what = STATE_CONNECTED;
                    handler.sendMessage(message1);
                    sendReceive = new SendReceive(socket);
                    sendReceive.start();
                } catch (IOException e) {
                    Message message1 = Message.obtain();
                    message1.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message1);
                    e.printStackTrace();
                }
            }

        }
    }

    private static class SendReceive extends Thread {
        private BluetoothSocket bluetoothSocket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(BluetoothSocket socket) {
            bluetoothSocket = socket;
            OutputStream tempOut = null;
            InputStream tempIn = null;

            try {
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            inputStream = tempIn;
            outputStream = tempOut;

        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int Byte;
            while (true) {
                try {
                    Byte = inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, Byte, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }

    }

}