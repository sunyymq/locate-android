package com.feifan.scanlib;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.TextUtils;

import com.feifan.baselib.utils.LogUtils;
import com.feifan.scanlib.service.CommandData;
import com.feifan.scanlib.service.ScanService;

/**
 * 扫描管理器
 * Created by bianying on 16/9/3.
 */
public class ScanManager {

    private static ScanManager client = null;
    private BeaconNotifier mNotifier;
    private Region mRegion;

    // auto
    private boolean autoStart = false;

    // 操作相关
    private String pendingPkgName;
    private int pendingPeriod;

    private ScanManager() {

    }

    /**
     * 获取扫描实例
     * @return
     */
    public static ScanManager getInstance() {
        if (client == null) {
            LogUtils.d("create a ScanManager instance");
            client = new ScanManager();
        }
        return client;
    }

    public boolean start(String packageName, int period) {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogUtils.w("Not supported prior to API 18.  Method invocation will be ignored");
            return false;
        }

        Message msg = Message.obtain(null, ScanService.MSG_START_SCAN, 0, 0);
        CommandData obj = new CommandData(packageName, period);
        msg.getData().putParcelable("data", obj);
        try {
            serviceMessenger.send(msg);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * 开启采集指定区域的beacon数据
     * @param packageName
     * @param period
     * @param region
     * @return
     */
    public boolean start(String packageName, int period, Region region) {
        this.mRegion = region;
        return start(packageName, period);
    }

    public boolean stop() {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogUtils.w("Not supported prior to API 18.  Method invocation will be ignored");
            return false;
        }

        Message msg = Message.obtain(null, ScanService.MSG_STOP_SCAN, 0, 0);
        try {
            serviceMessenger.send(msg);
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setAutoStart(boolean autoStart, String packageName, int period) {
        if(TextUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("packageName could not be empty");
        }
        this.autoStart = autoStart;
        this.pendingPkgName = packageName;
        this.pendingPeriod = period <= 0 ? 1000 : period;
    }

    /*-----------服务-----------*/

    // 连接扫描服务
    private Messenger serviceMessenger = null;
    ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogUtils.i("scan:connect to scan service");
            serviceMessenger = new Messenger(service);
            if(autoStart) {
                start(pendingPkgName, pendingPeriod);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogUtils.i("scan:disconnect to scan service");
            serviceMessenger = null;
            autoStart = false;
        }
    };

    public void bind(Context context){
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogUtils.w("Not supported prior to API 18.  Method invocation will be ignored");
            return;
        }
        Intent intent = new Intent(context.getApplicationContext(), ScanService.class);
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        LogUtils.d("bind to scan service");
    }

    public void unBind(Context context) {
        if (android.os.Build.VERSION.SDK_INT < 18) {
            LogUtils.w("Not supported prior to API 18.  Method invocation will be ignored");
            return;
        }
        if(autoStart) {
            stop();
        }
        mNotifier = null;
        mRegion = null;
        context.unbindService(mConnection);
        serviceMessenger = null;
    }

    /*----------END-----------*/

    public void setNotifier(BeaconNotifier notifier) {
        mNotifier = notifier;
    }

    public BeaconNotifier getNotifier() {
        return mNotifier;
    }

    public Region getRegion() {
        return mRegion;
    }
}
