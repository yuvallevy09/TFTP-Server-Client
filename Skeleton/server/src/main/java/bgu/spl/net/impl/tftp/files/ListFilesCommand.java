package bgu.spl.net.impl.tftp.files;
import java.io.Serializable;
import bgu.spl.net.impl.rci.Command;

public class ListFilesCommand<T> implements Command<Files<T>> {
 
    public ListFilesCommand() {}
 
    @Override
    public Serializable execute(Files<T> files) {
        return files.listFiles();
    }
}