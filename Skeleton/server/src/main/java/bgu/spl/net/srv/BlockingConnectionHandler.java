package bgu.spl.net.srv;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
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
                System.out.println("loop number " + numOfLoops);
                System.out.println("before decoding");
                T nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null) {
                    System.out.println("decode was a success");
                    protocol.process(nextMessage);
                    System.out.println("finished with the protocol, back at BlockingCH class");
                    numOfLoops++;
                }
            }
            System.out.println("finished the loop, preparing to close");
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
                    System.out.println("reached send method of connection handler");
                    out.write(encdec.encode(msg));
                    System.out.println("encoded the message successfully");
                    out.flush();
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

}
