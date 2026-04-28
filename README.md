# RFID Attendance Logger

An Arduino-based RFID attendance system that scans cards and logs attendance records to a CSV file with a Java GUI.

## Hardware Required

- Arduino Uno
- RFID-RC522 module
- Green LED + 220Ω resistor
- Red LED + 220Ω resistor
- Jumper wires and breadboard

## Wiring

| RC522 Pin | Arduino Pin |
|-----------|-------------|
| VCC       | 3.3V        |
| GND       | GND         |
| SDA (SS)  | Pin 10      |
| SCK       | Pin 13      |
| MOSI      | Pin 11      |
| MISO      | Pin 12      |
| RST       | Pin 9       |
| IRQ       | Not connected |

| Component  | Arduino Pin       |
|------------|-------------------|
| Green LED  | Pin 6 + 1KΩ      |
| Red LED    | Pin 7 + 1KΩ      |

## Arduino Setup

1. Open Arduino IDE
2. Install the **MFRC522** library via Tools → Manage Libraries
3. Open `arduino/rfid_access.ino`
4. Select board: Tools → Board → Arduino Uno
5. Select port: Tools → Port → COM3 (or your port)
6. Upload the sketch

### Adding valid cards

1. Upload the sketch and open Serial Monitor at 9600 baud
2. Scan your card — the UID will print e.g. `D9 21 B5 02`
3. Add it to the `validCards` array in the sketch:
```cpp
   const byte validCards[][4] = {
     {0xD9, 0x21, 0xB5, 0x02},   // Your Name
   };
```
4. Re-upload the sketch

## Java Setup

**Requirements:**
- Java JDK 17 or higher
- jSerialComm-2.11.4.jar (included in `/lib`)

**Compile:**
```bash
javac -cp .;lib/jSerialComm-2.11.4.jar java/AttendanceLoggerGUI.java
```

**Run:**
```bash
java --enable-native-access=ALL-UNNAMED -cp .;lib/jSerialComm-2.11.4.jar AttendanceLoggerGUI
```

Or just double-click `run.bat`.

## Usage

1. Upload the Arduino sketch
2. Close Arduino IDE Serial Monitor
3. Run the Java GUI
4. Scan a card — the attendance record appears in the table
5. Click **Save CSV** to export the log

## Registering New Cards

Edit the `getNameByUID()` method in `AttendanceLoggerGUI.java`:
```java
static String getNameByUID(String uid) {
    switch (uid.toUpperCase()) {
        case "D9:21:B5:02": return "Your Name";
        case "A1:B2:C3:D4": return "Employee A";
        default:            return "Unknown";
    }
}
```

## CSV Output Format

Time,UID,Name,Status
2024-01-15 08:32:11,D9:21:B5:02,Your Name,GRANTED
2024-01-15 08:45:03,UNKNOWN,Unknown,DENIED

## How It Works

1. RC522 reads the card UID and sends it to Arduino
2. Arduino checks UID against the whitelist
3. Green LED lights up for 3 seconds if valid, red flashes if denied
4. Arduino sends `GRANTED:UID` or `DENIED` over Serial (USB)
5. Java reads the Serial port and logs the record with a timestamp

The run.bat file to include:
@echo off
java --enable-native-access=ALL-UNNAMED -cp .;lib/jSerialComm-2.11.4.jar AttendanceLoggerGUI
pause

Make sure to change the COM port from COM3 to whatever port your Arduino is on before sharing — or add a note in the README that others need to update it in both the Arduino sketch and the Java file.
