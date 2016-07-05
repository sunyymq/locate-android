package com.feifan.sampling.scan;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.feifan.sampling.Constants;
import com.feifan.sampling.R;
import com.feifan.sampling.base.log.config.APPLogConfig;
import com.feifan.sampling.base.log.request.CursorRequest;
import com.feifan.sampling.provider.SampleData;
import com.feifan.sampling.scan.model.CursorSaveModel;
import com.libs.base.sensor.dici.DiciService;
import com.libs.ui.fragments.CommonMenuFragment;
import com.libs.utils.DateTimeUtils;
import com.libs.utils.PrefUtil;
import com.mm.beacon.BeaconServiceManager;
import com.mm.beacon.IBeacon;
import com.mm.beacon.blue.ScanData;
import com.mm.beacon.data.Region;
import com.wanda.logger.log.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mengmeng on 16/6/15.
 */
public class ScanFragment extends CommonMenuFragment implements BeaconServiceManager.OnBeaconDetectListener,DiciService.OnSensorCallBack {
  private Button mScanBtn;
  private TextView mScanStatusText;
  private EditText mIntervalEdit;
  private EditText mCountEdit;
  private BeaconServiceManager mBeaconManager;
  private List<IBeacon> mTemplist = new ArrayList<IBeacon>();
  private List<ScanData> mRawlist = new ArrayList<ScanData>();
  private Map<String, IBeacon> mMacBeacon = new HashMap<String, IBeacon>();
  private int mIntervalCount = 100;
  private int mInterval;
  private long mIntervalNum;
  private final String CVS_SCAN_SAVE_NAME = "scan_save_cvs";
  private final String RAW_SCAN_SAVE_NAME = "scan_save_raw";
  private final String COM_SCAN_SAVE_NAME = "scan_save_combine";
  private String mSpotid = "";
  private String mDirection = "";
  private float mRealDirection = 0f;
  private DiciService mDiciService;
  private Handler mHandler = new Handler() {
    public void handleMessage(Message msg) {
      super.handleMessage(msg);
      mScanStatusText.setText(String.format(getString(R.string.scan_interval_txt),
          (mIntervalCount - mInterval)));

      if (mInterval == mIntervalCount) {
        mScanStatusText.setText(String.format(getString(R.string.scan_write_file)));
        String beaconName =
            CVS_SCAN_SAVE_NAME + "_" + DateTimeUtils.getCurrentTime("yyyy-MM-dd HH-mm-ss") + "_"
                + mIntervalNum + "_" + mIntervalCount+ "_" +mDirection;
        String[] header = buildHeader();
        ScanHelper.SaveIBeacon(header, getCvsData(), beaconName);

        String rawName =
            RAW_SCAN_SAVE_NAME + "_" + DateTimeUtils.getCurrentTime("yyyy-MM-dd HH-mm-ss") + "_"
                + mIntervalNum + "_" + mIntervalCount+ "_" +mDirection;;
        String[] rawheader = buildRawHeader();
        ScanHelper.SaveIBeacon(rawheader, getRawCvsData(), rawName);
        sendCombineCvs();
        mBeaconManager.stopService();
      }
    }
  };

