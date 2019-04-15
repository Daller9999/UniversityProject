/******************************************************

   Сервер BLE, который после получения соединения будет отправлять периодические уведомления.
   Служба оповещения: 6E400001-B5A3-F393-E0A9-E50E24DCCA9E
   Has a characteristic of: 6E400002-B5A3-F393-E0A9-E50E24DCCA9E - used for receiving data with "WRITE" 
   Has a characteristic of: 6E400003-B5A3-F393-E0A9-E50E24DCCA9E - used to send data with  "NOTIFY"

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

#define SERVICE_UUID           "6E400001-B5A3-F393-E0A9-E50E24DCCA9E" // UART service UUID
#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E" // Read data
#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E" // Write data

class MyServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer) { isDevBLE = true; };
    void onDisconnect(BLEServer* pServer) { isDevBLE = false; }
};

class MyCallbacks: public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic *pCharacteristic) {
        std::string rxValue = pCharacteristic->getValue();
      
        if (rxValue.length() > 0) { 
            String readBLE = "";   
            for (int i = 0; i < rxValue.length(); i++) {
              readBLE = readBLE + rxValue[i];  
            } 
            Serial.println("incomming message: " + readBLE); // Write on com3 incomming message  
        }    
    }
};

void setup() {
    Serial.begin(115200);
    while (!Serial);	
    Serial.println("Starting BLE!");
    BLEDevice::init("BLE");
    pServer = BLEDevice::createServer();                          // Create the BLE Server
    pServer->setCallbacks(new MyServerCallbacks());
    BLEService *pService = pServer->createService(SERVICE_UUID);  // Create the BLE Service
  
    pTxCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_TX, BLECharacteristic::PROPERTY_NOTIFY );// Create a BLE Characteristic
  
    pTxCharacteristic->addDescriptor(new BLE2902());
  
    BLECharacteristic * pRxCharacteristic = pService->createCharacteristic(CHARACTERISTIC_UUID_RX, BLECharacteristic::PROPERTY_WRITE );
  
    pRxCharacteristic->setCallbacks(new MyCallbacks());
    pService->start();                                              // Start the service
  
    pServer->getAdvertising()->start();                             // Start advertising
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
