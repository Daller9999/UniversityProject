package com.example.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;

public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice bluetoothDevice;
    private ClientSocket clientSocket;
    private boolean isConnected = false;
    private UUID SERVICE_UUID;
    private UUID CHARACTERISTIC_UUID;
    private UUID CHARACTERISTIC_UUID_WRITE = UUID.fromString("6e400002-b5a3-f393-e0A9-e50e24dcca9e");
    private TextView incomingMessageText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        requestPermissions(new String[]{Manifest.permission.ACCESS_WIFI_STATE}, 1);
        requestPermissions(new String[]{Manifest.permission.CHANGE_WIFI_STATE}, 1);
        requestPermissions(new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 1);
        requestPermissions(new String[]{Manifest.permission.CHANGE_NETWORK_STATE}, 1);
        requestPermissions(new String[]{Manifest.permission.INTERNET}, 1);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, intentFilter);

        Button buttonConnect = findViewById(R.id.connectToESP);
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mBluetoothAdapter.startDiscovery();
            }
        });

        Button buttonDisconnect = findViewById(R.id.disconnectToESP);
        buttonDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothGatt.close();
            }
        });

        final EditText editTextMessage = findViewById(R.id.editTextMessage);
        incomingMessageText = findViewById(R.id.incommingMessage);

        Button sendMessage = findViewById(R.id.buttonSendMessage);
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeCharacteristic(editTextMessage.getText().toString().getBytes());
            }
        });

        final EditText editTextIP = findViewById(R.id.editTextIP);
        final EditText editTextPort = findViewById(R.id.editTextPort);

        Button buttonConnectServer = findViewById(R.id.buttonConnectServer);
        buttonConnectServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editTextIP.getText().toString().isEmpty() && !editTextPort.getText().toString().isEmpty()){
                    String ip = editTextIP.getText().toString();
                    int port = Integer.valueOf(editTextPort.getText().toString());
                    clientSocket = new ClientSocket(MainActivity.this, ip, port);
                    clientSocket.start();
                }
            }
        });

        final EditText editTextServerMessage = findViewById(R.id.editTextWiFiMessage);

        Button sendWiFiMessage = findViewById(R.id.buttonSendMessageServer);
        sendWiFiMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editTextServerMessage.getText().toString().isEmpty())
                    clientSocket.setMessage(editTextServerMessage.getText().toString());
            }
        });
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                if (deviceName != null && deviceName.equals("BLE")) {
                    bluetoothDevice = device;
                    bluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this, true, gattCallback);
                    mBluetoothAdapter.cancelDiscovery();
                }
            }
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices();
                showNoutifacation("Connected Success");
            } else if (newState == STATE_DISCONNECTED) {
                isConnected = false;
                showNoutifacation("Connected failed");
            }
        }

        @Override public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int status) {
            boolean find = false;
            UUID serviceUUID = UUID.fromString("6e400001-b5a3-f393-e0A9-e50e24dcca9e");
            UUID characterUUID = UUID.fromString("6e400003-b5a3-f393-e0A9-e50e24dcca9e");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (BluetoothGattService service : bluetoothGatt.getServices()) {
                    if (service != null) {
                        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                            if (characteristic != null) {
                                for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                                    if (descriptor != null) {
                                        SERVICE_UUID = service.getUuid();
                                        CHARACTERISTIC_UUID = characteristic.getUuid();
                                        if (SERVICE_UUID.equals(serviceUUID) && CHARACTERISTIC_UUID.equals(characterUUID)) {
                                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                                            bluetoothGatt.writeDescriptor(descriptor);
                                            bluetoothGatt.setCharacteristicNotification(characteristic, true);
                                            isConnected = true;
                                            showNoutifacation("Gatt connected success");
                                            find = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (find) break;
                        }
                    }
                    if (find) break;
                }
            } else {
                showNoutifacation("Gatt failed");
            }
        }

        @Override public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
            showNoutifacation("End of success connection");
        }

        @Override public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            final String s = new String(characteristic.getValue());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    incomingMessageText.setText(s);
                }
            });
        }

        @Override public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) { }

        @Override public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            showNoutifacation("Read: " + byteArrToString(characteristic.getValue()));
        }
    };

    private void showNoutifacation(final String s) {
        runOnUiThread(new Runnable() {
            @Override public void run() { Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show(); }
        });
    }

    private String byteArrToString(byte[] value) {
        StringBuilder answer = new StringBuilder();
        for (byte b : value)
            answer.append(String.valueOf(b));
        return answer.toString();
    }

    public void writeCharacteristic(byte[] value) {
        if (bluetoothGatt != null && isConnected) {
            BluetoothGattCharacteristic characteristic = bluetoothGatt.getService(SERVICE_UUID).getCharacteristic(CHARACTERISTIC_UUID_WRITE);
            characteristic.setValue(value);
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            bluetoothGatt.requestConnectionPriority(BluetoothGatt.GATT_SUCCESS);
            bluetoothGatt.writeCharacteristic(characteristic);
        }
    }
}
