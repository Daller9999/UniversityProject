package com.example.bluetooth;

import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class ClientSocket extends Thread {
    private MainActivity activity;
    private Socket socket;
    private String message = "";
    private ServerListen serverListen;
    private String address;
    private int port = -1;

    void setMessage(String message) {
        this.message = message;
    }

    ClientSocket(MainActivity activity, String address, int port) { // Инициализируем сокет клиента
        this.activity = activity;
        this.address = address;
        this.port = port;
    }

    public void run() {
        try {
            InetAddress serverAddr = InetAddress.getByName(address); // Инициализируем ip адрес сервера
            socket = new Socket(serverAddr, port);

            serverListen = new ServerListen(socket); // Инициализируем слушатель сервера
            serverListen.start();

            while (socket.isConnected()) {
                if (!message.isEmpty())
                    try {
                        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true); // Поток отправки сообщений на сервер
                        out.println(message);
                        message = "";
                    } catch(final Exception e) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(activity, "Error: " + e.toString(), Toast.LENGTH_SHORT).show(); // Сообщение об ошибке
                            }
                        });
                    }
            }
        } catch (IOException ex) {
            //
        }
    }

    private class ServerListen extends Thread { // Класс который слушает сервер
        private Socket socket;

        ServerListen(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                while (socket.isConnected()) {
                    final String read = reader.readLine();
                    if (read != null && read.length() != 0)
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(activity, read, Toast.LENGTH_SHORT).show();
                            }
                        });
                }
            } catch (IOException ex) {
                //
            }

        }
    }

    public void diconnect() {
        try {
            socket.close();
        } catch (IOException ex) {
            Toast.makeText(activity, "Can't close socket", Toast.LENGTH_SHORT).show();
        }
    }

}


