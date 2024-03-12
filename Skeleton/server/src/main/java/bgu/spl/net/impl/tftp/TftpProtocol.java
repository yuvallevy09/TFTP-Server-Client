package bgu.spl.net.impl.tftp;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.ConnectionsImpl;


public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private int connectionId;
    private ConnectionsImpl<byte[]> connections;
    private boolean shouldTerminate;
    private short opCode;
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
        System.out.println("protocol of "+ connectionId + " started"); //Flag
    }

    @Override
    public void process(byte[] message){
        // TODO implement this
        opCode = (short)(((short)message[0] & 0xFF)<<8|(short)(message[1] & 0xFF)); 
        if(opCode == op_LOGRQ){
            System.out.println("enter LOGRQ block"); //Flag
            String username = new String(message, 2, message.length - 3, StandardCharsets.UTF_8);
            System.out.println( "username received as: " + username); //Flag
            if(!loggedIn) { // if username does not exist 
                holder.ids_login.put(connectionId, username); 
                System.out.println(username + " was inserted to the ids login"); //Flag  
                loggedIn = true;
                byte[] msgACK = packAck((short)0);
                String ack = new String(msgACK, StandardCharsets.UTF_8);
                System.out.println("msgACK created as: " + ack); //Flag
                connections.send(connectionId, msgACK);
            } else {
                String error = "User already logged in" + '\0';
                byte[] msgERROR = packError(error);
                connections.send(connectionId, msgERROR);
            }
        }
        else if ( 10 < opCode | 1 > opCode ){
            System.out.println("enter invalid opcode block"); //Flag
            String error = "Illegal TFTP Operation - Unknown Opcode" + '\0';
            byte[] msgERROR = packError(error);
            connections.send(connectionId, msgERROR);
        }
        else if(!loggedIn){
            System.out.println("enter not logged in block"); //Flag
            String error = "User not logged in" + '\0';
            byte[] msgERROR = packError(error);
            connections.send(connectionId, msgERROR);
        }
        else if(opCode == op_DATA){
            System.out.println("enter DATA block"); //Flag
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
                // create new file in Files folder
                String pathName = "Skeleton/server/Files/" + uploadingFileName;
                File f = new File(pathName);
                f.getParentFile().mkdirs(); 
                try {
                    f.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // add content to new file 
                try (FileOutputStream fos = new FileOutputStream(pathName)) {
                    fos.write(uploadFile);
                    fos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                holder.filesMap.put(uploadingFileName, f);
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
            System.out.println("enter DELRQ block"); //Flag
            String filename = new String(message, 2, message.length - 3, StandardCharsets.UTF_8); 
            if(holder.filesMap.containsKey(filename)){ //check to see what happend if someone download the file at the moment
                File f = holder.filesMap.remove(filename);
                try {
                Files.delete(f.toPath());
                System.out.println("file " + filename + "deleted!");
                } catch (NoSuchFileException x) {
                    System.err.format("%s: no such" + " file or directory%n", f.toPath());
                } catch (DirectoryNotEmptyException x) {
                    System.err.format("%s not empty%n", f.toPath());
                } catch (IOException x) {
                    // File permission problems are caught here.
                    System.err.println(x);
                }
                byte[] msgACK = packAck((short)0);
                String ack = new String(msgACK, StandardCharsets.UTF_8); //Flag
                System.out.println("msgACK created as: " + ack); //Flag
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
            System.out.println("enter DIRQ block"); //Flag
            
            if(!holder.filesMap.isEmpty()){
                // iterate on hashmap
                String filesList = new String();
                for(String filename : holder.filesMap.keySet()){
                    filesList+= filename.concat("\0");
                } 
                byte[] namesArr = filesList.getBytes();
                sendDataPackets(namesArr);
            } else {
                String error = "Nothing." + '\0';
                byte[] msgERROR = packError(error);
                connections.send(connectionId, msgERROR);
            }
        }
        else if(opCode == op_RRQ){ //Download
            System.out.println("enter RRQ block"); //Flag
            String filename = new String(message, 2, message.length - 3, StandardCharsets.UTF_8);
            if(holder.filesMap.containsKey(filename)){
                System.out.println(filename + " found in filesMap"); // Flag
                File f = holder.filesMap.get(filename);
                byte[] fileBytes = null;
                try {
                    fileBytes = Files.readAllBytes(f.toPath());
                    System.out.println(f.getName() + " converted to byte array"); //Flag
                } catch (IOException e) {
                    e.printStackTrace();
                }
                sendDataPackets(fileBytes); // helper functions
            } else {
                System.out.println(filename + " not found in map"); //Flag
                String error = "deleted created file." + '\0';
                byte[] msgERROR = packError(error);
                connections.send(connectionId, msgERROR);
            }
        }
        else if(opCode == op_WRQ){ //Upload
            System.out.println("enter WRQ block"); //Flag
            String filename = new String(message, 2, message.length - 3, StandardCharsets.UTF_8);
            if(!holder.filesMap.containsKey(filename)){
                byte[] msgACK = packAck((short) 0);
                connections.send(connectionId, msgACK);
                String ack = new String(msgACK, StandardCharsets.UTF_8); //Flag
                System.out.println("msgACK created as: " + ack); //Flag
                uploadingFileName = filename;
            } else {
                String error = "stop transfer." + '\0';
                byte[] msgERROR = packError(error); 
                connections.send(connectionId, msgERROR);
            }
        }
        else if(opCode == op_DISC) {  
            System.out.println("enter DISC block"); //Flag
            if(loggedIn){ // check if this condition is right 
                byte[] msgACK = packAck((short) 0);
                String ack = new String(msgACK, StandardCharsets.UTF_8); //Flag
                System.out.println("msgACK created as: " + ack); //Flag
                shouldTerminate = true;
                connections.send(connectionId, msgACK);
                connections.disconnect(connectionId);
            } else { // check this 
                String error = "Don't exit the program." + '\0';
                byte[] msgERROR = packError(error); 
                connections.send(connectionId, msgERROR);
            }   
        } 
        System.out.println("reached end of protocol"); //Flag
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
        System.out.println("constructing data packets"); //Flag

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
            System.out.println("sending data packet num: " + blockNum ); //Flag
            connections.send(connectionId, msgDATA);
    
            pos += 512;
            blockNum++;
        }
        System.out.println("all data packets were sent"); //Flag
    }

}