import java.util.*; 

public class FileSystem {
    private SuperBlock sb; 
    private Directory dir; 
    private FileTable ft; 

    public FileSystem() {

    }

    public boolean format (int files) {

    } 

    
    public FileTableEntry open (String filename, String mode) {
        FileTableEntry ftEnt = ft.falloc(filename, mode); 
        if (mode.equals("w")) {
            if (deallocAllBlocks(ftEnt) == false) {
                return null;
            }
        }
        return ftEnt; 
    }
    public int read(FileTableEntry ftEnt, byte[] buffer) { 
        // need to do 
    }

    public int write(FileTableEntry ftEnt, byte[] buffer) { 
        // need to do 
    }

    public int seek(FileTableEntry ftEnt, byte[] buffer) { 
        // need to do 
    }

    public int close(int fd) { 
        // need to do 
    }

    public int delete(String fileName) { 
        // need to do 
    }

    public int fsize(int fd) { 
        // need to do 
    }
}
