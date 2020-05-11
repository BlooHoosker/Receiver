package receiver;

import java.util.ArrayList;
import java.util.List;

public class SlidingWindowReceive {

    private int size;
    private int delta;
    private int confirmed;
    private int windowEnd;
    private DataPacket[] onHold;

    public SlidingWindowReceive(int size, int start, int delta){
        this.size = size;
        this.confirmed = start;
        this.windowEnd = size;
        this.delta = delta;
        this.onHold = new DataPacket[size];
    }

    // Processes packet and returns a list of packets that fit in the data stream
    public List<DataPacket> processPacket(DataPacket packet){

        List<DataPacket> confirmedPackets = new ArrayList<>();

        int pos = getPos(packet);
        if (pos >= 0) {
            // Packet gets stored on position in array
            // If first element is not null it will add it to downloaded packets and shift window and repeats until it can
            onHold[pos] = packet;
            System.out.println("On hold: " + packet.seqNum + " on pos " + pos);
            while (onHold[0] != null) {
                confirmedPackets.add(onHold[0]);
                shiftWindow();
            }
        }
        return confirmedPackets;
    }

    // Gets position in the onHold array based on packet, if packet doesnt fit returns -1
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

    public void shiftWindow(){
        confirmed = (confirmed + onHold[0].data.length) % 65536;
        System.out.println("Confirmed: " + confirmed);
        for (int i = 1; i < size; i++) {
            onHold[i - 1] = onHold[i];
        }
        onHold[size - 1] = null;
    }

    public int nextNum(int num){
        return (num + delta) % 65536;
    }

    public int getConfirmed(){
        return confirmed;
    }

}
