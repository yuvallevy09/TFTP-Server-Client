package bgu.spl.net.impl.tftp.connections;
import java.io.Serializable;
import bgu.spl.net.impl.rci.Command;
import bgu.spl.net.impl.tftp.Connections;


public class SendCommand<T> implements Command<Connections<T>> {
 
    private int connectionId;
    private T msg;
 
    public SendCommand(int connectionId, T msg) {
        this.connectionId = connectionId;
    }
 
    @Override
    public Serializable execute(Connections<T> connections) {
        connections.send(connectionId, msg);
        return "OK";
    }
}