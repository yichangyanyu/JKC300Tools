package intest.android.com.jkc300tools.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetWorkUtil {
    /**
     * 判断手机是否有网络信号
     *
     * @param context
     * @return
     */
    public static boolean hasNetWork(Context context) {
        //连接管理器
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        //获取网络连接信息
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();//需要一个access_netwrok_state权限
        if (activeNetworkInfo == null) {
            return false;
        } else if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {//当前手机在使用2g/3g网络
            return true;
        } else if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {//当前手机已经连接上wifi
            return true;
        }
        return false;
    }
}
