package whs.bot.nxt;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.Iterator;
import java.util.List;

public class NXTActivity extends AppCompatActivity implements NXT.StateChangeListener {

    public static final String ARG_NXT_ADDR = "nxtaddr";
    private RobotService robotService;
    private NXTStub stub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nxt);

        final String addr;

        Bundle args = getIntent().getExtras();
        if(args != null && args.containsKey(ARG_NXT_ADDR)) {
            addr = args.getString(ARG_NXT_ADDR);
        } else {
            addr = null;
        }

        Intent intent = new Intent(this, RobotService.class);
        this.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                synchronized (NXTActivity.this) {
                    robotService = ((RobotService.Binder) service).getService();
                    List<NXTStub> stubs = robotService.getNxtStubs();
                    stub = stubs.get(0);
                    if (addr != null) {
                        for (Iterator<NXTStub> i = stubs.iterator(); i.hasNext(); ) {
                            NXTStub s = i.next();
                            if (s.getBtDev().getAddress().equals(addr)) {
                                stub = s;
                                break;
                            }
                        }
                    }

                    stub.connect(robotService.getRobot());
                    buildUi(stub);
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                synchronized (NXTActivity.this) {
                    robotService = null;
                }
            }
        }, Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT);
    }

    private EditText name;
    private TextView status;
    private ProgressBar battery;

    private void buildUi(NXTStub stub) {
        this.setTitle("NXT: " + stub.getName());
        name = (EditText) findViewById(R.id.nxt_detail_name);
        name.setText(stub.getName());
        name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dialog = new SetNXTNameDialog();
                dialog.show(getSupportFragmentManager(), "setnxtname");
            }
        });

        TextView addr = (TextView) findViewById(R.id.nxt_detail_addr);
        addr.setText(stub.getBtDev().getAddress());

        status = (TextView) findViewById(R.id.nxt_detail_status);
        battery = (ProgressBar) findViewById(R.id.nxt_detail_battery);
        battery.setMax(10000);

        robotService.addNXTListener(this);
        stateChanged(stub.getNXT());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        robotService.removeNXTListener(this);
    }

    @Override
    public void stateChanged(final NXT nxt) {
        if(nxt == this.stub.getNXT()) {
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    name.setText(nxt.getStub().getName());
                    status.setText(nxt.getConnectionStatus().toString());
                    battery.setProgress(nxt.getBatteryLevel());
                }
            });
        }
    }

    public static class SetNXTNameDialog extends DialogFragment {
        private NXTActivity activity;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            final EditText field = new EditText(getActivity());
            field.setText(activity.stub.getName());

            builder.setMessage(R.string.set_nxt_name)
                    .setView(field)
                    .setPositiveButton(R.string.set_nxt_name_confirm, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            activity.changeName(field.getText().toString());
                        }
                    })
                    .setNegativeButton(R.string.set_nxt_name_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            return builder.create();
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            this.activity = (NXTActivity) activity;
        }
    }

    private void changeName(String s) {
        stub.getNXT().changeBrickName(s);
    }
}
