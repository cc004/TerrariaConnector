package com.example.trconnector;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;

public class NetManager {
    private ByteBuffer buffer;
    private SocketAddress udpep, serverep;
    //private static final String WORLD_NAME = "TrConnector";
    //private static final String WORLD_NAME = "world";

    private static void PutStringToBuffer(ByteBuffer buffer, String content)
    {
        byte[] buf = content.getBytes(StandardCharsets.UTF_8);
        int num;
        for (num = buf.length; num > 127; num >>= 7)
        {
            buffer.put((byte)(num & 127));
        }
        buffer.put((byte)num);
        buffer.put(buf);
    }

    public void start(String host, int port, String name) {
        new Thread(() -> {
            try {
                udpep = new InetSocketAddress(Inet4Address.getLocalHost(), 8888);
                serverep = new InetSocketAddress(host, port);
                server = new ServerSocket(0);

                buffer = ByteBuffer.allocate(256);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.putInt(1010);
                buffer.putInt(server.getLocalPort());
                PutStringToBuffer(buffer, name);
                PutStringToBuffer(buffer, host + ":" + port);
                buffer.putShort((short)4200); // Main.maxTilesX
                buffer.put((byte)0); // IsCrimson
                buffer.putInt(0); // gameMode
                buffer.put((byte)8); // maxNetPlayers
                buffer.put((byte)0); // playerCount
                buffer.put((byte)0); // isHardMode
                buffer.flip();

                new Thread(() -> {
                    try {
                        DatagramChannel channel = DatagramChannel.open();
                        while (!stopped)
                        {
                            channel.send(buffer, udpep);
                            buffer.rewind();
                            Thread.sleep(1000);
                        }
                    } catch (Throwable e) {
                        Log.e(TAG, "udp broadcast error", e);
                    }
                }).start();

                new Thread(() -> {
                    while (!stopped && !server.isClosed()) {
                        try {
                            Socket client = server.accept();
                            Socket client2 = new Socket();
                            client2.connect(serverep);
                            startTunnel(client, client2);
                        } catch (Throwable e) {
                            Log.e(TAG, "tcp listen error", e);
                        }
                    }
                }).start();

            } catch (Throwable e) {
                Log.e(TAG, "error init", e);
            }
        }).start();
    }

    private static final String TAG = NetManager.class.getSimpleName();
    private Boolean stopped = false;
    private ServerSocket server;

    private void startTunnel(InputStream in, OutputStream out) {
        byte[] buf = new byte[1024];
        while (!stopped) {
            try {
                int size = in.read(buf);
                out.write(buf, 0, size);
            } catch (IOException e) {
                Log.e(TAG, "error in tunnel", e);
            }
        }
    }
    private void startTunnel(Socket sock1, Socket sock2) {
        new Thread(() -> {
            try {
                startTunnel(sock1.getInputStream(), sock2.getOutputStream());
            } catch (IOException e) {
                Log.e(TAG, "error when starting tunnel", e);
            }
        }).start();
        new Thread(() -> {
            try {
                startTunnel(sock2.getInputStream(), sock1.getOutputStream());
            } catch (IOException e) {
                Log.e(TAG, "error when starting tunnel", e);
            }
        }).start();
    }

    public void stop(){
        stopped = true;
        try {
            server.close();
        } catch (IOException e) {
            Log.e(TAG, "stop server error", e);
        }
    }
}
