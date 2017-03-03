package whs.bot.nxt;

import android.os.Looper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Robot {
	private List<Driver> drivers;
	private List<Subsystem> subsystems;
	private List<Battery> batteries;

    public NXT.StateChangeListener nxtListener;
    private List<SubsystemsListener> subsystemsListeners;
    private List<DriverListener> driverListeners;

	public Robot() {
		drivers = new ArrayList<>();
		subsystems = new ArrayList<>();
		batteries = new ArrayList<>();
        subsystemsListeners = new ArrayList<>();
        driverListeners = new ArrayList<>();
	}

    public void addSubsystemsListener(SubsystemsListener l) {
        subsystemsListeners.add(l);
    }
    public void removeSubsystemsListener(SubsystemsListener l) {
        subsystemsListeners.remove(l);
    }

    public void addDriverListener(DriverListener l) {
        driverListeners.add(l);
    }
    public void removeDriverListener(DriverListener l) {
        driverListeners.remove(l);
    }

    public void queueJob(DriverJob j) {
        for(Iterator<Driver> i = drivers.iterator(); i.hasNext();) {
            i.next().queueJob(j);
        }
    }

	public void addNXT(NXT nxt) {
		batteries.add(nxt);
        queueJob(new DriverJobUpdateBatteries());
	}

    public void addSubsystem(Subsystem sub) {
        System.out.println("bot sub");
        subsystems.add(sub);
        queueJob(new DriverJobUpdateSubsystems());
        for(Iterator<SubsystemsListener> i = subsystemsListeners.iterator(); i.hasNext();) {
            i.next().addSubsystem(sub);
        }
    }

    public void removeNXT(NXT nxt) {
        batteries.remove(nxt);
        queueJob(new DriverJobUpdateBatteries());
    }

    public void removeSubsystem(Subsystem sub) {
        subsystems.remove(sub);
        sub.destroy();
        queueJob(new DriverJobUpdateSubsystems());
        for(Iterator<SubsystemsListener> i = subsystemsListeners.iterator(); i.hasNext();) {
            i.next().removeSubsystem(sub);
        }
    }

    public void driverChanged(Subsystem s) {
        for(Iterator<SubsystemsListener> i = subsystemsListeners.iterator(); i.hasNext();) {
            i.next().subsystemDriverChanged(s);
        }
    }

	public List<Driver> getDrivers() {
		return drivers;
	}
	public List<Subsystem> getSubsystems() {
		return subsystems;
	}
	public List<Battery> getBatteries() {
		return batteries;
	}
	
	public void addDriver(Driver driver) {
		drivers.add(driver);
        for(Iterator<DriverListener> i = driverListeners.iterator(); i.hasNext();) {
            i.next().addDriver(driver);
        }
	}

	public void removeDriver(Driver driver) {
		drivers.remove(driver);
        log("driver " + driver.getName() + " disconnected");
        for(Iterator<DriverListener> i = driverListeners.iterator(); i.hasNext();) {
            i.next().removeDriver(driver);
        }
    }
	
	public synchronized void log(String msg) {
        if(Looper.getMainLooper() == Looper.myLooper()) {
            queueJob(new DriverJobLog(msg));
        } else {
            for(Iterator<Driver> i = drivers.iterator(); i.hasNext(); ) {
                i.next().log(msg);
            }
        }
		System.out.println(msg);
	}

    public void updateNXT(NXT nxt) {
        if(nxtListener != null) {
            nxtListener.stateChanged(nxt);
        }
    }

    public interface SubsystemsListener {
        public void addSubsystem(Subsystem s);
        public void removeSubsystem(Subsystem s);
        public void subsystemDriverChanged(Subsystem s);
    }

    public interface DriverListener {
        public void addDriver(Driver d);
        public void removeDriver(Driver d);
    }
}
