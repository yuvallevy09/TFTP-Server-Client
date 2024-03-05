package bgu.spl.net.api;

public class MessagingEncDecClass<T> implements MessageEncoderDecoder<T>{

    //Fields
    private final byte[] lengthBytes = new byte[4];
    private int lengthBytesIndex = 0;
    private final byte[] opCodeBytes = new byte[2];
    private int opCodeBytesIndex = 0;
    private byte[] objectBytes = null;
    private int objectBytesIndex = 0;

    //Methods
    public T decodeNextByte(byte nextByte){
        if(objectBytes == null){
            opCodeBytes[opCodeBytesIndex++] = nextByte;

            if(opCodeBytesIndex == opCodeBytes.length){
                switch (opCodeBytes[0]) {
                    case 0:
                    switch (opCodeBytes[1]) {
                        case 1: //RRQ
                        decodeDownload();
                        
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
        }

    }

    public void decodeDownload(){

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


}