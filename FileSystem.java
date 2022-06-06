import java.util.*; 

public class FileSystem {
    private SuperBlock sb; 
    private Directory dir; 
    private FileTable ft; 

    public FileSystem(int blocks) {
        sb = new SuperBlock(blocks);
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
        if (ftEnt == null)
        return false;

    short blockID = 0;
    // deallocate blocks from direct pointers
    for (int offset = 0; offset < ftEnt.inode.directSize; offset++) {
        blockID = ftEnt.inode.direct[offset];
        //nothing to deallocate
        if (blockID == -1)
            continue;
        //else add block to superblock free list and reset
        sb.returnBlock(blockID);
        ftEnt.inode.direct[offset] = -1;
    }
    byte[] indirectData = ftEnt.inode.unregisterIndexBlock();
    if (indirectData != null){
        //add blocks pointed to by the indirect block to the free list
        for (int offset = 0; offset < 512; offset += 2) {
            blockID = SysLib.bytes2short(indirectData, offset);
            if (blockID == -1)
                break;
            sb.returnBlock(blockID);
        }
        //finally, add the indirect block itself back to the free list
        sb.returnBlock(ftEnt.inode.indirect);
    }
    //update disk with cleared inode
    ftEnt.inode.toDisk(ftEnt.iNumber);
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

	
public int read(FileTableEntry ftEnt, byte[] buffer) { 
    // need to do 
    if (ftEnt == null || ftEnt.mode == ("w") || ftEnt.mode == ("a")) {
        return -1; 
    }
    
    int readBuffer = 0; 
    int bufferLength = buffer.length; 
    int fileLength = fsize(ftEnt); 

    synchronized(ftEnt) {
        while (readBuffer < bufferLength && ftEnt.seekPtr < fileLength) {
            byte[] temp = new byte[512]; 
            short id = (short)ftEnt.inode.findTargetBlock(ftEnt.seekPtr); 
            SysLib.rawread(id, temp); 

            for (int i = ftEnt.seekPtr % 512; i < temp.length && readBuffer < bufferLength && 
            ftEnt.seekPtr < fileLength; i++) {
                buffer[readBuffer] = temp[i]; 
                readBuffer++; 
                ftEnt.seekPtr++; 
            }

        }
    }
    return readBuffer; 
}

    public int write(FileTableEntry ftEnt, byte[] buffer){

        int bLength = buffer.length; 
        int prev = fsize(ftEnt); 
        int written = 0; 

        if (ftEnt == null || ftEnt.mode == "r") {
            return -1; 
        }

        synchronized (ftEnt) { 
            while (0 < bLength) { 
                int location = ftEnt.inode.findTargetBlock(ftEnt.seekPtr); 
                if (location == -1) {
                    short freeLocation = (short) sb.getFreeBlock();
                    int res = ftEnt.inode.registerTargetBlock(ftEnt.seekPtr, freeLocation);
                    if (res == Inode.ErrorIndirectNull) {
                        short nextFreeLocation = (short) sb.getFreeBlock(); 
                        if (ftEnt.inode.registerIndexBlock(nextFreeLocation) == false) {
                            return -1; 
                        } 
                        if (ftEnt.inode.registerTargetBlock(ftEnt.seekPtr, freeLocation) != Inode.NoError) {
                            return -1; 
                        }
                    } else if (res != Inode.NoError) { 
                        return -1;
                    }
                    location = freeLocation; 
                }
                byte[] temp = new byte[512]; 
                SysLib.rawread(location, temp);
                int a = ftEnt.seekPtr % 512; 
                int left = 512 - a; 

                int incre = left; 
                int set = bLength - left; 
                if (left > bLength) {
                    incre = bLength; 
                    set = 0; 
                }
                System.arraycopy(buffer, written, temp, a, incre);
                SysLib.rawwrite(location, temp); 
                ftEnt.seekPtr += incre; 
                written += incre; 
                bLength = set; 
            }
            if (ftEnt.seekPtr > prev) {
                ftEnt.inode.length = ftEnt.seekPtr;
            }
        }
        return written;
    }

    public int seek(FileTableEntry ftEnt, int offset, int whence) { 
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
            ftEnt.inode.count--;
            if (ftEnt.count == 0) {         //if count is zero, then free 
                return ft.ffree(ftEnt);                
            } 
        }
        return true; 
    }

    public boolean delete(String fileName) { 
        //FileTableEntry ftEnt = open(fileName, "w"); 
        //return close(ftEnt) && dir.ifree(ftEnt.iNumber);   
        FileTableEntry ftEnt = ft.falloc(fileName, "w");
        if (deallocAllBlocks(ftEnt) && dir.ifree(ftEnt.iNumber) && close(ftEnt)) {
            return true;
        }
        return false;
    }

    public int fsize(FileTableEntry ftEnt) { 
        synchronized (ftEnt) {
            return ftEnt.inode.length; 
        }               
    }          
}