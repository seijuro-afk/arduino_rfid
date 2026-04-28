import com.fazecast.jSerialComm.SerialPort;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class AttendanceLogger {

    static final String PORT     = "COM3";
    static final String LOG_FILE = "attendance.csv";
    static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static String getNameByUID(String uid) {
        switch (uid.toUpperCase()) {
            case "D9:21:B5:02": return "Your Name";
            case "A1:B2:C3:D4": return "Employee A";
            case "E5:F6:A7:B8": return "Employee B";
            default:            return "Unknown";
        }
    }

    public static void main(String[] args) throws Exception {
        SerialPort port = SerialPort.getCommPort(PORT);
        port.setBaudRate(9600);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);

        if (!port.openPort()) {
            System.out.println("Failed to open " + PORT + ". Is Serial Monitor still open?");
            return;
        }

        System.out.println("Connected to " + PORT);
        System.out.println("Logging attendance to " + LOG_FILE);

        // Write CSV header only if file doesn't exist yet
        File f = new File(LOG_FILE);
        if (!f.exists()) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
                bw.write("Timestamp,UID,Name,Status");
                bw.newLine();
            }
        }

        System.out.println("Ready — scan a card...\n");

        Scanner scanner = new Scanner(port.getInputStream());

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            // Ignore boot message from Arduino
            if (line.startsWith("RFID ready")) continue;

            String timestamp = LocalDateTime.now().format(fmt);

            if (line.startsWith("GRANTED:")) {
                String uid  = line.substring(8).toUpperCase();
                String name = getNameByUID(uid);

                // Log to CSV
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
                    bw.write(timestamp + "," + uid + "," + name + ",GRANTED");
                    bw.newLine();
                }

                System.out.println("[" + timestamp + "] ACCESS GRANTED");
                System.out.println("  UID  : " + uid);
                System.out.println("  Name : " + name);
                System.out.println();

            } else if (line.startsWith("DENIED")) {
                // Log to CSV
                try (BufferedWriter bw = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
                    bw.write(timestamp + ",UNKNOWN,Unknown,DENIED");
                    bw.newLine();
                }

                System.out.println("[" + timestamp + "] ACCESS DENIED — unknown card");
                System.out.println();
            }
        }

        port.closePort();
    }
}