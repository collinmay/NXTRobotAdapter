package whs.bot.nxt;

import java.io.IOException;

/**
 * Created by misson20000 on 3/5/16.
 */
public class DriverJobUpdateSubsystems implements DriverJob {

    @Override
    public void run(Driver driver) throws IOException {
        driver.writeSubsystemInfo();
        driver.log("wrote subsystem info");
    }
}
