package Sender;

import java.nio.ByteBuffer;
import java.util.zip.Adler32;

public class PACKET {
    public static void main(String args[]){
        byte[] data = new byte[150];
         byte [] test = createPacket(true,data,false);
         System.out.println(getAck(test));
        System.out.println(isCorrupt(test));
        System.out.println(isEnd(test));
    };

    //size 512 byte
    public static byte[] createPacket(boolean ACKNumber, byte[] data, boolean end){

        byte[] result = new byte[512];
        byte acknr = (byte) (ACKNumber?1:0); // convert boolean to byte
        byte endflag = (byte) (end?1:0);
        result[8] = acknr;                  // store in packet
        result[9] = endflag;
        int size = data.length;
        result[10] = (byte) (size >> 24);
        result[11] = (byte) (size >> 16);
        result[12] = (byte) (size >> 8);
        result[13] = (byte) size;
        System.arraycopy(data,0,result,14,size); //store data
        Adler32 adler32 = new Adler32();
        adler32.update(result,8,data.length + 2); //calculate checksum
        long checksum = adler32.getValue();
        result[0] = (byte) (checksum >> 56); //store checksum
        result[1] = (byte) (checksum >> 48);
        result[2] = (byte) (checksum >> 40);
        result[3] = (byte) (checksum >> 32);
        result[4] = (byte) (checksum >> 24);
        result[5] = (byte) (checksum >> 16);
        result[6] = (byte) (checksum >> 8);
        result[7] = (byte) checksum;


        return result;
    }

    public static boolean isCorrupt(byte[] paket){

        long checksum = paket[0] << 56 + paket[1] << 48 + paket[2] << 40 + paket[3] << 32
                + paket[4] << 24 + paket[5] << 16 + paket[6] << 8 + paket[7];
        int size = paket[10] << 24 + paket[11] << 16 + paket[12] << 8 + paket[13];
        Adler32 adler32 = new Adler32();
        adler32.update(paket,8,size + 6); //calculate checksum
        long otherchecksum = adler32.getValue();
        return checksum == otherchecksum;
    }

    public static boolean getAck(byte[] paket){

        if(paket[8] == 1){
            return true;
        }
        return false;
    }

    public static int getSize(byte[] paket) {
        return ByteBuffer.wrap(paket, 10, 4).getInt();
    }

    public static byte[] getContent(byte[] paket) {
        byte[] content = new byte[getSize(paket)];
        System.arraycopy(paket, 14, content, 0, getSize(paket));
        return content;
    }

    public static boolean isEnd(byte[] paket){

        if(paket[9] == 1){
            return true;
        }
        return false;
    }
}
