#include "mainwindow.h"
#include "ui_mainwindow.h"
#include <QString>
#include <QNetworkInterface>

MainWindow::MainWindow(QWidget *parent) :
    QMainWindow(parent),
    ui(new Ui::MainWindow)
{
    ui->setupUi(this);
    lineEditPort = ui->lineEditPort;
    plainTextEdit = ui->plainTextEdit;

    QString locIP;
    QList<QHostAddress> addr = QNetworkInterface::allAddresses();
    ui->currentIP->setText(addr[4].toString());
}

MainWindow::~MainWindow()
{
    delete ui;
}

void MainWindow::on_pushButton_clicked()
{
    QString stringPort = lineEditPort->text();
    quint16 *port = new quint16(stringPort.toUInt());
    mytcpserver = new MyTcpServer(this, plainTextEdit, port);
}

void MainWindow::on_pushButton_2_clicked()
{
    // mytcpserver->
}
