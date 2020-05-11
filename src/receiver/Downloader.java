package receiver;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Downloader {

        protected DatagramSocket socket;
        protected InetAddress address;
        protected long connectionID;
        protected int port;

        private List<DataPacket> downloadedPackets;
        private SlidingWindowReceive slidingWindowReceive;
        
        public Downloader(DatagramSocket socket, InetAddress address, int port){
            this.socket = socket;
            this.address = address;
            this.port = port;
            this.downloadedPackets = new ArrayList<>();
        }

        protected boolean checkPacket(DataPacket packet){
            if (connectionID != packet.id || (packet.flags != 0x00 && packet.flags != DataPacket.syn() && packet.flags != DataPacket.rst() && packet.flags != DataPacket.fin())){
                return false;
            }
            return true;
        }

        public boolean openConnection(byte[] command){

            byte[] sendBytes = DataPacket.toBytes(0, 0, 0, DataPacket.syn(), command);
            DatagramPacket sendPacket = new DatagramPacket(sendBytes, sendBytes.length, address, port);
            DataPacket print = new DataPacket(sendBytes, sendBytes.length);

            byte[] receiveBytes = new byte[264];
            DatagramPacket receivePacket = new DatagramPacket(receiveBytes, receiveBytes.length);

            int attemptCount = 0;
            DataPacket receivedData;
            while(true){
                try{
                    System.out.println("SEND: " + print.toString());
                    socket.send(sendPacket);
                    socket.receive(receivePacket);
                } catch (SocketTimeoutException e){
                    attemptCount++;
                    // if we already tried 20 times;
                    if (attemptCount > 20){
                        return false;
                    }
                    continue;
                } catch (IOException e){
                    System.out.println("Error sending/receiving packet");
                    return false;
                }
                receivedData = new DataPacket(receivePacket.getData(), receivePacket.getLength());
                System.out.println("RECEIVE: " + receivedData.toString());
                if (receivedData.flags == DataPacket.syn()){
                    break;
                }
                attemptCount++;
                // if we already tried 20 times;
                if (attemptCount > 20){
                    return false;
                }
            }

            connectionID = receivedData.id;
            return receivedData.data.length == 1 && receivedData.data[0] == command[0];
        }

        public boolean downloadFile(){

            slidingWindowReceive = new SlidingWindowReceive( 8, 0, 255);

            byte[] receiveBytes = new byte[264];
            DatagramPacket receivePacket = new DatagramPacket(receiveBytes, receiveBytes.length);

            DataPacket receivedData;
            int attemptCount = 0;
            while(true){

                // If we sent 30x the same seqNum it has to cancel the connection
                attemptCount++;
                if (attemptCount > 30){
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

                // Packed gets parsed into DataPacket class
                receivedData = new DataPacket(receivePacket.getData(), receivePacket.getLength());
                System.out.println("RECEIVE: " + receivedData.toString());

                // Checking parsed packet for errors
                if (!checkPacket(receivedData)){
                    return false;
                }

                // Checking for flags
                if (receivedData.flags == DataPacket.fin()){
                    if (receivedData.data.length != 0){
                        return false;
                    }
                    return true;
                } else if (receivedData.flags == DataPacket.rst()){
                    return false; // todo idk if it is send or if i just close the conncetion
                }

                // Sliding window process
                List<DataPacket> confirmedPackets = slidingWindowReceive.processPacket(receivedData);
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
            byte[] sendBytes = DataPacket.toBytes(0, slidingWindowReceive.getConfirmed(), 0, DataPacket.fin(), new byte[0]);
            DatagramPacket sendPacket = new DatagramPacket(sendBytes, sendBytes.length, address, port);

            try{
                socket.send(sendPacket);
            } catch (IOException e){
                System.out.println("Error sending/receiving packet");
                return false;
            }

            return true;
        }

        public boolean sendConf(){
            byte[] sendBytes;
            DatagramPacket sendPacket;
            sendBytes = DataPacket.toBytes(connectionID, 0, slidingWindowReceive.getConfirmed(), (byte) 0x00, new byte[0]);
            sendPacket = new DatagramPacket(sendBytes, sendBytes.length, address, port);
            try{
                DataPacket test = new DataPacket(sendBytes, sendBytes.length);
                System.out.println("SEND: " + test.toString());
                socket.send(sendPacket);
            } catch (IOException e){
                System.out.println("Error sending/receiving packet");
                return false;
            }
            return true;
        }

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

        public boolean saveFile(){
            try(FileOutputStream outFile = new FileOutputStream("out.png")){
                for(DataPacket packet : downloadedPackets){
                    outFile.write(packet.data);
                    outFile.flush();
                }
            } catch (IOException e){
                System.out.println("File error");
                return false;
            }
            return true;
        }

}
