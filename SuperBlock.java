class SuperBlock {
    private final int defaultInodeBlocks = 64;
    public int totalBlocks; 
    public int inodeBlocks; 
    public int freeList;   


    public SuperBlock( int diskSize ) {

        byte[] superBlock = new byte[Disk.blockSize]; // read the superblock from disk. 
        SysLib.rawread(0, superBlock);
        totalBlocks = SysLib.bytes2int(superBlock, 0);
        inodeBlocks = SysLib.bytes2int(superBlock, 4);
        freeList = SysLib.bytes2int(superBlock, 8);

        if (totalBlocks == diskSize && inodeBlocks > 0 && freeList >= 2) { // check disk contents are valid.
            return;
        }
        else {    
            totalBlocks = diskSize;
            format(defaultInodeBlocks); // if invalid, call format( ). 
        }
    }

    void format(int blocks) {
        byte[] data = new byte[512]; // initialize the superblock 
        totalBlocks = 1000;
        inodeBlocks = blocks; // initialize each inode and immediately write it back to disk
        freeList =  blocks / 16 + 1;

    if (blocks % 16 == 0) {
        freeList++;
    }
    SysLib.int2bytes(totalBlocks, data, 0);
    SysLib.int2bytes(inodeBlocks, data, 4);
    SysLib.int2bytes(freeList, data, 8);

    SysLib.rawwrite(0, data);
    SysLib.rawwrite(0, data);

    for (int i = freeList; i < totalBlocks; i++) { // initialize free blocks
        for (int j = 0; j < Disk.blockSize; j++) {
            data[j] = (byte)0;
        }

        if (i != totalBlocks - 1) {
            SysLib.int2bytes(i + 1, data, 0);
        }

        SysLib.rawwrite(i, data);
    }

}

    void sync() {
        byte[] superBlock = new byte[Disk.blockSize]; 
        SysLib.rawread(0, superBlock); 
        SysLib.int2bytes(totalBlocks, superBlock, 0); // write back totalblocks
        SysLib.int2bytes(inodeBlocks, superBlock, 4); // write back iNodeBlocks
        SysLib.int2bytes(freeList, superBlock, 8); // write back freeList
        SysLib.rawwrite(0, superBlock);// write back in-memory superblock to disk: SysLib.rawwrite( 0, superblock );
    }

   
    public int getFreeBlock() {
        if (freeList > -1 && freeList < totalBlocks) { // dequeue the top block from the free list
       
        int freeBlockNumber = freeList; 
        byte[] data = new byte[Disk.blockSize]; 
        SysLib.rawread(freeList, data);

        freeList = SysLib.bytes2int(data, 0);   

        SysLib.int2bytes(0, data, 0);         

        SysLib.rawwrite(freeBlockNumber, data);         

        sync();                               
        
        return freeBlockNumber; // get a new free block from the freelist 
        }      
        return -1;   
 
    }

    public boolean returnBlock(int oldBlockNumber) {
        byte[] data = new byte[512];        
        for(int i = 0; i < Disk.blockSize; i++)
        {
            data[i] = (byte)0;
        }

        if (oldBlockNumber > 0 && oldBlockNumber < totalBlocks) { // enqueue a given block to the end of the free list
            SysLib.int2bytes(freeList, data, 0);
            SysLib.rawwrite(oldBlockNumber, data); 
            freeList = oldBlockNumber;
            sync();
            return true; // return this old block to the free list. The list can be a stack. 
        }
    
    return false;
    }
}