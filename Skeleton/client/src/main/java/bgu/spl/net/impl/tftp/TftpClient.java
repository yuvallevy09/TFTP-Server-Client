package bgu.spl.net.impl.tftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;


public class TftpClient<T> implements Closeable{
    //TODO: implement the main logic of the client, when using a thread per client the main logic goes here

    private final TftpClientEncDec encdec; 
    private final Socket sock;
    private final BufferedInputStream in;
    private final BufferedOutputStream out;
    private boolean shouldTerminate;
    private int pos;
    private byte[] sendingFile;
    private short block;
    private short expectedBlocks;
    private int blocksSent;
    private byte[] downloadFile;
    private String filesPath;

    //OpCode fields
    final short op_RRQ = 1; final short op_WRQ = 2; final short op_DATA = 3; final short op_ACK = 4; final short op_ERROR = 5;
    final short op_DIRQ = 6; final short op_LOGRQ = 7; final short op_DELRQ = 8; final short op_BCAST = 9; final short op_DISC = 10;

    public TftpClient(String host, int port) throws IOException {
        sock = new Socket(host, port);
        encdec = new TftpClientEncDec();
        in = new BufferedInputStream(sock.getInputStream());
        out = new BufferedOutputStream(sock.getOutputStream());
        shouldTerminate = false;
        pos = 0;
        block = 1;
        expectedBlocks = 0;
        blocksSent = 0;
        downloadFile = new byte[1<<10];
        Path p = Paths.get("");
        File dir = p.toFile();
        filesPath = dir.getAbsolutePath();
    }


