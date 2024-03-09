package bgu.spl.net.impl.tftp.files;
import java.io.Serializable;
import bgu.spl.net.impl.rci.Command;

public class DownloadFileCommand<T> implements Command<Files<T>> {
 
    private String fileName;
 
    public DownloadFileCommand(String fileName) {
        this.fileName = fileName;
    }
 
    @Override
    public Serializable execute(Files<T> files) {
        return (Serializable) files.download(fileName);
    }
}