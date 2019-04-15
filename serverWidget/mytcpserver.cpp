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

    if (!mTcpServer->listen(QHostAddress::Any, *this->port)) {
        this->plainTextEdit->appendPlainText("Server is NOT started(");
    } else {
        this->plainTextEdit->appendPlainText("Server is started!");
    }
}

void MyTcpServer::slotNewConnection()
{
    mTcpSocket = mTcpServer->nextPendingConnection();

    connect(mTcpSocket, &QTcpSocket::readyRead, this, &MyTcpServer::slotServerRead);
    connect(mTcpSocket, &QTcpSocket::disconnected, this, &MyTcpServer::slotClientDisconnected);
}

void MyTcpServer::slotServerRead()
{
    while(mTcpSocket->bytesAvailable() > 0)
    {
        QByteArray array = mTcpSocket->readAll();        
        QString DataAsString = QString::fromStdString(array.data());
        plainTextEdit->appendPlainText(DataAsString);        
    }
}

void MyTcpServer::slotClientDisconnected()
{
    mTcpSocket->close();
}
