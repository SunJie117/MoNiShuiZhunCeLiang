package com.camark.monishuizhunceliang;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.camark.monishuizhunceliang.util.Md5Util;
import com.camark.monishuizhunceliang.util.MyOpenHelperUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class BluetoothChatActivity extends AppCompatActivity {
    private static final String TAG = "BluetoothChat";
    private static final boolean D = false;

    private static final int STATE2 = 20;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int  MESSAGE_CLEAR = 6;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final int REQUEST_WEN_JIAN_LIST = 4;

    private static final int REQUEST_WRITE = 1;//申请权限的请求码

    public static final String QIAN_HOU_CI_BIAO_SHI_RB = "Rb";
    public static final String QIAN_HOU_CI_BIAO_SHI_RF = "Rf";
    public static final String CUO_WU_SHU_JU_BIAO_SHI = "#####";

    public static final String XIAN_LU_WEN_JIAN = "线路文件";
    public static final String FIlE_NAME = "WenJianJia";

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private EditText mCiEditText;

    private Button mSendButton;
    private Button mChongFaButton;
    private Button mChongCeButton;
    private Button mZiDongButton;
    private TextView mStateTextView;


    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;

    List<String> mDatLineList = new ArrayList<>();

    List<String> mQianHouShiBiaoShiList = new ArrayList<>();
    List<String> mDianHaoList = new ArrayList<>();
    List<String> mShiJuList = new ArrayList<>();
    List<String> mChiDuShuList = new ArrayList<>();
    List<String> mGuanCeTimeList = new ArrayList<>();
    List<String> mCeZhanList = new ArrayList<>();
    String mYiFaLine = null;

    int mDateNum = 0;
    SimpleDateFormat mFormatTime = new SimpleDateFormat("HH:mm:ss");
    Timer mZiDongTimer = new Timer();
    TimerTask mZiDongTimerTask = null;
    volatile boolean mIsZiDong = false;

    private File mXianLuWenJianPath = null;// 线路文件

    MyOpenHelperUtil mMyOpenHelper;

    int mState2;


    private boolean hasWriteExternalStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case REQUEST_WRITE:

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Set up the window layout
        setContentView(R.layout.main);

        mMyOpenHelper = new MyOpenHelperUtil(this.getApplicationContext(), SplashActivity.DB_NAME, null, 1);

        String[] states = getState();

        if (states != null) {
            mState2 = getState2(states[0],states[1]);

        } else {
            mState2 = -1;
        }

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mXianLuWenJianPath = new File(Environment.getExternalStorageDirectory(),
                XIAN_LU_WEN_JIAN);

        boolean zhiXingScanner = false;
        if (!mXianLuWenJianPath.exists()) {
            mXianLuWenJianPath.mkdir();
            zhiXingScanner = true;
        }

        if (zhiXingScanner) {
            MediaScannerConnection.scanFile(this, new String[] { Environment
                    .getExternalStorageDirectory().getPath() }, null, null);
        }
    }

    public static String getQianHouChiBiaoShiFromShuJuLine(String line)
    {
        line = line.trim();
        return line.substring(49, 51).trim();
    }

    public static boolean panDuanDuShuLine(String line)
    {
        String qianHouChiBiaoShi = getQianHouChiBiaoShiFromShuJuLine(line);
        return (qianHouChiBiaoShi.equals(QIAN_HOU_CI_BIAO_SHI_RB) ) || (qianHouChiBiaoShi.equals(QIAN_HOU_CI_BIAO_SHI_RF));
    }

    public static boolean panDuanCuoWuShuJuLine(String line)
    {
        return line.contains(CUO_WU_SHU_JU_BIAO_SHI);
    }

    public static String getGuanCeDianNameFromShuJuLine(String line) {
        line = line.trim();
        return line.substring(20, 29).trim();
    }
    public static String getShiJuFromShuJuLine(String line) {
        line = line.trim();
        return line.substring(74, 89).trim();
    }
    public static String getGaoChengFromShuJuLine(String line) {
        line = line.trim();
        return line.substring(51, 66).trim();
    }
    public static String getGuanCeTimeFromShuJuLine(String line) {
        line = line.trim();
        return line.substring(35, 43).trim();
    }

    public static void getShuJuLineFromDat(File datWeiJian,
                                           List<String> datLineList,
                                           List<String> qianHouShiBiaoShiList,
                                           List<String> dianHaoList,
                                           List<String> shiJuList,
                                           List<String> chiDuShuList,
                                           List<String> guanCeTimeList,
                                           List<String> ceZhanList) {

        if (datWeiJian.exists()) {
            BufferedReader bufr = null;
            try {
                bufr = new BufferedReader(new FileReader(datWeiJian));
                String line;

                while ((line = bufr.readLine()) != null) {
                    line = line.trim();

                    if (!line.isEmpty()) {
                        if (!line.isEmpty() && panDuanDuShuLine(line) && !panDuanCuoWuShuJuLine(line))
                        {
                            String qianHouShiBiaoShi = getQianHouChiBiaoShiFromShuJuLine(line);
                            String dianHao = getGuanCeDianNameFromShuJuLine(line);
                            String shiJu = getShiJuFromShuJuLine(line);
                            String chiDuShu = getGaoChengFromShuJuLine(line);
                            String guanCeTime = getGuanCeTimeFromShuJuLine(line);


                            qianHouShiBiaoShiList.add(qianHouShiBiaoShi);
                            dianHaoList.add(dianHao);
                            shiJuList.add(shiJu);
                            chiDuShuList.add(chiDuShu);
                            guanCeTimeList.add(guanCeTime);


                            datLineList.add(line);
                            int num = datLineList.size();

                            if ((num % 4) == 0) {

                                String houQianDian;

                                if (((num / 4) % 2) == 0 ) {
                                    houQianDian = String.format("%d, %s , %s",num / 4,dianHaoList.get(num-2),dianHaoList.get(num-1));
                                } else {
                                    houQianDian = String.format("%d, %s , %s",num / 4,dianHaoList.get(num-1),dianHaoList.get(num-2));
                                }
                                ceZhanList.add(houQianDian);
                            }

                        }

                    }
                }


            } catch (FileNotFoundException e) {
                e.printStackTrace();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (bufr != null) {
                    try {
                        bufr.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }

    }

    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    private void moNiCeLiang() {
        if (mDateNum < mDatLineList.size()) {

            mYiFaLine = mDatLineList.get(mDateNum);
            String benCiTimeStr = mGuanCeTimeList.get(mDateNum);

            synchronized (mZiDongTimer) {

                if (mZiDongTimerTask != null) {
                    mZiDongTimerTask.cancel();

                }

                mZiDongTimer.purge();
            }

            sendMessage(mDatLineList.get(mDateNum++)+"\r\n");


            if (mDateNum < mDatLineList.size()) {
                String xiaCiTimeStr = mGuanCeTimeList.get(mDateNum);
                if (((mDateNum) % 4) == 0) {
                    mHandler.obtainMessage(BluetoothChatActivity.MESSAGE_CLEAR, -1, -1, null)
                            .sendToTarget();
                    mHandler.obtainMessage(BluetoothChatActivity.MESSAGE_WRITE, -1, -1, mCeZhanList.get((mDateNum / 4)).getBytes())
                            .sendToTarget();
                    mHandler.obtainMessage(BluetoothChatActivity.MESSAGE_WRITE, -1, -1, mDatLineList.get(mDateNum).getBytes())
                            .sendToTarget();




                } else {
                    mHandler.obtainMessage(BluetoothChatActivity.MESSAGE_WRITE, -1, -1, mDatLineList.get(mDateNum).getBytes())
                            .sendToTarget();
                }

                try {
                    Date benCiTime =  mFormatTime.parse(benCiTimeStr);
                    Date xiaCiTime = mFormatTime.parse(xiaCiTimeStr);

                    long jianGeTime = xiaCiTime.getTime() - benCiTime.getTime();

                    if (mIsZiDong) {
                        synchronized (mZiDongTimer) {

                            mZiDongTimerTask = new TimerTask() {
                                @Override
                                public void run() {
                                    moNiCeLiang();
                                }
                            };

                            mZiDongTimer.schedule(mZiDongTimerTask,jianGeTime);

                        }


                    }


                } catch (ParseException e) {
                    e.printStackTrace();

                }



            } else {
                mHandler.obtainMessage(BluetoothChatActivity.MESSAGE_WRITE, -1, -1, "结束".getBytes())
                        .sendToTarget();

            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");


        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        mCiEditText = (EditText) findViewById(R.id.edit_text_ci);
        //mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                //TextView view = (TextView) findViewById(R.id.edit_text_out);
                //String message = view.getText().toString();

                moNiCeLiang();

            }
        });

        mChongFaButton = (Button) findViewById(R.id.button_chong_fa);

        mChongFaButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mYiFaLine != null) {
                    if (mIsZiDong) {

                        synchronized (mZiDongTimer) {

                            if (mZiDongTimerTask != null) {
                                mZiDongTimerTask.cancel();
                                mZiDongTimerTask = null;

                            }

                            mZiDongTimer.purge();

                        }


                    }
                    sendMessage(mYiFaLine+"\r\n");


                }


            }
        });



        mChongCeButton = (Button) findViewById(R.id.button_chong_ce);

        mChongCeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (mIsZiDong) {

                    synchronized (mZiDongTimer) {

                        if (mZiDongTimerTask != null) {
                            mZiDongTimerTask.cancel();
                            mZiDongTimerTask = null;

                        }

                        mZiDongTimer.purge();

                    }

                }

                String zhanNumStr = mOutEditText.getText().toString();
                String ciNumStr = mCiEditText.getText().toString();

                int zhanNum = Integer.parseInt(zhanNumStr);
                int ciNum = Integer.parseInt(ciNumStr);
                mDateNum = (zhanNum-1)*4 + ciNum - 1;

                if (mDateNum < mDatLineList.size()) {

                    mHandler.obtainMessage(BluetoothChatActivity.MESSAGE_CLEAR, -1, -1, null)
                            .sendToTarget();
                    mHandler.obtainMessage(BluetoothChatActivity.MESSAGE_WRITE, -1, -1, mCeZhanList.get((zhanNum-1)).getBytes())
                            .sendToTarget();

                    for(int i = 0;i < ciNum; i++) {
                        mHandler.obtainMessage(BluetoothChatActivity.MESSAGE_WRITE, -1, -1, mDatLineList.get((zhanNum - 1) * 4 + i).getBytes())
                                .sendToTarget();
                    }

                }

            }
        });

        mZiDongButton = (Button) findViewById(R.id.button_zi_dong);

        mZiDongButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mIsZiDong) {
                    mIsZiDong = false;
                    mZiDongButton.setText(R.string.zi_dong);

                    synchronized (mZiDongTimer) {

                        if (mZiDongTimerTask != null) {
                            mZiDongTimerTask.cancel();
                            mZiDongTimerTask = null;

                        }

                        mZiDongTimer.purge();

                    }


                } else {
                    mIsZiDong = true;
                    mZiDongButton.setText(R.string.shou_dong);
                }


            }
        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");

        mStateTextView = (TextView) findViewById(R.id.text_view_state);
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        mZiDongTimer.cancel();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    }

    /*
    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            if(D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };
    */

    private final void setStatus(int resId) {
        //final ActionBar actionBar = getActionBar();
        //actionBar.setSubtitle(resId);
        mStateTextView.setText(resId);
    }

    private final void setStatus(CharSequence subTitle) {
        //final ActionBar actionBar = getActionBar();
        //actionBar.setSubtitle(subTitle);
        mStateTextView.setText(subTitle);
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            //mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case MESSAGE_CLEAR:
                    mConversationArrayAdapter.clear();
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);

                    mConversationArrayAdapter.add(writeMessage);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);

                    if (readMessage.contains("?0100")) {
                        BluetoothChatActivity.this.sendMessage("!0100  |         0743158     \r\n");

                    }
                    mConversationArrayAdapter.add(readMessage);
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public  String[] getState() {
        SQLiteDatabase db = mMyOpenHelper.getWritableDatabase();
        String imei = "";
        String[] States = null;

        try {

            Cursor cursor = db.rawQuery("select * from t_state", null);

            if (cursor.moveToNext()) {
                States = new String[2];

                States[0] = cursor.getString(cursor.getColumnIndex("state1"));

                States[1] = cursor.getString(cursor.getColumnIndex("state2"));

            }
            cursor.close();


        } finally {
            db.close();
        }

        return States;
    }

    public  int getState2(String imei,String state2) {
        int result = -1;
        for (int i = 1; i <= STATE2; i++) {
            if (Md5Util.encode(imei + i).equals(state2)) {
                result = i;
            }
        }

        return result;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case REQUEST_WEN_JIAN_LIST:
                if (resultCode == Activity.RESULT_OK) {
                    //////////////////////////////////////////////////////////////////////////

                    if ((mState2 != -1) && (mState2 <= STATE2)) {
                        SQLiteDatabase db = mMyOpenHelper.getWritableDatabase();
                        try {

                            Cursor cursor = db.rawQuery("select * from t_state", null);

                            if (cursor.moveToNext()) {

                                String id = cursor.getString(cursor.getColumnIndex("_id"));
                                String state1 = cursor.getString(cursor.getColumnIndex("state1"));

                                ContentValues values = new ContentValues();

                                values.put("state2", Md5Util.encode(state1 + ++mState2));
                                db.update("t_state", values, "_id = ?", new String[]{id});


                            }
                            cursor.close();


                        } finally {
                            db.close();
                        }

                        //////////////////////////////////////////////////////////////////////////////////
                        String xianLuWenJianMing = data.getExtras().getString(
                                XianLuWenJianActivity.EXTRA_XIAN_LU_WEN_JIAN);

                        mDatLineList.clear();
                        mQianHouShiBiaoShiList.clear();
                        mDianHaoList.clear();
                        mShiJuList.clear();
                        mChiDuShuList.clear();
                        mGuanCeTimeList.clear();
                        mCeZhanList.clear();

                        getShuJuLineFromDat(new File(mXianLuWenJianPath, xianLuWenJianMing),mDatLineList,
                                mQianHouShiBiaoShiList,
                                mDianHaoList,
                                mShiJuList,
                                mChiDuShuList,
                                mGuanCeTimeList,
                                mCeZhanList);

                        if (mDatLineList.size() > 0) {
                            mHandler.obtainMessage(BluetoothChatActivity.MESSAGE_CLEAR, -1, -1, null)
                                    .sendToTarget();
                            mHandler.obtainMessage(BluetoothChatActivity.MESSAGE_WRITE, -1, -1, mCeZhanList.get(0).getBytes())
                                    .sendToTarget();
                            mHandler.obtainMessage(BluetoothChatActivity.MESSAGE_WRITE, -1, -1, mDatLineList.get(0).getBytes())
                                    .sendToTarget();


                        }

                    } else {
                        Toast.makeText(this, "请联网获取授权!", Toast.LENGTH_LONG).show();
                    }




                }
                break;
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
            case R.id.secure_connect_scan:
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            case R.id.insecure_connect_scan:
                // Launch the DeviceListActivity to see devices and do scan
                //serverIntent = new Intent(this, DeviceListActivity.class);
                //startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);


                displayXianLuWenJianList();

                return true;
            case R.id.discoverable:
                // Ensure this device is discoverable by others
                //ensureDiscoverable();
                showUpdateDialog();
                return true;
        }
        return false;
    }

    /**
     * 显示线路文件List
     */

    public void displayXianLuWenJianList() {
        if ((mState2 != -1) && (mState2 <= STATE2)) {
            Intent intent = new Intent(this, XianLuWenJianActivity.class);
            intent.putExtra(FIlE_NAME, mXianLuWenJianPath);
            startActivityForResult(intent, REQUEST_WEN_JIAN_LIST);// 需返回结果时调用
        } else {
            Toast.makeText(this, "请联网获取授权!", Toast.LENGTH_LONG).show();
        }

    }

    /**
     * 弹出对话框,提示用户更新
     */
    protected void showUpdateDialog() {

        //对话框,是依赖于activity存在的
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //设置左上角图标
        //builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle("退出系统");
        //设置描述内容
        builder.setMessage("是否退出系统?");

        //积极按钮,立即更新
        builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //下载apk,apk链接地址,downloadUrl
                finish();
            }
        });

        builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {


            }
        });



        builder.show();
    }

}
