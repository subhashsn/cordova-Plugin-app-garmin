package com.hutchind.cordova.plugins.Garmin;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.garmin.health.AuthCompletion;
import com.garmin.health.Device;
import com.garmin.health.DeviceConnectionStateListener;
import com.garmin.health.DeviceManager;
import com.garmin.health.DevicePairedStateListener;
import com.garmin.health.GarminDeviceScanCallback;
import com.garmin.health.GarminHealth;
import com.garmin.health.GarminRequest;
import com.garmin.health.GarminRequestManager;
import com.garmin.health.PairingCallback;
import com.garmin.health.PairingCompletion;
import com.garmin.health.ScannedDevice;
import com.garmin.health.bluetooth.FailureCode;
import com.garmin.health.bluetooth.PairingFailedException;
import com.garmin.health.settings.Gender;
import com.garmin.health.settings.UserSettings;
import com.garmin.health.sleep.RawSleepData;
import com.garmin.health.sleep.SleepResult;
import com.garmin.health.sleep.SleepResultListener;
import com.garmin.health.sync.SyncListener;
import com.garmin.health.sync.SyncResult;
import com.google.gson.Gson;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Future;

import io.ionic.init.SyncDataHandler;
import io.ionic.init.SyncManager;

public class Garmin extends CordovaPlugin implements
  DevicePairedStateListener, DeviceConnectionStateListener {

  private static final String TAG = "Garmin";
  private static final String ACTION_LAUNCH = "launch";
  private static final int LAUNCH_REQUEST = 0;
  private static final String ACTION_SCAN_DEVICE = "scanForDevice";
  private static final String ACTION_CONNECT_GETSLEEPDATA = "getSleepData";
  private static final String ACTION_INITIALIZE_GARMIN = "garminInitializer";
  private static final String ACTION_REQUEST_SLEEP_DATA = "requestSleepData";

  private static String pin;

  private CallbackContext callback;
  private DeviceScanner mDeviceScanner;

  private Future mPairingFuture;
  private Boolean isparing = false;

  private GarminRequest mGarminSleepRequest;
  private Device mDevice;

  private SyncProgressListener mDeviceSyncListener;

  private SharedPreferences prefs;

  private static final int REQUEST_COURSE_LOCATION = 0;

  @Override
  public void onDeviceConnected(Device device) {
    Log.e(TAG, "onDeviceConnected");
  }

  @Override
  public void onDeviceDisconnected(Device device) {
    Log.e(TAG, "onDeviceDisconnected");
  }

  @Override
  public void onDeviceConnectionFailed(Device device, FailureCode failureCode) {
    Log.e(TAG, "onDeviceConnectionFailed");
  }

  private abstract class LauncherRunnable implements Runnable {
    public CallbackContext callbackContext;

    LauncherRunnable(CallbackContext cb) {
      this.callbackContext = cb;
    }
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.callback = callbackContext;
    if (ACTION_LAUNCH.equals(action)) {
      return launch(args);
    } else if (ACTION_SCAN_DEVICE.equals(action)) {
      return scanForDevice(args);
    } else if (ACTION_REQUEST_SLEEP_DATA.equals(action)) {
      return requestSleepData(args);
    } else if (ACTION_INITIALIZE_GARMIN.equals(action)) {
      return garminInitializer(args);
    }
    return false;
  }

  /*private boolean getcallback(JSONArray args) {
    //connectGarmin();
    Log.d("getcallback ::=>", "inside getcallback");
    return true;
  }
*/
  /*private boolean getSyncData(JSONArray args) {
    Log.d("getSyncData ::=>", "inside getSyncData");
    connectGarmin();
    return true;
  }*/

  private boolean requestSleepData(JSONArray args) {
    Log.d("getSleepData ::=>", "inside getSleepData");
    GarminRequestManager requestManager = GarminRequestManager.getRequestManager();
    if (mDevice != null && SyncDataHandler.getInstance().getCombinedRawData(mDevice.address()) != null) {
      RawSleepData rawSleepData = SyncDataHandler.getInstance().getCombinedRawData(mDevice.address());
      mGarminSleepRequest = requestManager.requestSleepData(rawSleepData, new SleepResultListenerImpl(rawSleepData));
    } else {
      if (prefs == null) {
        prefs = PreferenceManager.getDefaultSharedPreferences(cordova.getActivity().getApplicationContext());
      }
      Gson gson = new Gson();
      String json = prefs.getString("vivo", "");
      SyncResult syncResult = gson.fromJson(json, SyncResult.class);
      RawSleepData rawSleepData = RawSleepData.createRawSleepData(syncResult);
      mGarminSleepRequest = requestManager.requestSleepData(rawSleepData, new SleepResultListenerImpl(rawSleepData));

    }

    return true;
  }

  private boolean garminInitializer(JSONArray args) {

    boolean systemBonding = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    try {
      GarminHealth.initialize(cordova.getContext(), systemBonding, "CgsCAwQFBgcICQoLDBKAAg07vRkogG9YE8K0ouT4zK+dBlN/NWTlTytpt9NReXCa4Ht/UwLeD+FjlQu5fTtgjDN5Uf08rPb9z8RMX304rK4atN01Ii9EIVW5IfAEcYy+v4Z2jeUWceGhcCfr9RzNm6G/vo8LpJynnwDOW4/hPgHWATonDVYlcnB5j22GQaCTdLLXAcIpq08O0+uw04Ez5r6DB1ns63uoEELICwegUzyyqbs65DENeJd87cYAj+AtL2jIJ+Zt6KW0bT47JpfJMsY8cXb7oN1MQaTZ3ijSiZImt02d3I92rR5yBAVzuZ40utOQaVCJyLjvFFTD+WFy0sQwNpgOmvp7rqKrgpDsMfoYgPCQr8As",
        "x3AtHNPxTVdApMGy6ZMiHiIGFTg6mcZd",
        "yxrLo5nqUzLxicbd");

    } catch (Error | Exception e) {
      Log.e(TAG, "Exception in initialize");
    }
    requestLocationPermission();

    SyncDataHandler.init();

    SyncManager.init();

    return true;

  }

  private void requestLocationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      return;
    }
    if (ContextCompat.checkSelfPermission(cordova.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
      return;
    }

    ActivityCompat.requestPermissions(cordova.getActivity(), new String[]{
      Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_COURSE_LOCATION);
  }

  private boolean scanForDevice(JSONArray array) {

    try {
      JSONObject rec = array.getJSONObject(0);
      pin = rec.getString("pin");
    } catch (JSONException e) {
      e.printStackTrace();
    }

    garminInitializer(array);

    DeviceManager deviceManager = DeviceManager.getDeviceManager();

    deviceManager.addPairedStateListener(this);

    startBleScan();

    prefs = PreferenceManager.getDefaultSharedPreferences(cordova.getActivity().getApplicationContext());

    return true;
  }


  private void startBleScan() {
    mDeviceScanner = new DeviceScanner();
    BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    if (btAdapter == null || !btAdapter.isEnabled()) {
      Log.e(TAG, "Bluetooth isn't available");
      return;
    }
    LocationManager lm = (LocationManager) this.cordova.getActivity().getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
    if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) && !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
      Log.e(TAG, "Location services are not enabled");
      return;
    }
    BluetoothLeScanner scanner = btAdapter.getBluetoothLeScanner();
    if (scanner == null) {
      Log.e(TAG, "Could not obtain BluetoothLeScanner to start scan.");
      return;
    }
    scanner.startScan(mDeviceScanner);
  }

  private class DeviceScanner extends GarminDeviceScanCallback {

    @Override
    public void onScannedDevice(ScannedDevice device) {
      Log.d(TAG, "garminDeviceFound(device = " + device.pin() + " with address: " + device.friendlyName() + ")");
      if(!pin.isEmpty() && pin.equals(device.pin())){
        pair(device);
      }else {
        setResponseCallBack("Incorrect Pin");
      }
    }

    @Override
    public void onScanFailed(int errorCode) {
      Log.d(TAG, "scanFailed(failure.name() = " + errorCode + ")");
    }
  }


  @Override
  public void onDevicePaired(Device device) {

    Log.d(TAG, "onDevicePaired(deviceAddress = " + device.address() + ")");
    mDevice = device;
    mDeviceSyncListener = new SyncProgressListener();
    DeviceManager deviceManager = DeviceManager.getDeviceManager();
    deviceManager.addConnectionStateListener(this);
    deviceManager.getSyncManager().addSyncListener(mDeviceSyncListener);

  }

  @Override
  public void onDeviceUnpaired(String deviceId) {
    Log.d(TAG, "onDeviceUnpaired(deviceAddress = " + deviceId + ")");
  }

  private void pair(ScannedDevice scannedDevice) {

    Log.d(TAG, "pair()");
    UserSettings.Builder builder = new UserSettings.Builder();
    builder.setAge(23);
    builder.setHeight(1.2f);
    builder.setWeight(55);
    builder.setGender(Gender.MALE);
    //Save future to cancel on back
    DeviceManager deviceManager = DeviceManager.getDeviceManager();
    Boolean isPaired = prefs.getBoolean("pair", false);
    if (!isparing && !isPaired) {
      mPairingFuture = deviceManager.pair(scannedDevice, builder.build(), new PairingListener());
      isparing = true;
    }
  }

  private class PairingListener implements PairingCallback {

    @Override
    public void connectionReady(Device device, final PairingCompletion completion) {
      Log.d(TAG, "pairingComplete(device = " + device.friendlyName() + ")");
      //If app cancelled pairing, don't need to show message
      if (mPairingFuture.isCancelled()) {
        return;
      }
      completion.complete();
    }

    @Override
    public void pairingSucceeded(final Device device) {
      Log.d(TAG, "PairingSucceeded(device = " + device.friendlyName() + ")");
      prefs.edit().putBoolean("pair", true).apply();
    }


    @Override
    public void pairingFailed(PairingFailedException cause) {
      Log.e(TAG, "pairingFailed", cause);
      //If app cancelled pairing, don't need to show message
      if (mPairingFuture.isCancelled()) {
        return;
      }
    }

    @Override
    public void authRequested(final AuthCompletion completion) {
      Log.e(TAG, "authRequested");
      //If app cancelled pairing, don't need to show message
      if (mPairingFuture.isCancelled()) {
        return;
      }
    }
  }

  private class SyncProgressListener implements SyncListener {

    @Override
    public void onSyncStarted(final Device device) {
      Log.d(TAG, "onSyncStarted(device = " + device.address() + ")");
    }

    @Override
    public void onSyncProgress(Device device, int progress) {
      Log.d(TAG, "onSyncProgress(device = " + device.address() + ", progress = " + progress + ")");
    }

    @Override
    public void onSyncComplete(Device device) {

      Log.d(TAG, "onSyncComplete(device = " + device.address() + ")");
      SyncResult results = SyncDataHandler.getInstance().getLatestSyncData(device.address());
      if (results != null) {
        Gson gson = new Gson();
        gson.toJson(results);
        setResponseCallBack(gson.toJson(results));
        Log.d(TAG, "result json: " + gson.toJson(results));
        prefs.edit().putString("vivo", gson.toJson(results)).apply();
      }
    }

    @Override
    public void onSyncFailed(final Device device, Exception e) {
      Log.d(TAG, "onSyncFailed: " + e.getMessage());
    }
  }

  private class SleepResultListenerImpl implements SleepResultListener {

    private RawSleepData mRawSleepData;

    SleepResultListenerImpl(RawSleepData rawSleepData) {
      this.mRawSleepData = rawSleepData;
    }


    @Override
    public void onSuccess(SleepResult result) {
      if (result != null) {
        Gson gson = new Gson();
        gson.toJson(result);
        //Log.d(TAG, "results.raw: " + results.getRawFilePaths().size());
        Log.d(TAG, "result json: " + gson.toJson(result));
        setResponseCallBack(gson.toJson(result));
      }
    }

    @Override
    public void onError(String errorMessage) {
      Log.d(TAG, "onError(errorMessage = " + errorMessage + " )");
    }
  }

  public void setResponseCallBack(String responseJson) {

    cordova.getActivity().runOnUiThread(() -> callback.success(responseJson));

  }


  private ActivityInfo getAppInfo(final Intent intent, final String appPackageName) {
    final PackageManager pm = webView.getContext().getPackageManager();
    try {
      Log.d(TAG, pm.getApplicationInfo(appPackageName, 0) + "");
    } catch (NameNotFoundException e) {
      Log.i(TAG, "No info found for package: " + appPackageName);
    }
    return null;
  }

  private boolean launch(JSONArray args) throws JSONException {
    final JSONObject options = args.getJSONObject(0);
    Bundle extras = null;
    if (options.has("extras")) {
      extras = createExtras(options.getJSONArray("extras"));
    } else {
      extras = new Bundle();
    }
    int flags = 0;
    if (options.has("flags")) {
      flags = options.getInt("flags");
    }

    if (options.has("uri") && (options.has("packageName") || options.has("dataType"))) {
      String dataType = null;
      String packageName = null;
      if (options.has("packageName")) {
        packageName = options.getString("packageName");
      }
      if (options.has("dataType")) {
        dataType = options.getString("dataType");
      }
      launchAppWithData(packageName, options.getString("uri"), dataType, extras);
      return true;
    } else if (options.has("packageName")) {
      launchApp(options.getString("packageName"), extras);
      return true;
    } else if (options.has("uri")) {
      launchIntent(options.getString("uri"), extras, flags);
      return true;
    } else if (options.has("actionName")) {
      launchAction(options.getString("actionName"), extras);
      return true;
    }
    return false;
  }

  private Bundle createExtras(JSONArray extrasObj) throws JSONException {
    Bundle extras = new Bundle();
    for (int i = 0, size = extrasObj.length(); i < size; i++) {
      JSONObject extra = extrasObj.getJSONObject(i);
      if (extra.has("name") && extra.has("value") && extra.has("dataType")) {
        String extraName = extra.getString("name");
        String dataType = extra.getString("dataType");
        try {
          if (dataType.equalsIgnoreCase("Byte")) {
            try {
              extras.putByte(extraName, ((byte) extra.getInt("value")));
            } catch (Exception e) {
              Log.e(TAG, "Error converting to byte for extra: " + extraName);
              e.printStackTrace();
              throw e;
            }
          } else if (dataType.equalsIgnoreCase("ByteArray")) {
            try {
              extras.putByteArray(extraName, ParseTypes.toByteArray(extra.getJSONArray("value")));
            } catch (Exception e) {
              Log.e(TAG, "Error converting to byte for extra: " + extraName);
              e.printStackTrace();
              throw e;
            }
          } else if (dataType.equalsIgnoreCase("Short")) {
            try {
              extras.putShort(extraName, ((short) extra.getInt("value")));
            } catch (Exception e) {
              Log.e(TAG, "Error converting to short for extra: " + extraName);
              e.printStackTrace();
              throw e;
            }
          } else if (dataType.equalsIgnoreCase("ShortArray")) {
            extras.putShortArray(extraName, ParseTypes.toShortArray(extra.getJSONArray("value")));
          } else if (dataType.equalsIgnoreCase("Int")) {
            extras.putInt(extraName, extra.getInt("value"));
          } else if (dataType.equalsIgnoreCase("IntArray")) {
            extras.putIntArray(extraName, ParseTypes.toIntArray(extra.getJSONArray("value")));
          } else if (dataType.equalsIgnoreCase("IntArrayList")) {
            extras.putIntegerArrayList(extraName, ParseTypes.toIntegerArrayList(extra.getJSONArray("value")));
          } else if (dataType.equalsIgnoreCase("Long")) {
            extras.putLong(extraName, extra.getLong("value"));
          } else if (dataType.equalsIgnoreCase("LongArray")) {
            extras.putLongArray(extraName, ParseTypes.toLongArray(extra.getJSONArray("value")));
          } else if (dataType.equalsIgnoreCase("Float")) {
            try {
              extras.putFloat(extraName, Float.parseFloat(extra.getString("value")));
            } catch (Exception e) {
              Log.e(TAG, "Error parsing float for extra: " + extraName);
              e.printStackTrace();
              throw e;
            }
          } else if (dataType.equalsIgnoreCase("FloatArray")) {
            try {
              extras.putFloatArray(extraName, ParseTypes.toFloatArray(extra.getJSONArray("value")));
            } catch (Exception e) {
              Log.e(TAG, "Error parsing float for extra: " + extraName);
              e.printStackTrace();
              throw e;
            }
          } else if (dataType.equalsIgnoreCase("Double")) {
            extras.putDouble(extraName, extra.getDouble("value"));
          } else if (dataType.equalsIgnoreCase("DoubleArray")) {
            extras.putDoubleArray(extraName, ParseTypes.toDoubleArray(extra.getJSONArray("value")));
          } else if (dataType.equalsIgnoreCase("Boolean")) {
            extras.putBoolean(extraName, extra.getBoolean("value"));
          } else if (dataType.equalsIgnoreCase("BooleanArray")) {
            extras.putBooleanArray(extraName, ParseTypes.toBooleanArray(extra.getJSONArray("value")));
          } else if (dataType.equalsIgnoreCase("String")) {
            extras.putString(extraName, extra.getString("value"));
          } else if (dataType.equalsIgnoreCase("StringArray")) {
            extras.putStringArray(extraName, ParseTypes.toStringArray(extra.getJSONArray("value")));
          } else if (dataType.equalsIgnoreCase("StringArrayList")) {
            extras.putStringArrayList(extraName, ParseTypes.toStringArrayList(extra.getJSONArray("value")));
          } else if (dataType.equalsIgnoreCase("Char")) {
            extras.putChar(extraName, ParseTypes.toChar(extra.getString("value")));
          } else if (dataType.equalsIgnoreCase("CharArray")) {
            extras.putCharArray(extraName, ParseTypes.toCharArray(extra.getString("value")));
          } else if (dataType.equalsIgnoreCase("CharSequence")) {
            extras.putCharSequence(extraName, extra.getString("value"));
          } else if (dataType.equalsIgnoreCase("CharSequenceArray")) {
            extras.putCharSequenceArray(extraName, ParseTypes.toCharSequenceArray(extra.getJSONArray("value")));
          } else if (dataType.equalsIgnoreCase("CharSequenceArrayList")) {
            extras.putCharSequenceArrayList(extraName, ParseTypes.toCharSequenceArrayList(extra.getJSONArray("value")));
					/*
					} else if (dataType.equalsIgnoreCase("Size") && Build.VERSION.SDK_INT >= 21) {
						extras.putSize(extraName, extra.getJSONObject("value"));
					} else if (dataType.equalsIgnoreCase("SizeF") && Build.VERSION.SDK_INT >= 21) {
						extras.putSizeF(extraName, extra.getJSONObject("value"));
					*/
          } else if (dataType.toLowerCase().contains("parcelable")) {
            if (!extra.has("paType")) {
              Log.e(TAG, "Property 'paType' must be provided if dataType is " + dataType + ".");
              throw new Exception("Missing property paType.");
            } else {
              String paType = extra.getString("paType").toUpperCase();
              if (ParseTypes.SUPPORTED_PA_TYPES.contains(paType)) {
                if (dataType.equalsIgnoreCase("Parcelable")) {
                  extras.putParcelable(extraName, ParseTypes.toParcelable(extra.getString("value"), paType));
                } else if (dataType.equalsIgnoreCase("ParcelableArray")) {
                  extras.putParcelableArray(extraName, ParseTypes.toParcelableArray(extra.getJSONArray("value"), paType));
                } else if (dataType.equalsIgnoreCase("ParcelableArrayList")) {
                  extras.putParcelableArrayList(extraName, ParseTypes.toParcelableArrayList(extra.getJSONArray("value"), paType));
                } else if (dataType.equalsIgnoreCase("SparseParcelableArray")) {
                  extras.putSparseParcelableArray(extraName, ParseTypes.toSparseParcelableArray(extra.getJSONObject("value"), paType));
                }
              } else {
                Log.e(TAG, "ParcelableArray type '" + paType + "' is not currently supported.");
                throw new Exception("Provided parcelable array type not supported.");
              }
            }
          }
        } catch (Exception e) {
          Log.e(TAG, "Error processing extra. Skipping: " + extraName);
        }
      } else {
        Log.e(TAG, "Extras must have a name, value, and datatype.");
      }
    }

    Log.d(TAG, "EXTRAS");
    Log.d(TAG, "" + extras);

    return extras;
  }

  private void launchAppWithData(final String packageName, final String uri, final String dataType, final Bundle extras) throws JSONException {
    final CordovaInterface mycordova = cordova;
    final CordovaPlugin plugin = this;
    final CallbackContext callbackContext = this.callback;
    cordova.getThreadPool().execute(new LauncherRunnable(this.callback) {
      public void run() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (dataType != null) {
          intent.setDataAndType(Uri.parse(uri), dataType);
        } else {
          intent.setData(Uri.parse(uri));
        }

        if (packageName != null && !packageName.equals("")) {
          intent.setPackage(packageName);
        }

        intent.putExtras(extras);

        try {
          mycordova.startActivityForResult(plugin, intent, LAUNCH_REQUEST);
          ((Garmin) plugin).callbackLaunched();
        } catch (ActivityNotFoundException e) {
          Log.e(TAG, "Error: No applications installed that can handle uri " + uri);
          e.printStackTrace();
          callbackContext.error("Application not found for uri.");
        }

      }
    });
  }

  private void launchApp(final String packageName, final Bundle extras) {
    final CordovaInterface mycordova = cordova;
    final CordovaPlugin plugin = this;
    Log.i(TAG, "Trying to launch app: " + packageName);
    cordova.getThreadPool().execute(new LauncherRunnable(this.callback) {
      public void run() {
        final PackageManager pm = plugin.webView.getContext().getPackageManager();
        final Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
        boolean appNotFound = launchIntent == null;

        if (!appNotFound) {
          try {
            launchIntent.putExtras(extras);
            mycordova.startActivityForResult(plugin, launchIntent, LAUNCH_REQUEST);
            ((Garmin) plugin).callbackLaunched();
          } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Error: Activity for package" + packageName + " was not found.");
            e.printStackTrace();
            callbackContext.error("Activity not found for package name.");
          }
        } else {
          callbackContext.error("Activity not found for package name.");
        }
      }
    });
  }

  private void launchIntent(final String uri, final Bundle extras, final int flags) {
    final CordovaInterface mycordova = cordova;
    final CordovaPlugin plugin = this;
    cordova.getThreadPool().execute(new LauncherRunnable(this.callback) {
      public void run() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(uri));
        if (flags != 0) {
          intent.setFlags(flags);
        }
        try {
          intent.putExtras(extras);
          mycordova.startActivityForResult(plugin, intent, LAUNCH_REQUEST);
          ((Garmin) plugin).callbackLaunched();
        } catch (ActivityNotFoundException e) {
          Log.e(TAG, "Error: Activity for " + uri + " was not found.");
          e.printStackTrace();
          callbackContext.error("Activity not found for uri.");
        }
      }
    });
  }

  private void launchAction(final String actionName, final Bundle extras) {
    final CordovaInterface mycordova = cordova;
    final CordovaPlugin plugin = this;
    cordova.getThreadPool().execute(new LauncherRunnable(this.callback) {
      public void run() {
        Intent intent = new Intent(actionName);
        try {
          intent.putExtras(extras);
          mycordova.startActivityForResult(plugin, intent, LAUNCH_REQUEST);
          ((Garmin) plugin).callbackLaunched();
        } catch (ActivityNotFoundException e) {
          Log.e(TAG, "Error: Activity for " + actionName + " was not found.");
          e.printStackTrace();
          callbackContext.error("Activity not found for action name.");
        }
      }
    });
  }


  public void callbackLaunched() {
    try {
      JSONObject json = new JSONObject();
      json.put("isLaunched", true);
      PluginResult result = new PluginResult(PluginResult.Status.OK, json);
      result.setKeepCallback(true);
      callback.sendPluginResult(result);
    } catch (JSONException e) {
      PluginResult result = new PluginResult(PluginResult.Status.OK, "{'isLaunched':true}");
      result.setKeepCallback(true);
      callback.sendPluginResult(result);
    }
  }


}
