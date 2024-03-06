package bgu.spl.net.api;

import java.io.ByteArrayInputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

public class MessagingEncDecClass<T> implements MessageEncoderDecoder<T>{

    //Fields
    private final byte[] opCodeBytes = new byte[2];
    private int opCodeBytesIndex = 0;
    private boolean hasOpCode = false;
    private byte[] objectBytes = new byte[1<<10]; //Initialize size for the message byte array;
    private int objectBytesIndex = 2; //marks the index where the message starts in the objectBytes
    
    T result;

    //Methods
    public T decodeNextByte(byte nextByte){
        if(!hasOpCode){
            opCodeBytes[opCodeBytesIndex++] = nextByte;}

        if(opCodeBytesIndex == opCodeBytes.length){
            hasOpCode = true;

            switch (opCodeBytes[0]) {
                case 0:
                switch (opCodeBytes[1]) {
                    case 1: //RRQ
                    objectBytes = decodeDownload(nextByte);
                    if(objectBytes[objectBytesIndex] != '0'){
                        break;
                    }
                    result = deserializeObject();
                    //Reset fields
                    objectBytesIndex = 2;
                    objectBytes = null;

                    return result;
                    }
                    
                    case 2: //WRQ
                    decodeUpload();

                    case 6: //DIRQ
                    decodeFilesList();

                    case 7: //LOGRQ
                    decodeLogin();

                    case 8: //DELRQ
                    decodeDelete();

                    case 10: //DISC
                    decodeDisconnect();
                        
                        break;
                }

                    break;
            }
        
        }

    

    public byte[] decodeDownload(byte nextByte){
        objectBytes[0] = opCodeBytes[0]; objectBytes[1] = opCodeBytes[1]; //insert the opCode bytes inside the message
        if(nextByte == '0'){ //if we reached the end of the message
            objectBytes[objectBytesIndex] = nextByte; //insert 0 at the end and return the object

            return objectBytes; 
        }
          if (objectBytesIndex >= objectBytes.length) { //need more cells for the message
            objectBytes = Arrays.copyOf(objectBytes, objectBytesIndex * 2); // extends the byte array times 2
            objectBytes[objectBytesIndex] = nextByte;
            objectBytesIndex++;
        }
        return null;

    }


    public void decodeUpload(){

    }

    public void decodeFilesList(){

    }
    
    public void decodeLogin(){

    }

    public void  decodeDelete(){

    }

    public void decodeDisconnect(){

    }


    public byte[] encode(T message){


    }

    private Serializable deserializeObject(){
        try{
        ObjectInput in = new ObjectInputStream(new ByteArrayInputStream(objectBytes));
        return (Serializable) in.readObject();
        }
        catch(Exception ex){
            throw new IllegalArgumentException("cannot deserialze", ex);
        }

    }


}