#define MYTCPSERVER_H

#include <QObject>
#include <QTcpServer>
#include <QTcpSocket>
#include <QPlainTextEdit>
#include <QString>
#include <QLabel>
#include <QVector>

class MyTcpServer : public QObject
{
    Q_OBJECT
public:
    explicit MyTcpServer(QObject *parent, QPlainTextEdit *plainTextEdit, quint16 *port);

public slots:
    void slotNewConnection();
    void slotServerRead();
    void slotClientDisconnected();

private:
    QTcpServer * mTcpServer;
    QTcpSocket * mTcpSocket;
    QPlainTextEdit *plainTextEdit;
    quint16 *port;
    // QVector<QTcpSocket*> *clientsVector;
    std::vector<QTcpSocket*> clientsVector;
};
