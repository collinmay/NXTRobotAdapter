package whs.bot.nxt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.widget.ProgressBar;

/*
 * Multithreading?
 *  - battery thread
 *     checks battery life
 *  - output thread
 *     outputs motor powers
 *  locks:
 *  - NXT monitor
 *    - output buffer
 */


public class NXT implements Battery {
	private BluetoothDevice dev;
	private BluetoothSocket sock;
	private OutputStream outStream;
	private InputStream inStream;
	
	private Robot robot;
	private Thread thread;
	
	private NXTMotor[] motors;
	
	private short battery;
	
	private ByteBuffer out = ByteBuffer.allocate(66);
	private ByteBuffer in = ByteBuffer.allocate(66);
	private long nextUpdate;

    private ConnectionStatus status = ConnectionStatus.DISCONNECTED;

    private List<StateChangeListener> stateListeners;

    private boolean alive = true;

    private NXTStub stub;
    private String changeBrickName;

    private double hertz(int midi) {
		return 440.0 * Math.pow(Math.pow(2, 1.0/12.0), midi-69);
	}
	
	private synchronized void playTone(int midi, int duration) throws IOException {
		out.put((byte) 0x80);
		out.put((byte) 0x03);
		out.putShort((short) hertz(midi));
		out.putShort((short) duration);
		writeCmd();
	}
	
	private void writeCmd() throws IOException {
        out.flip();
        int len = out.remaining() - 2;
        out.putShort((short) len);
        out.position(0);
        outStream.write(out.array(), out.arrayOffset() + out.position(), out.remaining());
        out.clear();
        out.position(2);
        nextUpdate = System.currentTimeMillis() + 20;
	}
	
	private int readCmd() throws IOException {
		in.position(in.position() + inStream.read(in.array(), in.arrayOffset() + in.position(), in.remaining()));
		in.flip();
		return in.getShort();
	}

    public void kill() {
        alive = false;
    }

