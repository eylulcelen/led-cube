// TLC5940 uses these pins (fixed by library):
// Pin 13 → SCLK
// Pin 11 → SIN (MOSI)
// Pin 10 → XLAT
// Pin 9  → BLANK
// Pin 3  → GSCLK

// Layer MOSFET gate pins (you can change these)
#define LAYER_0_PIN 2
#define LAYER_1_PIN 4
#define LAYER_2_PIN 5
#define LAYER_3_PIN 6
#define LAYER_4_PIN 7

// DHT11
#define DHT_PIN  8
#define DHT_TYPE DHT11

// LDR
#define LDR_PIN A0

// HC-05 Bluetooth (SoftwareSerial)
// RX=A1, TX=A2 — moved away from 10/11 since TLC5940 uses those
#define BT_RX_PIN A1
#define BT_TX_PIN A2

#include <Tlc5940.h>
#include <SoftwareSerial.h>
#include <DHT.h>

// ----------------------------------------------------------------
// Pin definitions
// ----------------------------------------------------------------
#define LAYER_0_PIN 2
#define LAYER_1_PIN 4
#define LAYER_2_PIN 5
#define LAYER_3_PIN 6
#define LAYER_4_PIN 7

#define DHT_PIN     8
#define DHT_TYPE    DHT11

#define LDR_PIN     A0
#define BT_RX_PIN   A1
#define BT_TX_PIN   A2

// ----------------------------------------------------------------
// Protocol constants
// ----------------------------------------------------------------
const byte START_MARKER   = 0xAB;
const byte END_MARKER     = 0xCD;
const byte CMD_DRAW_FRAME = 0x01;
const int  RGB_DATA_SIZE  = 375; // 125 LEDs × 3

// ----------------------------------------------------------------
// Objects
// ----------------------------------------------------------------
SoftwareSerial bt(BT_RX_PIN, BT_TX_PIN);
DHT            dht(DHT_PIN, DHT_TYPE);

// ----------------------------------------------------------------
// LED data buffer
// leds[z][ledIndex][channel] where:
//   z         = layer 0-4
//   ledIndex  = 0-24 (y*5 + x)
//   channel   = 0=R, 1=G, 2=B
// ----------------------------------------------------------------
uint8_t leds[5][25][3];

// Layer MOSFET pins in order
const uint8_t LAYER_PINS[5] = {
LAYER_0_PIN, LAYER_1_PIN, LAYER_2_PIN,
LAYER_3_PIN, LAYER_4_PIN
};

// ----------------------------------------------------------------
// Multiplexing state
// ----------------------------------------------------------------
volatile uint8_t currentLayer = 0;

// ----------------------------------------------------------------
// Sensor timing
// ----------------------------------------------------------------
unsigned long lastSensorSend = 0;
const unsigned long SENSOR_INTERVAL = 1000;

// ----------------------------------------------------------------
// Setup
// ----------------------------------------------------------------
void setup() {
    Serial.begin(9600);
    bt.begin(9600);
    dht.begin();

    // Layer MOSFET pins — all off initially
    for (int i = 0; i < 5; i++) {
    pinMode(LAYER_PINS[i], OUTPUT);
    digitalWrite(LAYER_PINS[i], LOW);
}

    // Init TLC5940 — all channels off
    Tlc.init(0);

    // Clear LED buffer
    clearLeds();

    Serial.println("CubeApp firmware ready");
}

// ----------------------------------------------------------------
// Main loop
// ----------------------------------------------------------------
void loop() {
    // 1. Handle incoming Bluetooth packets
    if (bt.available()) {
    byte b = bt.read();
    if (b == START_MARKER) {
    handlePacket();
}
}

    // 2. Multiplex one layer per loop iteration
    multiplexLayer();

    // 3. Send sensor data every second
    unsigned long now = millis();
    if (now - lastSensorSend >= SENSOR_INTERVAL) {
    lastSensorSend = now;
    sendSensorData();
}
}

// ----------------------------------------------------------------
// Packet handling
// ----------------------------------------------------------------
void handlePacket() {
    byte cmd = readByte();
    if (cmd == CMD_DRAW_FRAME) {
    receiveFrame();
}
    // Future commands can go here
}

