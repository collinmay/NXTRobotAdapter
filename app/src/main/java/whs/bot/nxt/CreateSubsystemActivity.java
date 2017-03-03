package whs.bot.nxt;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

public class CreateSubsystemActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LEFT_MOTOR = 1;

    private RobotService robotService;

    private Spinner leftNXT;
    private Spinner leftPort;
    private Spinner rightNXT;
    private Spinner rightPort;

    private NXTStubAdapter leftNXTAdapter;
    private NXTStubAdapter rightNXTAdapter;

    private EditText nameField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_subsystem);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        leftNXT = (Spinner) findViewById(R.id.left_motor_nxt);
        leftPort = (Spinner) findViewById(R.id.left_motor_port);

        rightNXT = (Spinner) findViewById(R.id.right_motor_nxt);
        rightPort = (Spinner) findViewById(R.id.right_motor_port);

        leftNXTAdapter  = new NXTStubAdapter(this);
        rightNXTAdapter = new NXTStubAdapter(this);

        leftNXT.setAdapter(leftNXTAdapter);
        rightNXT.setAdapter(rightNXTAdapter);

         leftPort.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, NXTMotor.MotorPort.values()));
        rightPort.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, NXTMotor.MotorPort.values()));

        leftPort.setSelection(1);
        rightPort.setSelection(2);

        nameField = (EditText) findViewById(R.id.subsystem_add_name);

        Intent intent = new Intent(this, RobotService.class);
        bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                synchronized (CreateSubsystemActivity.this) {
                    robotService = ((RobotService.Binder) service).getService();

                    leftNXTAdapter.addAll(robotService.getNxtStubs());
                    robotService.addNXTListener(leftNXTAdapter);
                    leftNXTAdapter.notifyDataSetChanged();

                    rightNXTAdapter.addAll(robotService.getNxtStubs());
                    robotService.addNXTListener(rightNXTAdapter);
                    rightNXTAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                synchronized (CreateSubsystemActivity.this) {
                    robotService = null;
                }
            }
        }, Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT);

        final CheckBox  leftReverse = (CheckBox) findViewById(R.id. left_motor_reverse);
        final CheckBox rightReverse = (CheckBox) findViewById(R.id.right_motor_reverse);

        Button cancel = (Button) findViewById(R.id.create_subsystem_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button confirm = (Button) findViewById(R.id.create_subsystem_confirm);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(nameField.getText().length() == 0) {
                    Snackbar.make(findViewById(android.R.id.content), "Must specify a name", Snackbar.LENGTH_LONG).show();
                } else if(leftNXT.getSelectedItem() == rightNXT.getSelectedItem() && leftPort.getSelectedItem() == rightPort.getSelectedItem()) {
                    Snackbar.make(findViewById(android.R.id.content), "Must use two different motors", Snackbar.LENGTH_LONG).show();
                } else {
                    NXTMotor left  = ((NXTStub)  leftNXT.getSelectedItem()).getMotor((NXTMotor.MotorPort)  leftPort.getSelectedItem());
                    NXTMotor right = ((NXTStub) rightNXT.getSelectedItem()).getMotor((NXTMotor.MotorPort) rightPort.getSelectedItem());
                    if(left.inUse()) {
                        Snackbar.make(findViewById(android.R.id.content), "Left motor is already in use", Snackbar.LENGTH_LONG).show();
                    } else if(right.inUse()) {
                        Snackbar.make(findViewById(android.R.id.content), "Right motor is already in use", Snackbar.LENGTH_LONG).show();
                    } else {
                        left.setReversed(leftReverse.isChecked());
                        right.setReversed(rightReverse.isChecked());
                        NXTSubsystem sub = new NXTSubsystem(robotService.getRobot(),
                                robotService.getRobot().getSubsystems().size(),
                                nameField.getText().toString(), left, right);
                        robotService.getRobot().addSubsystem(sub);
                        finish();
                    }
                }
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        robotService.removeNXTListener(leftNXTAdapter);
        robotService.removeNXTListener(rightNXTAdapter);
    }
}
