package bgu.spl.net.impl.tftp.files;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Files<T> {

    private ConcurrentHashMap<String, T> files = new ConcurrentHashMap<>();
    private ArrayList<String> list = new ArrayList<>(); // need to synchronize

    public T download(String fileName) {
        return files.get(fileName);
    }

    public void upload(String fileName, T file) {
        files.put(fileName, file);
    }

    public void delete(String fileName) {
        files.remove(fileName);
    }

    public ArrayList<String> listFiles() {
        return list;
    }

}