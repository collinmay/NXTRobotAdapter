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

public class NXTStubAdapter extends ArrayAdapter<NXTStub> implements NXT.StateChangeListener {

	private int resourceId;

    private Handler handler = new Handler();

	public NXTStubAdapter(Context context) {
		super(context, R.layout.item_nxt);
		this.resourceId = R.layout.item_nxt;
	}

	@Override
	public View getView(int pos, View convertView, ViewGroup parent) {
		View row = convertView;
		ViewCache cache;
		
		if(row == null) {
			LayoutInflater inflater = ((Activity) this.getContext()).getLayoutInflater();
			row = inflater.inflate(resourceId, parent, false);
			
			cache = new ViewCache(
                    (TextView)    row.findViewById(R.id.nxt_bt_name),
                    (TextView)    row.findViewById(R.id.nxt_bt_addr),
                    (TextView)    row.findViewById(R.id.nxt_connstatus),
                    (ProgressBar) row.findViewById(R.id.nxt_battery));
			row.setTag(cache);
		} else {
			cache = (ViewCache) row.getTag();
		}
		
		NXTStub stub = this.getItem(pos);
		cache.name.setText(stub.getName());
		cache.addr.setText(stub.getBtDev().getAddress());
		cache.status.setText(stub.getStatus().toString());
        if(stub.getStatus() != NXT.ConnectionStatus.DISCONNECTED) {
            cache.battery.setVisibility(View.VISIBLE);
            cache.battery.setMax(10000);
            cache.battery.setProgress(stub.getNXT().getBatteryLevel());
        } else {
            cache.battery.setVisibility(View.INVISIBLE);
        }

		return row;
	}

    @Override
    public View getDropDownView(int pos, View convertView, ViewGroup parent) {
        return getView(pos, convertView, parent);
    }

    @Override
    public void stateChanged(NXT nxt) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    private class ViewCache {
		public TextView name;
		public TextView addr;
        public TextView status;
		public ProgressBar battery;

		public ViewCache(TextView name, TextView addr, TextView status, ProgressBar battery) {
			this.name = name;
			this.addr = addr;
            this.status = status;
            this.battery = battery;
		}
	}
}
