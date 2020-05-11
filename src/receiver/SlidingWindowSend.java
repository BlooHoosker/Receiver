package receiver;

import javax.xml.crypto.Data;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class SlidingWindowSend {

    private final int size;
    private final int delta;
    private int confirmed;
    private final DataPacket[] windowPackets;

    private final FileInputStream fileReader;
    private int fileBytesRead;
    
    private final DatagramSocket socket;
    private final InetAddress address;
    private final int port;
    private final long connectionID;

    public SlidingWindowSend(int size, int delta, DatagramSocket socket, InetAddress address, int port, FileInputStream fileReader, long connectionID){
        this.size = size;
        this.delta = delta;
        this.windowPackets = new DataPacket[size];
        this.socket = socket;
        this.address = address;
        this.port = port;
        this.fileReader = fileReader;
        this.connectionID = connectionID;
        this.fileBytesRead = 0;
        this.confirmed = 0;
    }

    // Initializes data in the window array
    public void init() throws IOException{
        System.out.println("Initiating window");
        DataPacket packet;
        for (int i = 0; i < size; i++){
            packet = readPacket();
            if (packet != null){
                windowPackets[i] = packet;
            } else {
                break;
            }
        }
    }

    // Processes packet, returns true if upload will continue or false if upload finished
    public int processPacket(DataPacket packet) throws IOException{
        boolean shift = false;
        while(checkWindow(packet)){
            shift = true;
            shiftWindow();
            if (windowPackets[0] == null){
                return -1;
            }
        }
        if (shift){
            return 1;
        } else {
            return 0;
        }
    }

    // Checks if the received packet has higher number than last confirmed packet
    private boolean checkWindow(DataPacket packet){

        if(windowPackets[0] == null){
            return true;
        }

        int end = 0;
        for (DataPacket tmp : windowPackets){
            if (tmp != null){
                end = tmp.seqNum;
            }
        }

        for (DataPacket tmp : windowPackets){
            if (tmp != null && tmp.seqNum < packet.confirmNum && packet.confirmNum <= end + delta){
                return true;
            }
        }
        return false;
    }

    private void shiftWindow() throws IOException{
        System.out.println("Shifting window");
        // Shifts data packets in window to the left
        confirmed = (confirmed + windowPackets[0].data.length) % 65536;
        for (int i = 1; i < size; i++){
            windowPackets[i-1] = windowPackets[i];
        }

        // Reads new packet from file
        windowPackets[size-1] = readPacket();

        // If packet was read it will send it to the client
        if (windowPackets[size-1] != null){
            sendPacket(windowPackets[size-1]);
        }

    }

    // sends all data in window
    public void sendWindow() throws IOException{
        System.out.println("Sending window");
        for (DataPacket packet : windowPackets){
            if (packet == null){
                break;
            }
            sendPacket(packet);
        }
    }

    // Sends only the first data in window
    public void sendFirst() throws IOException{
        System.out.println("Sending first one");
        if (windowPackets[0] != null){
            sendPacket(windowPackets[0]);
        }
    }

    // Reads datachunk from file and returns it as packet
    public DataPacket readPacket() throws IOException {
        DataPacket packet;
        byte[] data = new byte[delta];
        int bytesRead = 0;

        bytesRead = fileReader.read(data, 0, delta);
        if(bytesRead <= 0) {
            return null;
        }

        packet = new DataPacket(connectionID, fileBytesRead % 65536, 0, (byte) 0, data);
        System.out.println("Adding packet to window: " +packet.toString());
        fileBytesRead += bytesRead;

        return packet;
    }

    private void sendPacket(DataPacket packet) throws IOException{
        byte[] sendBytes = packet.toBytes();
        DataPacket test = new DataPacket(sendBytes, sendBytes.length);
        System.out.println("Sending: " + test.toString());
        socket.send(new DatagramPacket(sendBytes, sendBytes.length, address, port));
    }

    public int getConfirmed(){
        return confirmed;
    }

}
