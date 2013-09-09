/*
 * Copyright 2004-2007 Daniel F. Savarese
 * Copyright 2009 Savarese Software Research Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.savarese.com/software/ApacheLicense-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package example;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.util.Enumeration;
import java.util.concurrent.*;

import org.savarese.vserv.tcpip.*;
import com.savarese.rocksaw.net.RawSocket;
import static com.savarese.rocksaw.net.RawSocket.PF_INET;
import static com.savarese.rocksaw.net.RawSocket.PF_INET6;
import static com.savarese.rocksaw.net.RawSocket.getProtocolByName;
import static org.savarese.vserv.tcpip.ICMPPacket.OFFSET_ICMP_CHECKSUM;

/**
 * <p>The Ping class is a simple demo showing how you can send
 * ICMP echo requests and receive echo replies using raw sockets.
 * It has been updated to work with both IPv4 and IPv6.</p>
 *
 * <p>Note, this is not a model of good programming.  The point
 * of the example is to show how the RawSocket API calls work.  There
 * is much kluginess surrounding the actual packet and protocol
 * handling, all of which is outside of the scope of what RockSaw
 * does.</p>
 * 
 * @author <a href="http://www.savarese.org/">Daniel F. Savarese</a>
 */

public class Ping {

  public static interface EchoReplyListener {
    public void notifyEchoReply(ICMPEchoPacket packet,
                                byte[] data, int dataOffset,
                                byte[] srcAddress)
      throws IOException;
  }

  public static class Pinger {
    private static final int TIMEOUT = 10000;

    protected RawSocket socket;
    protected ICMPEchoPacket sendPacket, recvPacket;
    protected int offset, length, dataOffset;
    protected int requestType, replyType;
    protected byte[] sendData, recvData, srcAddress;
    protected int sequence, identifier;
    protected EchoReplyListener listener;

    protected Pinger(int id, int protocolFamily, int protocol)
      throws IOException
    {
      sequence   = 0;
      identifier = id;
      setEchoReplyListener(null);

      sendPacket = new ICMPEchoPacket(1);
      recvPacket = new ICMPEchoPacket(1);
      sendData = new byte[84];
      recvData = new byte[84];

      sendPacket.setData(sendData);
      recvPacket.setData(recvData);
      sendPacket.setIPHeaderLength(5);
      recvPacket.setIPHeaderLength(5);
      sendPacket.setICMPDataByteLength(56);
      recvPacket.setICMPDataByteLength(56);

      offset     = sendPacket.getIPHeaderByteLength();
      dataOffset = offset + sendPacket.getICMPHeaderByteLength();
      length     = sendPacket.getICMPPacketByteLength();

      socket = new RawSocket();
      socket.open(protocolFamily, protocol);

      try {
        socket.setSendTimeout(TIMEOUT);
        socket.setReceiveTimeout(TIMEOUT);
      } catch(java.net.SocketException se) {
        socket.setUseSelectTimeout(true);
        socket.setSendTimeout(TIMEOUT);
        socket.setReceiveTimeout(TIMEOUT);
      }
    }

    public Pinger(int id) throws IOException {
      this(id, PF_INET, getProtocolByName("icmp"));

      srcAddress  = new byte[4];
      requestType = ICMPPacket.TYPE_ECHO_REQUEST;
      replyType   = ICMPPacket.TYPE_ECHO_REPLY;
    }

    protected void computeSendChecksum(InetAddress host)
      throws IOException
    {
      sendPacket.computeICMPChecksum();
    }

    public void setEchoReplyListener(EchoReplyListener l) {
      listener = l;
    }

    /**
     * Closes the raw socket opened by the constructor.  After calling
     * this method, the object cannot be used.
     */
    public void close() throws IOException {
      socket.close();
    }

    public void sendEchoRequest(InetAddress host) throws IOException {
      sendPacket.setType(requestType);
      sendPacket.setCode(0);
      sendPacket.setIdentifier(identifier);
      sendPacket.setSequenceNumber(sequence++);

      OctetConverter.longToOctets(System.nanoTime(), sendData, dataOffset);

      computeSendChecksum(host);

      socket.write(host, sendData, offset, length);
    }

    public void receive() throws IOException {
      socket.read(recvData, srcAddress);
    }

    public void receiveEchoReply() throws IOException {
      do {
        receive();
      } while(recvPacket.getType() != replyType ||
              recvPacket.getIdentifier() != identifier);

      if(listener != null)
        listener.notifyEchoReply(recvPacket, recvData, dataOffset, srcAddress);
    }

    /**
     * Issues a synchronous ping.
     *
     * @param host The host to ping.
     * @return The round trip time in nanoseconds.
     */
    public long ping(InetAddress host) throws IOException {
      sendEchoRequest(host);
      receiveEchoReply();

      long end   = System.nanoTime();
      long start = OctetConverter.octetsToLong(recvData, dataOffset);

      return (end - start);
    }

    /**
     * @return The number of bytes in the data portion of the ICMP ping request
     * packet.
     */
    public int getRequestDataLength() {
      return sendPacket.getICMPDataByteLength();
    }

    /** @return The number of bytes in the entire IP ping request packet. */
    public int getRequestPacketLength() {
      return sendPacket.getIPPacketLength();
    }
  }

  public static class PingerIPv6 extends Pinger {
    private static final int IPPROTO_ICMPV6           = 58;
    private static final int ICMPv6_TYPE_ECHO_REQUEST = 128;
    private static final int ICMPv6_TYPE_ECHO_REPLY   = 129;

