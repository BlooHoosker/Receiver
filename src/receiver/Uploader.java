package receiver;

import javafx.util.Pair;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

// V případě uploadu se toto spojení uzavře po 20. opakování SYN paketu.
// Vysílač se snaží mít v komunikačním kanále právě tolik nepotvrzených bytů odesílaného proudu dat, jako je velikost okénka.
// Při zahájení komunikace zapíše do kanálu W bytů a čeká na potvrzení.
// Jakmile vysílací strana přijme paket s takovým potvrzením, které snižuje počet nepotvrzených dat v odesílaném proudu, odešle další data, tak, aby bylo v kanále opět W nepotvrzených bytů (říkáme, že posune okénko).
// Vysílač si pamatuje čas odeslání posledního paketu (označme T).
// Pokud server nepřijme do času T + Tout žádné nové potvrzení, které snižuje počet nepotvrzených dat, odešle vysílač W bytů od nevyššího přijatého potvrzovacího čísla (říkáme, že odešle celé okénko).
// Nastaví také T na novou hodnotu.
// Pokud vysílač přijme 3x po sobě stejné potvrzovací číslo, odešle ihned do kanálu 1 paket s maximálním možným množstvím dat od pořadového čísla shodného s přijatým potvrzovacím číslem a nastaví T na novou hodnotu.
// Pokud dojde 20x po sobě k opakovanému odvysílání paketu se stejným sekvenčním číslem, je spojení přerušeno, klient musí vypsat chybu při přenosu. To platí i při uzavírání spojení, kdy je odesílán příznak FIN.

public class Uploader extends Receiver {

    public Uploader(DatagramSocket socket, InetAddress address, int port, File file){
        super(socket, address, port);
    }

    public boolean upload(){
        //sliding window

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