    public void send(byte[] msg) throws IOException {
        byte[] encoded = encdec.encode(msg);
        String error = "Invalid Command";
        if (Arrays.equals(encoded, error.getBytes())){
            encdec.request = "";
            System.out.println(error);
            return;
        } else if(encdec.request.equals("RRQ ")){
            String pathName = filesPath + File.separator + encdec.downloadFileName;
            File f = new File(pathName);
            f.getParentFile().mkdirs(); 
            try {
                if (f.createNewFile()) { // returns true if file does not exist 
                    out.write(encoded);
                    synchronized (this) {
                        out.flush(); // send packet
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    System.out.println("file already exists");
                    encdec.request = ""; // finished handling command 
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        } else if (encdec.request.equals("WRQ ") ) {
            String pathName = filesPath + File.separator +  encdec.sendingFileName;
            if (new File(pathName).exists()){
                out.write(encoded);
                synchronized (this) {
                    out.flush(); // send packet
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println("file does not exist");
                encdec.request = ""; // finished handling command 
            }
            return;
        } else {
            out.write(encoded);
            synchronized (this) {
                out.flush(); // send packet
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void receive() throws IOException {
        int read;
        while ((read = in.read()) >= 0) {
            byte[] msg = encdec.decodeNextByte((byte) read);
            if (msg != null) {
                short opCode = (short)(((short)msg[0] & 0xFF)<<8|(short)(msg[1] & 0xFF));
                switch (opCode) {
                    case op_ACK:
                        short blockNum = (short)(((short)msg[2] & 0xFF)<<8|(short)(msg[3] & 0xFF));
                        if (encdec.request.equals("WRQ ")) {
                            if (blockNum == (short)0) { // prepare array of content and send first data pack
                                String pathName = filesPath + File.separator +  encdec.sendingFileName;
                                File f = new File(pathName);
                                try {
                                    sendingFile = Files.readAllBytes(f.toPath());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                sendNextPack();
                            } else if (pos < sendingFile.length){
                                sendNextPack();
                            } else {
                                block = 1; 
                                pos = 0;
                                encdec.request = "";
                                System.out.println("WRQ " + encdec.sendingFileName + " complete");
                                synchronized (this) {
                                    notify();
                                }
                            }
                        } else if (encdec.request.equals("DISC")) {
                            encdec.request = "";
                            shouldTerminate = true;
                            synchronized (this) {
                                notify();
                            }
                        } else {
                            encdec.request = "";
                            synchronized (this) {
                                notify();
                            }
                        }
                        System.out.println("ACK " + blockNum);
                        break;

                    case op_ERROR:
                        short errNum = (short)(((short)msg[2] & 0xFF)<<8|(short)(msg[3] & 0xFF));
                        String errMsg = "";
                        int length = msg.length - 5;
                        if (length > 0){
                            errMsg = new String(msg, 4, length, StandardCharsets.UTF_8);
                        }
                        System.out.println("Error " + errNum + " " + errMsg);
                        encdec.request = "";
                        synchronized (this) {
                            notify();
                        }
                        break;

                    case op_BCAST:
                        short added = ((short)msg[2]);
                        String str = "del";
                        if (added == 1) {
                            str = "add";
                        }
                        String fileName = new String(msg, 3, msg.length - 4, StandardCharsets.UTF_8);
                        System.out.println("BCAST " + str + " " + fileName);
                        break;

                    case op_DATA:
                        short packSize = (short)(((short)msg[2] & 0xFF)<<8|(short)(msg[3] & 0xFF));
                        short bNum = (short)(((short)msg[4] & 0xFF)<<8|(short)(msg[5] & 0xFF));
                        byte[] data = Arrays.copyOfRange(msg, 6, msg.length - 1);
            
                        if (packSize < 512){
                            expectedBlocks = bNum;
                        }
            
                        if (packSize + blocksSent*512 > downloadFile.length) { // in case uploadFile array is not big enough
                            System.out.println("resizing array");
                            byte[] temp = new byte[downloadFile.length*2];
                            System.arraycopy(downloadFile, 0, temp, 0, downloadFile.length);
                            downloadFile = temp;
                        }
            
                        // add data to downloadFile
                        System.arraycopy(data, 0, downloadFile, blocksSent*512, packSize);
                        blocksSent++;
            
                        // send ACK that data packet was received 
                        byte[] msgACK = packAck((short)blocksSent);
                        out.write(msgACK);
                        out.flush();
            
                        // if all the blocks were sent 
                        if (expectedBlocks == blocksSent) {
                            if (encdec.request.equals("RRQ ")) {
                                String pathName = filesPath + File.separator + encdec.downloadFileName;
                                // add content to new file 
                                try (FileOutputStream fos = new FileOutputStream(pathName)) {
                                    fos.write(downloadFile);
                                    fos.flush();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                System.out.println("RRQ " + encdec.downloadFileName + " complete");
                            } else if (encdec.request.equals("DIRQ")) {
                                String s = new String(downloadFile);
                                System.out.println(s);
                            } 
                            downloadFile = new byte[1<<10];
                            blocksSent = 0;
                            expectedBlocks = 0;
                            encdec.request = "";
                            synchronized (this) {
                                notify();
                            }
                        }
                        break;

                    default:
                    System.out.println("Unrecognized packet received");
                        break;
                }
            }
        }
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

    private void sendNextPack() {
        short packetSize;
        if (pos + 512 < sendingFile.length) {
            packetSize = 512;
        } else {
            packetSize =  (short) (sendingFile.length - pos);
        }
        byte[] msgDATA = new byte[6 + packetSize];
        msgDATA[0] = (byte) (op_DATA >> 8);
        msgDATA[1] = (byte) (op_DATA & 0xff);
        msgDATA[2] = (byte) (packetSize >> 8);
        msgDATA[3] = (byte) (packetSize & 0xff);
        msgDATA[4] = (byte) (block >> 8);
        msgDATA[5] = (byte) (block & 0xff);
        System.arraycopy(sendingFile, pos, msgDATA, 6 , packetSize); 
        System.out.println("sending data packet num: " + block ); //Flag
        try {
            out.write(msgDATA);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        pos += 512;
        block++;
    }
}