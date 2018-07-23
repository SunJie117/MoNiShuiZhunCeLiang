package com.camark.monishuizhunceliang;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;


public class XianLuWenJianActivity extends Activity {

    private class GongChengListAapter extends BaseAdapter {

        @Override
        public int getCount() {
            return gongChengMingList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = null;

            if (convertView == null) {

                // getApplicationContext() 获取的Context和当前的Activity不是同一对象
                //Log.i("listview", "getApplicationContext = " + getApplicationContext().toString());
                //Log.i("listview", "MainActivity = " + MainActivity.this.toString());
                view = View.inflate(XianLuWenJianActivity.this, R.layout.gong_cheng_list_item, null);
            } else {
                view = convertView;
            }

            ((TextView)view.findViewById(R.id.tvIdGongChengListGongChengMing)).setText(gongChengMingList.get(position) );

            return view;
        }
    }

    // Return Intent extra
    public static String EXTRA_XIAN_LU_WEN_JIAN = "xian_lu_wen_jian";



    ListView lvGongChengList;
    Button btGuanCeJieGuoFanHui;
    ArrayList<String> gongChengMingList;
    File xianLuWenJianPath = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_jian_xie_dian);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_xian_lu_wen_jian);

        // Set result CANCELED incase the user backs out
        setResult(Activity.RESULT_CANCELED);

        xianLuWenJianPath =(File) getIntent().getSerializableExtra(BluetoothChatActivity.FIlE_NAME);

        File[] files = xianLuWenJianPath.listFiles();
        gongChengMingList = new ArrayList<String>();

        for (File file : files) {
            if (file.isFile()) {
                gongChengMingList.add(getWenJianMingFromAbsolutePath(file.getAbsolutePath()));
            }
        }
        Collections.sort(gongChengMingList);


        initData();
        initView();
        setListener();
    }

    private String getWenJianMingFromAbsolutePath(String absolutePath) {
        String wenJianMing = absolutePath;
        int num = absolutePath.lastIndexOf(File.separator);

        if (num > -1) {
            wenJianMing = absolutePath.substring(num + 1);
        }
        return wenJianMing;
    }

    /**
     * 初始化数据
     */
    private void initData() {

    }

    /**
     * 初始化view控件 jiejian
     */
    private void initView() {
        lvGongChengList = findViewById(R.id.lvIdGuanCeJieGuoList);

        lvGongChengList.setAdapter(new GongChengListAapter());//设置listView适配器

        btGuanCeJieGuoFanHui =  findViewById(R.id.btIdGuanCeJieGuoFanHui);
    }

    /**
     * 创建活动时初始化所有监听事件
     */
    private void setListener() {
        setbtGuanCeJieGuoFanHuiOnClickListener();
        setlvGongChengListOnItemClickListener();
    }

    //////////////////////设置所有控件的监听事件////////////////////////////////////
    /**
     * 设置按钮返回事件
     */
    private void setbtGuanCeJieGuoFanHuiOnClickListener() {
        btGuanCeJieGuoFanHui.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setlvGongChengListOnItemClickListener() {
        lvGongChengList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // TODO Auto-generated method stub

                // Create the result Intent and include the MAC address
                Intent intent = new Intent();
                intent.putExtra(EXTRA_XIAN_LU_WEN_JIAN, gongChengMingList.get(position));

                // Set result and finish this Activity
                setResult(Activity.RESULT_OK, intent);
                finish();

            }
        });

    }
}
