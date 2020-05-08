package receiver;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {

    public static void main(String[] args) {

        DatagramSocket socket;
        InetAddress address;
        try{
            socket = new DatagramSocket();
            socket.setSoTimeout(100);
            //todo make it through args
            address = InetAddress.getByName("192.168.0.111");
        } catch (UnknownHostException e){
            System.out.println("Error Unknown host");
            return;
        } catch (IOException e){
            System.out.println("Error making socket");
            return;
        }

        //todo make it through args
            switch(2){
                case 2:
                    Receiver receiver;
                    receiver = new Receiver(socket, address);
                    System.out.println("Opening connection");
                    if (!receiver.openConnection()){
                        receiver.terminateConnection();
                        socket.close();
                        break;
                    }
                    System.out.println("succ");

                    if (!receiver.downloadFile()){
                        receiver.terminateConnection();
                        socket.close();
                        break;
                    }

                    if (!receiver.closeConnection()){
                        receiver.terminateConnection();
                        socket.close();
                        break;
                    }
                    break;
                case 3:

                    break;
                default:
                    System.out.println("Not enough arguments");
            }

        socket.close();
    }
}
