package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class WriteToFile {
	
	private static final WriteToFile inst = new WriteToFile();
	private RandomAccessFile randomAccessFile;

    private WriteToFile() {
        super();
    }
    
    public synchronized void appendContents(String sFileName, String sContent) {
        try {

            File oFile = new File(sFileName);
            if (!oFile.exists()) {
                oFile.createNewFile();
            }
            randomAccessFile = new RandomAccessFile(oFile, "rw");
			FileChannel fileChannel = randomAccessFile.getChannel();
            FileLock lock = fileChannel.lock();
            if (oFile.canWrite()) {
                BufferedWriter oWriter = new BufferedWriter(new FileWriter(sFileName, true));
                oWriter.write (sContent);
                oWriter.close();
            }
            lock.release();

        }
        catch (IOException oException) {
            throw new IllegalArgumentException("Error appending/File cannot be written: \n" + sFileName);
        }
    }

    public static WriteToFile getInstance() {
        return inst;
    }

}
