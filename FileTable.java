import java.util.*;

public class FileTable {
    private Vector table;                           // the actual entity of this file table
    private Directory dir;                          // the root directory
    int UNUSED = 0; 
    int USED = 1; 
    int READ = 2; 
    int WRITE = 3; 
    int TODELETE = 4; 
    public FileTable( Directory directory ) {       // constructor
        table = new Vector( );                      // instantiate a file (structure) table
        dir = directory;                            // receive a reference to the Director
    }                                               // from the file system

    // major public methods
    public synchronized FileTableEntry falloc( String filename, String mode ) {
        short iNumber = -1; 
        Inode inode = null; 
        while (true) {
            iNumber = (filename.equals("/") ? 0 : dir.namei(filename)); 
            if (iNumber >= 0) {
                inode = new Inode(iNumber); 
                if (mode.compareTo("r") == 0) {                         // if mode == read 
                    if (inode.flag == UNUSED || inode.flag == USED) {   // check if used to unused
                        inode.flag = (short)READ;                       // if so then set to read and break 
                        break; 
                    }
                    else if (inode.flag <= READ) {                      // if flag is read then just break 
                        break; 
                    } else if (inode.flag == WRITE) {                   // if flag is write we need to wait 
                        try { wait(); } catch(InterruptedException e) {} 
                    } else if (inode.flag == TODELETE) {                // if flag is todelete then set inum to -1 and return null 
                        iNumber = -1; 
                        return null; 
                    }
                } else if (mode.compareTo("w") == 0||                   // if mode is w, w+, a
                mode.compareTo("w+") == 0 || mode.compareTo("a") == 0) {
                    if (inode.flag == USED || inode.flag == UNUSED) {   // if flag is unused or used set to write then break; 
                        inode.flag = (short)WRITE; 
                        break; 
                    } else {                                            // else we just wait 
                        try { wait(); } catch(InterruptedException e) {} 
                    }
                }
            } else if (! (mode.compareTo("r") == 0)) {                  // if inode is less than 0, file doesnt exist 
                iNumber = dir.ialloc(filename);                         // allocate inum
                inode = new Inode();                                    // make node for file 
                inode.flag = (short)WRITE;                              // set flag to write 
                break; 
            } else {
                return null; 
            }
        }
        inode.count++;                                                  // increment count
        inode.toDisk(iNumber);                                          // add to disk 
        FileTableEntry e = new FileTableEntry(inode, iNumber, mode);    // make new ftent
        table.addElement(e);                                            // add to table 
        return e;                                                       // return ftent
    }

    public synchronized boolean ffree( FileTableEntry e ) {
        if (table.contains(e) == true) {                                // check if table contains ftent
            e.inode.flag = (short)UNUSED;                               // if so set to unused 
            e.inode.toDisk(e.iNumber);                                  // add to disk the inum
            table.remove(e);                                            // remove from table
            notifyAll();                                                // notify all to wake up all threads waiting on this
            return true;
        }
        return false; 
    }

    public synchronized boolean fempty( ) {
        return table.isEmpty( ); // return if table is empty
    }
}