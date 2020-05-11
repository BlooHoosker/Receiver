package receiver;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class DataPacket {

    public final long id;
    public final int seqNum;
    public final int confirmNum;
    public final byte flags;
    public final byte[] data;

    public DataPacket(byte[] packet, int length){

        ByteBuffer buffer = ByteBuffer.wrap(packet);

        long tmpId = buffer.getInt(0);
        int tmpSeq = buffer.getShort(4);
        int tmpCon = buffer.getShort(6);
        id = tmpId & 4294967295L;
        seqNum = tmpSeq & 65535;
        confirmNum = tmpCon & 65535;

        flags = packet[8];

        data = new byte[length - 9];

        if (data.length > 0){
            for (int i = 9; i < data.length; i++){
                data[i-9] = packet[i];
            }
        }

        if (data.length > 0){
            for (int i = 0; i < data.length; i++){
                data[i] = packet[i+9];
            }
        }
    }

    public DataPacket(long id, int seqNum, int confirmNum, byte flags, byte[] data){
        this.id = id;
        this.seqNum = seqNum;
        this.confirmNum = confirmNum;
        this.flags = flags;
        this.data = data.clone();
    }

    public static byte[] toBytes(long id, int seqNum, int confirmNum, byte flags, byte[] data){
        byte[] bytes = new byte[9 + data.length];

        // Parsing id
        bytes[0] = (byte) (id >>> 24);
        bytes[1] = (byte) (id >>> 16);
        bytes[2] = (byte) (id >>> 8);
        bytes[3] = (byte) (id);

        // Parsing seqNum
        bytes[4] = (byte) (seqNum >>> 8);
        bytes[5] = (byte) (seqNum);

        // Parsing confirmNum
        bytes[6] = (byte) (confirmNum >>> 8);
        bytes[7] = (byte) (confirmNum);

        bytes[8] = flags;

        if (data.length > 0){
            for (int i = 0; i < data.length; i++){
                bytes[i+9] = data[i];
            }
        }

        return bytes;
    }

    public byte[] toBytes(){
        return toBytes(id, seqNum, confirmNum, flags, data);
    }

    public static byte syn(){ ;
        return (byte) (1 << 2);
    }

    public static byte fin(){
        return (byte) (1 << 1);
    }

    public static byte rst(){
        return (byte) 1;
    }

    @Override
    public String toString() {
        return "DataPacket{" +
                "id=" + id +
                ", seqNum=" + seqNum +
                ", confirmNum=" + confirmNum +
                ", flags=" + flags +
                ", data=" + data.length +
                '}';
    }
}
