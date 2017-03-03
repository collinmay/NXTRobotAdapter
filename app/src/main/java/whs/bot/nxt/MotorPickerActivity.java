package whs.bot.nxt;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class MotorPickerActivity extends AppCompatActivity {

    public static final String ARG_TITLE = "title";

    public static final String RESULT_NXT_ADDR = "nxtaddr";
    public static final String RESULT_MOTOR_PORT = "motorport";
    public static final int RESULT_CODE_SUCCESS = 1;
    public static final int RESULT_CODE_FAILIURE = 0;

    private RobotService robotService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motor_picker);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            if(extras.containsKey(ARG_TITLE)) {
                this.setTitle(extras.getString(ARG_TITLE));
            }
        }

        final Spinner nxt = (Spinner) this.findViewById(R.id.motor_picker_nxt);
        final ArrayAdapter<NXTStub> nxtAdapter = new NXTStubAdapter(this);
        nxt.setAdapter(nxtAdapter);

        final Spinner port = (Spinner) this.findViewById(R.id.motor_picker_port);
        final ArrayAdapter<NXTMotor.MotorPort> portAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, NXTMotor.MotorPort.values());
        port.setAdapter(portAdapter);

        Intent intent = new Intent(this, RobotService.class);
        bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                synchronized (MotorPickerActivity.this) {
                    robotService = ((RobotService.Binder) service).getService();
                    nxtAdapter.addAll(robotService.getNxtStubs());
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                synchronized (MotorPickerActivity.this) {
                    robotService = null;
                }
            }
        }, Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT);

        Button pick = (Button) this.findViewById(R.id.pick_motor_pick);
        pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent result = new Intent();
                result.putExtra(RESULT_NXT_ADDR, ((NXTStub) nxt.getSelectedItem()).getBtDev().getAddress());
                result.putExtra(RESULT_MOTOR_PORT, port.getSelectedItemPosition());
                setResult(RESULT_CODE_SUCCESS, result);
                finish();
            }
        });

        Button cancel = (Button) this.findViewById(R.id.pick_motor_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CODE_FAILIURE);
                finish();
            }
        });
    }

}
