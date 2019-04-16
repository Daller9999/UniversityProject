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
    private UUID SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0A9-e50e24dcca9e");
    private UUID CHARACTERISTIC_UUID = UUID.fromString("6e400003-b5a3-f393-e0A9-e50e24dcca9e");;
    private UUID CHARACTERISTIC_UUID_WRITE = UUID.fromString("6e400002-b5a3-f393-e0A9-e50e24dcca9e");
    private TextView incomingMessageText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //  Запрашиваем необходимые разарешение для работы WiFi и Bluetooth
        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        requestPermissions(new String[]{Manifest.permission.ACCESS_WIFI_STATE}, 1);
        requestPermissions(new String[]{Manifest.permission.CHANGE_WIFI_STATE}, 1);
        requestPermissions(new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, 1);
        requestPermissions(new String[]{Manifest.permission.CHANGE_NETWORK_STATE}, 1);
        requestPermissions(new String[]{Manifest.permission.INTERNET}, 1);

        // Инициализируем адаптер и настраиваем филтр отклика
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, intentFilter);

        // Инициализируем кнопку и вешаем на неё лисенер
        Button buttonConnect = findViewById(R.id.connectToESP);
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mBluetoothAdapter.startDiscovery(); // Начинаем поиск доступных устройств для подключения
            }
        });

        Button buttonDisconnect = findViewById(R.id.disconnectToESP); // Инициализируем кнопку отключеия от платы
        buttonDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothGatt.close(); // Закрываем подлючение
            }
        });

        final EditText editTextMessage = findViewById(R.id.editTextMessage); // Инициализируем поле ввода
        incomingMessageText = findViewById(R.id.incommingMessage); // Иницилизируем поле входящих сообщений с платы

        Button sendMessage = findViewById(R.id.buttonSendMessage); // Иницилизируем кнопку отправки сообщений
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeCharacteristic(editTextMessage.getText().toString().getBytes()); // Отправляем байты данных из поля ввода
            }
        });

        final EditText editTextIP = findViewById(R.id.editTextIP); // Иницилизируем поле ввода IP
        final EditText editTextPort = findViewById(R.id.editTextPort); // Иницилизируем поле ввода порта

        Button buttonConnectServer = findViewById(R.id.buttonConnectServer); // Иницилизируем кнопку поделючения к серверу
        buttonConnectServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editTextIP.getText().toString().isEmpty() && !editTextPort.getText().toString().isEmpty()) { // Проверяем не пустые ли поля ввода
                    String ip = editTextIP.getText().toString();
                    int port = Integer.valueOf(editTextPort.getText().toString());
                    clientSocket = new ClientSocket(MainActivity.this, ip, port); // Устанавливаем подключение к серверу
                    clientSocket.start();
                }
            }
        });

        final EditText editTextServerMessage = findViewById(R.id.editTextWiFiMessage); // Иницилизируем поле ввода для отправки сообщений по WiFi

        Button sendWiFiMessage = findViewById(R.id.buttonSendMessageServer);
        sendWiFiMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!editTextServerMessage.getText().toString().isEmpty()) // Проверяем пустое ли поле ввода
                    clientSocket.setMessage(editTextServerMessage.getText().toString()); // Отправляем сообщение
            }
        });
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) { // Ресивер отвлекающийся на поиск найденных устройств
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE); // Получаем данные устройства
                String deviceName = device.getName();
                if (deviceName != null && deviceName.equals("BLE")) { // Проверяем наше ли это устройство
                    bluetoothDevice = device;
                    bluetoothGatt = bluetoothDevice.connectGatt(MainActivity.this, true, gattCallback); // Уставливаем соединение с платой
                    mBluetoothAdapter.cancelDiscovery(); // Закрываем поиск устройств
                }
            }
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() { // Коллбэк обратных вызывов по bluetooth

        @Override public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) { // Проверяем состояние подключение и выводим сообщение для каждого из подключений
                gatt.discoverServices();
                showNoutifacation("Connected Success");
            } else if (newState == STATE_DISCONNECTED) {
                isConnected = false;
                showNoutifacation("Connected failed");
            }
        }

        @Override public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int status) {
            BluetoothGattService bluetoothGattService = bluetoothGatt.getService(SERVICE_UUID); // Иницилизируем наш сервис
            BluetoothGattCharacteristic bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(CHARACTERISTIC_UUID); // Иницилизируем нашу характеристику
            if (status == BluetoothGatt.GATT_SUCCESS) { // Проверяем статус подключения
                for (BluetoothGattDescriptor descriptor : bluetoothGattCharacteristic.getDescriptors()) { // Проходим по всем дескрипторам(описания)
                    if (descriptor != null) { // Проверяем не нулевой ли наш дескриптор
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); // Уставливаем значение принятия сообщений
                        bluetoothGatt.writeDescriptor(descriptor); // Уставливаеи значение bluetooth
                        bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true); // Ставим разрешение на получение уведомлений
                        isConnected = true; // Ставим статус подключения true
                        showNoutifacation("Gatt connected success"); // Выводим уведомление об успешном подключении
                    }
                }
            } else {
                showNoutifacation("Gatt failed");
            }
        }

        @Override public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
            showNoutifacation("End of success connection"); // Выводим сообщение об успешном подключении
        }

        @Override public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            final String s = new String(characteristic.getValue());
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    incomingMessageText.setText(s); // Выводим сообщение с платы
                }
            });
        }

        @Override public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) { }

        @Override public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            showNoutifacation("Read: " + byteArrToString(characteristic.getValue())); // Сообщения для прочтение характеристик
        }
    };

    private void showNoutifacation(final String s) { // Функция для вывода сообщения в основном потоке, т.к все остальные операции bluetooth проходят в дополнительном потоке
        runOnUiThread(new Runnable() {
            @Override public void run() { Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show(); }
        });
    }

    private String byteArrToString(byte[] value) { // Перевод из байтов в стринг
        StringBuilder answer = new StringBuilder();
        for (byte b : value)
            answer.append(String.valueOf(b));
        return answer.toString();
    }

    public void writeCharacteristic(byte[] value) { // Отправка сообщения на плату(смена характеристики нашего сервиса)
        if (bluetoothGatt != null && isConnected) {
            BluetoothGattCharacteristic characteristic = bluetoothGatt.getService(SERVICE_UUID).getCharacteristic(CHARACTERISTIC_UUID_WRITE); // Иницилизируем нашу характеристику
            characteristic.setValue(value); // уставливаем наше сообщение
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT); // Ставим значения отправки
            bluetoothGatt.requestConnectionPriority(BluetoothGatt.GATT_SUCCESS);
            bluetoothGatt.writeCharacteristic(characteristic); // Отправляем сообщение на плату
        }
    }
}
