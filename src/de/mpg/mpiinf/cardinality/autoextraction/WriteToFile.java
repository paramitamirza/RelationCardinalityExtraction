package de.mpg.mpiinf.cardinality.autoextraction;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.locks.ReentrantLock;

public class WriteToFile {
	
	private static final WriteToFile inst = new WriteToFile();
//	private RandomAccessFile randomAccessFile;
	private final ReentrantLock lock = new ReentrantLock();

    private WriteToFile() {
        super();
    }
    
    public synchronized void appendContents(String sFileName, String sContent) {
    	try {
    		lock.lock();
    		
            File oFile = new File(sFileName);
            if (!oFile.exists()) {
                oFile.createNewFile();
            }
//          randomAccessFile = new RandomAccessFile(oFile, "rw");
//			FileChannel fileChannel = randomAccessFile.getChannel();
//          FileLock lock = fileChannel.lock();
            
            if (oFile.canWrite()) {
                BufferedWriter oWriter = new BufferedWriter(new FileWriter(sFileName, true));
                oWriter.write (sContent);
                oWriter.close();
            }
//          lock.release();

        } catch (IOException oException) {
            throw new IllegalArgumentException("Error appending/File cannot be written: \n" + sFileName);
        
        } finally {
        	lock.unlock();
        }
    }

    public static WriteToFile getInstance() {
        return inst;
    }

}
