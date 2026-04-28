#include <SPI.h>
#include <MFRC522.h>

#define SS_PIN       10
#define RST_PIN       9
#define GREEN_LED     6
#define RED_LED       7
#define ACCESS_DELAY  3000

MFRC522 rfid(SS_PIN, RST_PIN);

const byte validCards[][4] = {
  {0xA1, 0xB2, 0xC3, 0xD4},   // Employee A
  {0xE5, 0xF6, 0xA7, 0xB8},   // Employee B
  {0xD9, 0x21, 0xB5, 0x02},   // Your card
};
const int NUM_CARDS = sizeof(validCards) / 4;

void setup() {
  Serial.begin(9600);
  SPI.begin();
  rfid.PCD_Init();
  rfid.PCD_SetAntennaGain(rfid.RxGain_max);

  pinMode(GREEN_LED, OUTPUT);
  pinMode(RED_LED,   OUTPUT);

  standby();
  Serial.println("RFID ready. Scan a card...");
}

void loop() {
  rfid.PCD_Init();

  if (!rfid.PICC_IsNewCardPresent()) return;
  if (!rfid.PICC_ReadCardSerial())   return;

  // Build UID string in D9:21:B5:02 format for Java to parse
  String uid = "";
  for (byte i = 0; i < rfid.uid.size; i++) {
    if (rfid.uid.uidByte[i] < 0x10) uid += "0";
    uid += String(rfid.uid.uidByte[i], HEX);
    if (i < rfid.uid.size - 1) uid += ":";
  }
  uid.toUpperCase();

  if (isAuthorised(rfid.uid.uidByte, rfid.uid.size)) {
    Serial.println("GRANTED:" + uid);  // Java reads this
    grantAccess();
  } else {
    Serial.println("DENIED:" +  uid);          // Java reads this
    denyAccess();
  }

  rfid.PICC_HaltA();
  rfid.PCD_StopCrypto1();
  delay(500);
}

bool isAuthorised(byte *uid, byte len) {
  for (int i = 0; i < NUM_CARDS; i++) {
    bool match = true;
    for (int j = 0; j < 4; j++) {
      if (uid[j] != validCards[i][j]) { match = false; break; }
    }
    if (match) return true;
  }
  return false;
}

void grantAccess() {
  digitalWrite(RED_LED,   LOW);
  digitalWrite(GREEN_LED, HIGH);
  delay(ACCESS_DELAY);
  standby();
}

void denyAccess() {
  for (int i = 0; i < 3; i++) {
    digitalWrite(RED_LED, LOW);  delay(150);
    digitalWrite(RED_LED, HIGH); delay(150);
  }
  standby();
}

void standby() {
  digitalWrite(GREEN_LED, LOW);
  digitalWrite(RED_LED,   HIGH);
}