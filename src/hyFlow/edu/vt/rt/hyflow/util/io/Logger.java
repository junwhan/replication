package edu.vt.rt.hyflow.util.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.deuce.transform.Exclude;

@Exclude
public class Logger {

	private static Integer verbose = Integer.getInteger("verbose", 0);
	private static SimpleDateFormat formatter = new SimpleDateFormat("hh:mm:ss SSS");
	private static boolean redirected = false;
	
	public static final int DEBUG 	= 5;
	public static final int INFO 	= 4;
	public static final int WARNING = 3;
	public static final int ERROR 	= 2;
	public static final int FETAL 	= 1;
	
	public static final String[] level = new String[]{
		"",
		"FETL ",
		"EROR ",
		"WARN ",
		"INFO ",
		"DEBG ",
	};
	public static final PrintStream[] levelStream = new PrintStream[]{
		null,
		System.err,
		System.err,
		System.err,
		System.out,
		System.out,
	};
	
	private static void log(Integer level, String msg){
		int v = Math.abs(verbose);
		if(v<level)
			return;
		
		if(verbose<0 && v!=level)
			return;
		
		String fullMsg = Logger.level[level] + Thread.currentThread().getName() + "@" + formatter.format(new Date()) +" :" + msg;
		levelStream[level].println(fullMsg);
	}
	
	public static void progress(String message, long time){
//		if(!verbose){
//			try {
//				Thread.sleep(time);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//			return;
//		}
		
		System.err.print(message + " ");
		long slice = time/100;
		for(int i=0; i<slice; i++){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.err.print(">");
		}
		System.err.println();
	}

	public static void whoCall(Object msg){
		if(msg!=null)
			Logger.error(String.valueOf(msg));
		try {
			throw new Exception();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void sysout(String msg){
		String fullMsg = Thread.currentThread().getName() + "@" + formatter.format(new Date()) +" :" + msg;
		System.err.println(fullMsg);
	}
	
	public static void fetal(String msg){
		log(FETAL, msg);
	}
	
	public static void error(String msg){
		log(ERROR, msg);
	}
	
	public static void warning(String msg){
		log(WARNING, msg);
	}

	public static void info(String msg){
		log(INFO, msg);
	}

	public static void debug(String msg){
		log(DEBUG, msg);
	}

	public static void redirect(File dir, String file) throws IOException {
  		File fetal = new File(dir, file + ".fetal");
  		if(!fetal.exists()) fetal.createNewFile();
  		levelStream[FETAL] = new PrintStream(new FileOutputStream(fetal,true));
  		Logger.fetal("- - - - - - - - - - [ Start ] - - - - - - - - - -");
  		
  		File err = new File(dir, file + ".err");
  		if(!err.exists()) err.createNewFile(); 
  		levelStream[ERROR] = new PrintStream(new FileOutputStream(err,true));
  		Logger.error("- - - - - - - - - - [ Start ] - - - - - - - - - -");

  		File warning = new File(dir, file + ".warn");
  		if(!warning.exists()) warning.createNewFile(); 
  		levelStream[WARNING] = new PrintStream(new FileOutputStream(warning,true));
  		Logger.warning("- - - - - - - - - - [ Start ] - - - - - - - - - -");

  		File info = new File(dir, file + ".info");
  		if(!info.exists()) info.createNewFile(); 
  		levelStream[INFO] = new PrintStream(new FileOutputStream(info,true));
  		Logger.debug("- - - - - - - - - - [ Start ] - - - - - - - - - -");

  		File debug = new File(dir, file + ".debug");
  		if(!debug.exists()) debug.createNewFile(); 
  		levelStream[DEBUG] = new PrintStream(new FileOutputStream(debug,true));
  		Logger.debug("- - - - - - - - - - [ Start ] - - - - - - - - - -");
  		
  		redirected = true;
	}
	
	@Override
	protected void finalize() throws Throwable {
		if(redirected)
			for(PrintStream stream: levelStream)
				if(levelStream!=null){
					stream.flush();
					stream.close();
				}
	}
}
