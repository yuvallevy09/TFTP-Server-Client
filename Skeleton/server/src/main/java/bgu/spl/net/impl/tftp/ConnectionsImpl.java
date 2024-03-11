package bgu.spl.net.impl.tftp;
import java.util.concurrent.ConcurrentHashMap;

import bgu.spl.net.srv.BlockingConnectionHandler;

public class ConnectionsImpl<T> implements Connections<T> {

    public ConcurrentHashMap<Integer, BlockingConnectionHandler<T>> connections = new ConcurrentHashMap<>();

    @Override
    public
    void disconnect(int connectionId){
        connections.remove(connectionId);
        holder.ids_login.remove(connectionId);
    }

    @Override
    public void connect(int connectionId, BlockingConnectionHandler<T> handler) {
        connections.put(connectionId, handler);    
    }

    @Override
    public boolean send(int connectionId, T msg) {
        //connections.get(connectionId).responses.add(msg);
        connections.get(connectionId).send(msg);
        return true;
    }
}
