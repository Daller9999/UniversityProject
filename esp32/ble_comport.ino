/******************************************************

   Сервер BLE, который после получения соединения будет отправлять периодические уведомления.
   Служба оповещения: 6E400001-B5A3-F393-E0A9-E50E24DCCA9E
   Has a characteristic of: 6E400002-B5A3-F393-E0A9-E50E24DCCA9E - uuid для чтения
   Has a characteristic of: 6E400003-B5A3-F393-E0A9-E50E24DCCA9E - uuid для записи

 Дизайн создания сервера BLE:
   1. Создание сервера BLE
   2. Создание службы BLE
   3. Создание характеристики BLE для службы
   4. Создание дескриптора признака
   5. Запуск службы
   6. начать оповещения

*****************************************************/

#ifndef ironBLE_h

#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <BLE2902.h>

BLEServer *pServer = NULL;
BLECharacteristic * pTxCharacteristic;

bool isDevBLE = false;
bool isOldDevBLE = false;

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E" // Имя сервиса 
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E" // Характеристика чтения данных
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E" // Характеристика записи(отправки) данных

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) { isDevBLE = true; };
    void onDisconnect(BLEServer* pServer) { isDevBLE = false; }
};

class MyCallbacks: public BLECharacteristicCallbacks { // Колбэк изменения характеристики(принятия данных)
    void onWrite(BLECharacteristic *pCharacteristic) {
        std::string rxValue = pCharacteristic->getValue();
      
        if (rxValue.length() > 0) { 
            String readBLE = "";   
            for (int i = 0; i < rxValue.length(); i++) {
              readBLE = readBLE + rxValue[i];  
            } 
            Serial.println("incomming message: " + readBLE); // Вывод сообщений на com3  
        }    
    }
};

void setup() {
    Serial.begin(115200); // Начинаем работать на скорости 
    while (!Serial);	
    Serial.println("Starting BLE!"); // Выводим сообщение о запуске платы
    BLEDevice::init("BLE");
    pServer = BLEDevice::createServer();                          // Создаём сервер BLE
    pServer->setCallbacks(new MyServerCallbacks());
    BLEService *pService = pServer->createService(SERVICE_UUID);  // Создаём сервис BLE
  
    pTxCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_TX, BLECharacteristic::PROPERTY_NOTIFY );// Create a BLE Characteristic
  
    pTxCharacteristic->addDescriptor(new BLE2902());
  
    BLECharacteristic * pRxCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_RX, BLECharacteristic::PROPERTY_WRITE );
  
    pRxCharacteristic->setCallbacks(new MyCallbacks());
    pService->start();                                              // Запускаем сервис
  
    pServer->getAdvertising()->start();                             // Запускаем поиск на сервере BLE
}

//---------------------------------
void loop () {
  	String str = "";
      
  	if (Serial.available()) {
  	    str = Serial.readString();
  	}  
  
  	if (str != "") {
        Serial.println("out message : " + str);
    	  if (isDevBLE) {
        		char buf_char[str.length()+1];
        		str.toCharArray(buf_char, str.length()+1);
        		pTxCharacteristic->setValue(buf_char);
        		pTxCharacteristic->notify();
        		delay(10);                      // стек bluetooth перейдет в перегрузку, если будет отправлено слишком много пакетов
        }
  
      	if ((!isDevBLE) && (isOldDevBLE)) {     // отключение
          	delay(500);                         // дайте стеку bluetooth шанс получить данные
          	pServer->startAdvertising();        // перезапустить оповещение
          	Serial.println("start advertising");
          	isOldDevBLE = isDevBLE;
      	}
      	if ((isDevBLE) && (!isOldDevBLE)) {// подключение
      		  isOldDevBLE = isDevBLE;        // выполнять код при подключение
      	}
    }
}
#endif
