package edu.vt.rt.hyflow.util.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

import org.deuce.transform.Exclude;

import edu.vt.rt.hyflow.helper.StatsAggregator;

@Exclude
public class FileLogger {

	public static PrintStream out;
	
	public static File init(String dir, String string) throws IOException {
		String context = System.getProperty("context");
		dir = "logs/"+dir.toLowerCase()+(context==null ? "" : context.replace("edu.vt.rt.hyflow.core.tm.", "_").replace(".Context", ""));
		File logsDir = new File(dir);
		
		if(!logsDir.exists())
			logsDir.mkdirs();
		
		StatsAggregator.logDir = logsDir.getPath();

  		File outFile = new File(logsDir, string + ".result");
  		if(!outFile.exists()) outFile.createNewFile(); 
  		out = new PrintStream(new FileOutputStream(outFile,true));
  		out.println("~~~~~~~~~~~~~~" + new Date()+ "~~~~~~~~~~~~~~");
  		
  		return logsDir;
	}
	
	public static void terminate(){
		if(out!=null){
			out.flush();
			out.close();
		}
	}
	
}
