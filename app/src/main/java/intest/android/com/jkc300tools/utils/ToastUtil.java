package intest.android.com.jkc300tools.utils;

import android.content.Context;
import android.widget.Toast;

public class ToastUtil {
    /**
     * 土丝显示间隔
     */
    private static long lastTimeTwo;

    public static void showMessage(Context context, String info) {
        long nowTime = System.currentTimeMillis();
        if (nowTime - lastTimeTwo < 2000) {
            return;
        }
        lastTimeTwo = nowTime;
        Toast.makeText(context, info, Toast.LENGTH_SHORT).show();
    }
}
