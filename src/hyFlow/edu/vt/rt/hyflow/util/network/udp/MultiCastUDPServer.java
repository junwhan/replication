package edu.vt.rt.hyflow.util.network.udp;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import edu.vt.rt.hyflow.util.io.Logger;

abstract public class MultiCastUDPServer extends Thread{

	private final Integer receivingPort;
	private final Integer sendingPort;
	private final String multiCastGroupAddress;
	private boolean running = true;
	
	public MultiCastUDPServer(Integer receivingPort, Integer sendingPort, String multiCastGroupAddress){
		this.receivingPort = receivingPort;
		this.sendingPort = sendingPort;
		this.multiCastGroupAddress = multiCastGroupAddress;
		setName("MultiCast UDP");
	}
	
	public void terminate(){
		running = false;
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if(isAlive())
			stop();
	}
	
	public static List<Object> read(byte[] bytes) {
		ObjectInputStream stream = null; 
		try {
			stream = new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(bytes));
			List<Object> data = new LinkedList<Object>();
			try{
				while(true)
					data.add(stream.readObject());
			}catch (EOFException e) {
			}catch (java.io.IOException ioe) {
			}
			return data;
		} catch (java.io.IOException ioe) {
			ioe.printStackTrace();
		} catch (java.lang.ClassNotFoundException cnfe) {
			cnfe.printStackTrace();
		}
		return null;
	}

	public byte[] getBytes(Object... objects) {
		java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
		java.io.ObjectOutputStream oos = null;
		try {
			oos = new java.io.ObjectOutputStream(baos);
			for (Object object : objects) {
				oos.writeObject(object);
			}
		} catch (java.io.IOException ioe) {
			ioe.printStackTrace();
		}finally{
			if(oos!=null)
				try {
					oos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return baos.toByteArray();
	}
	
	@Override
	public void run() {
		try {
			MulticastSocket serverSocket = new MulticastSocket(receivingPort);
			InetAddress group = InetAddress.getByName(multiCastGroupAddress);
			try {
				System.out.println("Listen on " + multiCastGroupAddress + ":" + receivingPort);
				serverSocket.joinGroup(group);
				
		        byte[] receiveData = new byte[1024];
		        byte[] sendData = new byte[1024];
		        while(running)
		           try{
		        	   Logger.debug("Waiting for message braodcasting");
		        	   DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		        	   serverSocket.receive(receivePacket);
		        	   Logger.debug("Braodcasted message received");
		        	   List<Object> data = read(receivePacket.getData());
		        	   InetAddress IPAddress = receivePacket.getAddress();
		        	   int port = receivePacket.getPort();
	
		        	   Object reply = process(IPAddress, data);
		        	   if(reply!=null){
		        		   sendData = getBytes(reply);
		        		   DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
		        		   serverSocket.send(sendPacket);
		        	   }
		           }catch (Exception e) {
		        	   e.printStackTrace();
		           }
			} catch (Exception e) {
				e.printStackTrace();
			} finally{
		           serverSocket.leaveGroup(group);
		           serverSocket.close();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	protected abstract Object process(InetAddress iPAddress, List<Object> data);

	public void publish(Object... objects) {
		try {
			InetAddress group = InetAddress.getByName(multiCastGroupAddress);
			byte[] data = getBytes(objects);
			DatagramSocket socket =  sendingPort==null ? new DatagramSocket() : new DatagramSocket(sendingPort);
			DatagramPacket packet = new DatagramPacket(data, data.length, group, receivingPort);
			socket.send(packet);
			socket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}