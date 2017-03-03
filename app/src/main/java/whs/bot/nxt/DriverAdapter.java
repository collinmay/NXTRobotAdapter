package whs.bot.nxt;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by misson20000 on 3/5/16.
 */
public class DriverAdapter extends ArrayAdapter<Driver> implements Robot.DriverListener {

    private int resourceId;
    private Handler handler = new Handler();

    public DriverAdapter(Context c) {
        super(c, R.layout.item_driver);
        resourceId = R.layout.item_driver;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewCache cache;

        if(row == null) {
            LayoutInflater inflater = ((Activity) this.getContext()).getLayoutInflater();
            row = inflater.inflate(resourceId, parent, false);

            cache = new ViewCache(
                    (TextView) row.findViewById(R.id.driver_name),
                    (TextView) row.findViewById(R.id.driver_addr));
            row.setTag(cache);
        } else {
            cache = (ViewCache) row.getTag();
        }

        Driver driver = this.getItem(pos);
        cache.name.setText(driver.getName());
        cache.addr.setText(driver.getAddress().getHostAddress());

        return row;
    }

    @Override
    public View getDropDownView(int pos, View convertView, ViewGroup parent) {
        return getView(pos, convertView, parent);
    }

    @Override
    public void addDriver(final Driver d) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                add(d);
            }
        });
    }

    @Override
    public void removeDriver(final Driver d) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                remove(d);
            }
        });
    }

    private class ViewCache {
        public TextView name;
        public TextView addr;

        public ViewCache(TextView name, TextView addr) {
            this.name = name;
            this.addr = addr;
        }
    }
}
