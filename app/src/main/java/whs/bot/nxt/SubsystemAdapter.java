package whs.bot.nxt;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by misson20000 on 3/5/16.
 */
public class SubsystemAdapter extends ArrayAdapter<Subsystem> implements Robot.SubsystemsListener {
    private int resourceId;
    private Handler handler = new Handler();
    private boolean contextMode = false;
    private Set<Integer> checked = new HashSet<>();

    public SubsystemAdapter(Context context) {
        super(context, R.layout.item_subsystem);
        this.resourceId = R.layout.item_subsystem;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewCache cache;

        if(row == null) {
            LayoutInflater inflater = ((Activity) this.getContext()).getLayoutInflater();
            row = inflater.inflate(resourceId, parent, false);

            cache = new ViewCache(
                    (TextView) row.findViewById(R.id.subsystem_name),
                    (TextView) row.findViewById(R.id.subsystem_driver),
                    (CheckBox) row.findViewById(R.id.subsystem_checkbox));
            row.setTag(cache);
        } else {
            cache = (ViewCache) row.getTag();
        }

        Subsystem sub = this.getItem(pos);
        cache.name.setText(sub.getName());
        cache.driver.setText(sub.getDriver() == null ? "No Driver" : sub.getDriver().getName());
        cache.selected.setVisibility(contextMode ? View.VISIBLE : View.GONE);
        cache.selected.setChecked(checked.contains(pos));

        return row;
    }

    @Override
    public View getDropDownView(int pos, View convertView, ViewGroup parent) {
        return getView(pos, convertView, parent);
    }

    @Override
    public void addSubsystem(final Subsystem s) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                add(s);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public void removeSubsystem(final Subsystem s) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                remove(s);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public void subsystemDriverChanged(final Subsystem s) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    public void clearChecked() {
        checked.clear();
        notifyDataSetChanged();
    }

    public void setChecked(int pos, boolean checked) {
        if(checked) {
            this.checked.add(pos);
        } else {
            this.checked.remove(pos);
        }
        notifyDataSetChanged();
    }

    public void enterContextMode() {
        contextMode = true;
        notifyDataSetChanged();
    }

    public void exitContextMode() {
        contextMode = false;
        notifyDataSetChanged();
    }

    public void deleteChecked(Robot r) {
        List<Subsystem> toDelete = new ArrayList<>();
        for(Iterator<Integer> i = checked.iterator(); i.hasNext();) {
            toDelete.add(this.getItem(i.next()));
        }
        for(Iterator<Subsystem> i = toDelete.iterator(); i.hasNext();) {
            r.removeSubsystem(i.next());
        }
    }

    private class ViewCache {
        public TextView name;
        public TextView driver;
        public CheckBox selected;

        public ViewCache(TextView name, TextView driver, CheckBox selected) {
            this.name = name;
            this.driver = driver;
            this.selected = selected;
        }
    }
}
