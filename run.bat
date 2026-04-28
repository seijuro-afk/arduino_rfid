@echo off
cd /d %~dp0
javac -cp .;lib/jSerialComm-2.11.4.jar java/AttendanceLoggerGUI.java -d .
java --enable-native-access=ALL-UNNAMED -cp .;lib/jSerialComm-2.11.4.jar AttendanceLoggerGUI
pause
