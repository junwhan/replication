/*
 * Aleph Toolkit
 *
 * Copyright 1999, Brown University, Providence, RI.
 * 
 *                         All Rights Reserved
 * 
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose other than its incorporation into a
 * commercial product is hereby granted without fee, provided that the
 * above copyright notice appear in all copies and that both that
 * copyright notice and this permission notice appear in supporting
 * documentation, and that the name of Brown University not be used in
 * advertising or publicity pertaining to distribution of the software
 * without specific, written prior permission.
 * 
 * BROWN UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR ANY
 * PARTICULAR PURPOSE.  IN NO EVENT SHALL BROWN UNIVERSITY BE LIABLE FOR
 * ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package aleph.comm.udp;

import aleph.Aleph;
import aleph.Config;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Serializable;
import java.lang.Math;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Packet implements Serializable, Constants {

  static final boolean DEBUG = false; // He's dead, Jim.

  private static DatagramSocket messageSocket;

  private byte[] buffer;	// the packet lives here
  private int    pos;		// index of next byte to read/write
  private int    size;		// total number of bytes in packet, including header
  public transient Packet next = null; // link to next packet

  // Index of each component in buffer.
  private static final int TYPE_INDEX    = 0;
  private static final int SEQNUM_INDEX  = 1;
  private static final int ACKNUM_INDEX  = 2;
  private static final int ADDRESS_INDEX = 3;
  private static final int PORT_INDEX    = 4;
  private static final int DATA_INDEX    = 5; // must be last

  private static byte[] fourBytes = new byte[4]; // for incoming InetAddress
  private static byte[] localHost; // initialized in static section
  private static int    localPort; // initialized in static section

  private static final int INT_LENGTH    = 4;// bytes in an integer
  // Number of bytes in the header
  private static final int HEADER_LENGTH = (DATA_INDEX * INT_LENGTH);

  public static void main(String[] args) {
    try {
      Packet sPacket = new Packet(DATA);
      String s = "hello";
      byte[] b = s.getBytes();
      sPacket.append(b, 0, b.length);
      System.out.println("sPacket is " + sPacket);
      DatagramSocket sSocket = new DatagramSocket();
      DatagramSocket rSocket = new DatagramSocket();
      Address address = new Address(rSocket);
      sPacket.send(sSocket, address);
      Packet rPacket = Packet.receive(rSocket);
      System.out.println("rPacket is " + rPacket);
      System.out.println("rPacket available " + rPacket.available());
      byte[] c = new byte[MAX_DATA_SIZE];
      int count = rPacket.read(c, 0, c.length);
      System.out.println("count is " + count);
      System.out.println("string is " + new String(c, 0, count));
    } catch (Exception e) {
      System.err.println(e);
    }
    System.exit(0);
  }

  /**
   * Constructor
   * @param type what kind of packet?
   * @see aleph.comm.datagram.Constants
   **/
  public Packet(int type) {
    buffer = new byte[MAX_DATA_SIZE]; // bytes live here
    size = buffer.length;	// use everything
    pos  = HEADER_LENGTH;	// cursor position
    write4Bytes(ADDRESS_INDEX, localHost); // fill in local address
    writeInt(PORT_INDEX, localPort); // and port number
    setAcknum(-1);		// first ack
    setType(type);		// install type
  }

  /**
   * Internal constructor for reading packet from net.
   **/
  private Packet(byte[] buffer, int size) {
    this.buffer = buffer;
    this.size = size;
    this.pos = HEADER_LENGTH;
  }

  /* Packet type */
  public int getType() { return readInt(TYPE_INDEX); }
  public void setType(int type) { writeInt(TYPE_INDEX, type); }

  /* Sequence number */
  public int getSeqnum() { return readInt(SEQNUM_INDEX); }
  public void setSeqnum(int seq) { writeInt(SEQNUM_INDEX, seq); }

  /* Acknowledgment number */
  public int getAcknum() { return readInt(ACKNUM_INDEX); }
  public void setAcknum(int ack) { writeInt(ACKNUM_INDEX, ack); }

  /* Sender's address */
  public void setAddress(Address address) {
    write4Bytes(ADDRESS_INDEX, address.inetAddress.getAddress());
    writeInt(PORT_INDEX, address.port);
  }
  public Address getAddress() {
    try {
      read4Bytes(ADDRESS_INDEX, fourBytes);
      StringBuffer buf = new StringBuffer(12);
      for (int i = 0; i < 3; i++) {
	buf.append(Integer.toString(fourBytes[i] & 0xFF));
	buf.append(".");
      }
      buf.append(Integer.toString(fourBytes[3] & 0xFF));
      InetAddress inetAddress = InetAddress.getByName(buf.toString());
      int port = readInt(PORT_INDEX);
      return new Address(inetAddress, port);
    } catch (Exception e) {
      Aleph.panic("Packet" + e);
    }
    return null;			// not reached
  }
  
  /**
   * @return number of unused data bytes in packet.
   **/
  public int available () {
    return size - pos;
  }  

  /**
   * @return any data bytes in packet?
   **/
  public boolean empty() {
    return pos == HEADER_LENGTH;
  }

  /**
   * Write byte to packet.  Beware: panics on overflow!
   * @param b byte written
   **/
  public void append(int b) {
    if (size == pos)
      Aleph.panic("Packet overflow");
    buffer[pos++] = (byte) b;
  }

  /**
   * Write bytes to packet.  Beware: panics on overflow!
   * @param b array of bytes
   * @param off starting offset
   * @param len number of bytes to write
   **/
  public void append(byte[] b, int off, int len) {
    if (size < pos + len)
      Aleph.panic("Packet overflow: writing " + len + " bytes to " + this );
    System.arraycopy(b, off, buffer, pos, len);
    pos += len;
  }

  /**
   * Returns next byte from packet, or -1 if nothing left.
   **/
  public int read() {
    if (pos == size)
      return -1;
    return buffer[pos++] & 0xff;
  }

  /**
   * Reads up to <code>len</code> bytes of data into an array of bytes.
   * @param b target array
   * @param off offset in array
   * @param len number of bytes
   * @return number actually copied
   **/
  public int read(byte b[], int off, int len) {
    if (pos == size)
      return -1;
    len = Math.min(available(), len);
    System.arraycopy(buffer, pos, b, off, len);
    pos += len;
    return len;
  }

  /**
   * Send the packet.
   * @param socket  This PE's socket
   * @param address Destination address
   **/
  public void send (DatagramSocket socket, Address destination)
    throws IOException {
    if (DEBUG)
      Aleph.debug("Packet.send " + this + "(" + pos +
                         " bytes) to " + destination);
    try {
      socket.send(new DatagramPacket(buffer,
                                     pos,
                                     destination.inetAddress,
                                     destination.port));
    } catch (IOException e) {
      if (DEBUG)
        Aleph.debug("Packet.send catches " + e);
    }
  }

  /**
   * Receives and returns a packet from the network.
   * @param socket Datagram socket on which to listen.
   **/
  public static Packet receive (DatagramSocket socket)
    throws java.io.InterruptedIOException { // Indicates timeout.
      try {
	DatagramPacket datagram = new DatagramPacket(new byte[MAX_DATA_SIZE], 
						     MAX_DATA_SIZE);
	socket.receive(datagram); // blocking call
        if (DEBUG)
          Aleph.debug("Packet.receive got " + datagram.getLength() + " bytes");
	Packet packet = new Packet(datagram.getData(), datagram.getLength());
	return packet;
      } catch (IOException e) {
        throw new InterruptedIOException();
      } catch (Exception e) {
	Aleph.panic(e);
      }
      return null;		// never reached
  }

  // Pull an int out of the array.
  protected int readInt(int index) {
    int i = index * INT_LENGTH;
    int ch1 = buffer[i++] & 0xFF;
    int ch2 = buffer[i++] & 0xFF;
    int ch3 = buffer[i++] & 0xFF;
    int ch4 = buffer[i++] & 0xFF;
    return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
  }

  // Stuff an int into the array.
  protected void writeInt(int index, int v) {
    int i = index * INT_LENGTH;
    buffer[i++] = (byte) ((v >>> 24) & 0xFF);
    buffer[i++] = (byte) ((v >>> 16) & 0xFF);
    buffer[i++] = (byte) ((v >>>  8) & 0xFF);
    buffer[i++] = (byte) ((v >>>  0) & 0xFF);
  }
  // Pull four bytes out of the array.
  protected void read4Bytes(int index, byte[] b) {
    int start = index * INT_LENGTH;
    for (int i = 0; i < 4; i++)
      b[i] = buffer[start + i];
  }
  // Stuff four bytes into the array.
  protected void write4Bytes(int index, byte[] b) {
    int start = index * INT_LENGTH;
    for (int i = 0; i < 4; i++)
      buffer[start + i] = b[i];
  }

  public String toString() {
    StringBuffer result = new StringBuffer("Packet #");
    result.append(this.getSeqnum());
    result.append("[");
    result.append(packetType[getType()]);
     result.append(", from: ");
    result.append(getAddress());
    result.append(", Ack: ");
    result.append(this.getAcknum());
    result.append(", Pos: ");
    result.append(pos);
    result.append(", Size: ");
    result.append(size);
    result.append("]");
    return result.toString();
  }

  static {
    try {
      Address myAddress =
	(Address) CommunicationManager.getManager().getAddress();
      localHost = myAddress.inetAddress.getAddress();
      localPort = myAddress.port;
    } catch (ExceptionInInitializerError e) {
      Aleph.panic(e.getException());
    } catch (Exception e) {
      Aleph.panic(e);
    }
  } 
}
