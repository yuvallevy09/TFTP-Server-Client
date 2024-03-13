package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public class TftpClient<T> implements Closeable{
    //TODO: implement the main logic of the client, when using a thread per client the main logic goes here

    private final TftpClientEncDec encdec; 
    private final Socket sock;
    private final BufferedInputStream in;
    private final BufferedOutputStream out;
    private boolean shouldTerminate;
    private File cwd;

    //OpCode fields
    final short op_RRQ = 1; final short op_WRQ = 2; final short op_DATA = 3; final short op_ACK = 4; final short op_ERROR = 5;
    final short op_DIRQ = 6; final short op_LOGRQ = 7; final short op_DELRQ = 8; final short op_BCAST = 9; final short op_DISC = 10;

    public TftpClient(String host, int port) throws IOException {
        sock = new Socket(host, port);
        encdec = new TftpClientEncDec();
        in = new BufferedInputStream(sock.getInputStream());
        out = new BufferedOutputStream(sock.getOutputStream());
        shouldTerminate = false;
        cwd = new File("Skeleton/client");

    }


    // first need to encode msg, use request to decipher what actions are needed:
    // if rrq, check if file exists in cwd, if not create file in cwd and send request
        // if file exists then print ”file already exists” and don't send rrq
        // in receive (listening thread): if received error, print delete created file 
    // if wrq, check if file exists then send a WRQ packet
        // if does not exist, print to terminal ”file does not exists” and don’t send WRQ
    // else: simply send encoded message

    // need to restart request field after handling message


    public void send(byte[] msg) throws IOException {
        byte[] encoded = encdec.encode(msg);
        String error = "Invalid Command";
        if (encoded.equals(error.getBytes())){
            System.out.println(error);
            return;
        } else if(encdec.request == "RRQ "){
            String pathName = "Skeleton/client/" + encdec.downloadFileName;
            File f = new File(pathName);
            f.getParentFile().mkdirs(); 
            try {
                if (f.createNewFile()) { // returns true if file does not exist 
                    out.write(encoded);
                    out.flush(); // send packet
                } else {
                    System.out.println("file already exists");
                    encdec.request = ""; // finished handling command 
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        } else if (encdec.request == "WRQ ") {
            String pathName = "Skeleton/client/" + encdec.uploadFileName;
            if (new File(pathName).exists()){
                out.write(encoded);
                out.flush(); // send packet
            } else {
                System.out.println("file does not exist");
                encdec.request = ""; // finished handling command 
            }
            return;
        } else {
            out.write(encoded);
            out.flush(); // send packet
        }
    }

        // receive:
        // if data packet (rrq or dirq)
            // insert into uploadingFile and send ack with block number 
        // if ack: 
            // if bn == 1, prepare data packets of the sendingFile, sendNextPack
            // if bn > 1, sendNextPack
                // if bn == expected, print complete 
            // else tbd
            
        // // add content to new file 
        // try (FileOutputStream fos = new FileOutputStream(pathName)) {
        //     fos.write(uploadFile);
        //     fos.flush();
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }


    public void receive() throws IOException {
        int read;
        while ((read = in.read()) >= 0) {
            byte[] msg = encdec.decodeNextByte((byte) read);
            if (msg != null) {

                short opCode = (short)(((short)msg[0] & 0xFF)<<8|(short)(msg[1] & 0xFF));
            
                switch (opCode) {
                    case op_ACK:
                        short blockNum = (short)(((short)msg[2] & 0xFF)<<8|(short)(msg[3] & 0xFF));
                        System.out.println("ACK " + blockNum);
                        break;

                    case op_ERROR:
                        short errNum = (short)(((short)msg[2] & 0xFF)<<8|(short)(msg[3] & 0xFF));
                        String errMsg = "";
                        int length = msg.length - 4;
                        if (length > 0){
                            errMsg = new String(msg, 3, length, StandardCharsets.UTF_8);
                        }
                        System.out.println("Error " + errNum + " " + errMsg);
                        break;

                    case op_BCAST:
                        short added = (short)((short)msg[3]);
                        String str = "del";
                        if (added == 1) {
                            str = "add";
                        }
                        String fileName = new String(msg, 3, msg.length - 4, StandardCharsets.UTF_8);
                        System.out.println("BCAST " + str + " " + fileName);
                        break;

                    case op_DATA:
                        if (encdec.request == "RRQ") {
                            // save to file using file ops 
                        } 
                        else if (encdec.request == "DIRQ") {
                            // save to buffer
                        }
                        short blockN = (short)(((short)msg[4] & 0xFF)<<8|(short)(msg[5] & 0xFF));
                        byte[] msgACK = packAck(blockN);
                        out.write(msgACK);
                        out.flush();
                        break;

                    default:
                        break;
                }
            }
        }
        throw new IOException("disconnected before complete reading message");
    }

    @Override
    public void close() throws IOException {
        out.close();
        in.close();
        sock.close();
    }

    private byte[] packAck(short blockNum) {
        byte[] msgACK = new byte[4];
        msgACK[0] = (byte) (op_ACK >> 8);
        msgACK[1] = (byte) (op_ACK & 0xff);
        msgACK[2] = (byte) (blockNum >> 8);
        msgACK[3] = (byte) (blockNum & 0xff);
        return msgACK;
    }

    public boolean shouldTerminate() {
        return shouldTerminate;
    }
    
}