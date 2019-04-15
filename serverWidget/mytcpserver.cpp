#include "mytcpserver.h"
#include <QDebug>
#include <QCoreApplication>
#include <QTextCodec>

MyTcpServer::MyTcpServer(QObject *parent, QPlainTextEdit *plainTextEdit, quint16 *port)
    : QObject(parent)
{
    mTcpServer = new QTcpServer(this);

    this->plainTextEdit = plainTextEdit;

    this->port = port;

    connect(mTcpServer, &QTcpServer::newConnection, this, &MyTcpServer::slotNewConnection);

    // clientsVector = new QVector<QTcpSocket*>();

    if (!mTcpServer->listen(QHostAddress::Any, *this->port)) {
        this->plainTextEdit->appendPlainText("Server is NOT started(");
    } else {
        this->plainTextEdit->appendPlainText("Server is started!");
    }
}

void MyTcpServer::slotNewConnection()
{
    QTcpSocket* clientSocket = mTcpServer->nextPendingConnection();
    clientSocket->socketDescriptor();
    clientsVector.push_back(clientSocket);

    connect(clientSocket, &QTcpSocket::readyRead, this, &MyTcpServer::slotServerRead);
    connect(clientSocket, &QTcpSocket::disconnected, this, &MyTcpServer::slotClientDisconnected);
}

void MyTcpServer::slotServerRead()
{
    foreach (QTcpSocket* mTcpSocket, clientsVector) {
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
