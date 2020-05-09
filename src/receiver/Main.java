package receiver;

import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {

    public static int PORT = 4000;
    public static byte[] DOWNLOAD = {0x01};
    public static byte[] UPLOAD = {0x02};
    public static String ADDRESS = "192.168.0.111";

    public static void main(String[] args) {

        DatagramSocket socket;
        InetAddress address;
        try{
            socket = new DatagramSocket();
            socket.setSoTimeout(100);
            //todo make it through args
            address = InetAddress.getByName(ADDRESS);
        } catch (UnknownHostException e){
            System.out.println("Error Unknown host");
            return;
        } catch (IOException e){
            System.out.println("Error making socket");
            return;
        }


        Receiver receiver;
        Uploader uploader;
        //todo make it through args
        switch (2) {
            case 2:
                receiver = new Receiver(socket, address, PORT);
                System.out.println("Opening connection");
                if (!receiver.openConnection(DOWNLOAD)) {
                    receiver.terminateConnection();
                    break;
                }
                System.out.println("Connection Opened");
                System.out.println("Downloading");
                if (!receiver.downloadFile()) {
                    receiver.terminateConnection();
                    break;
                }
                System.out.println("Download finished");
                System.out.println("Closing connection");
                if (!receiver.closeConnection()) {
                    receiver.terminateConnection();
                }
                break;
            case 3:
                File file = new File("firmware.bin");
                if (!file.canRead()){
                    System.out.println("File couldnt be read");
                    break;
                }

                uploader = new Uploader(socket, address, PORT, file);
                System.out.println("Opening connection");
                if (!uploader.openConnection(UPLOAD)) {
                    uploader.terminateConnection();
                    break;
                }
                System.out.println("Connection Opened");

                break;
            default:
                System.out.println("Not enough arguments");
        }

        socket.close();
    }
}
