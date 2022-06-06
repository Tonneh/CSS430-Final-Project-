import java.util.*; 

public class FileSystem {
    private SuperBlock sb; 
    private Directory dir; 
    private FileTable ft; 

    public FileSystem() {
        sb = new SuperBlock(1000);
        dir = new Directory(sb.inodeBlocks);
        ft = new FileTable(dir);

        FileTableEntry ftEnt = open("/", "r"); 
        int size = fsize(ftEnt); 
        if (size > 0) {
            byte[] data = new byte[size]; 
            read(ftEnt, data); 
            dir.bytes2directory(data);
        }
        close(ftEnt); 
    }
    /*
     * formats the disk, (i.e., Disk.java's data contents). The parameter “files” specifies the maximum number of
        files to be created, (i.e., the number of inodes to be allocated) in your file system. The return value is 0 on
        success, otherwise -1.
     */
    public boolean format (int files) {
        sb.format(files); 
        dir = new Directory(sb.inodeBlocks); 
        ft = new FileTable(dir); 
        return true; 
    } 

    
    public boolean deallocAllBlocks(FileTableEntry ftEnt) { 
        if (ftEnt == null || ftEnt.inode.count != 1) {              // check if null or has more than one inode 
            return false; 
        }
        byte[] data = ftEnt.inode.unregisterIndexBlock();           // free indirect 
        if (data != null) { 
            int i = SysLib.bytes2int(data, 0);              // returns all indirect blocks in superblock 
            while (i != -1) { 
                sb.returnBlock(i); 
            }
        }
        for (int i = 0; i  < Inode.directSize; i++) {               // goes through ids and returning them in superblock
            if (ftEnt.inode.direct[i] != -1) {
                sb.returnBlock(ftEnt.inode.direct[i]); 
                ftEnt.inode.direct[i] = -1; 
            }
        }
        ftEnt.inode.toDisk(ftEnt.iNumber);                          // puts inode on disk 
        return true; 
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
    /*
     *  reads up to buffer.length bytes from the file indicated by fd, starting at the position currently pointed to by
        the seek pointer. If bytes remaining between the current seek pointer and the end of file are less than
        buffer.length, SysLib.read reads as many bytes as possible, putting them into the beginning of buffer. It
        increments the seek pointer by the number of bytes to have been read. The return value is the number of
        bytes that have been read, or a negative value upon an error. 
     */
    public int read(FileTableEntry ftEnt, byte[] buffer) { 
        // need to do 
        if (ftEnt == null || ftEnt.mode == ("w") || ftEnt.mode == ("a") {
            return -1; 
        }
        
        int readBuffer = 0; 
        int leftToRead = buffer.length; 
        int size = fsize(ftEnt); 

        synchronized(ftEnt) {
            while (leftToRead > 0 && ftEnt.seekPtr < size) {
                int curr = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);
                if (curr == -1) {
                    return readBuffer; 
                }
                byte[] data = new byte[512];
                Syslib.rawread(curr, data); 
                int offset = ftEnt.seekPtr % 512; 
                int remainingBlocksLength = 512 - offset; 
                int remainingFilesLength = size - ftEnt.seekPtr; 
                int a = Math.min(remainingBlocksLength, leftToRead); 
                int b = Math.min(a, remainingFilesLength);              //b is hwo much has been read 
                System.arraycopy(data, offset, buffer, readBuffer, b);
                leftToRead -= b; 
                ftEnt.seekPtr += b;
                readBuffer = b;  
 
            }
        }
        return readBuffer; 

    }

    public int write(FileTableEntry ftEnt, byte[] buffer) { 
        // need to do 
    }

    public int seek(FileTableEntry ftEnt, int offset, int whence) { 
        // need to do 
        synchronized (ftEnt) {
            if (whence == 0) {                             //seek_set offset 
                ftEnt.seekPtr = offset; 
            } else if (whence == 1) {                      //seek_cur itself + offset
                ftEnt.seekPtr += offset; 
            } else if (whence == 2) {                      //seek_end size of file + offset
                ftEnt.seekPtr = fsize(ftEnt) + offset; 
            } else {
                return -1; 
            }
            if (ftEnt.seekPtr < 0) {                       // check if seek pointer is negative, then clamp to zero
                ftEnt.seekPtr = 0; 
            }
            if (ftEnt.seekPtr > fsize(ftEnt)) {            // check if seekptr is beyond file size, if so set to end of file
                ftEnt.seekPtr = fsize(ftEnt); 
            }
            return ftEnt.seekPtr;
        }
    }

    public boolean close(FileTableEntry ftEnt) { 
        synchronized (ftEnt) {
            ftEnt.count--;                  //decrement
            if (ftEnt.count == 0) {         //if count is zero, then free 
                return ft.ffree(ftEnt);                
            } 
        }
        return true; 
    }

    public boolean delete(String fileName) { 
        FileTableEntry ftEnt = open(fileName, "w"); 
        return close(ftEnt) && dir.ifree(ftEnt.iNumber);   
    }

    public int fsize(FileTableEntry ftEnt) { 
        synchronized (ftEnt) {
            return ftEnt.inode.length; 
        }               
    }          
}