    /**
     * Operating system kernels are supposed to calculate the ICMPv6
     * checksum for the sender, but Microsoft's IPv6 stack does not do
     * this.  Nor does it support the IPV6_CHECKSUM socket option.
     * Therefore, in order to work on the Windows family of operating
     * systems, we have to calculate the ICMPv6 checksum.
     */
    private static class ICMPv6ChecksumCalculator extends IPPacket {
      ICMPv6ChecksumCalculator() { super(1); }

      private int computeVirtualHeaderTotal(byte[] destination, byte[] source,
                                            int icmpLength)
      {
        int total = 0;

        for(int i = 0; i < source.length;)
          total+=(((source[i++] & 0xff) << 8) | (source[i++] & 0xff));
        for(int i = 0; i < destination.length;)
          total+=(((destination[i++] & 0xff) << 8) | (destination[i++] & 0xff));

        total+=(icmpLength >>> 16);
        total+=(icmpLength & 0xffff);
        total+=IPPROTO_ICMPV6;

        return total;
      }

      int computeChecksum(byte[] data, ICMPPacket packet, byte[] destination,
                          byte[] source)
      {
        int startOffset    = packet.getIPHeaderByteLength();
        int checksumOffset = startOffset + OFFSET_ICMP_CHECKSUM;
        int ipLength       = packet.getIPPacketLength();
        int icmpLength     = packet.getICMPPacketByteLength();

        setData(data);

        return
          _computeChecksum_(startOffset, checksumOffset, ipLength,
                            computeVirtualHeaderTotal(destination, source,
                                                      icmpLength), true);
      }
    }

    private byte[] localAddress;
    private ICMPv6ChecksumCalculator icmpv6Checksummer;

    public PingerIPv6(int id) throws IOException {
      super(id, PF_INET6, IPPROTO_ICMPV6 /*getProtocolByName("ipv6-icmp")*/);

      icmpv6Checksummer = new ICMPv6ChecksumCalculator();
      srcAddress   = new byte[16];
      localAddress = new byte[16];
      requestType  = ICMPv6_TYPE_ECHO_REQUEST;
      replyType    = ICMPv6_TYPE_ECHO_REPLY;
    }

    protected void computeSendChecksum(InetAddress host)
      throws IOException
    {
      // This is necessary only for Windows, which doesn't implement
      // RFC 2463 correctly.
      socket.getSourceAddressForDestination(host, localAddress);
      icmpv6Checksummer.computeChecksum(sendData, sendPacket,
                                        host.getAddress(), localAddress);
    }

    public void receive() throws IOException {
      socket.read(recvData, offset, length, srcAddress);
    }

    public int getRequestPacketLength() {
      return (getRequestDataLength() + 40);
    }
  }

  public static final void main(String[] args) throws Exception {
    if(args.length < 1 || args.length > 2) {
      System.err.println("usage: Ping host [count]");
      System.exit(1);
    }

    final ScheduledThreadPoolExecutor executor =
      new ScheduledThreadPoolExecutor(2);

    try{
      final InetAddress address = InetAddress.getByName(args[0]);
      final String hostname = address.getCanonicalHostName();
      final String hostaddr = address.getHostAddress();
      final int count;
      // Ping programs usually use the process ID for the identifier,
      // but we can't get it and this is only a demo.
      final int id = 65535;
      final Pinger ping;

      if(args.length == 2)
        count = Integer.parseInt(args[1]);
      else
        count = 5;

      if(address instanceof Inet6Address)
        ping = new Ping.PingerIPv6(id);
      else
        ping = new Ping.Pinger(id);

      ping.setEchoReplyListener(new EchoReplyListener() {
          StringBuffer buffer = new StringBuffer(128);

          public void notifyEchoReply(ICMPEchoPacket packet,
                                      byte[] data, int dataOffset,
                                      byte[] srcAddress)
            throws IOException
          {
            long end   = System.nanoTime();
            long start = OctetConverter.octetsToLong(data, dataOffset);
            // Note: Java and JNI overhead will be noticeable (100-200
            // microseconds) for sub-millisecond transmission times.
            // The first ping may even show several seconds of delay
            // because of initial JIT compilation overhead.
            double rtt = (double)(end - start) / 1e6;

            buffer.setLength(0);
            buffer.append(packet.getICMPPacketByteLength())
              .append(" bytes from ").append(hostname).append(" (");
            buffer.append(InetAddress.getByAddress(srcAddress).toString());
            buffer.append("): icmp_seq=")
              .append(packet.getSequenceNumber())
              .append(" ttl=").append(packet.getTTL()).append(" time=")
              .append(rtt).append(" ms");
            System.out.println(buffer.toString());
          }
        });

      System.out.println("PING " + hostname + " (" + hostaddr + ") " +
                         ping.getRequestDataLength() + "(" +
                         ping.getRequestPacketLength() + ") bytes of data).");

      final CountDownLatch latch = new CountDownLatch(1);

      executor.scheduleAtFixedRate(new Runnable() {
          int counter = count;

          public void run() {
            try {
              if(counter > 0) {
                ping.sendEchoRequest(address);
                if(counter == count)
                  latch.countDown();
                --counter;
              } else
                executor.shutdown();
            } catch(IOException ioe) {
              ioe.printStackTrace();
            }
          }
        }, 0, 1, TimeUnit.SECONDS);

      // We wait for first ping to be sent because Windows times out
      // with WSAETIMEDOUT if echo request hasn't been sent first.
      // POSIX does the right thing and just blocks on the first receive.
      // An alternative is to bind the socket first, which should allow a
      // receive to be performed first on Windows.
      latch.await();

      for(int i = 0; i < count; ++i)
        ping.receiveEchoReply();

      ping.close();
    } catch(Exception e) {
      executor.shutdown();
      e.printStackTrace();
    }
  }

}
