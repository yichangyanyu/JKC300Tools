package intest.android.com.jkc300tools.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.util.Arrays;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import intest.android.com.jkc300tools.utils.MyLogger;

public class SocketTool {

    private SocketTool() {

    }

    private static SocketTool Socket_Tool = new SocketTool();
    public static final byte OutMsgWhat = 0x01;
    private final int OUT_Time = 15 * 1000;

    private Socket socket;
    private ReceiverNewMsgThread receiverMsgThread;
    private SendMsgThread sendMsgThread;

    private InputStream is;
    private OutputStream os;

    private Context mContext;
    private Handler out_Handler;


    private boolean isZhanbao = false;
    private byte[] receiver_b;
    private short receiver_real_length = 0;
    private byte[] receiver_temp_b;


    private boolean isStop = false;
    private boolean isSuccss = false;
    private boolean recThreadIsOk = false;
    private boolean sedThreadIsOk = false;

    public byte[] read_send_b;


    private final byte Delay_Con_What = 0x05;

    private ToolHandler handler = new ToolHandler(this);

    static class ToolHandler extends Handler {
        WeakReference<SocketTool> tools;

        public ToolHandler(SocketTool socketTool) {
            tools = new WeakReference<SocketTool>(socketTool);
        }

        @Override
        public void handleMessage(Message msg) {
            SocketTool thisTool = tools.get();
            if (null == thisTool) {
                return;
            }
            thisTool.reSendMsg(msg);
            super.handleMessage(msg);
        }
    }


    private void reSendMsg(Message msg) {
        switch (msg.what) {
            case 2000:
                if (null != read_send_b) {
                    sendMsg(read_send_b);
                    read_send_b = null;
                }
                break;
        }
    }

    public static SocketTool getInstance(Context mContext, Handler handler) {
        Socket_Tool.mContext = mContext;
        Socket_Tool.out_Handler = handler;
        return Socket_Tool;
    }

    public static SocketTool getInstance() {
        return Socket_Tool;
    }


    private void getIp() {

    }

    public void startConnect(final String host, final int port) {
        getIp();
        new Thread() {
            @Override
            public void run() {
                try {
                    isSuccss = false;
                    socket = new Socket(host, port);
                    socket.setSoTimeout(OUT_Time);
                    socket.setTcpNoDelay(true);
                    isStop = false;
                    is = socket.getInputStream();
                    os = socket.getOutputStream();

                    receiverMsgThread = new ReceiverNewMsgThread();
                    receiverMsgThread.start();
                    sendMsgThread = new SendMsgThread();
                    sendMsgThread.start();
                    handler.sendEmptyMessageDelayed(2005, 500);
                    MyLogger.jLog().e("it is success");
                } catch (Exception e) {
                    MyLogger.jLog().e("connect socket has a error");
                    MyLogger.jLog().e("连接失败，请检查网络");
                    e.printStackTrace();
                }
                super.run();
            }
        }.start();

    }

    private void sendTheClosedSocketBroadcast(Context context) {

    }

    public void closedConnect(boolean tellTheMainTab) {
        if (tellTheMainTab) {
            MyLogger.jLog().e("连接断开，下拉重连");
            sendTheClosedSocketBroadcast(mContext);
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    socket.close();
                    is.close();
                    os.close();
                    isSuccss = false;
                    clearConnectAndStream();
                    MyLogger.jLog().e("closed the socket success");
                } catch (Exception e) {
                    e.printStackTrace();
                    MyLogger.jLog().e("closed the socket has a error");
                }
            }
        }.start();
    }

    public void clearConnectAndStream() {
        socket = null;
        is = null;
        os = null;
    }

    public void stopConnect() {
        isStop = true;
    }

    public boolean isConnected() {
        boolean isC = false;
        try {
            isC = (null != socket && socket.isConnected());
        } catch (Exception e) {

        }
        return isC;
    }


    @SuppressLint("HandlerLeak")
    public void sendMsg(byte[] send_bs) {
        Message msg = Message.obtain();
        msg.obj = send_bs;
        msg.what = 1000;
        sendMsgThread.handler.sendMessage(msg);
    }

    private class ReceiverNewMsgThread extends Thread {

        @Override
        public void run() {
            while (true) {
                if (!isConnected()) {
                    return;
                }
                try {
                    int re_l = is.available();
                    if (re_l > 0) {
                        receiver_b = new byte[re_l];
                        int length = is.read(receiver_b);
                        MyLogger.jLog().e("receiver_b: " + Arrays.toString(receiver_b));
                        Message msg = out_Handler.obtainMessage();
                        msg.obj = receiver_b;
                        msg.what = 2004;
                        out_Handler.sendMessage(msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
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
                                byte[] send_byte = (byte[]) msg.obj;
                                os.write(send_byte);
                                MyLogger.jLog().e("sending the msg");
                                os.flush();
                            } catch (Exception e) {
                                closedConnect(true);
                                e.printStackTrace();
                            }
                            break;
                    }
                }
            };
            Looper.loop();
            sedThreadIsOk = true;
            isSuccss = true;
            super.run();
        }
    }
}
