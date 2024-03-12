package bgu.spl.net.srv;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.impl.tftp.TftpProtocol;
import bgu.spl.net.impl.tftp.holder;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.function.Supplier;

public abstract class BaseServer<T> implements Server<T> {

    private final int port;
    private final Supplier<BidiMessagingProtocol<T>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<T>> encdecFactory;
    private ServerSocket sock;
    private final ConnectionsImpl<T> connections;
    private int idCounter;
    int numOfAccepts = 0; //Flag 

    public BaseServer(
            int port,
            Supplier<BidiMessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> encdecFactory) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
		this.sock = null;
        connections = new ConnectionsImpl<T>();
        idCounter = 0;
       
    }

    @Override
    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
			System.out.println("Server started");
            
            //put all existing files in server on filesMap
            File dir = new File("Skeleton/server/Files");
            File[] directoryListing = dir.listFiles();
            if (directoryListing != null) {
                for (File f : directoryListing) {
                    holder.filesMap.put(f.getName(), f);
                }
            }

            this.sock = serverSock; //just to be able to close

            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSock = serverSock.accept();
                System.out.println("num Of Accepts" + numOfAccepts);
                BidiMessagingProtocol<T> protocol = protocolFactory.get();

                BlockingConnectionHandler<T> handler = new BlockingConnectionHandler<>(
                        clientSock,
                        encdecFactory.get(),
                        protocol);
                connections.connect(idCounter, handler);
                protocol.start(idCounter++, connections);
                execute(handler);
                System.out.println("handler thread started");
                numOfAccepts++;
            }
        } catch (IOException ex) {
        }
        
        
        System.out.println("server closed!!!");
    }

    @Override
    public void close() throws IOException {
		if (sock != null)
			sock.close();
            for (Map.Entry<Integer, BlockingConnectionHandler<T>> entry : connections.connections.entrySet()) {
                BlockingConnectionHandler<T> handler = entry.getValue();
                // close all connection handlers and disconnect all clients from server
                connections.disconnect(entry.getKey());
                handler.shouldFinish = true;
            }
        }


    protected abstract void execute(BlockingConnectionHandler<T>  handler);

}
