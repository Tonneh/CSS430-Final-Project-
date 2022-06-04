import java.util.*;

public class FileTable {
    private Vector table;                           // the actual entity of this file table
    private Directory dir;                          // the root directory
    public FileTable( Directory directory ) {       // constructor
        table = new Vector( );                      // instantiate a file (structure) table
        dir = directory;                            // receive a reference to the Director
    }                                               // from the file system

    // major public methods
    public synchronized FileTableEntry falloc( String filename, String mode ) {

        short iNumber = -1; 
        Inode inode = null; 
        while (true) {
            iNumber = (filename.equals("/") ? 0 : dir.namei (filename)); 
            if (iNumber >= 0) {
                inode = new Inode(iNumber); 
                if (mode.equals("r")) { 
                    if (inode.flag == 2) {
                        break; 
                    } else if (inode.flag == 3) {
                        try { wait(); } catch(InterruptedException e) {} 
                    } else if (inode.flag == 4) { 
                        iNumber = -1; 
                        return null; 
                    }
                } else if (mode.equals("w")) {
                    // not done yet. 

                }
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
    // receive a file table entry reference
    // save the corresponding inode to the disk
    // free this file table entry.
    // return true if this file table entry found in my table
    }

    public synchronized boolean fempty( ) {
        return table.isEmpty( ); // return if table is empty
    } // should be called before starting a format
}