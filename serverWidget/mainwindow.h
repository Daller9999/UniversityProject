#ifndef MAINWINDOW_H
#define MAINWINDOW_H

#include <QMainWindow>
#include <mytcpserver.h>
#include <QLineEdit>
#include <QPlainTextEdit>

namespace Ui {
class MainWindow;
}

class MainWindow : public QMainWindow
{
    Q_OBJECT

public:
    explicit MainWindow(QWidget *parent = nullptr);
    ~MainWindow();

private slots:
    void on_pushButton_clicked();

    void on_pushButton_2_clicked();

private:
    Ui::MainWindow *ui;
    MyTcpServer *mytcpserver; // Сервер
    QLineEdit *lineEditIP; // Поле вывода IP
    QLineEdit *lineEditPort; // Поле ввода порта
    QPlainTextEdit *plainTextEdit; // Поле вывода сообщений
};

#endif // MAINWINDOW_H
