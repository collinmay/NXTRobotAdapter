package whs.bot.nxt;

import java.nio.ByteBuffer;

public class NXTSubsystem extends Subsystem {
	private NXTMotor left;
    private NXTMotor right;
	private String name;
    private boolean active;

	public NXTSubsystem(Robot r, int id, String name, NXTMotor left, NXTMotor right) {
		super(r, id);
        this.name = name;
		this.left = left;
        this.right = right;
        left.setInUse(true);
        right.setInUse(true);

        active = left.isOnline() && right.isOnline();
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getNumericType() {
		return 1;
	}
	
	//private short lastLeft = 0;
	//private short lastRight = 0;
	
	@Override
	public synchronized void update(ByteBuffer inBuffer) {
		double left = inBuffer.getShort()/2048.0;
		double right = inBuffer.getShort()/2048.0;

        if(active) {
            this.left.setPower(left);
            this.right.setPower(right);
        }
	}

    public synchronized void destroy() {
        this.active = false;
        this.left.setInUse(false);
        this.right.setInUse(false);
    }

    public synchronized void statusChanged(NXTStub stub) {
        if(!(left.getStub().isOnline() && right.getStub().isOnline())) {
            if(active) {
                active = false;
                this.left.setPower(0);
                this.right.setPower(0);
            }
        } else {
            if(!active) {
                active = true;
            }
        }
    }

	/*@Override
	public boolean hasUpdate() {
		return lastLeft != nxt.getLeftOutput() || lastRight != nxt.getRightOutput();
	}
	
	@Override
	public void writeUpdate(ByteBuffer buf) {
		buf.putShort((short) 0);
		buf.putShort((short) 0);
		buf.putShort(nxt.getLeftOutput());
		buf.putShort((short) 0);
		buf.putShort((short) 0);
		buf.putShort(nxt.getRightOutput());
		lastLeft = nxt.getLeftOutput();
		lastRight = nxt.getRightOutput();
	}*/
}
