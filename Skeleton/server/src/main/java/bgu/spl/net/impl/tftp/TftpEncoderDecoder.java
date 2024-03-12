package bgu.spl.net.impl.tftp;

import java.util.Arrays;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    //TODO: Implement here the TFTP encoder and decoder
    private byte[] bytes = new byte[1<<10]; // start with 1KB
    private int length = 0;
    private short opCode;
    private int dataSize;


    @Override
    public byte[] decodeNextByte(byte nextByte) {
        // TODO: implement this
        if(length == 0){
            pushByte(nextByte);
        }
        else if(length == 1){
            pushByte(nextByte);
            opCode = (short)(((short)bytes[0] & 0xFF)<<8|(short)(bytes[1] & 0xFF)); // Combine bytes into an integer
        }
        else
        {
            if(opCode == 1 || opCode == 2 || opCode == 7 || opCode == 8){return decocdeNormal(nextByte);}
            if(opCode == 3){return decodeDATA(nextByte);}
            if(opCode == 4){return decodeACK(nextByte);}
            if(opCode == 5){return decodeERROR(nextByte);}
            if(opCode == 6){return decodeDIRQ(nextByte);}
            if(opCode == 9){return decodeBCAST(nextByte);}
            if(opCode == 10){return decodeDISC(nextByte);} 
        }
        return null;
    }

    @Override
    public byte[] encode(byte[] message) {
        //TODO: implement this
        System.out.println("entered encode");
        return message;
    }

    public void pushByte(byte nextByte){
        if (length >= bytes.length) {
            bytes = Arrays.copyOf(bytes, length * 2);
        }
        
        bytes[length++] = nextByte;
    }

    private byte[] decocdeNormal(byte nextByte){
        if(nextByte == 0){ // check if 0 needs to be sent as part of the message as a sign of end of message
            pushByte(nextByte);
            byte[] result = new byte[length];
            System.arraycopy(bytes, 0, result, 0 , length); 
            length = 0;
            return result;
        }
        else{
            pushByte(nextByte);
            return null;
        }    
    }

    private byte[] decodeDATA(byte nextByte){
        if(length == 2){
            pushByte(nextByte);
        }
        else if(length == 3){
            pushByte(nextByte);
            dataSize = (short)(((short)bytes[2] & 0xFF)<<8|(short)(bytes[3] & 0xFF)); // Packet DATA size
        }
        else if(length < dataSize + 4){
            pushByte(nextByte);
        }
        if(length == dataSize + 4){
            byte[] result = new byte[length];
            System.arraycopy(bytes, 0, result, 0 , length); 
            length = 0;
            return result;
        }

        return null;
    }

    private byte[] decodeACK(byte nextByte){
        pushByte(nextByte);
        if(length == 4){
            byte[] result = new byte[length];
            System.arraycopy(bytes, 0, result, 0 , length); 
            length = 0;
            return result;
        }
        return null;
    }
    
    private byte[] decodeERROR(byte nextByte){
        if(length < 4){
            pushByte(nextByte);
            return null;
        }
        if(nextByte == 0){
            pushByte(nextByte);
            byte[] result = new byte[length];
            System.arraycopy(bytes, 0, result, 0 , length); 
            length = 0;
            return result;
        }
        else{
            pushByte(nextByte);
            return null;
        }
    }

    private byte[] decodeDIRQ(byte nextByte){
        byte[] result = new byte[length];
        System.arraycopy(bytes, 0, result, 0 , length); 
        length = 0;
        return result;
    }

    private byte[] decodeBCAST(byte nextByte){
        if(length == 2){
            pushByte(nextByte);
            return null;
        }
        if(nextByte == 0){
            pushByte(nextByte);
            byte[] result = new byte[length];
            System.arraycopy(bytes, 0, result, 0 , length); 
            length = 0;
            return result;
        }
        pushByte(nextByte);
        return null;

    }

    private byte[] decodeDISC(byte nextByte){
        byte[] result = new byte[length];
        System.arraycopy(bytes, 0, result, 0 , length); 
        length = 0;
        return result;
    }

}