void receiveFrame() {
    byte buffer[RGB_DATA_SIZE];

    // Read 375 bytes
    for (int i = 0; i < RGB_DATA_SIZE; i++) {
    buffer[i] = readByte();
}

    // Read and verify checksum
    byte received_checksum = readByte();
    byte computed_checksum = 0;
    for (int i = 0; i < RGB_DATA_SIZE; i++) {
    computed_checksum ^= buffer[i];
}

    // Read end marker
    byte endMarker = readByte();

    if (computed_checksum != received_checksum || endMarker != END_MARKER) {
    Serial.println("BAD PACKET — discarding");
    return;
}

    // Unpack buffer into leds[z][ledIndex][rgb]
    // Java sends in order: x=0→4, y=0→4, z=0→4
    // So buffer index = (z*25 + y*5 + x) * 3
    for (int z = 0; z < 5; z++) {
    for (int y = 0; y < 5; y++) {
    for (int x = 0; x < 5; x++) {
    int ledIndex    = y * 5 + x;        // position within layer
    int bufferIndex = (z * 25 + y * 5 + x) * 3;
    leds[z][ledIndex][0] = buffer[bufferIndex];     // R
    leds[z][ledIndex][1] = buffer[bufferIndex + 1]; // G
    leds[z][ledIndex][2] = buffer[bufferIndex + 2]; // B
}
}
}

    Serial.println("Frame OK");
}

// ----------------------------------------------------------------
// Multiplexing
// ----------------------------------------------------------------

/**
 * Called every loop iteration.
 * Turns off the current layer, loads the next layer's data
 * into TLC5940, latches it, then turns the layer on.
 * Cycles through layers 0→4→0→...
 */
void multiplexLayer() {
    // Turn off current layer
    digitalWrite(LAYER_PINS[currentLayer], LOW);

    // Advance to next layer
    currentLayer = (currentLayer + 1) % 5;

    // Load this layer's RGB data into TLC5940
    loadLayerToTlc(currentLayer);

    // Latch data and turn layer on
    Tlc.update();
    digitalWrite(LAYER_PINS[currentLayer], HIGH);
}

/**
 * Maps LED buffer data for one layer into TLC5940 channels.
 *
 * TLC5940 channel assignment for 25 RGB LEDs (75 channels):
 * LED 0 (x=0,y=0): channels 0,1,2  (R,G,B)
 * LED 1 (x=1,y=0): channels 3,4,5
 * LED 2 (x=2,y=0): channels 6,7,8
 * ...
 * LED 24 (x=4,y=4): channels 72,73,74
 *
 * TLC5940 uses 12-bit values (0-4095).
 * We scale 8-bit Arduino values (0-255) → 12-bit (0-4095).
 * Since TLC5940 is constant current sink (cathode side),
 * higher value = brighter (NOT inverted).
 */
void loadLayerToTlc(uint8_t z) {
    for (int ledIndex = 0; ledIndex < 25; ledIndex++) {
    int baseChannel = ledIndex * 3;

    // Scale 0-255 → 0-4095
    uint16_t r = (uint16_t)leds[z][ledIndex][0] * 16;
    uint16_t g = (uint16_t)leds[z][ledIndex][1] * 16;
    uint16_t b = (uint16_t)leds[z][ledIndex][2] * 16;

    Tlc.set(baseChannel,     r);  // Red
    Tlc.set(baseChannel + 1, g);  // Green
    Tlc.set(baseChannel + 2, b);  // Blue
}
}

// ----------------------------------------------------------------
// Sensor data
// ----------------------------------------------------------------
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

// ----------------------------------------------------------------
// Helpers
// ----------------------------------------------------------------

/** Blocking single-byte read with 1 second timeout */
byte readByte() {
    unsigned long start = millis();
    while (!bt.available()) {
    if (millis() - start > 1000) {
    Serial.println("TIMEOUT");
    return 0xFF;
}
    // Keep multiplexing while waiting so display doesn't freeze
    multiplexLayer();
}
    return bt.read();
}

/** Clear all LED data to off */
void clearLeds() {
    for (int z = 0; z < 5; z++)
    for (int i = 0; i < 25; i++)
    leds[z][i][0] = leds[z][i][1] = leds[z][i][2] = 0;
}