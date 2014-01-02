package edu.vt.rt.hyflow.util.network.sctp;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

import edu.vt.rt.hyflow.util.io.Logger;
import edu.vt.rt.hyflow.util.network.Network;

public abstract class SCTPAssociationHandler extends Thread{

	private static final int CONTROL_STREAM = 0;
	private static final int BUFFER_SIZE = 4024;
	
	private Map<Integer, Channel> channels = new HashMap<Integer, Channel>();
	private SctpServerChannel ssc;

	// Terminal-data
	public static class AssociationInfo{
		private String host;
		private int port;
		public AssociationInfo(String host, int port){
			this.host = host;
			this.port = port;
		}
	}
	
	// Communication channel 
	public class Channel extends Thread {
		
		private SctpChannel sc;
		private int nodeId;
		public Channel(SctpChannel sc) {
			this.sc = sc;
			setName("SCTP Channel");
		}
		
		public boolean send(ByteBuffer buf, int stream){
			try {
				sc.send(buf, MessageInfo.createOutgoing(null, stream));
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
			return false;
		}

		@Override
		public void run() {
			ByteBuffer buff = ByteBuffer.allocateDirect(BUFFER_SIZE);
			MessageInfo messageInfo = null;
			do {
				try {
					messageInfo = sc.receive(buff, System.out, null);
					if (messageInfo == null)
						break;
					buff.flip();
					if (buff.remaining() > 0) {
						int stream = messageInfo.streamNumber();
						if(stream==CONTROL_STREAM){
							nodeId = buff.getInt();
							channels.put(nodeId, this);
							Logger.debug("Adding new channel " + nodeId);
						}
						else	
							dataReceived(buff, nodeId, messageInfo.streamNumber());
						Logger.debug("Receive message at stream: " + messageInfo.streamNumber());
					}
					buff.clear();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} while (messageInfo != null);
			try {
				sc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public SCTPAssociationHandler(int port) throws IOException {
		// create SCTP server socket
		ssc = SctpServerChannel.open();
		InetSocketAddress serverAddr = new InetSocketAddress(port);
		ssc.bind(serverAddr);
	}
	
	protected void start(AssociationInfo[] others){
		// try to establish connection to other peers
		connect(others);
		
		// wait for incoming requests
		start();
	}

	public void run() {
		while (true)
			try {
				newChannel(ssc.accept());
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	private void newChannel(SctpChannel sc){
		Channel channel = new Channel(sc);
		channel.start();
		ByteBuffer buff = ByteBuffer.allocate(10);
		buff.putInt(Network.getInstance().getMyID());
		buff.flip();
		channel.send(buff,  CONTROL_STREAM);
	}
	
	public void send(Integer host, ByteBuffer buf, int stream){
		channels.get(host).send(buf, stream);
	}

	private void connect(AssociationInfo[] others) {
		for (AssociationInfo associationInfo : others) {
		     InetSocketAddress serverAddr = new InetSocketAddress(associationInfo.host,  associationInfo.port);
		     try {
				newChannel(SctpChannel.open(serverAddr, 0, 0));
		     } catch (IOException e) {
		    	 Logger.debug("No connection found yet for " + associationInfo.host + ":" + associationInfo.port);
			}
		}
	}
	
	 public static OutputStream newOutputStream(final ByteBuffer buf) {
	        return new OutputStream() {
	            public synchronized void write(int b) throws IOException {
	                buf.put((byte)b);
	            }

	            public synchronized void write(byte[] bytes, int off, int len) throws IOException {
	                buf.put(bytes, off, len);
	            }
	        };
	    }
	 
	protected ByteBuffer getBuffer(Object... objects){
		 ByteBuffer buff = ByteBuffer.allocate(BUFFER_SIZE);
		try {
			ObjectOutputStream ostream = new ObjectOutputStream(newOutputStream(buff));
			for (Object object : objects) {
				ostream.writeObject(object);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
  	     buff.flip();
  	     return buff;
	}
	
	 public static InputStream newInputStream(final ByteBuffer buf) {
	     return new InputStream() {
	         public synchronized int read() throws IOException {
	             if (!buf.hasRemaining()) {
	                 return -1;
	             }
	             return buf.get();
	         }
	
	         public synchronized int read(byte[] bytes, int off, int len) throws IOException {
	             // Read only what's left
	             len = Math.min(len, buf.remaining());
	             buf.get(bytes, off, len);
	             return len;
	         }
	     };
	 }
	 
	public List<Object>  readBuffer(ByteBuffer buf){
		if (buf.remaining() > 0) {
			List<Object> list = new LinkedList<Object>();
			try {
				ObjectInputStream istream = new ObjectInputStream(newInputStream(buf));
				try{
					while(true){
						list.add(istream.readObject());
					}
				}catch(EOFException e){	}
				return list;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	protected abstract void dataReceived(ByteBuffer buff, int  nodeId, int stream);

}
