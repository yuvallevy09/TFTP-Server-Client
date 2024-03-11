package bgu.spl.net.impl.tftp;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

//Added by Tomer
// import java.io.IOException;
// import java.nio.file.Files;
// import java.nio.file.Path;
// import java.nio.file.Paths;
// import java.util.stream.Stream;



class holder{
    public static ConcurrentHashMap<Integer, String> ids_login = new ConcurrentHashMap<>(); //Holds all login id_s and username
}


public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private int connectionId;
    private ConnectionsImpl<byte[]> connections;
    private boolean shouldTerminate;
    private short opCode;
    String directoryPath = "Skeleton/server/Files";
    public FileOperations files = new FileOperations(directoryPath); //path to Files folder inside the project
    private byte[] uploadFile;
    private int blocksSent;
    private String uploadingFileName;
    private int expectedBlocks;
    private boolean loggedIn;

    //OpCode fields
    short op_RRQ = 1; short op_WRQ = 2; short op_DATA = 3; short op_ACK = 4; short op_ERROR = 5;
    short op_DIRQ = 6; short op_LOGRQ = 7; short op_DELRQ = 8; short op_BCAST = 9; short op_DISC = 10;
    
    

    @Override
    public void start(int _connectionId, ConnectionsImpl<byte[]> _connections) {
        // TODO implement this
        connectionId = _connectionId;
        connections = _connections;
        shouldTerminate = false;
        uploadFile = new byte[1<<10];
        blocksSent = 0;
        expectedBlocks = 0;
        loggedIn = false;
    }

    @Override
    public void process(byte[] message) {
        // TODO implement this
        opCode = (short)(((short)message[0] & 0xFF)<<8|(short)(message[1] & 0xFF)); 
        if(opCode == op_LOGRQ){
            String username = new String(message, 2, message.length - 1, StandardCharsets.UTF_8);
            if(!loggedIn) { // if username does not exist 
                holder.ids_login.put(connectionId, username);    
                loggedIn = true;
                byte[] msgACK = packAck((short)0);
                connections.send(connectionId, msgACK);
            } else {
                String error = "User already logged in" + '\0';
                byte[] msgERROR = packError(error);
                connections.send(connectionId, msgERROR);
            }
        }
        else if(!loggedIn){
            String error = "User not logged in" + '\0';
            byte[] msgERROR = packError(error);
            connections.send(connectionId, msgERROR);
        }
        else if(opCode == op_DATA){
            short packSize = (short)(((short)message[2] & 0xFF)<<8|(short)(message[3] & 0xFF));
            short blockNum = (short)(((short)message[4] & 0xFF)<<8|(short)(message[5] & 0xFF));
            byte[] data = Arrays.copyOfRange(message, 6, 6+packSize);

            if (packSize < 512){
                expectedBlocks = blockNum;
            }

            if (packSize + blocksSent*512 > uploadFile.length){ // in case uploadFile array is not big enough
                byte[] temp = new byte[uploadFile.length*2];
                System.arraycopy(uploadFile, 0, temp, 0, uploadFile.length);
                uploadFile = temp;
            }

            // add data to uploadFile
            System.arraycopy(data, 0, uploadFile, blocksSent*512, packSize);
            blocksSent++;

            // send ACK that data packet was received 
            byte[] msgACK = packAck(blockNum);
            connections.send(connectionId, msgACK);

            // if all the blocks were sent 
            if (expectedBlocks == blocksSent){
                files.writeFile(uploadingFileName, uploadFile);
                uploadFile = new byte[1<<10];
                blocksSent = 0;
                expectedBlocks = 0;
                byte[] filenameBytes = uploadingFileName.getBytes();
                byte[] msgBCAST = new byte[3 + filenameBytes.length];
                msgBCAST[0] = (byte) (op_BCAST >> 8);
                msgBCAST[1] = (byte) (op_BCAST & 0xff);
                msgBCAST[2] = (byte) ((short) 0 >> 8);
                System.arraycopy(filenameBytes, 0, msgBCAST, 3, filenameBytes.length);
                for( Integer id : holder.ids_login.keySet()){
                    connections.send(id, msgBCAST); // sends the BCAST to all login clients
                }
            }
        }
        else if(opCode == op_DELRQ){
            String filename = new String(message, 2, message.length - 1, StandardCharsets.UTF_8); // -1 beacuse last char is 0
                if(files.searchFile(filename)){ //check to see what happend if someone download the file at the moment
                    files.deleteFile(filename); //Delete file from directory // see if need to implemenet readers writers lock 
                    byte[] msgACK = packAck((short)0);
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
                    byte[] msgERROR = packError(error);
                    connections.send(connectionId, msgERROR);
                }  
        }
        else if(opCode == op_DIRQ){
            if(!files.isEmpty()){
                String filesList = files.getAllFiles();
                byte[] namesArr = filesList.getBytes();
                sendDataPackets(namesArr);
            } else {
                String error = "Nothing." + '\0';
                byte[] msgERROR = packError(error);
                connections.send(connectionId, msgERROR);
            }
        }
        else if(opCode == op_RRQ){ //Download
            String filename = new String(message, 2, message.length - 1, StandardCharsets.UTF_8);
            if(files.searchFile(filename)){
                sendDataPackets(files.readFile(filename)); // helper functions
            } else {
                String error = "deleted created file." + '\0';
                byte[] msgERROR = packError(error);
                connections.send(connectionId, msgERROR);
            }
        }
        else if(opCode == op_WRQ){ //Upload
            String filename = new String(message, 2, message.length - 1, StandardCharsets.UTF_8);
            if(!files.searchFile(filename)){
                byte[] msgACK = packAck((short) 0);
                connections.send(connectionId, msgACK);
                uploadingFileName = filename;
            } else {
                String error = "stop transfer." + '\0';
                byte[] msgERROR = packError(error); 
                connections.send(connectionId, msgERROR);
            }
        }
        else if(opCode == op_DISC) {   
            if(loggedIn){ // check if this condition is right 
                byte[] msgACK = packAck((short) 0);
                shouldTerminate = true;
                connections.send(connectionId, msgACK);
                connections.disconnect(connectionId);
            } else { // check this 
                String error = "Don't exit the program." + '\0';
                byte[] msgERROR = packError(error); 
                connections.send(connectionId, msgERROR);
            }   
        } else {
            String error = "Illegal TFTP Operation - Unknown Opcode" + '\0';
            byte[] msgERROR = packError(error);
            connections.send(connectionId, msgERROR);
        }
    }

    @Override
    public boolean shouldTerminate() {
       return shouldTerminate;
    }


    private byte[] packError(String error) {
        byte[] errorByte = error.getBytes(); //uses utf8 by default
        byte[] msgERROR = new byte[4 + errorByte.length];
        msgERROR[0] = (byte) (op_ERROR >> 8);
        msgERROR[1] = (byte) (op_ERROR & 0xff);
        msgERROR[2] = (byte) (opCode >> 8);
        msgERROR[3] = (byte) (opCode & 0xff);
        System.arraycopy(errorByte, 0, msgERROR, 4 , errorByte.length); 
        return msgERROR;
    }

    private byte[] packAck(short blockNum) {
        byte[] msgACK = new byte[4];
        msgACK[0] = (byte) (op_ACK >> 8);
        msgACK[1] = (byte) (op_ACK & 0xff);
        msgACK[2] = (byte) (blockNum >> 8 );
        msgACK[3] = (byte) (blockNum & 0xff);
        return msgACK;
    }

    private void sendDataPackets(byte[] file) {
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
    
            pos += 512;
            blockNum++;
        }
    }

    public void terminate() {
        shouldTerminate = true;
    }
}