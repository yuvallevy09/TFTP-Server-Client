package bgu.spl.net.impl.tftp;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;


class holder{
    static ConcurrentHashMap<Integer, String> ids_login = new ConcurrentHashMap<>(); //Holds all login id_s and username
}


public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private int connectionId;
    private Connections<byte[]> connections;
    private boolean shouldTerminate;
    private short opCode;
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

            String username = new String(message, 2, message.length, StandardCharsets.UTF_8);
            if(!holder.ids_login.containsValue(username)) {
                holder.ids_login.put(connectionId, username);    
                byte[] msgACK = new byte[4];
                msgACK[0] = (byte) (op_ACK >> 8);
                msgACK[1] = (byte) (op_ACK & 0xff);
                msgACK[2] = (byte) ((short) 0 >> 8 );
                msgACK[3] = (byte) ((short) 0 & 0xff);
                connections.send(connectionId, msgACK);
            }
            else
            {
                String error = "User already logged in" + '0';
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
        else if(opCode == op_DELRQ){
            // chek if file exist, if so delete it and send ACK to client and  BCAST to all, else send ERROR to client
            //if file is not in files
            String filename = new String(message, 2, message.length, StandardCharsets.UTF_8);

            if()// needs access to the Files Directory here
            ;
            else
            {
                String error = filename +  " not found" + '0';
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
        
        


    }

    @Override
    public boolean shouldTerminate() {
        // TODO implement this
        
       connections.disconnect(connectionId); //needs to happend in DISC process
       //needs to remove here the client from the logged in list
       return shouldTerminate;
    } 


    
}
