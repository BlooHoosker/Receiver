package receiver;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Receiver {

        private DatagramSocket socket;
        private InetAddress address;

        private long connectionID;
        private int port = 4000;

        private List<DataPacket> downloadedPackets;
        private SlidingWindow slidingWindow;



        public Receiver(DatagramSocket socket, InetAddress address){
            this.socket = socket;
            this.address = address;
            this.downloadedPackets = new ArrayList<>();
        }

        public boolean checkPacket(DataPacket packet){
            if (connectionID != packet.id || packet.flags != 0x00){
                return false;
            }
            return true;
        }

        public boolean sendConf(){
            byte[] sendBytes;
            DatagramPacket sendPacket;
            sendBytes = DataPacket.toBytes(connectionID, 0, slidingWindow.getConfirmed(), (byte) 0x00, new byte[0]);
            sendPacket = new DatagramPacket(sendBytes, sendBytes.length, address, port);
            try{
                DataPacket test = new DataPacket(sendBytes, sendBytes.length);
                //System.out.println("Sending: " + test.toString() + '\n');
                System.out.println();
                socket.send(sendPacket);
            } catch (IOException e){
                System.out.println("Error sending/receiving packet");
                return false;
            }
            return true;
        }
        /*
        public boolean receivePacket(){
            byte[] receiveBytes = new byte[264];
            DatagramPacket receivePacket = new DatagramPacket(receiveBytes, receiveBytes.length);

            DataPacket receivedData;

            // Receive packet of data
            try{
                socket.receive(receivePacket);
            } catch (SocketTimeoutException e){
                //todo idk what to do about timeout
            } catch (IOException e){
                System.out.println("Error sending/receiving packet");
                return false;
            }

            // Packed gets parsed into DataPacket structure
            receivedData = new DataPacket(receivePacket.getData(), receivePacket.getLength());
            System.out.println("Received: " + receivedData.toString());

            // Checking parsed packet for errors
            if (!checkPacket(receivedData)){
                if (receivedData.flags == DataPacket.fin()){
                    return true;
                } else if (receivedData.flags == DataPacket.rst()){
                    return false;
                }
            }
        }
         */

        public void terminateConnection(){
            // todo idk if this is correct what seq and con is supposed to be
            byte[] bytesSend = DataPacket.toBytes(connectionID, 0, 0, DataPacket.rst(), new byte[0]);
            try{
                socket.send(new DatagramPacket(bytesSend, bytesSend.length, address, port));
            } catch (IOException e){
                System.out.println("Terminate exception");
            }
            System.out.println("Terminating connection");
        }

        public boolean openConnection(){

            byte[] command = {0x01};
            byte[] sendBytes = DataPacket.toBytes(0, 0, 0, DataPacket.syn(), command);
            DatagramPacket sendPacket = new DatagramPacket(sendBytes, sendBytes.length, address, port);

            byte[] receiveBytes = new byte[264];
            DatagramPacket receivePacket = new DatagramPacket(receiveBytes, receiveBytes.length);

            int attemptCount = 0;
            DataPacket receivedData;
            DataPacket test = new DataPacket(sendBytes, sendBytes.length);
            while(true){
                try{
                    System.out.println("Sending: " + test.toString());
                    socket.send(sendPacket);
                    socket.receive(receivePacket);
                } catch (SocketTimeoutException e){
                    /*
                    attemptCount++;
                    // if we already tried 20 times;
                    if (attemptCount >= 20){
                        return false;
                    }
                    */
                    continue;
                } catch (IOException e){
                    System.out.println("Error sending/receiving packet");
                    return false;
                }
                receivedData = new DataPacket(receivePacket.getData(), receivePacket.getLength());
                System.out.println("Received: " + receivedData.toString());
                if (receivedData.flags == DataPacket.syn()){
                    break;
                }
                attemptCount++;
                if (attemptCount >= 20){
                    return false;
                }
            }

            connectionID = receivedData.id;
            return receivedData.data.length == 1 && receivedData.data[0] == 0x01;
        }

        public boolean downloadFile(){

            slidingWindow = new SlidingWindow(8, 255, 0, this);

            byte[] receiveBytes = new byte[264];
            DatagramPacket receivePacket = new DatagramPacket(receiveBytes, receiveBytes.length);

            DataPacket receivedData;


            int attemptCount = 0;
            while(true){

                // If we sent 20x the same seqNum it has to cancel the connection
                //attemptCount++;
                if (attemptCount > 20){
                    return false;
                }

                // Receive packet of data
                try{
                    socket.receive(receivePacket);
                } catch (SocketTimeoutException e){
                    //todo idk what to do about timeout
                } catch (IOException e){
                    System.out.println("Error sending/receiving packet");
                    return false;
                }

                // Packed gets parsed into DataPacket structure
                receivedData = new DataPacket(receivePacket.getData(), receivePacket.getLength());
                System.out.println("Received: " + receivedData.toString());

                // Checking parsed packet for errors
                if (!checkPacket(receivedData)){
                    if (receivedData.flags == DataPacket.fin()){
                        return true;
                    } else if (receivedData.flags == DataPacket.rst()){
                        return false;
                    }
                }

                // Sliding window process
                List<DataPacket> confirmedPackets = slidingWindow.processPacket(receivedData);
                if (confirmedPackets.size() != 0){
                    downloadedPackets.addAll(confirmedPackets);
                    attemptCount = 0;
                }

                // Parse our packet to bytes and then create Datagram packet and send
                if(!sendConf()){
                    return false;
                }
            }

        }

        public boolean closeConnection(){
            byte[] sendBytes = DataPacket.toBytes(0, slidingWindow.getConfirmed(), 0, DataPacket.fin(), new byte[0]);
            DatagramPacket sendPacket = new DatagramPacket(sendBytes, sendBytes.length, address, port);

            try{
                socket.send(sendPacket);
            } catch (IOException e){
                System.out.println("Error sending/receiving packet");
                return false;
            }

            /*
            byte[] receiveBytes = new byte[264];
            DatagramPacket receivePacket = new DatagramPacket(receiveBytes, receiveBytes.length);

            int attemptCount = 0;
            DataPacket receivedData;
            while(true){
                attemptCount++;
                try{
                    socket.send(sendPacket);
                    socket.receive(receivePacket);
                } catch (SocketTimeoutException e){

                    attemptCount++;
                    // if we already tried 20 times;
                    if (attemptCount >= 20){
                        return false;
                    }

                    continue;
                } catch (IOException e){
                    System.out.println("Error sending/receiving packet");
                    return false;
                }
                receivedData = new DataPacket(receivePacket.getData(), receivePacket.getLength());
                if (receivedData.flags == DataPacket.syn()){
                    break;
                }
                if (attemptCount >= 20){
                    return false;
                }
            }

            connectionID = receivedData.id;
            return receivedData.data.length == 1 && receivedData.data[0] == 0x01;

            */

            return true;
        }

        public List<DataPacket> getDownloadedPackets(){
            return downloadedPackets;
        }

}
