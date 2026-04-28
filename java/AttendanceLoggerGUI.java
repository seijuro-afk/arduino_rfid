import com.fazecast.jSerialComm.SerialPort;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class AttendanceLoggerGUI extends JFrame {
    
    static final String PORT = "COM3";
    static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private JTable attendanceTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;
    private JLabel countLabel;
    private JTextField csvNameField;
    private SerialPort port;
    private int attendanceCount = 0;
    
    public AttendanceLoggerGUI() {
        setTitle("RFID Attendance Logger");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setResizable(true);
        
        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Top panel with status
        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        statusLabel = new JLabel("Initializing...");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        countLabel = new JLabel("Total: 0");
        countLabel.setFont(new Font("Arial", Font.BOLD, 14));
        topPanel.add(statusLabel, BorderLayout.WEST);
        topPanel.add(countLabel, BorderLayout.EAST);
        
        // Table setup
        String[] columns = {"Time", "UID", "Name", "Status"};
        tableModel = new DefaultTableModel(columns, 0);
        attendanceTable = new JTable(tableModel);
        attendanceTable.setDefaultEditor(Object.class, null); // Read-only table
        attendanceTable.setFont(new Font("Arial", Font.PLAIN, 12));
        attendanceTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        
        // Column widths
        attendanceTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        attendanceTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        attendanceTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        attendanceTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        
        JScrollPane scrollPane = new JScrollPane(attendanceTable);
        
        // Bottom panel with CSV filename, save, and clear actions
        JPanel csvPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        csvPanel.add(new JLabel("CSV File:"));
        csvNameField = new JTextField("attendance.csv", 20);
        csvPanel.add(csvNameField);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton loadButton = new JButton("Load CSV");
        loadButton.addActionListener(e -> loadFromCsv());
        JButton saveButton = new JButton("Save CSV");
        saveButton.addActionListener(e -> saveToCsv(false));
        JButton saveClearButton = new JButton("Save & Clear");
        saveClearButton.addActionListener(e -> saveToCsv(true));
        JButton clearButton = new JButton("Clear Log");
        clearButton.addActionListener(e -> clearLog());
        actionPanel.add(loadButton);
        actionPanel.add(saveButton);
        actionPanel.add(saveClearButton);
        actionPanel.add(clearButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(csvPanel, BorderLayout.WEST);
        bottomPanel.add(actionPanel, BorderLayout.EAST);
        
        // Add components to main panel
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        setVisible(true);
    }
    
    static String getNameByUID(String uid) {
        switch (uid.toUpperCase()) {
            case "D9:21:B5:02": return "Sean Regindin";
            case "A1:B2:C3:D4": return "Employee A";
            case "E5:F6:A7:B8": return "Employee B";
            default:            return "Unknown";
        }
    }
    
    private void addAttendanceRecord(String timestamp, String uid, String name, String status) {
        SwingUtilities.invokeLater(() -> {
            tableModel.insertRow(0, new Object[]{timestamp, uid, name, status});
            attendanceCount++;
            countLabel.setText("Total: " + attendanceCount);
        });
    }
    
    private void saveToCsv(boolean clearAfter) {
        String fileName = getCsvFileName();
        File file = new File(fileName);

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write("Time,UID,Name,Status");
            bw.newLine();

            for (int row = 0; row < tableModel.getRowCount(); row++) {
                bw.write(csvEscape(tableModel.getValueAt(row, 0)));
                bw.write(",");
                bw.write(csvEscape(tableModel.getValueAt(row, 1)));
                bw.write(",");
                bw.write(csvEscape(tableModel.getValueAt(row, 2)));
                bw.write(",");
                bw.write(csvEscape(tableModel.getValueAt(row, 3)));
                bw.newLine();
            }

            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Saved " + file.getName() + " successfully.");
                statusLabel.setForeground(Color.BLUE);
                if (clearAfter) {
                    clearLog();
                    statusLabel.setText("Saved " + file.getName() + " and cleared log.");
                }
            });
        } catch (IOException ex) {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Save failed: " + ex.getMessage());
                statusLabel.setForeground(Color.RED);
            });
        }
    }

    private String getCsvFileName() {
        String fileName = csvNameField.getText().trim();
        if (fileName.isEmpty()) {
            fileName = "attendance.csv";
        }

        if (!fileName.toLowerCase().endsWith(".csv")) {
            fileName += ".csv";
        }

        return fileName;
    }

    private String csvEscape(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            text = text.replace("\"", "\"\"");
            return "\"" + text + "\"";
        }
        return text;
    }

    private void loadFromCsv() {
        String fileName = getCsvFileName();
        File file = new File(fileName);

        if (!file.exists()) {
            statusLabel.setText("File not found: " + file.getName());
            statusLabel.setForeground(Color.RED);
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();
            boolean hasHeader = false;
            if (line != null) {
                String[] header = parseCsvLine(line);
                if (header.length >= 4 && header[0].equalsIgnoreCase("Time") && header[1].equalsIgnoreCase("UID") && header[2].equalsIgnoreCase("Name") && header[3].equalsIgnoreCase("Status")) {
                    hasHeader = true;
                }
            }

            tableModel.setRowCount(0);
            attendanceCount = 0;

            if (!hasHeader && line != null) {
                String[] cells = parseCsvLine(line);
                if (cells.length >= 4) {
                    tableModel.addRow(new Object[]{cells[0], cells[1], cells[2], cells[3]});
                    attendanceCount++;
                }
            }

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cells = parseCsvLine(line);
                if (cells.length >= 4) {
                    tableModel.addRow(new Object[]{cells[0], cells[1], cells[2], cells[3]});
                    attendanceCount++;
                }
            }

            countLabel.setText("Total: " + attendanceCount);
            statusLabel.setText("Loaded " + file.getName() + " successfully.");
            statusLabel.setForeground(Color.BLUE);
        } catch (IOException ex) {
            statusLabel.setText("Load failed: " + ex.getMessage());
            statusLabel.setForeground(Color.RED);
        }
    }

    private String[] parseCsvLine(String line) {
        java.util.List<String> values = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    values.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }

        values.add(current.toString());
        return values.toArray(new String[0]);
    }
    
    private void clearLog() {
        tableModel.setRowCount(0);
        attendanceCount = 0;
        countLabel.setText("Total: 0");
    }
    
    private void startSerialListener() {
        new Thread(() -> {
            port = SerialPort.getCommPort(PORT);
            port.setBaudRate(9600);
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);

            if (!port.openPort()) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Failed to open " + PORT + ". Is Serial Monitor open?");
                    statusLabel.setForeground(Color.RED);
                });
                return;
            }

            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Connected to " + PORT + " — Ready to scan");
                statusLabel.setForeground(Color.GREEN);
            });

            Scanner scanner = new Scanner(port.getInputStream());

            while (scanner.hasNextLine() && port.isOpen()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("RFID ready")) continue;

                String timestamp = LocalDateTime.now().format(fmt);

                if (line.startsWith("GRANTED:")) {
                    String uid  = line.substring(8).toUpperCase();
                    String name = getNameByUID(uid);
                    addAttendanceRecord(timestamp, uid, name, "GRANTED");

                } else if (line.startsWith("DENIED")) {
                    String uid  = line.substring(8).toUpperCase();
                    addAttendanceRecord(timestamp, uid, "Unknown", "DENIED");
                }
            }

            port.closePort();
        }).start();
    }
    
    @Override
    public void dispose() {
        if (port != null && port.isOpen()) {
            port.closePort();
        }
        super.dispose();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AttendanceLoggerGUI gui = new AttendanceLoggerGUI();
            gui.startSerialListener();
        });
    }
}
