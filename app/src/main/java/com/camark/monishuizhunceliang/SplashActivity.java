package com.camark.monishuizhunceliang;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.camark.monishuizhunceliang.util.Md5Util;
import com.camark.monishuizhunceliang.util.MyOpenHelperUtil;
import com.camark.monishuizhunceliang.util.StreamUtils;
import com.camark.monishuizhunceliang.util.ToastUtil;
import com.lidroid.xutils.HttpUtils;
import com.lidroid.xutils.exception.HttpException;
import com.lidroid.xutils.http.ResponseInfo;
import com.lidroid.xutils.http.callback.RequestCallBack;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class SplashActivity extends AppCompatActivity {
    protected static final String tag = "SplashActivity";
    private static final int REQUEST_WRITE = 1;//申请权限的请求码
    private static final int REQUEST_READ_PHONE_STATE = 2;//申请权限的请求码

    private static final int RESULT_INSTALL_CODE = 1;
    private static final int RESULT_INSTALL_NEW_CODE = 2;
    public static final String DB_NAME = "state.db";


    /**
     * 更新新版本的状态码
     */
    protected static final int UPDATE_VERSION = 100;
    /**
     * 进入应用程序主界面状态码
     */
    protected static final int ENTER_HOME = 101;


    /**
     * url地址出错状态码
     */
    protected static final int URL_ERROR = 102;
    protected static final int IO_ERROR = 103;
    protected static final int JSON_ERROR = 104;

    protected static final int IDENTITY_VERIFICATION = 105;

    private int mLocalVersionCode;
    private String mVersionDes;
    private String mDownloadUrl;
    private TextView mVersionNameTextView;
    private TextView mInfoTextView;
    private String mImei;
    MyOpenHelperUtil mMyOpenHelper;

    private void setImei() {
        String imei = imei();

        if (imei != null) {
            mImei = Md5Util.encode(imei());

            //sIdentityVerification(mImei,mHandler);


        } else {
            Toast.makeText(this, "无法读取手机标识!", Toast.LENGTH_LONG).show();
            finish();

        }

    }

    public static void sIdentityVerification(String state, Message msg) {
        msg.what = IDENTITY_VERIFICATION;
        msg.obj = false;
        try {
            //1,封装url地址
            URL url = new URL("http://39.106.200.186:8080//verification.json");
            //2,开启一个链接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            //3,设置常见请求参数(请求头)

            //请求超时
            connection.setConnectTimeout(2000);
            //读取超时
            connection.setReadTimeout(2000);

            //默认就是get请求方式,
//					connection.setRequestMethod("POST");

            //4,获取请求成功响应码
            if (connection.getResponseCode() == 200) {
                msg.obj = true;

            }
        } catch (MalformedURLException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();

        }


    }


    private Handler mHandler = new Handler() {
        @Override
        //alt+ctrl+向下箭头,向下拷贝相同代码
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case UPDATE_VERSION:
                    //弹出对话框,提示用户更新
                    showUpdateDialog();
                    break;
                case IO_ERROR:

                case JSON_ERROR:
                case URL_ERROR:

                    SQLiteDatabase dbUrl = mMyOpenHelper.getWritableDatabase();
                    boolean result = false;
                    try {

                        Cursor cursor = dbUrl.rawQuery("select * from t_state", null);

                        if (cursor.moveToNext()) {

                            String imei = cursor.getString(cursor.getColumnIndex("state1"));

                            result = imei.equals(mImei);



                        } else {
                            result = false;


                        }
                        cursor.close();


                    } finally {
                        dbUrl.close();
                    }

                    if (result) {
                        enterHome();
                    } else {

                        ToastUtil.show(getApplicationContext(), "未授权!");
                        finish();
                    }
                    break;


                case IDENTITY_VERIFICATION:
                    Boolean isVerificated = (Boolean) msg.obj;

                    if (isVerificated) {
                        SQLiteDatabase db = mMyOpenHelper.getWritableDatabase();

                        try {

                            Cursor cursor = db.rawQuery("select * from t_state", null);

                            if (cursor.moveToNext()) {

                                String id = cursor.getString(cursor.getColumnIndex("_id"));
                                ContentValues values = new ContentValues();
                                values.put("state1", mImei);
                                values.put("state2", Md5Util.encode(mImei + "1"));
                                db.update("t_state", values, "_id = ?", new String[]{id});


                            } else {

                                ContentValues values = new ContentValues();
                                values.put("state1", mImei);
                                values.put("state2", Md5Util.encode(mImei + "1"));

                                db.insert("t_state", null, values);


                            }
                            cursor.close();


                        } finally {
                            db.close();
                        }
                        enterHome();
                    } else {
                        Toast.makeText(SplashActivity.this, "未授权!", Toast.LENGTH_LONG).show();
                        finish();
                    }

                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        initData();
        initUI();

        if (hasReadPhoneStatePermission()) {
            setImei();
            if (hasWriteExternalStoragePermission()) {
                checkVersion();

            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);

        }


    }

    @Override
    protected void onDestroy() {
        mMyOpenHelper.close();
        super.onDestroy();
    }

    public String imei() {
        String imei = null;
        TelephonyManager telephonyManager = (TelephonyManager)
                getSystemService(Context.TELEPHONY_SERVICE);
        imei = telephonyManager.getImei();
        return imei;
    }


    private boolean hasWriteExternalStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasReadPhoneStatePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case REQUEST_READ_PHONE_STATE:
                if (hasReadPhoneStatePermission()) {
                    setImei();
                    if (hasWriteExternalStoragePermission()) {
                        checkVersion();

                    } else {
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE);
                    }
                } else {
                    Toast.makeText(this, "需要读信息权限!", Toast.LENGTH_LONG).show();
                    finish();
                }

                break;
            case REQUEST_WRITE:
                if (hasWriteExternalStoragePermission()) {
                    checkVersion();

                } else {
                    Toast.makeText(this, "需要读写SD卡权限!", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;


            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }

    //开启一个activity后,返回结果调用的方法
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_INSTALL_CODE:
                //enterHome();
                Toast.makeText(this, "需要升级到最新版本!", Toast.LENGTH_LONG).show();
                finish();
                break;
            case RESULT_INSTALL_NEW_CODE:
                //enterHome();
                Toast.makeText(this, "需要升级到最新版本!", Toast.LENGTH_LONG).show();
                finish();
                break;
            default:
                //Toast.makeText(this, "default", Toast.LENGTH_LONG).show();
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 返回版本号
     *
     * @return 非0 则代表获取成功
     */
    private int getVersionCode() {
        //1,包管理者对象packageManager
        PackageManager pm = getPackageManager();
        //2,从包的管理者对象中,获取指定包名的基本信息(版本名称,版本号),传0代表获取基本信息
        try {
            PackageInfo packageInfo = pm.getPackageInfo(getPackageName(), 0);
            //3,获取版本名称
            return packageInfo.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 获取版本名称:清单文件中
     *
     * @return 应用版本名称    返回null代表异常
     */
    private String getVersionName() {
        //1,包管理者对象packageManager
        PackageManager pm = getPackageManager();
        //2,从包的管理者对象中,获取指定包名的基本信息(版本名称,版本号),传0代表获取基本信息
        try {
            PackageInfo packageInfo = pm.getPackageInfo(getPackageName(), 0);
            //3,获取版本名称
            return packageInfo.versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 检测版本号
     */
    private void checkVersion() {
        new Thread() {
            public void run() {
                //发送请求获取数据,参数则为请求json的链接地址
                Message msg = Message.obtain();
                long startTime = System.currentTimeMillis();
                try {
                    //1,封装url地址
                    URL url = new URL("http://39.106.200.186:8080//update.json");
                    //2,开启一个链接
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    //3,设置常见请求参数(请求头)

                    //请求超时
                    connection.setConnectTimeout(2000);
                    //读取超时
                    connection.setReadTimeout(2000);

                    //默认就是get请求方式,
//					connection.setRequestMethod("POST");

                    //4,获取请求成功响应码
                    if (connection.getResponseCode() == 200) {
                        //5,以流的形式,将数据获取下来
                        InputStream is = connection.getInputStream();
                        //6,将流转换成字符串(工具类封装)
                        String json = StreamUtils.streamToString(is);
                        Log.i(tag, json);
                        //7,json解析
                        JSONObject jsonObject = new JSONObject(json);


                        String versionName = jsonObject.getString("version_name");
                        mVersionDes = jsonObject.getString("description");
                        String versionCode = jsonObject.getString("version_code");
                        mDownloadUrl = jsonObject.getString("download_url");

                        //日志打印
                        Log.i(tag, versionName);
                        Log.i(tag, mVersionDes);
                        Log.i(tag, versionCode);
                        Log.i(tag, mDownloadUrl);

                        //8,比对版本号(服务器版本号>本地版本号,提示用户更新)
                        if (mLocalVersionCode < Integer.parseInt(versionCode)) {
                            //提示用户更新,弹出对话框(UI),消息机制
                            msg.what = UPDATE_VERSION;
                        } else {
                            //进入应用程序主界面
                            sIdentityVerification(mImei, msg);
                        }
                    } else {
                        msg.what = URL_ERROR;
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    msg.what = URL_ERROR;
                } catch (IOException e) {
                    e.printStackTrace();
                    msg.what = IO_ERROR;
                } catch (JSONException e) {
                    e.printStackTrace();
                    msg.what = JSON_ERROR;
                } finally {
                    //指定睡眠时间,请求网络的时长超过2秒则不做处理
                    //请求网络的时长小于2秒,强制让其睡眠满2秒钟
                    long endTime = System.currentTimeMillis();
                    if (endTime - startTime < 2000) {
                        try {
                            Thread.sleep(2000 - (endTime - startTime));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    mHandler.sendMessage(msg);
                }
            }
        }.start();

    }

    /**
     * 弹出对话框,提示用户更新
     */
    protected void showUpdateDialog() {

        //对话框,是依赖于activity存在的
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //设置左上角图标
        //builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle("版本更新");
        //设置描述内容
        builder.setMessage(mVersionDes);

        //积极按钮,立即更新
        builder.setPositiveButton("立即更新", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //下载apk,apk链接地址,downloadUrl
                downloadApk();
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(SplashActivity.this, "需要升级到最新版本!", Toast.LENGTH_LONG).show();
                finish();
            }
        });

        //点击取消事件监听
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                //即使用户点击取消,也需要让其进入应用程序主界面
                Toast.makeText(SplashActivity.this, "需要升级到最新版本!", Toast.LENGTH_LONG).show();

                dialog.dismiss();
                finish();
            }
        });

        builder.show();
    }

    /**
     * 进入应用程序主界面
     */
    protected void enterHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        //在开启一个新的界面后,将导航界面关闭(导航界面只可见一次)
        finish();
    }

    /**
     * 安装对应apk
     *
     * @param file 安装文件
     */
    protected void installApk(File file) {
        /*/系统应用界面,源码,安装apk入口
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(Uri.fromFile(file),"application/vnd.android.package-archive");
        startActivityForResult(intent, 0);*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri apkUri = FileProvider.getUriForFile(this, "com.camark.monishuizhunceliang.fileprovider", file);//在AndroidManifest中的android:authorities值
            Intent install = new Intent(Intent.ACTION_VIEW);
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            install.setDataAndType(apkUri, "application/vnd.android.package-archive");
            startActivityForResult(install, RESULT_INSTALL_NEW_CODE);
        } else {
            Intent intent = new Intent("android.intent.action.VIEW");
            intent.addCategory("android.intent.category.DEFAULT");
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            startActivityForResult(intent, RESULT_INSTALL_CODE);
        }
    }

    /**
     * 初始化UI方法	alt+shift+j
     */
    private void initUI() {
        mInfoTextView = (TextView) findViewById(R.id.info_text_view);
        mVersionNameTextView = (TextView) findViewById(R.id.version_name_text_view);
        //1,应用版本名称
        mVersionNameTextView.setText("版本名称:" + getVersionName());
        //3,获取服务器版本号(客户端发请求,服务端给响应,(json,xml))
        //http://www.oxxx.com/update74.json?key=value  返回200 请求成功,流的方式将数据读取下来
        //json中内容包含:
        /* 更新版本的版本名称
         * 新版本的描述信息
		 * 服务器版本号
		 * 新版本apk下载地址*/

    }

    /**
     * 获取数据方法
     */
    private void initData() {

        mLocalVersionCode = getVersionCode();
        mMyOpenHelper = new MyOpenHelperUtil(this.getApplicationContext(), DB_NAME, null, 1);

    }

    protected void downloadApk() {
        //apk下载链接地址,放置apk的所在路径

        //1,判断sd卡是否可用,是否挂在上
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            //2,获取sd路径
            String path = Environment.getExternalStorageDirectory().getAbsolutePath()
                    + File.separator + "moniceliang.apk";
            //3,发送请求,获取apk,并且放置到指定路径
            HttpUtils httpUtils = new HttpUtils();
            //4,发送请求,传递参数(下载地址,下载应用放置位置)
            httpUtils.download(mDownloadUrl, path, new RequestCallBack<File>() {
                @Override
                public void onSuccess(ResponseInfo<File> responseInfo) {
                    //下载成功(下载过后的放置在sd卡中apk)
                    Log.i(tag, "下载成功");
                    File file = responseInfo.result;
                    //提示用户安装
                    installApk(file);
                }


                @Override
                public void onFailure(HttpException arg0, String arg1) {
                    Toast.makeText(SplashActivity.this, "下载失败", Toast.LENGTH_LONG).show();
                    System.out.println(arg1);
                    arg0.printStackTrace();
                    enterHome();
                }

                @Override
                public void onLoading(long total, long current,
                                      boolean isUploading) {
                    mInfoTextView.setText("下载进度:" + current * 100 / total + "%");
                    super.onLoading(total, current, isUploading);
                }

            });

        }
    }

}
