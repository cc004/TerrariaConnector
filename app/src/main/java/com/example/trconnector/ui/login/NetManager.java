package com.example.trconnector.ui.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.StrictMode;
import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

import xyz.hexene.localvpn.ByteBufferPool;
import xyz.hexene.localvpn.LocalVPNService;
import xyz.hexene.localvpn.Packet;
import xyz.hexene.localvpn.TCPInput;

public class NetManager {

    private static InetAddress device;
    private static InetAddress dummy;
    private static byte[] buffer;

    private static final int HEADER_SIZE = Packet.IP4_HEADER_SIZE + Packet.UDP_HEADER_SIZE;

    static {
        try {
            device = InetAddress.getByName("10.0.0.2");
            dummy = InetAddress.getByName("10.0.0.3");
            buffer = ("\u00f2\u0003\u0000\u0000\u0061\u001e\u0000" +
                    "\u0000\u0005\u0077\u006f\u0072\u006c\u0064" +
                    "\u0000\u0000\u0000\u0000\u0000\u00ff\u0000\u0000").getBytes(StandardCharsets.UTF_8);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private static final String TAG = NetManager.class.getSimpleName();
    private static class HostPortPair{
        public InetAddress addr;
        public int port;
    }
    private static InetAddress host;
    private static int port;

    private static HostPortPair RedirectTcpHost(HostPortPair old)
    {
        if (old.addr.equals(dummy) && old.port == 7777)
        {
            old.addr = host;
            old.port = port;
        }
        if (old.addr.equals(host) && old.port == port)
        {
            old.addr = dummy;
            old.port = 7777;
        }
        return old;
    }

    public static void RedirectTcpPacketSource(Packet packet){
        NetManager.HostPortPair pair = new NetManager.HostPortPair();
        pair.addr = packet.ip4Header.sourceAddress;
        pair.port = packet.tcpHeader.sourcePort;
        Log.d(TAG, "tcp host resolve source: " + pair.addr.getHostAddress() + ":" + pair.port);
        NetManager.HostPortPair pair2 = NetManager.RedirectTcpHost(pair);
        packet.ip4Header.sourceAddress = pair2.addr;
        packet.tcpHeader.sourcePort = pair2.port;
    }

    public static void RedirectTcpPacketDest(Packet packet){
        NetManager.HostPortPair pair = new NetManager.HostPortPair();
        pair.addr = packet.ip4Header.destinationAddress;
        pair.port = packet.tcpHeader.destinationPort;
        NetManager.HostPortPair pair2 = NetManager.RedirectTcpHost(pair);
        Log.d(TAG, "tcp host resolve dest: " + pair.addr.getHostAddress() + ":" + pair.port);
        packet.ip4Header.destinationAddress = pair2.addr;
        packet.tcpHeader.destinationPort = pair2.port;
    }

    public static void restart(String host, String port, Activity context, Runnable oncomplete)
    {
        Intent result = VpnService.prepare(context);
        if (result != null) context.startActivityForResult(result, 0);
        else
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        NetManager.host = InetAddress.getByName(host);
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "unknown host", e);
                        return;
                    }
                    NetManager.port = Integer.parseInt(port);
                    context.startService(new Intent(context, LocalVPNService.class));
                    oncomplete.run();
                }
            }).start();
        }
    }

    private static class BroadcastRunnable implements Runnable {

        private ConcurrentLinkedQueue<ByteBuffer> queue;

        public BroadcastRunnable(ConcurrentLinkedQueue<ByteBuffer> queue){
            this.queue = queue;
        }

        private short id = 0;
        public int genId()
        {
            return ((int)(++id)) << 16;
        }

        @Override
        public void run() {
            Packet packet = new Packet();

            packet.ip4Header.destinationAddress = device;
            packet.ip4Header.sourceAddress = dummy;
            packet.ip4Header.headerChecksum = 0;
            packet.ip4Header.headerLength = 20;
            packet.ip4Header.identificationAndFlagsAndFragmentOffset = genId();
            packet.ip4Header.IHL = 5;
            packet.ip4Header.optionsAndPadding = 0;
            packet.ip4Header.TTL = 128;
            packet.ip4Header.totalLength = 50;
            packet.ip4Header.typeOfService = 0;
            packet.ip4Header.version = 4;

            packet.udpHeader.destinationPort = 8888;
            packet.udpHeader.sourcePort = 62550;
            packet.udpHeader.checksum = 0x03f7;
            packet.udpHeader.length = 30;

            while (true){

                ByteBuffer receiveBuffer = ByteBufferPool.acquire();
                // Leave space for the header
                receiveBuffer.position(HEADER_SIZE);
                receiveBuffer.put(buffer);

                int readBytes = buffer.length;

                packet.updateUDPBufferNoChecksum(receiveBuffer, readBytes);
                receiveBuffer.position(HEADER_SIZE + readBytes);

                queue.offer(receiveBuffer);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "interrupted", e);
                }
            }
        }
    }

    public static Runnable getBroadcastRunnable(ConcurrentLinkedQueue<ByteBuffer> queue)
    {
        return new BroadcastRunnable(queue);
    }
}
