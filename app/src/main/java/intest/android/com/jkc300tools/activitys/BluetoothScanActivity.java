package intest.android.com.jkc300tools.activitys;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import intest.android.com.jkc300tools.R;
import intest.android.com.jkc300tools.adapters.BluetoothScanAdapter;
import intest.android.com.jkc300tools.beans.BlueDevice;
import intest.android.com.jkc300tools.utils.MyLogger;

/**
 * 蓝牙搜索界面
 */
public class BluetoothScanActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, View.OnClickListener {

    private ListView lv_bluetoth_devices;
    private BluetoothScanAdapter bluetoothScanAdapter;

    /* 取得默认的蓝牙适配器 */
    private BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

    private List<BlueDevice> list_dev = null;
    private Button start_seach;
    private boolean seaching = false;
    private ImageView iv_run_scan;
    private TextView tv_title_right_id;
    private int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_scan);
        initView();
        initData();
        initBluetoothScan();

        //isWifi(getApplicationContext());
        getPermission();
    }

    private void getPermission() {
        //判断是否有权限
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //请求权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
            //判断是否需要 向用户解释，为什么要申请该权限
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                //Toast.makeText(this, "shouldShowRequestPermissionRationale", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void isWifi(Context mContext) {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetInfo != null && activeNetInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            //return true;
            MyLogger.jLog().e("打开wifi");
        } else {
            MyLogger.jLog().e("关闭wifi");
        }
        //return false;
    }

    @Override
    protected void onDestroy() {
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
            iv_run_scan.clearAnimation();
        }
        destoryBluetothScanBroadcastReceiver();
        super.onDestroy();
    }

    private void initView() {
        lv_bluetoth_devices = (ListView) findViewById(R.id.lv_bluetoth_devices);
        lv_bluetoth_devices.setOnItemClickListener(this);

        start_seach = (Button) findViewById(R.id.start_seach);
        start_seach.setOnClickListener(this);

        TextView tv_title_id = (TextView) findViewById(R.id.tv_title_id);
        tv_title_id.setText("蓝牙搜索");

        iv_run_scan = (ImageView) findViewById(R.id.iv_run_scan);
        runTesting(iv_run_scan);

        tv_title_right_id = (TextView) findViewById(R.id.tv_title_right_id);
        tv_title_right_id.setOnClickListener(this);
        tv_title_right_id.setVisibility(View.VISIBLE);
    }

    private void initData() {
        bluetoothScanAdapter = new BluetoothScanAdapter(getApplicationContext());
        lv_bluetoth_devices.setAdapter(bluetoothScanAdapter);
    }

    private void initBluetoothScan() {
        if (!mBtAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 3);
        } else {
            list_dev = new ArrayList<>();
            registerBluetothScanBroadcastReceiver();
            start_seach.setText("正在搜索...");
            tv_title_right_id.setText("停止搜索");
            seaching = false;
            mBtAdapter.startDiscovery();
        }
    }

    private void registerBluetothScanBroadcastReceiver() {
        IntentFilter discoveryFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, discoveryFilter);
    }

    private void destoryBluetothScanBroadcastReceiver() {
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        MyLogger.jLog().e("resultCode: " + resultCode);
        if (requestCode == 3 && resultCode == -1) {
            //蓝牙开启后开始搜索蓝牙设备
            list_dev = new ArrayList<>();
            registerBluetothScanBroadcastReceiver();
            start_seach.setText("正在搜索...");
            tv_title_right_id.setText("停止搜索");
            seaching = false;
            mBtAdapter.startDiscovery();
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                BlueDevice dev = new BlueDevice();
                dev.setMac(device.getAddress());
                dev.setName(device.getName());
                boolean isScanDevices = false;
                for (BlueDevice blueDevicelue :
                        list_dev) {
                    if (device.getAddress().equals(blueDevicelue.getMac())) {
                        isScanDevices = true;
                        break;
                    } else {
                        isScanDevices = false;
                    }
                }
                if (!isScanDevices) {
                    list_dev.add(dev);
                    bluetoothScanAdapter.setData(list_dev);
                    bluetoothScanAdapter.notifyDataSetChanged();
                }
                MyLogger.jLog().e("device.getName(): " + device.getName());
                // }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
            }
        }
    };

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BlueDevice blueDevice = bluetoothScanAdapter.getItem(position);
        MyLogger.jLog().e("blueDevice: " + blueDevice.getMac());
        Intent intent = new Intent(getApplicationContext(), BluetoothConnectionActivity.class);
        intent.putExtra("blueDevice", blueDevice);
        startActivity(intent);
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
            iv_run_scan.clearAnimation();
        }
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_seach:
            case R.id.tv_title_right_id:
                if (seaching) {
                    bluetoothScanAdapter.setData(null);
                    bluetoothScanAdapter.notifyDataSetChanged();
                    start_seach.setText("正在搜索");
                    tv_title_right_id.setText("停止搜索");
                    mBtAdapter.startDiscovery();
                    seaching = false;
                    runTesting(iv_run_scan);
                } else {
                    seaching = true;
                    mBtAdapter.cancelDiscovery();
                    start_seach.setText("开始搜索");
                    tv_title_right_id.setText("开始搜索");
                    iv_run_scan.clearAnimation();
                }
                break;
        }
    }

    private void runTesting(ImageView iv_runing) {
        Animation operatingAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.dialog_run_tip);
        LinearInterpolator lin = new LinearInterpolator();
        operatingAnim.setInterpolator(lin);
        iv_runing.startAnimation(operatingAnim);
    }
}
