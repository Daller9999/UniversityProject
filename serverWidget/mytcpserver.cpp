#include "mytcpserver.h"
#include <QDebug>
#include <QCoreApplication>
#include <QTextCodec>

MyTcpServer::MyTcpServer(QObject *parent, QPlainTextEdit *plainTextEdit, quint16 *port)
    : QObject(parent)
{
    mTcpServer = new QTcpServer(this); // Инициализируем сервер

    this->plainTextEdit = plainTextEdit;

    this->port = port;

    connect(mTcpServer, &QTcpServer::newConnection, this, &MyTcpServer::slotNewConnection);

    // clientsVector = new QVector<QTcpSocket*>();

    if (!mTcpServer->listen(QHostAddress::Any, *this->port)) { // Сообщение о старте сервера
        this->plainTextEdit->appendPlainText("Server is NOT started(");
    } else {
        this->plainTextEdit->appendPlainText("Server is started!");
    }
}

void MyTcpServer::slotNewConnection()
{
    QTcpSocket* clientSocket = mTcpServer->nextPendingConnection(); // Устанавливаем связь с сервером
    clientSocket->socketDescriptor(); // Сохраняем нашего клиента
    clientsVector.push_back(clientSocket); // Добавляем клиента в массив

    connect(clientSocket, &QTcpSocket::readyRead, this, &MyTcpServer::slotServerRead); // Подключаем слоты и сигналы
    connect(clientSocket, &QTcpSocket::disconnected, this, &MyTcpServer::slotClientDisconnected);
}

void MyTcpServer::slotServerRead()
{
    foreach (QTcpSocket* mTcpSocket, clientsVector) { // Проходимся по всем клиентам и выводим их сообщения, если таковые есть
        while(mTcpSocket->bytesAvailable() > 0)
        {
            QByteArray array = mTcpSocket->readAll();
            QString DataAsString = QString::fromStdString(array.data());
            plainTextEdit->appendPlainText(DataAsString);
        }
    }
}

void MyTcpServer::slotClientDisconnected()
{
    // clientsVector. TODO реализовать удаление клиента из массива при отключении
}
