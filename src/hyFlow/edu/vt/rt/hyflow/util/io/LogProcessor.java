package edu.vt.rt.hyflow.util.io;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;

import javax.swing.JOptionPane;

public class LogProcessor {

	private static void parse(File logDir, StringBuilder text) throws FileNotFoundException{
		for (File file: logDir.listFiles(new FileFilter(){
													public boolean accept(File file) {
														return file.isDirectory() || file.getName().endsWith(".result");
													}
												})) {
			if(file.isDirectory())
				parse(file, text);
			else{
				String NL = System.getProperty("line.separator");
				Scanner scanner = new Scanner(new FileInputStream(file));
				try {
					while (scanner.hasNextLine()){
						text.append(scanner.nextLine() + NL);
					}
				}
				finally{
					scanner.close();
				}
			}
		}
	}
	public static void main(String[] args) throws FileNotFoundException {
		String path= JOptionPane.showInputDialog("Enter log dire path", "C:/Users/Mohamed/Desktop/");
		if(path==null)
			return;
		StringBuilder text = new StringBuilder();
		parse(new File(path), text);
		
		for(int r=10; r<=100; r+=40){
			System.out.println(r);
			for(int i=1; i<=120; i++){
				Scanner scanner = new Scanner(text.toString());
				double throuput=0;
				int aborts=0, count=0, conflicts=0, timeouts=0, reads=0, writes=0, forwardings=0;
				try {
					while (scanner.hasNextLine()) {
						if (scanner.nextLine().contains("n=" + i + ", t=8, o=60, x=50, c=1, %=" + r + ",")) {
							
							String throuputLine = scanner.nextLine();
							if (!throuputLine.startsWith("Throughput:"))
								panic(throuputLine);
							throuput += Double.parseDouble(throuputLine.substring("Throughput: ".length()).trim());
							
							String readsLine = scanner.nextLine();
							if (!readsLine.startsWith("Reads: "))
								panic(readsLine);
							reads += Double.parseDouble(readsLine.substring("Reads: ".length()).trim());
							
							String writesLine = scanner.nextLine();
							if (!writesLine.startsWith("Writes: "))
								panic(writesLine);
							writes += Double.parseDouble(writesLine.substring("Writes: ".length()).trim());
							
//							String timeoutLine = scanner.nextLine();
//							if (!timeoutLine.startsWith("Timeouts: "))
//								panic(timeoutLine);
//							timeouts += Double.parseDouble(timeoutLine.substring("Timeouts: ".length()).trim());
	
//							String abortsLine = scanner.nextLine();
//							if (!abortsLine.startsWith("Aborts: "))
//								panic(abortsLine);
//							aborts += Double.parseDouble(abortsLine.substring("Aborts: ".length()).trim());
//							
//							String conflLine = scanner.nextLine();
//							if (!conflLine.startsWith("Conflicts: "))
//								panic(conflLine);
//							conflicts += Double.parseDouble(conflLine.substring("Conflicts: ".length()).trim());
//							
//							String lConflLine = scanner.nextLine();
//							if (!lConflLine.startsWith("Local Conflicts: "))
//								panic(lConflLine);
//							conflicts += Double.parseDouble(lConflLine.substring("Local Conflicts: ".length()).trim());
//							
//							String fwdLine = scanner.nextLine();
//							if (!fwdLine.startsWith("Forwarding: "))
//								panic(fwdLine);
//							forwardings += Double.parseDouble(fwdLine.substring("Forwarding: ".length()).trim());
	
							count++;
						}
					}
				} finally {
					scanner.close();
				}
				if(reads>0 || writes>0)
					System.out.println(i+","+count+","+throuput+","+aborts+","+conflicts+","+timeouts+","+reads+","+writes+","+forwardings);
			}
		}
		
	}

	private static void panic(String throuput) {
		System.err.println(throuput);
		System.exit(-1);
	}
}
