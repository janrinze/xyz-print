import gnu.io.NRSerialPort;

import javax.swing.*;
import java.io.*;

public class XyzPrint {
    private static final String MACHINE_INFO = "XYZ_@3D:";
    private static final String SEND_FIRMWARE = MACHINE_INFO + '3';
    private static final String SEND_FILE = MACHINE_INFO + '4';
    private static final String MACHINE_LIFE = MACHINE_INFO + '5';
    private static final String EXTRUDER1_INFO = MACHINE_INFO + '6';
    private static final String EXTRUDER2_INFO = MACHINE_INFO + '7';
    private static final String STATUS_INFO = MACHINE_INFO + '8';

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
//            JFileChooser
            JOptionPane.showMessageDialog(null, "You need to pass a gcode file");
        } else {
            int userChoice = JOptionPane.YES_OPTION;
            while (userChoice != JOptionPane.CANCEL_OPTION) {
                PrinterStatus printerStatus = isPrinterOn();
                if (printerStatus.isConnected()) {
                    userChoice = JOptionPane.showConfirmDialog(null, "Printer status: ", "Do you want to print " + args[0], JOptionPane.OK_CANCEL_OPTION);
                    if (userChoice == JOptionPane.YES_OPTION) {
                        print(args[0]);
                    }
                } else {
                    userChoice = JOptionPane.showConfirmDialog(null, "Printer status: Off", "Do you want to check printer?", JOptionPane.OK_CANCEL_OPTION);
                }
            }
        }
    }

    private static void print(String filename) throws IOException {
        NRSerialPort serial = new NRSerialPort("/dev/ttyACM0", 115200);

        if (serial.connect()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(serial.getInputStream()));
            PrintWriter writer = new PrintWriter(serial.getOutputStream());
            if ("OFFLINE_OK".equals(sendNReceive(reader,writer,SEND_FILE))) {
//                sendGCodeHeader("MyTest",fileLen)
//                readFile()
            }
        }
    }

    private static PrinterStatus isPrinterOn() throws IOException {
        NRSerialPort serial = new NRSerialPort("/dev/ttyACM0", 115200);
        PrinterStatus printerStatus = new PrinterStatus();

        if (serial.connect()) {
            printerStatus.setConnected(true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(serial.getInputStream()));
            PrintWriter writer = new PrintWriter(serial.getOutputStream());

            printerStatus.setModel(sendNReceive(reader, writer, MACHINE_INFO));//model, serial number, etc...
            printerStatus.setLife(sendNReceive(reader, writer, MACHINE_LIFE));//two numbers which indicate some sort of life
            printerStatus.setExtruderInfo(sendNReceive(reader, writer, EXTRUDER1_INFO));//unknown, unknown, unknown, unknown, Filament_Cart_Length, Filament_Remaining_Length, Extruder_Temp, Bed_Temp,unknown, unknown,unknown, unknown
            printerStatus.setStatusInfo(sendNReceive(reader, writer, STATUS_INFO));
            serial.disconnect();
        }
        return printerStatus;
    }

    private static String sendNReceive(BufferedReader reader, PrintWriter writer, String machineInfo) throws IOException {
        writer.println(machineInfo);
        writer.flush();
        return reader.readLine();
    }

    public static class PrinterStatus {

        private boolean connected;
        private String model;
        private String life;
        private String extruderInfo;
        private String statusInfo;

        public void setConnected(boolean connected) {
            this.connected = connected;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public void setLife(String life) {
            this.life = life;
        }

        public void setExtruderInfo(String extruderInfo) {
            this.extruderInfo = extruderInfo;
        }

        public void setStatusInfo(String statusInfo) {
            this.statusInfo = statusInfo;
        }

        public boolean isConnected() {
            return connected;
        }

        public String getModel() {
            return model;
        }

        public String getLife() {
            return life;
        }

        public String getExtruderInfo() {
            return extruderInfo;
        }

        public String getStatusInfo() {
            return statusInfo;
        }
    }
}


