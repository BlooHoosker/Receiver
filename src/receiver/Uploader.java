package receiver;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.time.LocalTime;

// V případě uploadu se toto spojení uzavře po 20. opakování SYN paketu. todo
// Vysílač se snaží mít v komunikačním kanále právě tolik nepotvrzených bytů odesílaného proudu dat, jako je velikost okénka.
// Při zahájení komunikace zapíše do kanálu W bytů a čeká na potvrzení.
// Jakmile vysílací strana přijme paket s takovým potvrzením, které snižuje počet nepotvrzených dat v odesílaném proudu, odešle další data, tak, aby bylo v kanále opět W nepotvrzených bytů (říkáme, že posune okénko).
// Vysílač si pamatuje čas odeslání posledního paketu (označme T). todo
// Pokud server nepřijme do času T + Tout žádné nové potvrzení, které snižuje počet nepotvrzených dat, odešle vysílač W bytů od nevyššího přijatého potvrzovacího čísla (říkáme, že odešle celé okénko). //todo
// Nastaví také T na novou hodnotu. todo
// Pokud vysílač přijme 3x po sobě stejné potvrzovací číslo, odešle ihned do kanálu 1 paket s maximálním možným množstvím dat od pořadového čísla shodného s přijatým potvrzovacím číslem a nastaví T na novou hodnotu. todo
// Pokud dojde 20x po sobě k opakovanému odvysílání paketu se stejným sekvenčním číslem, je spojení přerušeno, klient musí vypsat chybu při přenosu. To platí i při uzavírání spojení, kdy je odesílán příznak FIN. todo

public class Uploader extends Downloader {

    public int WINDOWSIZE = 8;
    public int DELTA = 255;
    public int TIMEOUT = 100;

    private final FileInputStream fileReader;
    private SlidingWindowSend slidingWindow;

    public Uploader(DatagramSocket socket, InetAddress address, int port, FileInputStream fileReader){
        super(socket, address, port);
        this.fileReader = fileReader;
    }

    public boolean uploadFile(){

        slidingWindow = new SlidingWindowSend(WINDOWSIZE, DELTA, socket, address, port, fileReader, connectionID);

        byte[] receiveBytes = new byte[264];
        DatagramPacket receivePacket = new DatagramPacket(receiveBytes, receiveBytes.length);

        LocalTime time;
        DataPacket receiveData;
        DataPacket lastData = null;
        int lastDataCount = 0;
        try{
            // Initialise window and send it
            slidingWindow.init();
            slidingWindow.sendWindow();
            time = LocalTime.now();

            while(true){
                try{
                    socket.receive(receivePacket);
                } catch (SocketTimeoutException e) {
                    // If timeout is called sends whole window and resets timer
                    System.out.println("Socket timeout, sending window");
                    slidingWindow.sendWindow();
                    time = LocalTime.now();
                    continue;
                }

                // Packed gets parsed into DataPacket class
                receiveData = new DataPacket(receivePacket.getData(), receivePacket.getLength());
                System.out.println("Received: " + receiveData.toString());

                // Checking parsed packet for errors
                if (!checkPacket(receiveData)){
                    System.out.println("Packet error");
                    return false;
                }

                // if lastData seq num is the same as the new one increases count
                if (lastData != null){
                    if (lastData.confirmNum == receiveData.confirmNum) {
                        lastDataCount++;
                        // If we received same data 3x in row sends the first one again
                        if (lastDataCount % 3 == 0) {
                            slidingWindow.sendFirst();
                            time = LocalTime.now();
                        }
                        continue;
                    }
                }
                lastDataCount = 1;
                lastData = receiveData;

                // Processing received data
                System.out.println("Processing packet");
                int state = slidingWindow.processPacket(receiveData);

                // If window was shifted reset time, if file uploaded then finishes upload
                if (state == 1){
                    time = LocalTime.now();
                    continue;
                } if (state == -1){
                    break;
                }

                // If we reached over timeout sends whole window and resets time;
                if (LocalTime.now().getNano() - time.getNano() >= TIMEOUT*100000){
                    System.out.println("Timeout, sending window");
                    slidingWindow.sendWindow();
                    time = LocalTime.now();
                }
            }

        } catch (IOException e ){
            System.out.println("IO exception");
            return false;
        }

        return true;
    }

    @Override
    public boolean closeConnection(){
        byte[] sendBytes = DataPacket.toBytes(0, slidingWindow.getConfirmed(), 0, DataPacket.fin(), new byte[0]);
        DatagramPacket sendPacket = new DatagramPacket(sendBytes, sendBytes.length, address, port);

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
                if (attemptCount >= 20) {
                    return false;
                }
                continue;
            } catch (IOException e){
                System.out.println("Error sending/receiving packet");
                return false;
            }
            receivedData = new DataPacket(receivePacket.getData(), receivePacket.getLength());
            // Checking parsed packet for errors
            if (checkPacket(receivedData)) {
                if (receivedData.flags == DataPacket.fin()){
                    break;
                }
            } else {
                return false;
            }

            if (attemptCount >= 20){
                return false;
            }
        }
        return true;
    }



}
