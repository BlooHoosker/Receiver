package receiver;

import javafx.util.Pair;

import javax.xml.crypto.Data;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SlidingWindow {

    private int windowSize;
    private int size;
    private int delta;
    private int confirmed;
    private int windowEnd;
    //private List<DataPacket> onHold;
    private DataPacket[] onHold;

    public SlidingWindow(int size, int start, int delta){
        this.windowSize = size;
        this.size = size;
        this.confirmed = start;
        this.windowEnd = size;
        this.delta = delta;
        //this.onHold = new LinkedList<>();
        this.onHold = new DataPacket[size];
    }

    public void shiftWindow(int delta){
        confirmed = (delta + confirmed) % 65536;
        windowEnd = (delta + windowEnd) % 65536;
    }

    public List<DataPacket> processPacket(DataPacket packet){

        List<DataPacket> confirmedPackets = new ArrayList<>();
        /*
        DataPacket tmp;
        if (add(packet)){
            while(!onHold.isEmpty()){
                tmp = onHold.get(0);
                if(tmp.seqNum != confirmed){
                    break;
                }
                System.out.println("Removing seqNum: " +tmp.seqNum);
                confirmedPackets.add(tmp);
                shiftWindow(tmp.data.length);
                onHold.remove(0);
            }
        }
         */

        int pos = getPos(packet);
        if (pos >= 0) {
            // Packet gets stored on position in array
            // If first element is not null it will add it to downloaded packets and shift window and repeats until it can
            onHold[pos] = packet;
            System.out.println("On hold: " + packet.seqNum + " on pos " + pos);
            while (onHold[0] != null) {
                confirmedPackets.add(onHold[0]);
                next(onHold[0].data.length);
                for (int i = 1; i < size; i++) {
                    onHold[i - 1] = onHold[i];
                }
                onHold[size - 1] = null;
            }
        }
        return confirmedPackets;
    }
/*
    public boolean add(DataPacket packet){

        // if the packet would be too large for for the window it wont be added
        if ((packet.seqNum + packet.data.length) % 65536> windowEnd){
            return false;
        }

        // If its empty we can simply add
        if (onHold.isEmpty()){
            onHold.add(packet);
            return true;
        }

        // Goes through packets in the list and if theres a packet with higher number than
        for (DataPacket tmp : onHold){
            if (tmp.seqNum != packet.seqNum){
                if (packet.seqNum < tmp.seqNum){
                    onHold.add(onHold.indexOf(tmp), packet);
                    return true;
                }
            } else {
                return false;
            }
        }

        // if it didnt get added anywhere it means it needs to be added at the end of the list
        onHold.add(packet);
        return true;
    }
*/
    public int getConfirmed(){
        return confirmed;
    }


    public int getPos(DataPacket packet){
        int seqNum = packet.seqNum;
        int tmp = confirmed;
        if(seqNum == confirmed){
            return 0;
        }
        for (int i = 1; i < size; i++){
            tmp = nextNum(tmp);
            if (seqNum == tmp){
                return i;
            }
        }
        return -1;
    }

    public int nextNum(int num){
        return (num + delta) % 65536;
    }

    public void next(int size){
        confirmed = (confirmed + size) % 65536;
        System.out.println("Confirmed: " + confirmed);
    }
}
