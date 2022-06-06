import java.lang.String;
import java.util.Arrays;
public class Directory {
    private static int maxChars = 30; // max characters of each file name
    // Directory entries
    private int fsize[]; // each element stores a different file size
    private char fnames[][]; // each element stores a different file name

    //directory constructor 
    public Directory(int maxInumber) {

        fsize = new int [maxInumber];
        for (int i =0; i < maxInumber; i++) {
            fsize[i] = 0; // all file size initiated to 0
        }
        fnames = new char[maxInumber][maxChars]; 
        String root = "/"; // entry(inode) 0 is "/"
        fsize[0] = root.length(); // fsize [0] is the size of "/"
        root.getChars(0, fsize[0], fnames[0], 0);  // fnames [0] includes "/"
    }

    public void bytes2directory (byte data[]) {
    // assumes data[] contains directory information retrieved from disk 
    // initialize the directory fsizes[] and fnames[] with this data[] 
    int offset = 0;
    for ( int i = 0; i < fsize.length; i++, offset += 4 ) {
        fsize[i] = SysLib.bytes2int( data, offset );
    }
        
    for ( int i = 0; i < fnames.length; i++, offset += maxChars * 2 ) {
         String fname = new String( data, offset, maxChars * 2 );
         fname.getChars( 0, fsize[i], fnames[i], 0 );
        }    
    }
    public byte[] directory2bytes( ) {
        // converts and return directory information into a plain byte array 
        // this byte array will be written back to disk 
        byte[] data = new byte[fsize.length * 4 + fnames.length * maxChars * 2];
        int offset = 0;
        for ( int i = 0; i < fsize.length; i++, offset += 4 ) {
            SysLib.int2bytes( fsize[i], data, offset );
        }
        for ( int i = 0; i < fnames.length; i++, offset += maxChars * 2 ) {
            String tableEntry = new String( fnames[i], 0, fsize[i] );
            byte[] bytes = tableEntry.getBytes( );
            System.arraycopy( bytes, 0, data, offset, bytes.length );
        }
        return data;
    }
    public short ialloc( String filename ) {
        // filename is the one of a file to be created.
        // allocates a new inode number for this filename
        if (namei( filename ) == -1) {                      // check to sure -1 
            int fs = filename.length();                     // get file size 
            for (int i = 0; i < fsize.length; i++) {        // loop through 
                if (fsize[i] == 0) {                        // check if == 0 
                    fsize[i] = fs;                          // set the fsize[i] to fs 
                    fnames[i] = filename.toCharArray();     // fname[i] gets a chararray from filename 
                    return (short) i;
                }
            }
        }
        return (short) -1;
    }
    public boolean ifree( short iNumber ) {
        // deallocates this inumber (inode number)
        // the corresponding file will be deleted.
        if (iNumber < fsize.length && fsize[iNumber] > 0) {     // checks iNum exist adn that fsize[iNum] is greater than 0
            fsize[iNumber] = 0;                                 // set to 0 for deletion 
            return true;
        }
        return false;
    }
    public short namei( String filename ) {
        // returns the inumber corresponding to this filename
        for (int i = 0; i < fnames.length; i++) {              // iterates thru looking for file name 
            String temp = new String(fnames[i],0, fsize[i]);    
            if (filename.equals(temp)) {                       // if equals return iNum 
                return (short) i;
            }
        }

        return (short) -1; // return -1 if not found 
    }
    
}