	public NXT(final NXTStub stub, final Robot rob, final BluetoothDevice dev) {
		this.dev = dev;
		this.robot = rob;
		this.stateListeners = new ArrayList<>();
		this.stub = stub;
        this.motors = new NXTMotor[] {stub.getMotor(NXTMotor.MotorPort.A), stub.getMotor(NXTMotor.MotorPort.B), stub.getMotor(NXTMotor.MotorPort.C)};

		final byte[] prefix = new byte[] { (byte) 0x80,  0x04 };
		final byte[] postfix = new byte[] { 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00 };
		
		thread = new Thread(new Runnable() {
			@Override
			public void run() {
				while(alive) {
                    sock = null;
					try {
                        synchronized(NXT.this) {
                            if (status == ConnectionStatus.DISCONNECTED) {
                                status = ConnectionStatus.CONNECTING;
                            } else {
                                status = ConnectionStatus.RECONNECTING;
                            }
                            updateState();
                        }

                        robot.log("attempting connection to " + getName() + " (" + dev.getAddress() + ")");
                        try {
                            sock = dev.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                            sock.connect();
                        } catch(IOException e) {
                            robot.log("normal attempt failed, trying fallback...");
                            try {
                                Method method = dev.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                                sock = (BluetoothSocket) method.invoke(dev, Integer.valueOf(1));
                                sock.connect();
                            } catch(Exception ree) {
                                throw new IOException(ree);
                            }
                        }
						inStream = sock.getInputStream();
						outStream = sock.getOutputStream();
						robot.log("established connection to " + getName() + " (" + dev.getAddress() + ")");

                        synchronized(NXT.this) {
                            status = ConnectionStatus.CONNECTED;
                            updateState();
                        }

						for(int i = 0; i < motors.length; i++) {
                            motors[i].setOutPower(0);
                        }
						
						nextUpdate = System.currentTimeMillis() + 40;
						long nextBattery = nextUpdate;

                        out.clear();
                        out.position(2);
                        out.order(ByteOrder.LITTLE_ENDIAN);

                        in.clear();
                        in.order(ByteOrder.LITTLE_ENDIAN);

						while(alive) {
							try {
								Thread.sleep(Math.max(0, System.currentTimeMillis() - nextUpdate));
							} catch (InterruptedException e) { }
							
							if(System.currentTimeMillis() > nextUpdate) {
								if(System.currentTimeMillis() > nextBattery) {
									out.put((byte) 0x00);
									out.put((byte) 0x0B);
									writeCmd();
									int len = readCmd();
									in.mark();
									if(in.get() != 0x02) {
										in.reset();
										in.position(in.position() + len);
										continue;
									}
									if(in.get() != 0x0B) {
										in.reset();
										in.position(in.position() + len);
										continue;
									}
									Status status = getStatus((char) in.get());
									if(status != Status.SUCCESS) {
										rob.log("error checking battery: " + status.getMessage());
										in.reset();
										in.position(in.position() + len);
									} else {
										battery = in.getShort();
                                        updateState();
										nextBattery+= 500;
									}
									in.reset();
									in.position(in.position() + len);
								} else {
                                    for(int i = 0; i < motors.length; i++) {
                                        NXTMotor m = motors[i];
                                        if(m.getInPower() != m.getOutPower()) {
                                            m.setOutPower(m.getInPower());
                                            out.put(prefix);
                                            out.put((byte) m.getPort().ordinal());
                                            out.put((byte) (m.getOutPower() * 100));
                                            out.put(postfix);
                                            writeCmd();
                                        }
                                    }
                                    if(changeBrickName != null) {
                                        out.put((byte) 0x81);
                                        out.put((byte) 0x98);
                                        out.put(changeBrickName.getBytes());
                                        for(int i = 16-changeBrickName.length(); i > 0; i--) {
                                            out.put((byte) 0);
                                        }
                                        writeCmd();
                                        stub.setName(changeBrickName);
                                        updateState();
                                        changeBrickName = null;
                                    }
								}
							}
						}
					} catch (IOException e) {
						e.printStackTrace();
                        try {
                            Thread.sleep(1500);
                        } catch (InterruptedException ie) { }
					} finally {
                        if(sock != null) {
                            try {
                                sock.close();
                            } catch(IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
				}
			}
		});
		
		thread.start();
						////                                     direct motor port  power               mode  reg   turn  state tacho limit
						//data = new byte[] { 0x0c, 0x00, (byte) 0x80,  0x04, 0x02, (byte) (left*100),  0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00,
						//    			   	 0x0c, 0x00, (byte) 0x80,  0x04, 0x01, (byte) (right*100), 0x07, 0x00, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00 };
	}
	
	public String getName() {
		return "NXT " + stub.getName();
	}

	public void setMotor(NXTMotor.MotorPort port, double power) {
		this.thread.interrupt();
	}
	
	public int getBatteryLevel() {
		return battery;
	}

    public ConnectionStatus getConnectionStatus() {
        return status;
    }

	private Status getStatus(char stat) {
		switch(stat) {
		case 0: return Status.SUCCESS;
		case 0x20: return Status.PENDING_COMM;
		case 0x40: return Status.MBOX_QUEUE_EMPTY;
		case 0xBD: return Status.REQUEST_FAILED;
		case 0xBE: return Status.UNKNOWN_OPCODE;
		case 0xBF: return Status.INSANE_PACKET;
		case 0xC0: return Status.OUT_OF_RANGE;
		case 0xDD: return Status.COMM_BUS_ERROR;
		case 0xDE: return Status.OOM_COMM_BUFFER;
		case 0xDF: return Status.CHANCON_INVALID;
		case 0xE0: return Status.CHANCON_BUSY;
		case 0xEC: return Status.NO_PROGRAM;
		case 0xED: return Status.BAD_SIZE;
		case 0xEE: return Status.BAD_MBOX_QUEUE;
		case 0xEF: return Status.BAD_STRUCTURE;
		case 0xF0: return Status.BAD_IO;
		case 0xFB: return Status.OOM;
		case 0xFF: return Status.BAD_ARG;
		default:
			return Status.UNKNOWN;
		}
	}

    private synchronized void updateState() {
        robot.updateNXT(this);
        this.stub.updateStatus();
    }

    public void changeBrickName(String s) {
        this.changeBrickName = s;
        this.thread.interrupt();
    }

    public NXTStub getStub() {
        return stub;
    }

    public enum Status {
		SUCCESS("success"),
		PENDING_COMM("pending communication transaction"),
		MBOX_QUEUE_EMPTY("mailbox queue empty"),
		REQUEST_FAILED("request failed (i.e. specified file not found)"),
		UNKNOWN_OPCODE("unknown command opcode"),
		INSANE_PACKET("insane packet"),
		OUT_OF_RANGE("data contains out-of-range values"),
		COMM_BUS_ERROR("communication bus error"),
		OOM_COMM_BUFFER("no free memory in communication buffer"),
		CHANCON_INVALID("specified channel/connection is not valid"),
		CHANCON_BUSY("specified channel/connection is not configured or busy"),
		NO_PROGRAM("no active program"),
		BAD_SIZE("illegal size speicifed"),
		BAD_MBOX_QUEUE("invalid mailbox queue ID specified"),
		BAD_STRUCTURE("attempted to access invalid field of a structure"),
		BAD_IO("bad input or output specified"),
		OOM("insufficient memory available"),
		BAD_ARG("bad arguments"),
		UNKNOWN("bad status byte received");
		
		private String message;
		Status(String msg) {
			this.message = msg;
		}
		
		public String getMessage() {
			return message;
		}
	}

    public enum ConnectionStatus {
        DISCONNECTED("Disconnected"),
        CONNECTING("Connecting..."),
        RECONNECTING("Reconnecting..."),
        CONNECTED("Connected");

        private String msg;

        ConnectionStatus(String msg) {
            this.msg = msg;
        }

        public String toString() {
            return msg;
        }
    }

    public interface StateChangeListener {
        public void stateChanged(NXT nxt);
    }
}
