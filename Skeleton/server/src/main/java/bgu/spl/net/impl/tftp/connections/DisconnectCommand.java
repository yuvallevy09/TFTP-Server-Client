package bgu.spl.net.impl.tftp.connections;
import java.io.Serializable;
import bgu.spl.net.impl.rci.Command;
import bgu.spl.net.impl.tftp.Connections;


public class DisconnectCommand<T> implements Command<Connections<T>> {
 
    private int connectionId;
 
    public DisconnectCommand(int connectionId) {
        this.connectionId = connectionId;
    }
 
    @Override
    public Serializable execute(Connections<T> connections) {
        connections.disconnect(connectionId);
        return "OK";
    }
}