package bgu.spl.net.impl.tftp;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionsImpl implements Connections<ConnectionHandler<T>> {

    private ConcurrentHashMap<Integer, ConnectionHandler<?>> connections = new ConcurrentHashMap<>();
    

    @Override
    void connect(int connectionId, ConnectionHandler<?> handler){
        connections.put(connectionId, handler);
    }

    @Override
    boolean send(int connectionId, T msg){

    }

    @Override
    void disconnect(int connectionId){
        connections.remove(connectionId);
    }
}
