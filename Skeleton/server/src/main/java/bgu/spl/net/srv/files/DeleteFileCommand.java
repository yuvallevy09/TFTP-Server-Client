import bgu.spl.net.impl.rci.Command; // fix this after moving to desired location
import java.io.Serializable;


public class DeleteFileCommand implements Command<Files> {
 
    private String fileName;
 
    public DeleteFileCommand(String fileName) {
        this.fileName = fileName;
    }
 
    @Override
    public Serializable execute(Files files) {
        files.delete(fileName);
    }
}