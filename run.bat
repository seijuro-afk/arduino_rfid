@echo off
cd /d C:\Users\sregi\Documents\attendance_rfid
javac -cp .;lib/jSerialComm-2.11.4.jar java/AttendanceLoggerGUI.java -d .
java --enable-native-access=ALL-UNNAMED -cp .;lib/jSerialComm-2.11.4.jar AttendanceLoggerGUI
pause