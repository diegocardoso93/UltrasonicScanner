// UNISC - Universidade de Santa Cruz do Sul
// Disciplina de Sistemas Embarcados
// com Marcio Pacheco 2017.1
//
// Scanner Ultrassonico
//
// Diego Cardoso               
// Douglas Tatsch
// Eduardo Henrique Machado
//

#include <CurieBLE.h>
#include "NewPing.h"

#define TRIGGER_PIN       12
#define ECHO_PIN          11
#define MAX_DISTANCE      250
#define MAX_PACKAGE_SIZE  20
#define SECURITY_KEY      {0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07}
#define FAKE_SECURITY_KEY {0x07,0x06,0x05,0x04,0x03,0x02,0x01,0x00}
#define IOT               0x02
#define EOT               0x04

NewPing scanner(TRIGGER_PIN, ECHO_PIN, MAX_DISTANCE);

BLEPeripheral blePeripheral;
BLEService bleService("19B10010-E8F2-537E-4F6C-D104768A1214");
BLECharacteristic bleCharacteristic("19B10011-E8F2-537E-4F6C-D104768A1214", BLERead | BLEWrite | BLENotify, MAX_PACKAGE_SIZE);

typedef struct PackageTemplate {
  byte iot;        // init of transmission 
  byte sk[8];      // security key
  byte pl;         // payload length
  byte payload[8]; // data
  byte crc;        // cyclic redundancy check
  byte eot;        // end of transmission
} Package;

Package blePack;

/* Instructions */
enum {
  NOP,
  REQ_READ_SCANNER_SENSOR,
  REQ_STOP_MESSAGES,
  REQ_SET_SCANNER_REFRESH_RATE,
  REQ_SET_SCANNER_MAX_DISTANCE,
  RESP_READ_SCANNER_SENSOR,
  RESP_STOP_MESSAGES,
  RESP_SET_SCANNER_REFRESH_RATE,
  RESP_SET_SCANNER_MAX_DISTANCE
};

void setup() {
  Serial.begin(9600);

  blePeripheral.setLocalName("ARDUINO UTRASONIC SCANNER");
  blePeripheral.setAdvertisedServiceUuid(bleService.uuid());

  blePeripheral.addAttribute(bleService);
  blePeripheral.addAttribute(bleCharacteristic);

  blePeripheral.begin();
}

unsigned int analogVal = 0;              // for save read angle
unsigned long pingVal = 0;               // for save read distance
byte bleByteArray[MAX_PACKAGE_SIZE];     // final package to send
int refreshRate = 6;                     // samples per second
int delayRate = 1000/refreshRate;        // in ms
long previousMillis = 0;                 // non-blocking time control
boolean stopped = true;                  // stopp notify control
unsigned int maxDistance = MAX_DISTANCE; // max distance control

void loop() {

  BLECentral central = blePeripheral.central();
  
  if (central) {
    Serial.print("Connected to central: ");
    Serial.println(central.address());
    stopped = true;
    while (central.connected()) {
      if (bleCharacteristic.written()) {
        Serial.println("Package received.");
        blePack = byteArrayToPackage(blePack, bleCharacteristic.value());
        if (checkSecurityKey(blePack.sk)) {
          if (calcultateChecksum(blePack) == blePack.crc) {
            if (blePack.payload[0]==REQ_READ_SCANNER_SENSOR){
              Serial.println("Starting send messages.");
              stopped = false;
            } else if (blePack.payload[0]==REQ_STOP_MESSAGES) {
              Serial.println("Stopping send messages.");
              stopped = true;
              stopSendMessage();
            } else if (blePack.payload[0]==REQ_SET_SCANNER_REFRESH_RATE) {
              refreshRate = blePack.payload[1];
              if (refreshRate>0) {
                Serial.print("Set refresh rate to: ");
                Serial.println(refreshRate);
                delayRate = 1000/refreshRate;
                refreshRateSendMessage();
              }
            } else if (blePack.payload[0]==REQ_SET_SCANNER_MAX_DISTANCE) {
              maxDistance = blePack.payload[1] + blePack.payload[2];
              Serial.print("Set max distance to: ");
              Serial.println(maxDistance);
              maxDistanceSendMessage();
            } else {
              Serial.println("Invalid request.");
            }
          } else {
            Serial.println("Invalid CRC.");
          }
        } else {
          Serial.print("Invalid Security Key.");
        }
      }
      
      long currentMillis = millis();
      if (currentMillis - previousMillis >= delayRate && !stopped) {
        previousMillis = currentMillis;
        if (checkSecurityKey(blePack.sk)) {
          updateScannerSendMessage();
        }
      }
    }
    Serial.print("Disconnected from central: ");
    Serial.println(central.address());
    Package blePack2;
    blePack = blePack2;
  }
}

