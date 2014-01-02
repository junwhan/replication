/*
 * Aleph Toolkit
 *
 * Copyright 1997, Brown University, Providence, RI.
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
/**
 * Constants used by datagram communication manager's reliable packet protocol.
 *
 * @author  Maurice Herlihy
 * @date    April 1997
 **/

public interface Constants {
  
  /* If you change any packet types, be sure to change maxTypes and PacketType
     array */
 
  final int UNUSED    = 0;      // detect improperly initialized packets
  final int ACK       = 1;      // acknowledgment
  final int NACK      = 2;      // complain about missing packet
  final int RSVP      = 3;      // demand acknowledgment
  final int DATA      = 4;      // contains a serialized Java object
  final int BYE       = 5;      // last packet

  final int maxType   = 5;	// max defined packet type

  final String [] packetType = {
    "UNUSED",                   // 0
    "ACK",                      // 1
    "NACK",                     // 2
    "RSVP",                     // 3
    "DATA",			// 4
    "BYE"			// 5
  };
 

  /**
   * Final shutdown timeout in milliseconds.
   **/
  final int SHUTDOWN_TIMEOUT = 5000;

  /**
   * Retransmission timeout in milliseconds.
   **/
  final int RETX_TIMEOUT     = 3000;

  /**
   * How long before retransmitting ack in milliseconds.
   **/
  final int ACK_TIMEOUT      = (RETX_TIMEOUT >> 2);
  
  /**
   * send window size (must be a power of 2).
   **/
  final int S_WSIZE = 8;
  /**
   * receive window size (must be a power of 2).
   **/
  final int R_WSIZE = 8;

  /**
   * Maximum packet size, in bytes.
   **/
  final int MAX_DATA_SIZE = 8192; 

}
