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
    QTcpServer * mTcpServer; // Наш сервер
    QPlainTextEdit *plainTextEdit; // Поле вывода сообщений
    quint16 *port; // Порт
    std::vector<QTcpSocket*> clientsVector; // Массив клиентов
};
