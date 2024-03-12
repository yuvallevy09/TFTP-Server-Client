package bgu.spl.net.srv;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.impl.tftp.TftpEncoderDecoder;
import bgu.spl.net.impl.tftp.TftpProtocol;
import bgu.spl.net.impl.tftp.holder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BlockingConnectionHandler<T> implements Runnable, ConnectionHandler<T> {

    private final BidiMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    public boolean shouldFinish;
    int numOfLoops = 0; // Flag
    //public ConcurrentLinkedQueue<T> responses;

    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<T> reader, BidiMessagingProtocol<T> protocol) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
        this.shouldFinish = false;
    }

    @Override
    public void run() {
        try{ 
            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream()); 
            int read;
            System.out.println("entered blocking connection handler");
            

            while (!shouldFinish && !protocol.shouldTerminate() && (read = in.read()) >= 0) {
                System.out.println("Decoding byte num " + numOfLoops);
                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    String msg = new String((byte[]) nextMessage, StandardCharsets.UTF_8);
                    // System.out.println("message was decoded as: " + msg);
                    protocol.process(nextMessage);
                    // System.out.println("message was proccessed, back to BlockingCH while loop");
                    numOfLoops = -1;
                }
                numOfLoops++;
            }
            System.out.println("finished, preparing to close");
            close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        sock.close();
        in.close();
        out.close();
    }

    @Override
    public void send(T msg) {
        try {
            //T response = (T) responses.poll(); //needs to insert somewhere in the process/encode the message recieved inside this Q
            if (msg != null) {
                synchronized (this){
                    System.out.println("writing to buffer of  (fill in later)");
                    out.write(encdec.encode(msg));
                    out.flush();
                    System.out.println("message sent!");
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
