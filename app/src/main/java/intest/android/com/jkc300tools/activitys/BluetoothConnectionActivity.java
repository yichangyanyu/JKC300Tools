package intest.android.com.jkc300tools.activitys;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.UUID;

import intest.android.com.jkc300tools.R;
import intest.android.com.jkc300tools.beans.BlueDevice;
import intest.android.com.jkc300tools.utils.MyLogger;
import intest.android.com.jkc300tools.utils.NetWorkUtil;
import intest.android.com.jkc300tools.utils.ToastUtil;

/**
 *
 */
public class BluetoothConnectionActivity extends AppCompatActivity implements View.OnClickListener {

    private BlueDevice blueDevice;

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private clientThread clientConnectThread = null;
    public static BluetoothSocket bluetoothSocket = null;
    private ReadMsgThread mreadThread = null;
    private BluetoothDevice bluetoothDevice;

    private TextView tv_bluetooth_state;
    private TextView tv_bluetooth_message;
    private Button btn_bluetooth_order;
    private OutputStream bluetoothSocketOutputStream;
    private InputStream bluetoothSocketInputStream;

    private String host = "120.202.21.191";//172.16.17.155    172.16.13.105   120.202.21.191
    //60605   60611   60613
    private int port = 60613;//1234   60000     60605   60611
    private TextView tv_platform_state;

    private final int OUT_Time = 15 * 1000;
    //60s没有接受到蓝牙数据
    private final int reviceBluetoothDataTimeOut = 60 * 1000;
    private final int reviceBluetoothData = 2007;
    private int connectinBluetoothFail = 0;
    private int connectinPlatformFail = 0;
    private Socket socket;
    private ReceiverNewMsgThread receiverMsgThread;
    private SendMsgThread sendMsgThread;
    private byte[] receiver_b;
    private InputStream is;
    private OutputStream os;
    private boolean socketConnectioning = false;
    private long mExitTime = 0;
    private boolean heartTip = false;
    private PowerManager.WakeLock wakeLock;

