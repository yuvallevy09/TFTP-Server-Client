package bgu.spl.net.impl.tftp.connections;
import java.io.Serializable;
import bgu.spl.net.impl.rci.Command;
import bgu.spl.net.impl.tftp.ConnectionHandler;
import bgu.spl.net.impl.tftp.Connections;


public class ConnectCommand<T> implements Command<Connections<T>> {
 
    private int connectionId;
    private ConnectionHandler<T> handler;
 
    public ConnectCommand(int connectionId, ConnectionHandler<T> handler) {
        this.connectionId = connectionId;
        this.handler = handler;
    }
 
    @Override
    public Serializable execute(Connections<T> connections) {
        connections.connect(connectionId, handler);
        return "OK";
    }
}