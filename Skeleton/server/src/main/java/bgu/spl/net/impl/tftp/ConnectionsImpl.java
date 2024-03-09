package bgu.spl.net.impl.tftp;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl<T> implements Connections<T> {

    public ConcurrentHashMap<Integer, ConnectionHandler<T>> connections = new ConcurrentHashMap<>();

    @Override
    public
    void disconnect(int connectionId){
        connections.remove(connectionId);
    }

    @Override
    public void connect(int connectionId, ConnectionHandler<T> handler) {
        connections.put(connectionId, handler);    
    }

    @Override
    public boolean send(int connectionId, T msg) {
        connections.get(connectionId).send(msg);
        return true;
    }
}
