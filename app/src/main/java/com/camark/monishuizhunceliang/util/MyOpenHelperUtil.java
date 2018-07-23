package com.camark.monishuizhunceliang.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by camark on 2018-07-22.
 */

public class MyOpenHelperUtil extends SQLiteOpenHelper {
    public MyOpenHelperUtil(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table t_state(" +
                "_id integer primary key autoincrement, " +
                "state1 varchar(50) not null unique, " +
                "state2 varchar(50))");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
