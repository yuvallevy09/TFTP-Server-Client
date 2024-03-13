package bgu.spl.net.impl.tftp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpClientEncDec implements MessageEncoderDecoder<byte[]> {

    private byte[] bytes = new byte[1<<10]; 
    private int length = 0;
    private short opCode = 0;
    private int dataSize;
    private String uploadFileName = null;
    public String request = "";

    //OpCode fields
    short op_RRQ = 1; short op_WRQ = 2; short op_DATA = 3; short op_ACK = 4; short op_ERROR = 5;
    short op_DIRQ = 6; short op_LOGRQ = 7; short op_DELRQ = 8; short op_BCAST = 9; short op_DISC = 10;

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

        // String request = "";
        for (int i = 0; i < 3; i++) {
            request = request + message[i];
        }

        switch(request) {
            case "LOG": return encodeLOG(message);
            case "DEL": return encodeDEL(message);
            case "RRQ": return encodeRRQ(message);
            case "WRQ": return encodeWRQ(message);
            case "DIR": return encodeDIR();
            case "DIS": return encodeDIS();
            default: return message; // check if this is correct 
        }
    }


    public void pushByte(byte nextByte){
        if (length >= bytes.length) {
            bytes = Arrays.copyOf(bytes, length * 2);
        }
        
        bytes[length++] = nextByte;
    }




    // decoders:


    private byte[] decocdeNormal(byte nextByte){
        if(nextByte == 0){ // check if 0 needs to be sent as part of the message as a sign of end of message
            pushByte(nextByte);
            return bytes;
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
            return bytes;
        }

        return null;
    }

    private byte[] decodeACK(byte nextByte){
        pushByte(nextByte);
        if(length == 4){
            return bytes;
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
            return bytes;
        }
        else{
            pushByte(nextByte);
            return null;
        }
    }

    private byte[] decodeDIRQ(byte nextByte){
        return bytes;
    }

    private byte[] decodeBCAST(byte nextByte){
        if(length == 2){
            pushByte(nextByte);
            return null;
        }
        if(nextByte == 0){
            pushByte(nextByte);
            return bytes; // check if 0 needs to be sent as part of the message as a sign of end of message
        }
        pushByte(nextByte);
        return null;

    }

    private byte[] decodeDISC(byte nextByte){
        return bytes;
    }




    // encoders

    private byte[] encodeLOG(byte[] message){
        byte[] packet = new byte[message.length - 3]; // +2 for opcode, +1 for 0, -6 for 'LOGRQ '
        packet[0] = (byte) (op_LOGRQ >> 8);
        packet[1] = (byte) (op_LOGRQ & 0xff);
        packet[packet.length-1] = (byte)0;
        System.arraycopy(message, 6, packet, 2, packet.length-3);
        return packet;
    }

    private byte[] encodeDEL(byte[] message){
        byte[] packet = new byte[message.length - 3]; // +2 for opcode, +1 for 0, -6 for 'DELRQ '
        packet[0] = (byte) (op_DELRQ >> 8);
        packet[1] = (byte) (op_DELRQ & 0xff);
        packet[packet.length-1] = (byte)0;
        System.arraycopy(message, 6, packet, 2, packet.length-3);
        return packet;
    }

    private byte[] encodeRRQ(byte[] message){
        byte[] packet = new byte[message.length - 1]; // +2 for opcode, +1 for 0, -4 for 'RRQ '
        packet[0] = (byte) (op_RRQ >> 8);
        packet[1] = (byte) (op_RRQ & 0xff);
        packet[packet.length-1] = (byte)0;
        System.arraycopy(message, 4, packet, 2, packet.length-3);
        return packet;
    }

    private byte[] encodeWRQ(byte[] message){
        byte[] packet = new byte[message.length - 1]; // +2 for opcode, +1 for 0, -4 for 'WRQ '
        packet[0] = (byte) (op_WRQ >> 8);
        packet[1] = (byte) (op_WRQ & 0xff);
        packet[packet.length-1] = (byte)0;
        System.arraycopy(message, 4, packet, 2, packet.length-3);
        uploadFileName = new String(message, 4, message.length - 4, StandardCharsets.UTF_8);
        return packet;
    }

    private byte[] encodeDIR(){
        byte[] packet = new byte[2]; // +2 for opcode, +1 for 0, -4 for 'WRQ '
        packet[0] = (byte) (op_DIRQ >> 8);
        packet[1] = (byte) (op_DIRQ & 0xff);
        return packet;
    }

    private byte[] encodeDIS() {
        byte[] packet = new byte[2]; // +2 for opcode, +1 for 0, -4 for 'WRQ '
        packet[0] = (byte) (op_DISC >> 8);
        packet[1] = (byte) (op_DISC & 0xff);
        return packet;
    }
    

}