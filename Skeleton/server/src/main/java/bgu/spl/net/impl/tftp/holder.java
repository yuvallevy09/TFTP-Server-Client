package bgu.spl.net.impl.tftp;

import java.util.concurrent.ConcurrentHashMap;

public class holder {
    public static ConcurrentHashMap<Integer, String> ids_login = new ConcurrentHashMap<>(); //Holds all login id_s and username
}

