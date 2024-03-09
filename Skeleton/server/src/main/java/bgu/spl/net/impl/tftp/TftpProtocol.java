package bgu.spl.net.impl.tftp;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.srv.Connections;

public class TftpProtocol implements BidiMessagingProtocol<byte[]>  {

    private int connectionId;
    private Connections<byte[]> connections;
    private boolean shouldTerminate = false;

    @Override
    public void start(int _connectionId, Connections<byte[]> _connections) {
        // TODO implement this
        connectionId = _connectionId;
        connections = _connections;
        connections.connect(connectionId, null);// needs to be here ???

       
    }

    @Override
    public void process(byte[] message) {
        // TODO implement this
        throw new UnsupportedOperationException("Unimplemented method 'process'");
    }

    @Override
    public boolean shouldTerminate() {
        // TODO implement this
       connections.disconnect(connectionId);
       //needs to remove here the client from the logged in list
       return shouldTerminate;
    } 


    
}
