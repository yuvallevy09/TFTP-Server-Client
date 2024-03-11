package bgu.spl.net.srv;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public interface Connections<T> {

    //public ConcurrentHashMap<Integer, ConnectionHandler<byte[]>> connections = new ConcurrentHashMap<>();

	void connect(int connectionId, BlockingConnectionHandler<T> handler);

    boolean send(int connectionId, T msg);

    void disconnect(int connectionId);
}
