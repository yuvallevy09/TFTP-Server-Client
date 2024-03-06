package bgu.spl.net.impl.tftp;

import java.io.IOException;

public interface Connections<T> {

    void connect(int connectionId, ConnectionHandler<T> handler);

    boolean send(int connectionId, T msg);

    void disconnect(int connectionId);
}
