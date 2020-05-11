package receiver;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {

    public static int PORT = 4000;
    public static byte[] DOWNLOAD = {0x01};
    public static byte[] UPLOAD = {0x02};
    //public static String ADDRESS = "192.168.0.111";

    public static void main(String[] args) {

        if (args.length < 1){
            System.out.println("Not enough arguments");
            return;
        }

        DatagramSocket socket;
        InetAddress address;
        FileInputStream fileReader;
        try{
            socket = new DatagramSocket();
            socket.setSoTimeout(100);
            address = InetAddress.getByName(args[0]);
        } catch (UnknownHostException e) {
            System.out.println("Error Unknown host");
            return;
        } catch (IOException e){
            System.out.println("Error making socket");
            return;
        }

        Downloader downloader;
        Uploader uploader;
        switch (args.length) {
            case 1:
                downloader = new Downloader(socket, address, PORT);
                System.out.println("Opening connection");
                if (!downloader.openConnection(DOWNLOAD)) {
                    downloader.terminateConnection();
                    break;
                }
                System.out.println("Connection Opened");
                System.out.println("Downloading");
                if (!downloader.downloadFile()) {
                    downloader.terminateConnection();
                    break;
                }
                System.out.println("Download finished");
                System.out.println("Closing connection");
                if (!downloader.closeConnection()) {
                    downloader.terminateConnection();
                    break;
                }
                System.out.println("Saving file");
                downloader.saveFile();
                break;
            case 2:
                try{
                    fileReader = new FileInputStream(args[1]);
                } catch (FileNotFoundException e){
                    System.out.println("File not found");
                    return;
                }

                uploader = new Uploader(socket, address, PORT, fileReader);
                System.out.println("Opening connection");
                if (!uploader.openConnection(UPLOAD)) {
                    uploader.terminateConnection();
                    break;
                }
                System.out.println("Connection Opened");
                System.out.println("Uploading");
                if (!uploader.uploadFile()) {
                    uploader.terminateConnection();
                    break;
                }
                System.out.println("Upload finished");
                System.out.println("Closing connection");
                if (!uploader.closeConnection()) {
                    uploader.terminateConnection();
                }
                break;
            default:
                System.out.println("Wrong number of arguments");
        }

        socket.close();
    }
}
