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
                if (mode.compareTo("r") == 0) {  // if mode == read 
                    if (inode.flag == READ) {
                        break; 
                    } else if (inode.flag == WRITE) {
                        try { wait(); } catch(InterruptedException e) { return null; } return null; 
                    } else if (inode.flag == TODELETE) { 
                        iNumber = -1; 
                        return null; 
                    }
                } else {    // if mode is w, w+, a
                    if (inode.flag == USED || inode.flag == UNUSED) { 
                        inode.flag = (short)WRITE; 
                        break; 
                    } else { 
                        try { wait(); } catch(InterruptedException e) { return null; } return null; 
                    }
                }
            } else { // if inode is less than 0, file doesnt exist 
                iNumber = dir.ialloc(filename); // allocate inum
                inode = new Inode();            // make node for file 
                break; 
            }

        }
        inode.count++; 
        inode.toDisk(iNumber); 
        FileTableEntry e = new FileTableEntry(inode, iNumber, mode); 
        table.addElement(e); 
        return e; 
    // allocate a new file (structure) table entry for this file name
    // allocate/retrieve and register the corresponding inode using dir
    // increment this inode's count
    // immediately write back this inode to the disk
    // return a reference to this file (structure) table entry
    }

    public synchronized boolean ffree( FileTableEntry e ) {
        if (table.contains(e) == true) {
            e.inode.flag = (short)UNUSED; 
            e.inode.toDisk(e.iNumber);
            table.remove(e); 
            notifyAll();
            return true;
        }
        return false; 
    // receive a file table entry reference
    // save the corresponding inode to the disk
    // free this file table entry.
    // return true if this file table entry found in my table
    }

    public synchronized boolean fempty( ) {
        return table.isEmpty( ); // return if table is empty
    } // should be called before starting a format
}