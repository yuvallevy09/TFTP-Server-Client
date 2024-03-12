package bgu.spl.net.impl.tftp;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

public class holder {
    public static ConcurrentHashMap<Integer, String> ids_login = new ConcurrentHashMap<>(); //Holds all login id_s and username
    public static ConcurrentHashMap<String, File> filesMap = new ConcurrentHashMap<>(); //Holds all files 
}

