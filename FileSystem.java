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

    public boolean format (int files) {         
        sb.format(files);                       // format files
        return true; 
    } 

    
    public boolean deallocAllBlocks(FileTableEntry ftEnt) {         
        if (ftEnt == null)                                              // if ftEnt is null return false 
        return false;

    short blockID = 0;                                                  // create blockID as short (2 bytes)
    for (int i = 0; i < ftEnt.inode.directSize; i++) {                  // loop through to deallocate blocks from director poitners
        blockID = ftEnt.inode.direct[i];
        if (blockID == -1)                                              // if theres nothing to dealloc then continue
            continue;
        sb.returnBlock(blockID);                                        // return block back to freelist 
        ftEnt.inode.direct[i] = -1;                                    
    }
    byte[] indirectData = ftEnt.inode.unregisterIndexBlock();
    if (indirectData != null){
        for (int i = 0; i < 512; i += 2) {                              // increment by 2
            blockID = SysLib.bytes2short(indirectData, i);  
            if (blockID == -1)
                break;
            sb.returnBlock(blockID);                                    // return block back to freelist 
        }
        sb.returnBlock(ftEnt.inode.indirect);                           // add indirect block to freelist 
    }
    ftEnt.inode.toDisk(ftEnt.iNumber);                                  // update disk 
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
    if (ftEnt == null || ftEnt.mode == ("w") || ftEnt.mode == ("a")) {      // if ftent or mode is write or append return -1 
        return -1; 
    }
    
    int readBuffer = 0;                                                     // readBuffer = how much has been read
    int bufferLength = buffer.length;                                       // length of buffer
    int fileLength = fsize(ftEnt);                                          // length of the ftEnt 

    synchronized(ftEnt) {
        while (readBuffer < bufferLength && ftEnt.seekPtr < fileLength) {   // check that readBuffer is less than bufferlength & seekptr is less than filelength
            byte[] temp = new byte[512]; 
            short id = (short)ftEnt.inode.findTargetBlock(ftEnt.seekPtr); 
            SysLib.rawread(id, temp); 

            for (int i = ftEnt.seekPtr % 512; i < temp.length               // loop to check if seekPtr % 512 is less than length of byte[] temp &
            && readBuffer < bufferLength &&                                 // readbuffer is less than bufferlength &&
            ftEnt.seekPtr < fileLength; i++) {                              // seekPtr is less than fileLength 
                buffer[readBuffer] = temp[i];                               // set buffer[readBuffer] to temp[i] 
                readBuffer++;                                               // increment readBuffer
                ftEnt.seekPtr++;                                            // increment seekptr 
            }

        }
    }
    return readBuffer;                                                      // return readBuffer 
}

public int write(FileTableEntry ftEnt, byte[] buffer){

    int bLength = buffer.length; // set bufferlength to buffer.length
    int prev = fsize(ftEnt); // set the previous file length to fsize(ftEnt)
    int written = 0;  // set written to 0

    if (ftEnt == null || ftEnt.mode == "r") { 
        return -1; // set = to -1 to represent not to write
    }

    synchronized (ftEnt) { 
        while (0 < bLength) { 
            int location = ftEnt.inode.findTargetBlock(ftEnt.seekPtr); // set location variable to findTargetBlock(ftEnt.seekPtr)
            if (location == -1) { // if location is -1 or null
                short freeLocation = (short) sb.getFreeBlock(); // get free block location
                int res = ftEnt.inode.registerTargetBlock(ftEnt.seekPtr, freeLocation);
                if (res == Inode.ErrorIndirectNull) { 
                    short nextFreeLocation = (short) sb.getFreeBlock(); // get next free block location 
                    if (ftEnt.inode.registerIndexBlock(nextFreeLocation) == false) {
                        return -1; 
                    } 
                    if (ftEnt.inode.registerTargetBlock(ftEnt.seekPtr, freeLocation) != Inode.NoError) {
                        return -1; 
                    }
                } else if (res != Inode.NoError) { 
                    return -1;
                }
                location = freeLocation;  // set location to free block location
            }
            byte[] temp = new byte[512]; // initalize byte temp
            SysLib.rawread(location, temp); 
            int a = ftEnt.seekPtr % 512; // set variable a to ftEnt.seekPtr % 512
            int left = 512 - a; 

            int incre = left; // set variable incre to left variable
            int set = bLength - left; 
            if (left > bLength) { // if left is greater than buffer length
                incre = bLength;  // incre variable is equal to buffer length
                set = 0; // make set variable = to 0
            }
            System.arraycopy(buffer, written, temp, a, incre); // array copy
            SysLib.rawwrite(location, temp); 
            ftEnt.seekPtr += incre; // increment seekptr by incre variable
            written += incre; // increment written by incre variable 
            bLength = set; // set bufferlength to set
        }
        if (ftEnt.seekPtr > prev) { // if seekPtr is greater than prev
            ftEnt.inode.length = ftEnt.seekPtr; // set ftEnt.length to seekptr
        }
    }
    return written; // return written 
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
        FileTableEntry ftEnt = ft.falloc(fileName, "w");
        if (deallocAllBlocks(ftEnt) && dir.ifree(ftEnt.iNumber) // call deallocallblocks, ifree and close 
        && close(ftEnt)) {
            return true;
        }
        return false;
    }

    public int fsize(FileTableEntry ftEnt) { 
        synchronized (ftEnt) {
            return ftEnt.inode.length;  // return inode length 
        }               
    }          
}