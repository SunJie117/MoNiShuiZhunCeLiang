package com.camark.monishuizhunceliang.util;

import android.content.Context;
import android.widget.Toast;

/**
 * Created by Administrator on 2018-07-18.
 */

public class ToastUtil {
    /**
     * @param ctx	上下文环境
     * @param msg	打印文本内容
     */
    public static void show(Context ctx, String msg) {
        Toast.makeText(ctx, msg, 0).show();
    }
}