void updateScannerSendMessage() {
  //    0 --->   0 degrees
  // 1024 ---> 270 degrees  (default pot max rotation)
  analogVal = analogRead(A0);
  analogVal *= 0.263671875; // 0 ~ 270

  blePack.iot = IOT;
  blePack.eot = EOT;
  blePack.pl = 8;
  blePack.payload[1] = analogVal < 255 ? analogVal : 255;
  blePack.payload[2] = analogVal > 255 ? analogVal - 255 : 0;
  pingVal = scanner.ping_cm(maxDistance);
  blePack.payload[3] = pingVal < 255 ? pingVal : 255;
  blePack.payload[4] = pingVal > 255 ? pingVal - 255 : 0;
  blePack.payload[5] = refreshRate;
  blePack.payload[6] = maxDistance < 255 ? maxDistance : 255;
  blePack.payload[7] = maxDistance > 255 ? maxDistance - 255 : 0;

  blePack.payload[0] = RESP_READ_SCANNER_SENSOR;
  blePack.crc = calcultateChecksum(blePack);

  packageToByteArray(bleByteArray, blePack);
  bleCharacteristic.setValue(bleByteArray, MAX_PACKAGE_SIZE);
}

void stopSendMessage() {
  blePack.iot = IOT;
  blePack.eot = EOT;
  blePack.pl = 1;
  blePack.payload[0] = RESP_STOP_MESSAGES;
  blePack.crc = calcultateChecksum(blePack);
  packageToByteArray(bleByteArray, blePack);
  bleCharacteristic.setValue(bleByteArray, MAX_PACKAGE_SIZE);
}

void refreshRateSendMessage() {
  blePack.iot = IOT;
  blePack.eot = EOT;
  blePack.pl = 1;
  blePack.payload[0] = RESP_SET_SCANNER_REFRESH_RATE;
  blePack.crc = calcultateChecksum(blePack);
  packageToByteArray(bleByteArray, blePack);
  bleCharacteristic.setValue(bleByteArray, MAX_PACKAGE_SIZE);
}

void maxDistanceSendMessage() {
  blePack.iot = IOT;
  blePack.eot = EOT;
  blePack.pl = 1;
  blePack.payload[0] = RESP_SET_SCANNER_MAX_DISTANCE;
  blePack.crc = calcultateChecksum(blePack);
  packageToByteArray(bleByteArray, blePack);
  bleCharacteristic.setValue(bleByteArray, MAX_PACKAGE_SIZE);
}

void packageToByteArray(byte m[], Package p) {
  memset(m, 0, MAX_PACKAGE_SIZE);
  m[0] = p.iot;
  for(byte i=0;i<8;i++){
    m[1+i] = p.sk[i];
  }
  m[9] = p.pl;
  for(byte i=0;i<p.pl;i++){
    m[10+i] = p.payload[i];
  }
  m[10+p.pl] = p.crc;
  m[11+p.pl] = p.eot;
  m[12+p.pl] = '\0';
}

Package byteArrayToPackage(Package p, const unsigned char m[]) {
  p.iot = m[0];
  for(byte i=0;i<8;i++){
    p.sk[i] = m[1+i];
  }
  p.pl = m[9];
  for(byte i=0;i<p.pl;i++){
    p.payload[i] = m[10+i];
  }
  p.crc = m[10+p.pl];
  p.eot = m[11+p.pl];
  return p;
}

byte calcultateChecksum(Package p) {
  byte result = p.iot;
  for(byte i=0;i<8;i++){
    result ^= p.sk[i];
  }
  result ^= p.pl;
  for(byte i=0;i<p.pl;i++){
    result ^= p.payload[i];
  }
  return result;  
}

bool checkSecurityKey(byte sk[]) {
  byte localSk[8] = SECURITY_KEY;
  for(byte i=0;i<8;i++){
    if(sk[i]!=localSk[i]){
      return false;
    }
  }
  return true;
}

