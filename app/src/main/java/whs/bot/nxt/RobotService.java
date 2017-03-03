package whs.bot.nxt;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.ArrayAdapter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by misson20000 on 3/4/16.
 */
public class RobotService extends Service implements NXT.StateChangeListener {
    private boolean isRunning;
    private Robot robot;
    private Thread discoveryThread;
    private Thread serverThread;
    private List<NXTStub> nxtStubs;
    private List<NXT.StateChangeListener> nxtListeners;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!isRunning) {
            isRunning = true;

            robot = new Robot();
            robot.nxtListener = this;

            nxtStubs = new ArrayList<>();
            nxtListeners = new ArrayList<>();
            BluetoothAdapter bt = BluetoothAdapter.getDefaultAdapter();

            Set<BluetoothDevice> paired = bt.getBondedDevices();
            for(Iterator<BluetoothDevice> i = paired.iterator(); i.hasNext();) {
                BluetoothDevice b = i.next();
                if(b.getBluetoothClass().getDeviceClass() == BluetoothClass.Device.TOY_ROBOT) {
                    nxtStubs.add(new NXTStub(b));
                }
            }

            WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            if (wifi != null) {
                WifiManager.MulticastLock lock = wifi.createMulticastLock("robolock");
                lock.acquire();
            }

            discoveryThread = new Thread(new DiscoveryThread());
            discoveryThread.start();

            serverThread = new Thread(new ServerThread());
            serverThread.start();
        }

        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    public void connectNxt(NXT nxt) {
        this.robot.addNXT(nxt);
    }

    public Robot getRobot() {
        return robot;
    }

    public List<NXTStub> getNxtStubs() {
        return nxtStubs;
    }

    public void addNXTListener(NXT.StateChangeListener l) {
        nxtListeners.add(l);
    }
    public void removeNXTListener(NXT.StateChangeListener l) {
        nxtListeners.remove(l);
    }

    @Override
    public void stateChanged(NXT nxt) {
        for(Iterator<NXT.StateChangeListener> i = nxtListeners.iterator(); i.hasNext();) {
            i.next().stateChanged(nxt);
        }
    }

    private class ServerThread implements Runnable {
        @Override
        public void run() {
            while(isRunning) {
                try {
                    ServerSocketChannel sock = ServerSocketChannel.open();
                    try {
                        sock.socket().bind(new InetSocketAddress(25600));
                        while(isRunning) {
                            SocketChannel client = sock.accept();
                            robot.addDriver(new Driver(robot, client));
                        }
                    } catch(IOException e) {

                    } finally {
                        sock.close();
                    }
                } catch(IOException e) {

                }
            }
        }
    }

    private class DiscoveryThread implements Runnable {
        @Override
        public void run() {
            MulticastSocket sock = null;
            try {
                sock = new MulticastSocket(25601);
                sock.joinGroup(InetAddress.getByName("238.160.102.2"));

                byte[] inBuffer = new byte[11];
                DatagramPacket inPacket = new DatagramPacket(inBuffer, 11);

                byte[] outBuffer = "Android NXT Adapter".getBytes();
                DatagramPacket outPacket = new DatagramPacket(outBuffer, outBuffer.length);
                while(isRunning) {
                    sock.receive(inPacket);
                    if (new String(inBuffer).equals("find robots")) {
                        outPacket.setSocketAddress(inPacket.getSocketAddress());
                        outPacket.setPort(inPacket.getPort());
                        sock.send(outPacket);
                    }
                }
            } catch (IOException e) {

            } finally {
                if (sock != null) {
                    sock.close();
                }
            }
        }
    }

    public class Binder extends android.os.Binder {
        public RobotService getService() {
            return RobotService.this;
        }
    }
}
