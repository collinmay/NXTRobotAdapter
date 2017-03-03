package whs.bot.nxt;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.debug.hv.ViewServer;

public class MainActivity extends AppCompatActivity {

    private Thread discoveryThread;
    private Thread serverThread;
    private Robot robot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.robot = new Robot();

        startService(new Intent(this, RobotService.class));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        viewPager.setAdapter(new ViewPagerAdapter());

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        ViewServer.get(this).addWindow(this);

        this.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ViewServer.get(this).removeWindow(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        ViewServer.get(this).setFocusedWindow(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class ViewPagerAdapter extends FragmentPagerAdapter {
        public ViewPagerAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public Fragment getItem(int position) {
            switch(position) {
                case 0: return new NXTListFragment();
                case 1: return new SubsystemsFragment();
                case 2: return new DriversFragment();
                default:
                    return new NXTListFragment();
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch(position) {
                case 0: return "NXTs";
                case 1: return "Subsystems";
                case 2: return "Drivers";
            }
            return "";
        }
    }

    public static class NXTListFragment extends Fragment {

        private RobotService robotService;

        private NXTStubAdapter adapter;

        public NXTListFragment() {

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.fragment_nxtlist, container, false);

            adapter = new NXTStubAdapter(this.getContext());

            Intent intent = new Intent(getActivity(), RobotService.class);
            getActivity().bindService(intent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (NXTListFragment.this) {
                        robotService = ((RobotService.Binder) service).getService();
                        adapter.addAll(robotService.getNxtStubs());
                        robotService.addNXTListener(adapter);
                        adapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    synchronized (NXTListFragment.this) {
                        robotService = null;
                    }
                }
            }, Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT);

            ListView l = (ListView) root.findViewById(R.id.bt_device_list);
            l.setAdapter(adapter);
            l.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    synchronized (NXTListFragment.this) {
                        if (robotService != null) {
                            NXTStub stub = adapter.getItem(position);
                            if (stub.getNXT() == null) {
                                NXT nxt = stub.connect(robotService.getRobot());
                                robotService.connectNxt(nxt);
                                adapter.notifyDataSetChanged();
                            } else {
                                stub.disconnect(robotService.getRobot());
                                adapter.notifyDataSetChanged();
                            }
                        }
                    }
                }
            });
            l.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    if(robotService != null) {
                        NXTStub stub = adapter.getItem(position);
                        Intent nxtIntent = new Intent(getActivity(), NXTActivity.class);
                        nxtIntent.putExtra(NXTActivity.ARG_NXT_ADDR, stub.getBtDev().getAddress());
                        startActivity(nxtIntent);
                        return true;
                    }
                    return false;
                }
            });

            return root;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            robotService.removeNXTListener(adapter);
        }
    }

    public static class SubsystemsFragment extends Fragment {
        private RobotService robotService;
        private SubsystemAdapter adapter;

        public SubsystemsFragment() {

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.fragment_subsystems, container, false);

            final ListView l = (ListView) root.findViewById(R.id.subsystem_list);
            adapter = new SubsystemAdapter(this.getContext());
            l.setAdapter(adapter);

            l.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    l.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
                    l.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
                        @Override
                        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                            adapter.setChecked(position, checked);
                        }

                        @Override
                        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                            MenuInflater inflater = getActivity().getMenuInflater();
                            inflater.inflate(R.menu.subsystem_contextual_menu, menu);
                            adapter.enterContextMode();
                            adapter.clearChecked();
                            return true;
                        }

                        @Override
                        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                            return false;
                        }

                        @Override
                        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                            switch(item.getItemId()) {
                                case R.id.subsystem_cam_delete:
                                    adapter.deleteChecked(robotService.getRobot());
                                    break;
                            }
                            mode.finish();
                            return false;
                        }

                        @Override
                        public void onDestroyActionMode(ActionMode mode) {
                            adapter.exitContextMode();
                        }
                    });
                    return false;
                }
            });

            FloatingActionButton fab = (FloatingActionButton) root.findViewById(R.id.subsystem_fab_add);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), CreateSubsystemActivity.class);
                    startActivity(intent);
                }
            });

            Intent intent = new Intent(getActivity(), RobotService.class);
            getActivity().bindService(intent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (SubsystemsFragment.this) {
                        robotService = ((RobotService.Binder) service).getService();
                        adapter.addAll(robotService.getRobot().getSubsystems());
                        adapter.notifyDataSetChanged();
                        robotService.getRobot().addSubsystemsListener(adapter);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    synchronized (SubsystemsFragment.this) {
                        robotService = null;
                    }
                }
            }, Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT);

            return root;
        }

        @Override
        public void onDestroy() {
            robotService.getRobot().removeSubsystemsListener(adapter);
        }
    }

    public static class DriversFragment extends Fragment {

        private RobotService robotService;
        private DriverAdapter adapter;

        public DriversFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View root = inflater.inflate(R.layout.fragment_drivers, container, false);

            final ListView l = (ListView) root.findViewById(R.id.driver_list);
            adapter = new DriverAdapter(this.getContext());
            l.setAdapter(adapter);

            Intent intent = new Intent(getActivity(), RobotService.class);
            getActivity().bindService(intent, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (DriversFragment.this) {
                        robotService = ((RobotService.Binder) service).getService();
                        adapter.addAll(robotService.getRobot().getDrivers());
                        adapter.notifyDataSetChanged();
                        robotService.getRobot().addDriverListener(adapter);
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    synchronized (DriversFragment.this) {
                        robotService = null;
                    }
                }
            }, Context.BIND_AUTO_CREATE | Context.BIND_ABOVE_CLIENT);

            return root;
        }

        @Override
        public void onDestroy() {
            robotService.getRobot().removeDriverListener(adapter);
        }
    }
}
