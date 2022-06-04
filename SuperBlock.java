class SuperBlock {
    private final int defaultInodeBlocks = 64;
    public int totalBlocks; 
    public int inodeBlocks; 
    public int freeList;   
   
    public SuperBlock( int diskSize ) {
         // read the superblock from disk. 
        // check disk contents are valid.
        // if invalid, call format( ). 
        byte[] superBlock = new byte[Disk.blockSize];
        SysLib.rawread(0, superBlock);
        totalBlocks = SysLib.bytes2int(superBlock, 0);
        inodeBlocks = SysLib.bytes2int(superBlock, 4);
        freeList = SysLib.bytes2int(superBlock, 8);

        if (totalBlocks == diskSize && inodeBlocks > 0 && freeList >= 2) {
            return;
        }
        else {
            totalBlocks = diskSize;
            format(defaultInodeBlocks);
        }
    }

    void format(int blocks) {
        // initialize the superblock 
        byte[] data = new byte[512];
        // initialize each inode and immediately write it back to disk
        // initialize free blocks
        totalBlocks = 1000;
        inodeBlocks = blocks;
        freeList =  blocks / 16 + 1;

    if (blocks % 16 == 0) {
        freeList++;
    }
    SysLib.int2bytes(totalBlocks, data, 0);
    SysLib.int2bytes(inodeBlocks, data, 4);
    SysLib.int2bytes(freeList, data, 8);

    SysLib.rawwrite(0, data);
    SysLib.rawwrite(0, data);

    for (int i = freeList; i < totalBlocks; i++) {
        // Fill block with 0.
        for (int j = 0; j < Disk.blockSize; j++) {
            data[j] = (byte)0;
        }

        if (i != totalBlocks - 1) {
            SysLib.int2bytes(i + 1, data, 0);
        }
            
        SysLib.rawwrite(i, data);
    }

}


    //write back totalBlocks, iNodeBlocks, and freeList
    // write back in-memory superblock to disk: SysLib.rawwrite( 0, superblock );
    void sync() {
        byte[] superBlock = new byte[Disk.blockSize];
        SysLib.rawread(0, superBlock);
        SysLib.int2bytes(totalBlocks, superBlock, 0);
        SysLib.int2bytes(inodeBlocks, superBlock, 4);
        SysLib.int2bytes(freeList, superBlock, 8);
        SysLib.rawwrite(0, superBlock);
    }

    //dequeue the top block from the free list
    public int getFreeBlock() {
        if (freeList > -1 && freeList < totalBlocks) {
        // get a new free block from the freelist 
        int freeBlockNumber = freeList; 
        byte[] data = new byte[Disk.blockSize]; // Read in the data from the first free block
        SysLib.rawread(freeList, data);

        freeList = SysLib.bytes2int(data, 0);   

        SysLib.int2bytes(0, data, 0);         

        SysLib.rawwrite(freeBlockNumber, data);         

        sync();                               
        
        return freeBlockNumber;      
        }      
        return -1;   
 
    }

    //enqueue a given block to the end of the free list
    public boolean returnBlock(int oldBlockNumber) {
        // return this old block to the free list. The list can be a stack. 
        byte[] data = new byte[512];        
        for(int i = 0; i < Disk.blockSize; i++)
        {
            data[i] = (byte)0;
        }

        if (oldBlockNumber > 0 && oldBlockNumber < totalBlocks) {
            SysLib.int2bytes(freeList, data, 0);
            SysLib.rawwrite(oldBlockNumber, data); 
            freeList = oldBlockNumber;
            sync();
            return true;
        }
    
    return false;
    }



}

    

