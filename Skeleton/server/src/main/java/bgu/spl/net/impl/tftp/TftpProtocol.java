package bgu.spl.net.impl.tftp;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

//Added by Tomer
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;



class holder{
    static ConcurrentHashMap<Integer, String> ids_login = new ConcurrentHashMap<>(); //Holds all login id_s and username
}


public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private int connectionId;
    private Connections<byte[]> connections;
    private boolean shouldTerminate;
    private short opCode;
    String directoryPath = "Skeleton/server/Files";
    public FileOperations files = new FileOperations(directoryPath); //path to Files folder inside the project
    private u

    //OpCode fields
    short op_RRQ = 1; short op_WRQ = 2; short op_DATA = 3; short op_ACK = 4; short op_ERROR = 5;
    short op_DIRQ = 6; short op_LOGRQ = 7; short op_DELRQ = 8; short op_BCAST = 9; short op_DISC = 10;
    
    

    @Override
    public void start(int _connectionId, Connections<byte[]> _connections) {
        // TODO implement this
        connectionId = _connectionId;
        connections = _connections;
        shouldTerminate = false;
    }

    @Override
    public void process(byte[] message) {
        // TODO implement this
        opCode = (short)(((short)message[0] & 0xFF)<<8|(short)(message[1] & 0xFF)); 
        if(opCode == op_LOGRQ){
            String username = new String(message, 2, message.length - 1, StandardCharsets.UTF_8);
            if(!holder.ids_login.containsValue(username)) { // if username does not exist 
                holder.ids_login.put(connectionId, username);    
                byte[] msgACK = new byte[4];
                msgACK[0] = (byte) (op_ACK >> 8);
                msgACK[1] = (byte) (op_ACK & 0xff);
                msgACK[2] = (byte) ((short) 0 >> 8 );
                msgACK[3] = (byte) ((short) 0 & 0xff);
                connections.send(connectionId, msgACK);
            } else {
                String error = "User already logged in" + '\0';
                byte[] errorByte = error.getBytes(); //uses utf8 by default
                byte[] msgERROR = new byte[4 + errorByte.length];
                msgERROR[0] = (byte) (op_ERROR >> 8);
                msgERROR[1] = (byte) (op_ERROR & 0xff);
                msgERROR[2] = (byte) (op_LOGRQ >> 8);
                msgERROR[3] = (byte) (op_LOGRQ & 0xff);

                System.arraycopy(errorByte, 0, msgERROR, 4 , errorByte.length); //Copies the error inside the error msg
                connections.send(connectionId, msgERROR);
            }
        }
        else if(opCode == op_DATA){
            byte[] data = Arrays.copyOfRange(message, 2, message.length -1);



        }
        else if(opCode == op_DELRQ){
            String filename = new String(message, 2, message.length - 1, StandardCharsets.UTF_8); // -1 beacuse last char is 0
                if(files.searchFile(filename)){ //check to see what happend if someone download the file at the moment
                    files.deleteFile(filename); //Delete file from directory // see if need to implemenet readers writers lock 
                    byte[] msgACK = new byte[4];
                    msgACK[0] = (byte) (op_ACK >> 8);
                    msgACK[1] = (byte) (op_ACK & 0xff);
                    msgACK[2] = (byte) ((short) 0 >> 8 );
                    msgACK[3] = (byte) ((short) 0 & 0xff);
                    connections.send(connectionId, msgACK); // send ack to client that sent request 

                    byte[] filenameBytes = filename.getBytes(); 
                    byte[] msgBCAST = new byte[3 + filename.length()];
                    msgBCAST[0] = (byte) (op_BCAST >> 8);
                    msgBCAST[1] = (byte) (op_BCAST & 0xff);
                    msgBCAST[2] = (byte) ((short) 0 >> 8);
                    System.arraycopy(filenameBytes, 0, msgBCAST, 3, filenameBytes.length);
                    for( Integer id : holder.ids_login.keySet()){
                        connections.send(id, msgBCAST); // sends the BCAST to all login clients
                    }
                } else {
                    String error = filename +  " not found" + '\0'; 
                    byte[] errorByte = error.getBytes(); //uses utf8 by default
                    byte[] msgERROR = new byte[4 + errorByte.length];
                    msgERROR[0] = (byte) (op_ERROR >> 8);
                    msgERROR[1] = (byte) (op_ERROR & 0xff);
                    msgERROR[2] = (byte) (op_DELRQ >> 8);
                    msgERROR[3] = (byte) (op_DELRQ & 0xff);

                    System.arraycopy(errorByte, 0, msgERROR, 4 , errorByte.length); //Copies the error inside the error msg
                    connections.send(connectionId, msgERROR);
                }  
        }
        else if(opCode == op_DIRQ){
            if(!files.isEmpty()){
                String filesList = files.getAllFiles();
                byte[] namesArr = filesList.getBytes();
                sendDataPackets(namesArr);
            }
            else
            {
                String error = "Nothing." + '\0';
                byte[] errorByte = error.getBytes(); //uses utf8 by default
                byte[] msgERROR = new byte[4 + errorByte.length];
                msgERROR[0] = (byte) (op_ERROR >> 8);
                msgERROR[1] = (byte) (op_ERROR & 0xff);
                msgERROR[2] = (byte) (op_DIRQ >> 8);
                msgERROR[3] = (byte) (op_DIRQ & 0xff);

                System.arraycopy(errorByte, 0, msgERROR, 4 , errorByte.length); 
                connections.send(connectionId, msgERROR);
            }

        }
        else if(opCode == op_RRQ){
            String filename = new String(message, 2, message.length - 1, StandardCharsets.UTF_8);
                if(files.searchFile(filename)){
                    sendDataPackets(files.readFile(filename)); // helper functions
                }
                else
                {
                    String error = "deleted created file." + '\0';
                    byte[] errorByte = error.getBytes(); //uses utf8 by default
                    byte[] msgERROR = new byte[4 + errorByte.length];
                    msgERROR[0] = (byte) (op_ERROR >> 8);
                    msgERROR[1] = (byte) (op_ERROR & 0xff);
                    msgERROR[2] = (byte) (op_RRQ >> 8);
                    msgERROR[3] = (byte) (op_RRQ & 0xff);

                    System.arraycopy(errorByte, 0, msgERROR, 4 , errorByte.length); 
                    connections.send(connectionId, msgERROR);
                }
            
        }
        else if(opCode == op_WRQ){
            String filename = new String(message, 2, message.length - 1, StandardCharsets.UTF_8);
                if(!files.searchFile(filename)){
                    byte[] msgACK = new byte[4];
                    msgACK[0] = (byte) (op_ACK >> 8);
                    msgACK[1] = (byte) (op_ACK & 0xff);
                    msgACK[2] = (byte) ((short) 0 >> 8 );
                    msgACK[3] = (byte) ((short) 0 & 0xff);
                    connections.send(connectionId, msgACK);
                    
                    //needs to move it inside else if block of opCode == op_DATA
                    byte[] uploadFile = Arrays.copyOfRange(message, 2, message.length -1);
                    files.writeFile(filename, uploadFile);
                    

                    byte[] filenameBytes = filename.getBytes();
                    byte[] msgBCAST = new byte[3 + filename.length()];
                    msgBCAST[0] = (byte) (op_BCAST >> 8);
                    msgBCAST[1] = (byte) (op_BCAST & 0xff);
                    msgBCAST[2] = (byte) ((short) 0 >> 8);
                    System.arraycopy(filenameBytes, 0, msgBCAST, 3, filenameBytes.length);
                    for( Integer id : holder.ids_login.keySet()){
                        connections.send(id, msgBCAST); // sends the BCAST to all login clients
                    }
                }
                else
                {
                    String error = "stop transfer." + '\0';
                    byte[] errorByte = error.getBytes(); //uses utf8 by default
                    byte[] msgERROR = new byte[4 + errorByte.length];
                    msgERROR[0] = (byte) (op_ERROR >> 8);
                    msgERROR[1] = (byte) (op_ERROR & 0xff);
                    msgERROR[2] = (byte) (op_WRQ >> 8);
                    msgERROR[3] = (byte) (op_WRQ & 0xff);
                    System.arraycopy(errorByte, 0, msgERROR, 4 , errorByte.length); 
                    connections.send(connectionId, msgERROR);
                
                
            }
            
        }
        else if(opCode == op_DISC)
        {
            String username = new String(message, 2, message.length, StandardCharsets.UTF_8);
            if(holder.ids_login.containsValue(username)){ // check if this condition is right 
                holder.ids_login.remove(connectionId); // check if possible to remove by Value and not by Key
                byte[] msgACK = new byte[4];
                msgACK[0] = (byte) (op_ACK >> 8);
                msgACK[1] = (byte) (op_ACK & 0xff);
                msgACK[2] = (byte) ((short) 0 >> 8 );
                msgACK[3] = (byte) ((short) 0 & 0xff);
                connections.send(connectionId, msgACK);
                connections.disconnect(connectionId);
                shouldTerminate = true;
                //needs to close the program for the client that asked to disconnect
            }
            else
            {
                String error = "Don’t exit the program." + '\0';
                byte[] errorByte = error.getBytes(); //uses utf8 by default
                byte[] msgERROR = new byte[4 + errorByte.length];
                msgERROR[0] = (byte) (op_ERROR >> 8);
                msgERROR[1] = (byte) (op_ERROR & 0xff);
                msgERROR[2] = (byte) (op_DISC >> 8);
                msgERROR[3] = (byte) (op_DISC & 0xff);
                System.arraycopy(errorByte, 0, msgERROR, 4 , errorByte.length); 
                connections.send(connectionId, msgERROR);
            }   
        }

    }

    @Override
    public boolean shouldTerminate() {
       return shouldTerminate;
    }


    public void sendDataPackets(byte[] file) {
        int pos = 0; 
        short blockNum = 0; // check that does not need to start at 1 if there's multiple packets
        short packetSize;
        while (pos < file.length) {
            if (pos + 512 < file.length) {
                packetSize = 512;
            } else {
                packetSize =  (short) (file.length - pos);
            }

            byte[] msgDATA = new byte[6 + packetSize];
            msgDATA[0] = (byte) (op_DATA >> 8);
            msgDATA[1] = (byte) (op_DATA & 0xff);
            msgDATA[2] = (byte) (packetSize >> 8);
            msgDATA[3] = (byte) (packetSize & 0xff);
            msgDATA[4] = (byte) (blockNum >> 8);
            msgDATA[5] = (byte) (blockNum & 0xff);
            System.arraycopy(file, pos, msgDATA, 6 , packetSize); 
            connections.send(connectionId, msgDATA);

            pos+=512;
            blockNum++;
        }
    }

    public byte[] unpackDataPacket() {
        

    }
    
}

