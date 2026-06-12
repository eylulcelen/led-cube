#include <SoftwareSerial.h>
#include <DHT.h>

// HC-05
SoftwareSerial bt(10, 11);

// DHT11 on pin 7
#define DHT_PIN  7
#define DHT_TYPE DHT11
DHT dht(DHT_PIN, DHT_TYPE);

// LDR on A0
#define LDR_PIN A0

// --- Protocol constants (same as before) ---
const byte START_MARKER   = 0xAB;
const byte END_MARKER     = 0xCD;
const byte CMD_DRAW_FRAME = 0x01;
const int  RGB_DATA_SIZE  = 125 * 3;

byte leds[125][3];

unsigned long lastSensorSend = 0;
const unsigned long SENSOR_INTERVAL = 1000; // ms

void setup() {
    bt.begin(9600);
    Serial.begin(9600);
    dht.begin();
}

void loop() {
    // Receive frame if available
    if (bt.available() && bt.read() == START_MARKER) {
    handlePacket();
}

    // Send sensor data every second
    unsigned long now = millis();
    if (now - lastSensorSend >= SENSOR_INTERVAL) {
    lastSensorSend = now;
    sendSensorData();
}
}

void sendSensorData() {
    int   ldr  = analogRead(LDR_PIN);
    float temp = dht.readTemperature();
    float hum  = dht.readHumidity();

    if (isnan(temp) || isnan(hum)) {
    // DHT read failed — send LDR only
    bt.print("LDR:");
    bt.print(ldr);
    bt.print("\n");
    return;
}

    bt.print("LDR:");
    bt.print(ldr);
    bt.print(",TEMP:");
    bt.print(temp, 1);
    bt.print(",HUM:");
    bt.print(hum, 1);
    bt.print("\n");
}

void handlePacket() {
    byte cmd = readByte();
    if (cmd == CMD_DRAW_FRAME) receiveFrame();
}

void receiveFrame() {
    byte buffer[RGB_DATA_SIZE];
    for (int i = 0; i < RGB_DATA_SIZE; i++) buffer[i] = readByte();

    byte received_checksum = readByte();
    byte computed_checksum = 0;
    for (int i = 0; i < RGB_DATA_SIZE; i++) computed_checksum ^= buffer[i];

    byte end = readByte();
    if (computed_checksum != received_checksum || end != END_MARKER) {
    Serial.println("BAD PACKET");
    return;
}

    for (int i = 0; i < 125; i++) {
    leds[i][0] = buffer[i * 3];
    leds[i][1] = buffer[i * 3 + 1];
    leds[i][2] = buffer[i * 3 + 2];
}
    drawCube();
}

void drawCube() {
    // TODO: your LED driving code here
    Serial.println("Frame OK");
}

byte readByte() {
    unsigned long start = millis();
    while (!bt.available()) {
    if (millis() - start > 1000) return 0xFF;
}
    return bt.read();
}