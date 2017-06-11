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

#define TRIGGER_PIN      12
#define ECHO_PIN         11
#define MAX_DISTANCE     200
#define MAX_PACKAGE_SIZE 45
#define SECURITY_KEY     {0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07}

NewPing scanner(TRIGGER_PIN, ECHO_PIN, MAX_DISTANCE);

BLEPeripheral blePeripheral;
BLEService bleService("19B10010-E8F2-537E-4F6C-D104768A1214");
BLECharacteristic bleCharacteristic("19B10011-E8F2-537E-4F6C-D104768A1214", BLERead | BLEWrite, MAX_PACKAGE_SIZE);

typedef struct PackageTemplate {
  byte iot; // init of transmission 
  byte sk[8] = SECURITY_KEY; // security key
  byte pl;  // payload length
  byte payload[32];
  byte crc; //crc
  byte eot;  // end of transmission
} Package;

Package blePack;

enum {
  NOP,
  REQ_READ_SCANNER_SENSOR,
  RESP_READ_SCANNER_SENSOR,
  SET_SCANNER_REFRESH_RATE,
  SET_SCANNER_MAX_DISTANCE
};

void setup() {
  Serial.begin(9600);

  blePeripheral.setLocalName("ARDUINO 101");
  blePeripheral.setAdvertisedServiceUuid(bleService.uuid());

  blePeripheral.addAttribute(bleService);
  blePeripheral.addAttribute(bleCharacteristic);

  blePeripheral.begin();
  while(!Serial);
  Serial.write("start");
}

int analogVal = 0;
byte bleByteArray[MAX_PACKAGE_SIZE];
int refreshRate = 5; // samples per second
int delayRate = 1000/refreshRate;

void loop() {

  delay(delayRate);

  blePeripheral.poll();

  if (bleCharacteristic.written()) {
    Serial.println("Package received.");
    blePack = byteArrayToPackage(blePack, bleCharacteristic.value());
    if (checkSecurityKey(blePack.sk)) {
      Serial.print(calcultateChecksum(blePack));
      Serial.print(blePack.crc);
      if (calcultateChecksum(blePack) == blePack.crc) {
        if (blePack.payload[0]==SET_SCANNER_REFRESH_RATE) {
          refreshRate = blePack.payload[1];
          if (refreshRate>0) {
            Serial.print("Set refresh rate to: ");
            Serial.println(refreshRate);
            delayRate = 1000/refreshRate;
          }
        }
        //    0 --->   0 degrees
        // 1024 ---> 270 degrees  (default pot max rotation)
        analogVal = analogRead(A0);
        analogVal *= 0.263671875; // 0 ~ 270

        blePack.iot = 2;
        blePack.eot = 4;
        blePack.pl = 4;    
        blePack.payload[1] = analogVal;
        blePack.payload[2] = scanner.ping_cm();
        blePack.payload[3] = refreshRate;

        blePack.payload[0] = RESP_READ_SCANNER_SENSOR;
        blePack.crc = calcultateChecksum(blePack);

        packageToByteArray(bleByteArray, blePack);
        bleCharacteristic.setValue(bleByteArray, MAX_PACKAGE_SIZE);
      }else{
        Serial.println("Invalid CRC.");
      }
    } else {
      Serial.print("Invalid Security Key.");
    }

  }

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

