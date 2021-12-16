/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package examaes;

import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Client {

    // send/receive tối đa 256 bytes 
    private static final int MAXBYTE = 256;

    public static void main(String[] args) throws Exception {

        Scanner scan = new Scanner(System.in);

        int selection = 0;

        while (selection != 3) {

            System.out.println("-------------Xin chào------------.");
            System.out.println("Lựa chọn.");
            System.out.println("1. Gửi File");
            System.out.println("2. Nhận File");
            System.out.println("3. Thoát");
            System.out.print("------------------------------------ \n");

            selection = scan.nextInt();

            switch (selection) {
                case 1: {
                    System.out.print("Nhập port: ");

                    // Nhập port của bên gửi
                    int port = scan.nextInt();

                    System.out.print("Nhập đường dẫn file (C:/file.txt): ");

                    // Nhập file cần gửi
                    String filePath = scan.next();

                    scan.nextLine();
                    System.out.println("Chon key: 128 || 192 || 256 ");
                    int keybit = scan.nextInt();
                    String key = "";
                    boolean check = true;
                    while (check) {
                        switch (keybit) {
                            case 128:
                                System.out.println("Enter key 128:");
                                key = scan.next();
                                
                                if (key.length() == 16) {
                                    check = false;
                                } else {
                                    System.out.println("Nhập đủ 16 kí tự !  :");

                                    check = true;
                                }
                                break;
                            case 192:
                                System.out.println("Enter key 192");
                                key = scan.next();
                                if (key.length() == 24) {
                                    check = false;
                                } else {
                                    System.out.println("Nhập đủ 24 kí tự !  :");

                                    check = true;
                                }
                                break;
                            case 256:
                                System.out.println("Enter key 256");
                                key = scan.next();
                                if (key.length() == 32) {
                                    check = false;
                                } else {
                                    System.out.println("Nhập đủ 32 kí tự !  :");

                                    check = true;
                                }
                                break;
                        }
                    }

                    // Khởi tạo server socket
                    ServerSocket serverSocket = new ServerSocket(port);

                    // Khởi tạo logger cho tiến trình
                    Logger threadLogger = Logger.getLogger("serverLogger");
                    System.out.println("Server đang đọc file !");
                    // Đọc file cần gửi 
                    String fileContent = readFile(filePath);

                    if (!fileContent.equalsIgnoreCase("")) {
                        // Lấy file name
                        String fileName = getFileName(filePath);

                        System.out.println("Server đã khởi tạo thành công với port " + port + ". Có thể nhận file từ máy khác");

                        while (true) {
                            Socket clientSocket = serverSocket.accept();

                            // Khởi tạo 1 tiến trình cho client mới
                            Thread thread = new Thread(new Server(clientSocket, threadLogger, fileName, fileContent, key));
                            thread.start();
                            threadLogger.info("Created and started new thread " + thread.getName() + " for client.");
                        }
                    } else {
                        System.err.println("Không tìm thấy file.");
                    }

                    break;
                }

                case 2:
                    //Nhập server 
                    System.out.println("Nhập tên server để nhận : localhost.");
                    String server = scan.next();

                    //Nhập port của server
                    System.out.println("Nhập port của server.");
                    int servPort = scan.nextInt();

                    Socket clientSocket = new Socket(server, servPort);

                    DataInputStream fromServer = new DataInputStream(clientSocket.getInputStream());
                    DataOutputStream toServer = new DataOutputStream(clientSocket.getOutputStream());

                    String key = fromServer.readUTF();
                    System.out.println(key);

                    //
                    System.out.println("Tên tệp :");

                    try {
                        // Lấy đường dẫn file gửi từ server
                        String fName = fromServer.readUTF();

                        // Đọc file đã mã hóa từ server
                        String encryptedFile = "";
                        String line;
                        while (!(line = fromServer.readUTF()).equalsIgnoreCase("")) {

                            encryptedFile += line;

                            if (line.isEmpty()) {
                                break;
                            }
                        }
                        //Lấy tên file
                        String fileLoc = fName.split(Pattern.quote(File.separator))[fName.split(Pattern.quote(File.separator)).length - 1];
                        // Giải mã file
                        System.out.println(fileLoc);
                        System.out.println("Nội dung : ");

                        String decryptedFile = decryptFile(encryptedFile, key);
                        System.out.println(decryptedFile);

                        // Ghi đoạn văn đã giải mã vào file
                        writeFile(fileLoc, decryptedFile);

                        // Thông báo
                        System.out.println("File được download thành công . Saved in .D:/" + fileLoc + "\n");

                    } catch (Exception e) {
                        System.err.println("Lỗi khởi tạo server: " + e);
                    }

                    break;
                case 3:
                    System.out.println("Thoát");
                    break;
                default:
                    System.out.println("Lựa chọn 1,2 hoặc 3");
                    break;
            }

        }

    }

    // Giải mã đoạn văn bản với secret key
    public static String decryptFile(String encryptedText, String secretKey) {
        return new String(new AES(secretKey.getBytes()).ECB_decrypt(Base64.getDecoder().decode(encryptedText)));
    }

    // Khởi tạo SecretKey từ các giá trị đã cho random
    private static Key generateKey(byte[] sharedKey) {
        // Mã hóa AES 128 bit
        byte[] byteKey = new byte[16];
        for (int i = 0; i < 16; i++) {
            byteKey[i] = sharedKey[i];
        }

        // chuyển sang định dạng AES
        try {
            Key key = new SecretKeySpec(byteKey, "AES");

            return key;
        } catch (Exception e) {
            System.err.println("Error while generating key: " + e);
        }

        return null;
    }

    // Đọc file
    public static String readFile(String fileName) {
        InputStream inStream = null;
        String fileContent = "";
        try {
            inStream = new FileInputStream(fileName);

            int fileSize = inStream.available();
            for (int i = 0; i < fileSize; i++) {
                fileContent += (char) inStream.read();
            }
        } catch (Exception e) {
            System.err.println("File not found: " + fileName);
        } finally {
            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch (Exception ex) {
                System.err.println("Error while closing File I/O: " + ex);
            }
        }
        return fileContent;
    }

    // Ghi file
    public static void writeFile(String fileName, String fileContent) {
        OutputStream outStream = null;
        try {
            outStream = new FileOutputStream(fileName);

            byte[] fileContentBytes = fileContent.getBytes();

            outStream.write(fileContentBytes);

        } catch (Exception e) {
            System.err.println("Error while writing into file " + fileName + ": " + e);
        } finally {
            try {
                if (outStream != null) {
                    outStream.close();
                }
            } catch (Exception ex) {
                System.err.println("Error while closing File I/O: " + ex);
            }
        }
    }

    // Hàm lấy tên file từ path
    public static String getFileName(String filePath) {

        String[] split = filePath.split("\\/");

        return split[split.length - 1];
    }

}