  @Override
  public View onCreateCustomView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.biz_scan_fragment_layout, container, false);
    initView(view);
    return view;
  }

  private void initView(View view) {
    mScanBtn = (Button) view.findViewById(R.id.scan_btn);
    mScanStatusText = (TextView) view.findViewById(R.id.scan_status);
    mIntervalEdit = (EditText) view.findViewById(R.id.scan_interval_edit);
    mCountEdit = (EditText) view.findViewById(R.id.scan_count_edit);
    mScanBtn.setOnClickListener(new View.OnClickListener() {
      /**
       * Called when a view has been clicked.
       *
       * @param v The view that was clicked.
       */
      @Override
      public void onClick(View v) {
        String interval = mIntervalEdit.getEditableText().toString();
        String scanCount = mCountEdit.getEditableText().toString();
        if (!TextUtils.isEmpty(interval) && TextUtils.isDigitsOnly(interval)) {
          int scanInterval = Integer.valueOf(interval);
          mBeaconManager.setDelay(scanInterval);
        }
        if (!TextUtils.isEmpty(scanCount) && TextUtils.isDigitsOnly(scanCount)) {
          mIntervalCount = Integer.valueOf(scanCount);
        }

        mBeaconManager.startService();
      }
    });
    Bundle bundle = getArguments();
    if (bundle != null) {
      mSpotid = bundle.getString(Constants.EXTRA_KEY_SPOT_ID);
      String name = bundle.getString(Constants.EXTRA_KEY_SPOT_NAME);
      mDirection = bundle.getString(Constants.EXTRA_KEY_SPOT_DIRECTION);
      setTitle(name);
    }
    initData();
  }

  private void initData(){
    mIntervalNum = PrefUtil.getLong(getContext(), Constants.SHAREPREFERENCE.RECYCLE_TIME_INTERVAL,Constants.SHAREPREFERENCE.DEFAULT_SCAN_TIME);
    mIntervalEdit.setText(mIntervalNum+"");
    int scancount = PrefUtil.getInt(getContext(), Constants.SHAREPREFERENCE.SCAN_MAX_COUNT,Constants.SHAREPREFERENCE.DEFAULT_SCAN_NUM);
    mCountEdit.setText(scancount+"");
    mDiciService = DiciService.getInstance(getActivity().getApplicationContext());
  }
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    mBeaconManager = BeaconServiceManager.getInstance(getActivity().getApplicationContext());
    mBeaconManager.registerBeaconListerner(this);
    mDiciService.startMagicScan();
  }

  @Override
  public void onBeaconDetected(List<IBeacon> beaconlist) {
    if (beaconlist != null && !beaconlist.isEmpty()) {
      buildScanIndex(beaconlist);
      mTemplist.addAll(beaconlist);
    }
    mInterval++;
    mHandler.sendEmptyMessage(mInterval);
  }

  private void buildScanIndex(List<IBeacon> beaconlist) {
    if (beaconlist != null && beaconlist.size() > 0) {
      for (int i = 0; i < beaconlist.size(); i++) {
        IBeacon beacon = beaconlist.get(i);
        beacon.setIndex(mInterval);
        beacon.setmDirection(mRealDirection);
        String mac = beacon.getMac();
        if (!TextUtils.isEmpty(mac)) {
          if (!mMacBeacon.containsKey(mac)) {
            mMacBeacon.put(mac, beacon);
          }
        }
      }
    }
  }

  private ArrayList<IBeacon> buildCombineData() {
    if (!mRawlist.isEmpty() && !mMacBeacon.isEmpty()) {
      ArrayList<IBeacon> beaconList = new ArrayList<IBeacon>();
      for (int i = 0; i < mRawlist.size(); i++) {
        ScanData data = mRawlist.get(i);
        if (data != null) {
          String mac = data.device.getAddress();
          IBeacon beacon = IBeacon.fromScanData(data);
          if (beacon != null) {
            beacon.setIndex(data.index);
            beacon.setTime(data.time);
            beacon.setMac(mac);
            beaconList.add(beacon);
          } else {
            IBeacon ibeacon = mMacBeacon.get(mac);
            if (ibeacon != null) {
              ibeacon.setIndex(data.index);
              ibeacon.setTime(data.time);
              ibeacon.setTime(data.rssi);
              beaconList.add(ibeacon);
            }
          }
        }
      }
      return beaconList;
    }
    return null;
  }

  private void buildRawScanIndex(List<ScanData> beaconlist) {
    if (beaconlist != null && beaconlist.size() > 0) {
      for (int i = 0; i < beaconlist.size(); i++) {
        ScanData beacon = beaconlist.get(i);
        beacon.index = mInterval;
      }
    }
  }

  @Override
  public void onBeaconRawDataDetect(List<ScanData> beaconlist) {
    if (beaconlist != null && !beaconlist.isEmpty()) {
      buildRawScanIndex(beaconlist);
      mRawlist.addAll(beaconlist);
    }
  }

  private String[] buildHeader() {
    String[] header = new String[7];
    header[0] = "index";
    header[1] = "mac";
    header[2] = "uuid";
    header[3] = "major";
    header[4] = "minor";
    header[5] = "rssi";
    header[6] = "time";
    return header;
  }

  private String[] buildRawHeader() {
    String[] header = new String[4];
    header[0] = "index";
    header[1] = "mac";
    header[2] = "rssi";
    header[3] = "time";
    return header;
  }

  private List<String[]> getRawCvsData() {
    if (mRawlist != null && mRawlist.size() > 0) {
      List<String[]> list = new ArrayList<String[]>();
      for (int i = 0; i < mRawlist.size(); i++) {
        ScanData scandata = mRawlist.get(i);
        if (scandata != null) {
          String[] items = new String[4];
          items[0] = String.valueOf(scandata.index);
          items[1] = String.valueOf(scandata.device.getAddress());
          items[2] = String.valueOf(scandata.rssi);
          items[3] = String.valueOf(scandata.time);
          list.add(items);
        }
      }
      return list;
    }
    return null;
  }

  private List<String[]> getCvsData() {
    if (mTemplist != null && mTemplist.size() > 0) {
      List<String[]> list = new ArrayList<String[]>();
      for (int i = 0; i < mTemplist.size(); i++) {
        IBeacon beacon = mTemplist.get(i);
        if (beacon != null) {
          String[] items = new String[7];
          items[0] = String.valueOf(beacon.getIndex());
          items[1] = String.valueOf(beacon.getMac());
          items[2] = beacon.getProximityUuid();
          items[3] = String.valueOf(beacon.getMajor());
          items[4] = String.valueOf(beacon.getMinor());
          items[5] = String.valueOf(beacon.getRssi());
          items[6] = String.valueOf(beacon.getTime());
          list.add(items);
        }
      }
      return list;
    }
    return null;
  }

  private void sendCombineCvs() {
    ArrayList<IBeacon> beaconList = buildCombineData();
    ArrayList<String[]> combineList = getCombineCvsData(beaconList);
    String beaconName =
        COM_SCAN_SAVE_NAME + "_" + DateTimeUtils.getCurrentTime("yyyy-MM-dd HH-mm-ss") + "_"
            + mInterval + "_" + mIntervalCount;
    String[] header = buildHeader();
    ScanHelper.SaveIBeacon(header, combineList, beaconName);
    saveBeaconDb(beaconList);
  }

  private void saveBeaconDb(ArrayList<IBeacon> beaconList) {
    if (beaconList != null && beaconList.size() > 0) {
      CursorSaveModel model = new CursorSaveModel();
      model.setDirection(mDirection);
      model.setSpotId(mSpotid);
      model.setName(DateTimeUtils.getCurrentTime("yyyy-MM-dd HH-mm-ss") + "_"
          + mInterval + "_" + mIntervalCount);
      model.setUri(SampleData.BeaconDetail.CONTENT_URI);
      model.setList(beaconList);
      CursorRequest request =
          new CursorRequest(new APPLogConfig(""), getActivity().getApplicationContext());
      request.setLog(model);
      Logger.writeRequest(request);
      //通过spotid来判断当前界面是从drawlayout传过来的还是从spotlist界面传过来的
      if (!TextUtils.isEmpty(mSpotid)) {
        startUploadService(beaconList);
      }
    }
  }

  private void startUploadService(ArrayList<IBeacon> beaconList) {
    if (beaconList != null && !beaconList.isEmpty()) {
      Intent intent = new Intent(getActivity(), UploadService.class);
      intent.putParcelableArrayListExtra("beacon", beaconList);
      intent.putExtra("spotid", mSpotid);
      getActivity().startService(intent);
    }
  }

  private ArrayList<String[]> getCombineCvsData(List<IBeacon> beaconlist) {
    if (beaconlist != null && beaconlist.size() > 0) {
      ArrayList<String[]> list = new ArrayList<String[]>();
      for (int i = 0; i < beaconlist.size(); i++) {
        IBeacon beacon = beaconlist.get(i);
        if (beacon != null) {
          String[] items = new String[7];
          items[0] = String.valueOf(beacon.getIndex());
          items[1] = String.valueOf(beacon.getMac());
          items[2] = beacon.getProximityUuid();
          items[3] = String.valueOf(beacon.getMajor());
          items[4] = String.valueOf(beacon.getMinor());
          items[5] = String.valueOf(beacon.getRssi());
          items[6] = String.valueOf(beacon.getTime());
          list.add(items);
        }
      }
      return list;
    }
    return null;
  }

  @Override
  public void onBeaconEnter(Region region) {

  }

  @Override
  public void onBeaconExit(Region region) {

  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    mBeaconManager.stopService();
    mBeaconManager.unRegisterBeaconListener(this);
  }

  @Override
  public void onSensorCallBack(float[] prefvalues) {
    if (prefvalues != null){
      mRealDirection = prefvalues[0];
    }
  }
}
