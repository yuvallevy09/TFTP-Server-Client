package bgu.spl.net.impl.tftp;

import java.io.IOException;
import bgu.spl.net.srv.BlockingConnectionHandler;

public interface Connections<T> {

    void connect(int connectionId, BlockingConnectionHandler<T> handler);

    boolean send(int connectionId, T msg);

    void disconnect(int connectionId);
}
