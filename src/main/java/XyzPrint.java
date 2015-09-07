import gnu.io.NRSerialPort;

import javax.swing.*;
import java.io.*;
import java.util.Arrays;

public class XyzPrint {
    private static final String MACHINE_INFO = "XYZ_@3D:";
//    private static final String SEND_FIRMWARE = MACHINE_INFO + '3';
    private static final String SEND_FILE = MACHINE_INFO + '4';
    private static final String MACHINE_LIFE = MACHINE_INFO + '5';
    private static final String EXTRUDER1_INFO = MACHINE_INFO + '6';
//    private static final String EXTRUDER2_INFO = MACHINE_INFO + '7';
    private static final String STATUS_INFO = MACHINE_INFO + '8';

//todo - convert to a dialog/frame which shows and allows change of file using JFileChooser
//todo - shows and allow change of port
//todo - shows printer status and print button
//todo - show job transfer progress with cancel
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            JOptionPane.showMessageDialog(null, "You need to pass a gcode file");
        } else if (new File(args[0]).isFile()) {
            int userChoice = JOptionPane.YES_OPTION;
            while (userChoice != JOptionPane.CANCEL_OPTION) {
                PrinterStatus printerStatus = isPrinterOn("/dev/ttyACM0");
                if (printerStatus.isConnected()) {
                    String printerInfo = "Printer Model: " + Arrays.toString(printerStatus.getModel()) +
                            "\nPrinter Life: " + Arrays.toString(printerStatus.getLife()) +
                            "\nExtruder Info: " + printerStatus.getExtruderInfo() +
                            "\nPrinter status: " + Arrays.toString(printerStatus.getStatusInfo());
                    userChoice = JOptionPane.showConfirmDialog(null, printerInfo, "Do you want to print " + args[0], JOptionPane.OK_CANCEL_OPTION);
                    if (userChoice == JOptionPane.YES_OPTION) {
                        NRSerialPort serial = new NRSerialPort("/dev/ttyACM0", 115200);
                        if (serial.connect()) {
                            try {
                                print(serial, args[0]);
                            } finally {
                                serial.disconnect();
                            }
                            break;
                        }
                    }
                } else {
                    userChoice = JOptionPane.showConfirmDialog(null, "Printer status: Off", "Do you want to check printer?", JOptionPane.OK_CANCEL_OPTION);
                }
            }
        } else {
            JOptionPane.showMessageDialog(null, "File does not exist");
        }
    }

    private static void print(NRSerialPort serial, String filename) throws IOException, InterruptedException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(serial.getInputStream()));
        OutputStream outputStream = serial.getOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);
        File gcodeFile = new File(filename);
        FileReader fileReader = new FileReader(gcodeFile);
        long fileLength = gcodeFile.length();
        if ("OFFLINE_OK".equals(sendNReceive(reader,writer,SEND_FILE, 1)[0])) {
            if ("OFFLINE_OK".equals(sendNReceive(reader,writer, "M1:" + "10mm.gcode" + "," + fileLength + ",1.3.49,EE1_OK,EE2_OK", 1)[0])) {
                int fileOffset = 0;
                char[] charBuffer = new char[10236];
                byte[] byteBuffer = new byte[10240];
                while(true) {
                    int charsRead = fileReader.read(charBuffer, 0, 10236);
                    addCheckSumInByteBuffer(charBuffer, byteBuffer, charsRead);
                    outputStream.write(byteBuffer, 0, charsRead + 4);
                    outputStream.flush();

                    String[] response = readLines(reader, 1);
                    log("Response: " + Arrays.toString(response));
                    if (charsRead != 10236) {
                        break;
                    }
                    fileOffset = fileOffset + 10236;
                }
            } else {
                log("Invalid response on M1 command");
            }
        } else {
            log("Printer is not ready");
        }
        fileReader.close();
    }

    private static void addCheckSumInByteBuffer(char[] charBuffer, byte[] byteBuffer, int chars) {
        long checkSum = 0;
        for (int i = 0; i < chars; i++) {
            byteBuffer[i] = (byte) charBuffer[i];
            checkSum += byteBuffer[i];
        }

        byteBuffer[chars] = (byte) (checkSum >> 24);
        byteBuffer[chars + 1] = (byte) (checkSum >> 16);
        byteBuffer[chars + 2] = (byte) (checkSum >> 8);
        byteBuffer[chars + 3] = (byte) (checkSum & 0xff);
        log("Checksum for chunk is: " + checkSum);
        log("Total byte array size = " + (chars + 4));
//        log("RAW-Sending: " + new String(byteBuffer, 0, chars + 4));
    }

    private static PrinterStatus isPrinterOn(String serialPort) throws IOException, InterruptedException {
        NRSerialPort serial = new NRSerialPort(serialPort, 115200);
        PrinterStatus printerStatus = new PrinterStatus();

        if (serial.connect()) {
            printerStatus.setConnected(true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(serial.getInputStream()));
            PrintWriter writer = new PrintWriter(serial.getOutputStream());

            printerStatus.setModel(sendNReceive(reader, writer, MACHINE_INFO, 4));//model, serial number, etc...
            printerStatus.setLife(sendNReceive(reader, writer, MACHINE_LIFE, 3));//two numbers which indicate some sort of life
            printerStatus.setExtruderInfo(sendNReceive(reader, writer, EXTRUDER1_INFO, 1));//unknown, unknown, unknown, unknown, Filament_Cart_Length, Filament_Remaining_Length, Extruder_Temp, Bed_Temp,unknown, unknown,unknown, unknown
            printerStatus.setStatusInfo(sendNReceive(reader, writer, STATUS_INFO, 7));
            serial.disconnect();
        } else {
            log("Unable to connect to " + serialPort);
        }
        return printerStatus;
    }

    private static void log(String message) {
        System.out.println(message);
    }

    private static String[] sendNReceive(BufferedReader reader, PrintWriter writer, String command, int responseLines) throws IOException, InterruptedException {
        writer.print(command);
        writer.flush();
        return readLines(reader, responseLines);
    }

    private static String[] readLines(BufferedReader reader, int responseLines) throws InterruptedException, IOException {
        Thread.sleep(1000);
        String[] responses = new String[responseLines];
        for (int i = 0; i < responseLines; i++) {
            responses[i] = reader.readLine();
        }
        return responses;
    }

    public static class PrinterStatus {

        private boolean connected;
        private String[] model;
        private String[] life;
        private String extruderInfo;
        private String[] statusInfo;

        public void setConnected(boolean connected) {
            this.connected = connected;
        }

        public void setModel(String[] model) {
            this.model = model;
        }

        public void setLife(String[] life) {
            this.life = life;
        }

        public void setExtruderInfo(String[] extruderInfo) {
            this.extruderInfo = extruderInfo[0];
        }

        public void setStatusInfo(String[] statusInfo) {
            this.statusInfo = statusInfo;
        }

        public boolean isConnected() {
            return connected;
        }

        public String[] getModel() {
            return model;
        }

        public String[] getLife() {
            return life;
        }

        public String getExtruderInfo() {
            return extruderInfo;
        }

        public String[] getStatusInfo() {
            return statusInfo;
        }
    }
}


