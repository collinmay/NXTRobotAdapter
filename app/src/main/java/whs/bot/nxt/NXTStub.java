package whs.bot.nxt;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by misson20000 on 3/5/16.
 */
public class NXTStub {
    private BluetoothDevice btDev;
    private NXT nxt;
    private NXTMotor motorA;
    private NXTMotor motorB;
    private NXTMotor motorC;
    private List<StatusChangeListener> statusListeners;
    private String name;

    public NXTStub(BluetoothDevice btDev) {
        this.btDev = btDev;
        this.motorA = new NXTMotor(this, NXTMotor.MotorPort.A);
        this.motorB = new NXTMotor(this, NXTMotor.MotorPort.B);
        this.motorC = new NXTMotor(this, NXTMotor.MotorPort.C);
        this.statusListeners = new ArrayList<>();
        this.name = btDev.getName();
    }

    public NXT.ConnectionStatus getStatus() {
        if(nxt == null) {
            return NXT.ConnectionStatus.DISCONNECTED;
        } else {
            return nxt.getConnectionStatus();
        }
    }

    public boolean isOnline() {
        return nxt != null && nxt.getConnectionStatus() == NXT.ConnectionStatus.CONNECTED;
    }

    public NXT connect(Robot r) {
        if(nxt == null) {
            return nxt = new NXT(this, r, btDev);
        } else {
            return nxt;
        }
    }

    public void disconnect(Robot r) {
        nxt.kill();
        r.removeNXT(nxt);
        nxt = null;
    }

    public BluetoothDevice getBtDev() {
        return btDev;
    }

    public NXTMotor getMotor(NXTMotor.MotorPort port) {
        switch(port) {
            case A: return motorA;
            case B: return motorB;
            case C: return motorC;
        }
        return motorA;
    }

    public void updateStatus() {
        for(Iterator<StatusChangeListener> i = statusListeners.iterator(); i.hasNext();) {
            i.next().statusChanged(this);
        }
    }

    public void addStatusChangeListener(StatusChangeListener l) {
        statusListeners.add(l);
    }

    public void removeStatusChangeListener(StatusChangeListener l) {
        statusListeners.remove(l);
    }

    public NXT getNXT() {
        return nxt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public interface StatusChangeListener {
        public void statusChanged(NXTStub stub);
    }
}