    private Handler timerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 2000:
                    //蓝牙连接断开重新建立连接
                    tv_bluetooth_state.setText("正在重连蓝牙...");
                    shutdownClient();
                    connectionBluetoth();
                    break;
                case 2001:
                    //蓝牙连接成功
                    tv_bluetooth_state.setText("连接蓝牙成功");
                    Toast.makeText(getApplicationContext(), "连接蓝牙成功", Toast.LENGTH_SHORT).show();
                    cleanBluetooth();
                    break;
                case 2002:
                    //蓝牙连接失败
                    //Toast.makeText(getApplicationContext(), "蓝牙连接失败", Toast.LENGTH_SHORT).show();
                    /*shutdownClient();
                    connectionBluetoth();*/
                    shutdownClient();
                    tv_bluetooth_state.setText("连接蓝牙失败，重新连接");
                    cleanBluetooth();
                    break;
                case 2003:
                    //接受到的蓝牙的数据
                    int messgae = (int) msg.obj;
                    tv_bluetooth_message.setText("上行转发：" + messgae + "字节");
                    break;
                case 2004:
                    //接受到的平台的数据
                    String platformMessgae = (String) msg.obj;
                    //tv_bluetooth_message.setText(messgae);
                    break;
                case 2005:
                    tv_platform_state.setText("连接平台成功");
                    Toast.makeText(getApplicationContext(), "连接平台成功", Toast.LENGTH_SHORT).show();
                    cleanPlatform();
                    break;
                case 2006:
                    try {
                        tv_platform_state.setText("正在连接平台...");
                        connectionPlatform();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case reviceBluetoothData:
                    //重连蓝牙、平台
                    tv_bluetooth_state.setText("正在重连蓝牙...");
                    tv_platform_state.setText("正在连接平台...");
                    MyLogger.jLog().e("正在重连蓝牙...");
                    MyLogger.jLog().e("正在重连平台...");
                    shutdownClient();
                    connectionBluetoth();
                    connectionPlatform();
                    break;
                case 2008:
                    //连接平台失败
                    tv_platform_state.setText("连接平台失败，重新连接");
                    closedConnect();
                    cleanPlatform();
                    break;
            }
        }
    };
    private ImageView iv_connection_bluetooth;
    private ImageView iv_connection_platform;
    //private HeartMsgThread heartMsgThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);
        //屏幕常亮
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //创建PowerManager对象
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        //保持cpu一直运行，不管屏幕是否黑屏
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CPUKeepRunning");
        wakeLock.acquire();

        getIntentData();
        initView();
        connectionBluetoth();
        connectionPlatform();
        //connectionPlatform(host, port);
        //setWindowBrightness(100);
    }

    //设置当前窗口亮度
    private void setWindowBrightness(int brightness) {
        try {
            Window window = getWindow();
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.screenBrightness = brightness / 255.0f;
            window.setAttributes(lp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothSocket = null;
        }
        shutdownClient();
        closedConnect();
        if (timerHandler != null) {
            timerHandler.removeMessages(2002);
            timerHandler.removeMessages(reviceBluetoothData);
        }
        if (wakeLock != null) {
            wakeLock.release();
        }
        super.onDestroy();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        blueDevice = intent.getParcelableExtra("blueDevice");
        MyLogger.jLog().e("blueDevice:" + blueDevice.getMac());
    }

    private void initView() {
        tv_bluetooth_state = (TextView) findViewById(R.id.tv_bluetooth_state);
        tv_platform_state = (TextView) findViewById(R.id.tv_platform_state);
        tv_bluetooth_message = (TextView) findViewById(R.id.tv_bluetooth_message);
        btn_bluetooth_order = (Button) findViewById(R.id.btn_bluetooth_order);
        btn_bluetooth_order.setOnClickListener(this);

        TextView tv_title_id = (TextView) findViewById(R.id.tv_title_id);
        tv_title_id.setText("数据交互");

        iv_connection_bluetooth = (ImageView) findViewById(R.id.iv_connection_bluetooth);
        iv_connection_platform = (ImageView) findViewById(R.id.iv_connection_platform);

        TextView tv_bluetooth_platform_port = (TextView) findViewById(R.id.tv_bluetooth_platform_port);
        tv_bluetooth_platform_port.setText("连接平台端口: " + port);

        TextView tv_version = (TextView) findViewById(R.id.tv_version);
        try {
            tv_version.setText("版本号: " + getMobileVisionName(getApplicationContext()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getMobileVisionName(Context context) {
        String vision = "";
        try {
            vision = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vision;
    }

    /**************************和蓝牙建立连接****************************/
    //和蓝牙建立连接
    private void connectionBluetoth() {
        if (blueDevice != null) {
            String mac = blueDevice.getMac();
            if (!mac.equals("null")) {
                runTesting(iv_connection_bluetooth);
                bluetoothDevice = mBluetoothAdapter.getRemoteDevice(mac);
                clientConnectThread = new clientThread();
                clientConnectThread.start();
                // 创建一个Socket连接：只需要服务器在注册时的UUID号
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_bluetooth_order:
                tv_bluetooth_state.setText("正在连接蓝牙...");
                tv_platform_state.setText("正在连接平台...");
                timerHandler.removeMessages(reviceBluetoothData);
                timerHandler.sendEmptyMessageDelayed(reviceBluetoothData, reviceBluetoothDataTimeOut);
                shutdownClient();
                connectionBluetoth();
                connectionPlatform();
                break;
        }
    }

    /*
     * 数据通信部分代码
     */
    // 开启客户端
    private class clientThread extends Thread {
        public void run() {
            try {
                // 创建一个Socket连接：只需要服务器在注册时的UUID号
                // socket =
                // device.createRfcommSocketToServiceRecord(BluetoothProtocols.OBEX_OBJECT_PUSH_PROTOCOL_UUID);
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID
                        .fromString("00001101-0000-1000-8000-00805F9B34FB"));
                // 连接
//				Message msg2 = new Message();
//				Log.e("zhanglian", "it is on the connectting");
//				msg2.obj = "请稍候，正在连接服务器:" + Bluetooth.BlueToothAddress;
//				msg2.what = 0;
//				LinkDetectedHandler.sendMessage(msg2);

                bluetoothSocket.connect();
                timerHandler.sendEmptyMessageDelayed(2001, 0);

//				Message msg = new Message();
//				msg.obj = "已经连接上服务端！";
//				msg.what = 0;
//				LinkDetectedHandler.sendMessage(msg);
                // 启动接收数据
                mreadThread = new ReadMsgThread();
                mreadThread.start();

//				timerHandler.sendEmptyMessageDelayed(1000, 2 * 1000) ;
                timerHandler.sendEmptyMessageDelayed(1000, 300);
                MyLogger.jLog().e("it is conected连接蓝牙");
                connectinBluetoothFail = 0;
                timerHandler.sendEmptyMessageDelayed(reviceBluetoothData, reviceBluetoothDataTimeOut);
            } catch (IOException e) {
                MyLogger.jLog().e("it is has a error on the connect连接蓝牙");
                connectinBluetoothFail++;
                if (connectinBluetoothFail < 2) {
                    try {
                        Thread.sleep(2 * 1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    timerHandler.sendEmptyMessageDelayed(2000, 0);
                } else {
                    timerHandler.sendEmptyMessageDelayed(2002, 0);
                }
//				Message msg = new Message();
//				msg.obj = "连接服务端异常！断开连接重新试一试。";
//				msg.what = 0;
//				LinkDetectedHandler.sendMessage(msg);
//				timerHandler.sendEmptyMessageDelayed(1000, 2* 1000) ;
                //handlerTheErrorMsg ();
                e.printStackTrace();
            }
        }
    }

    private void connectionPlatform() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!NetWorkUtil.hasNetWork(getApplicationContext())) {
                    tv_platform_state.setText("与平台连接断开");
                    ToastUtil.showMessage(getApplicationContext(), "请检测手机网络!");
                    return;
                }
                if (!socketConnectioning) {
                    runTesting(iv_connection_platform);
                    socketConnectioning = true;
                    closedConnect();
                    connectionPlatform(host, port);
                    MyLogger.jLog().e("conection platform ing");
                } else {
                    MyLogger.jLog().e("conection platform ing，丢弃");
                }
            }
        });
    }

    private class ReadMsgThread extends Thread {
        @Override
        public void run() {
            try {
                bluetoothSocketInputStream = bluetoothSocket.getInputStream();
                bluetoothSocketOutputStream = bluetoothSocket.getOutputStream();
                byte[] rec_b = new byte[2048];
                while (null != bluetoothSocket && bluetoothSocket.isConnected()) {
                    int length = bluetoothSocketInputStream.read(rec_b);
                    if (length > 0) {
                        try {
                            timerHandler.removeMessages(reviceBluetoothData);
                            timerHandler.sendEmptyMessageDelayed(reviceBluetoothData, reviceBluetoothDataTimeOut);
                            MyLogger.jLog().e("reviceByte: " + Arrays.toString(rec_b));
                            byte[] bytes = new byte[length];
                            System.arraycopy(rec_b, 0, bytes, 0, length);
                            Message obtainMessage = timerHandler.obtainMessage();
                            obtainMessage.what = 2003;
                            obtainMessage.obj = length;
                            timerHandler.sendMessage(obtainMessage);
                            if (isConnected()) {
                                sendMsg(bytes);
                                MyLogger.jLog().e("conection platform normal");
                            } else {
                                if (!socketConnectioning) {
                                    timerHandler.sendEmptyMessageDelayed(2006, 0);
                                } else {
                                    MyLogger.jLog().e("conection platform normal 2...");
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                MyLogger.jLog().e("not conection");
                //timerHandler.sendEmptyMessageDelayed(2000, 100);
            } catch (Exception e) {

            }
            super.run();
        }
    }

    /* 停止客户端连接 */
    private void shutdownClient() {
        if (clientConnectThread != null) {
            clientConnectThread.interrupt();
            clientConnectThread = null;
        }
        if (mreadThread != null) {
            mreadThread.interrupt();
            mreadThread = null;
        }
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothSocket = null;
        }
        if (bluetoothSocketInputStream != null) {
            try {
                bluetoothSocketInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (bluetoothSocketOutputStream != null) {
            try {
                bluetoothSocketInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //发送数据给蓝牙
    private void sendMessageToBluetooth(byte[] message) {
        try {
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                /*timerHandler.removeMessages(reviceBluetoothData);
                timerHandler.sendEmptyMessageDelayed(reviceBluetoothData, reviceBluetoothDataTimeOut);*/
                bluetoothSocketOutputStream.write(message);
                bluetoothSocketOutputStream.flush();
            } else {
                timerHandler.sendEmptyMessageDelayed(2000, 0);
            }
        } catch (IOException e) {
            if (bluetoothSocket != null) {
                try {
                    bluetoothSocket.close();
                    timerHandler.sendEmptyMessageDelayed(2000, 0);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**************************和平台建立连接****************************/
    //连接平台
    private synchronized void connectionPlatform(String ip, int port) {
        if (socket == null || !socket.isConnected()) {
            startConnect(ip, port);
        }
    }

    public void startConnect(final String host, final int port) {
        new Thread() {
            @Override
            public void run() {
                try {
            /*InetAddress ad = InetAddress.getByName(host);
            boolean state = ad.isReachable(2000);//测试是否可以达到该地址 ,判断ip是否可以连接 1000是超时时间
            if (state) {
                MyLogger.jLog().e("it is success连接成功" + ad.getHostAddress());
            } else {
                MyLogger.jLog().e("it is success连接失败");
                throw new IOException();
            }*/
                    int status = 0;
                    try {
                        Process p = Runtime.getRuntime().exec("ping -c 1 -w 2 " + host);
                        status = p.waitFor();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (status == 0) {
                        MyLogger.jLog().e("it is success 连接成功");
                    } else {
                        MyLogger.jLog().e("it is fail 连接失败");
                        throw new IOException();
                    }
                    SocketAddress address = new InetSocketAddress(host, port);
                    socket = new Socket();
                    socket.connect(address, 2 * 1000);
                    //socket = new Socket(host, port);
                    socket.setSoTimeout(OUT_Time);
                    socket.setKeepAlive(true);
                    //socket.setTcpNoDelay(true);
                    is = socket.getInputStream();
                    os = socket.getOutputStream();

                    receiverMsgThread = new ReceiverNewMsgThread();
                    receiverMsgThread.start();
                    sendMsgThread = new SendMsgThread();
                    sendMsgThread.start();
                    heartTip = true;
                   /* heartMsgThread = new HeartMsgThread();
                    heartMsgThread.start();*/
                    timerHandler.sendEmptyMessageDelayed(2005, 0);
                    MyLogger.jLog().e("it is success连接平台");
                } catch (Exception e) {
                    connectinPlatformFail++;
                    socketConnectioning = false;
                    if (connectinPlatformFail < 2) {
                        connectionPlatform();
                    } else {
                        timerHandler.sendEmptyMessageDelayed(2008, 0);
                    }
                    MyLogger.jLog().e("connect socket has a error");
                    MyLogger.jLog().e("连接平台，请检查网络");
                    e.printStackTrace();
                }
                socketConnectioning = false;
                super.run();
            }
        }.start();

    }

    /*private class HeartMsgThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (heartTip) {
                if (socket != null) {
                    try {
                        if (isConnected()) {
                            MyLogger.jLog().e("与平台发送 sendUrgentData");
                            socket.sendUrgentData(0xff);
                        }
                        Thread.sleep(5 * 1000);
                    } catch (IOException e) {
                        MyLogger.jLog().e("与平台连接断开");
                        e.printStackTrace();
                        timerHandler.sendEmptyMessageDelayed(reviceBluetoothData, 0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }*/

    private class ReceiverNewMsgThread extends Thread {

        @Override
        public void run() {
            while (isConnected()) {
                try {
                    int re_l = is.available();
                    if (re_l > 0) {
                        //获取PC工具发送的数据
                        receiver_b = new byte[re_l];
                        is.read(receiver_b);
                        MyLogger.jLog().e("receiver_b: " + Arrays.toString(receiver_b));
                        sendMessageToBluetooth(receiver_b);

                        /*byte[] reviceData = new byte[2048];
                        int length = 0;
                        while ((length = is.read(reviceData)) != -1) {
                            sendMessageToBluetooth(reviceData);
                        }*/
                    }
                } catch (Exception e) {
                    MyLogger.jLog().e("异常");
                    //timerHandler.sendEmptyMessage(2006);
                    e.printStackTrace();
                    break;
                }
                super.run();
            }
        }
    }

    private class SendMsgThread extends Thread {
        private Handler handler = null;

        @Override
        public void run() {
            Looper.prepare();

            handler = new Handler() {
                @Override
                public void handleMessage(android.os.Message msg) {
                    switch (msg.what) {
                        case 1000:
                            try {
                                MyLogger.jLog().e("socketConnectioning: " + socketConnectioning);
                                if (isConnected()) {
                                    byte[] send_byte = (byte[]) msg.obj;
                                    os.write(send_byte);
                                    MyLogger.jLog().e("连接平台正常 sending the msg");
                                    os.flush();
                                } else {
                                    connectionPlatform();
                                }
                            } catch (Exception e) {
                                connectionPlatform();
                                e.printStackTrace();
                            }
                            break;
                    }
                }
            };
            Looper.loop();
            super.run();
        }
    }

    public boolean isConnected() {
        boolean isC = false;
        try {
            isC = (null != socket && socket.isConnected());
        } catch (Exception e) {

        }
        return isC;
    }

    public void sendMsg(byte[] send_bs) {
        Message msg = Message.obtain();
        msg.obj = send_bs;
        msg.what = 1000;
        sendMsgThread.handler.sendMessage(msg);
    }

    public void closedConnect() {
        try {
            heartTip = false;
            /*if (heartMsgThread != null) {
                heartMsgThread.interrupt();
                heartMsgThread = null;
            }*/
            heartTip = false;
            if (socket != null) {
                socket.close();
                socket = null;
            }
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
            MyLogger.jLog().e("closed the socket success");
        } catch (Exception e) {
            e.printStackTrace();
            MyLogger.jLog().e("closed the socket has a error");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_POWER) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if ((System.currentTimeMillis() - mExitTime) > 2000) {
            Toast.makeText(getApplicationContext(), "请再按一次返回键", Toast.LENGTH_SHORT).show();
            mExitTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    private void runTesting(final ImageView iv_runing) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (iv_runing.getAnimation() != null) {
                    iv_runing.clearAnimation();
                }
                iv_runing.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_pt));
                Animation operatingAnim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.dialog_run_tip);
                LinearInterpolator lin = new LinearInterpolator();
                operatingAnim.setInterpolator(lin);
                iv_runing.startAnimation(operatingAnim);
            }
        });
    }

    private void cleanBluetooth() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                iv_connection_bluetooth.clearAnimation();
                iv_connection_bluetooth.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_pt_2));
            }
        });
    }

    private void cleanPlatform() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                iv_connection_platform.clearAnimation();
                iv_connection_platform.setBackgroundDrawable(getResources().getDrawable(R.drawable.bg_pt_2));
            }
        });
    }
}
