/*
 * Copyright (c) 2016 Garmin International. All Rights Reserved.
 * <p/>
 * This software is the confidential and proprietary information of
 * Garmin International.
 * You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement
 * you entered into with Garmin International.
 * <p/>
 * Garmin International MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. Garmin International SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 * <p/>
 *
 * Created by tritsch on 7/26/2016.
 */

package io.ionic.init;

import android.util.Log;

import com.garmin.health.Device;
import com.garmin.health.DeviceManager;
import com.garmin.health.GarminHealth;
import com.garmin.health.GarminRequestManager;
import com.garmin.health.sleep.RawSleepData;
import com.garmin.health.sleep.SleepResult;
import com.garmin.health.sleep.SleepResultListener;
import com.garmin.health.sync.FileInfo;
import com.garmin.health.sync.FileSyncDelegate;
import com.garmin.health.sync.Stress;
import com.garmin.health.sync.SyncDelegate;
import com.garmin.health.sync.SyncManager;
import com.garmin.health.sync.SyncResult;
import com.garmin.health.sync.WellnessSyncSummary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Manager responsible for processing the result data from a sync.
 */
public class SyncDataHandler implements SyncDelegate, FileSyncDelegate {
  private static final String TAG = SyncDataHandler.class.getSimpleName();

  //Sample object for summarizing data from multiple syncs
  private class SampleSummary {
    private final Date date;

    int steps;
    double distance;
    double floorsClimbed;

    // Handle whichever fields are needed

    public SampleSummary(Date date) {
      this.date = date;
    }
  }

  private static SyncDataHandler sInstance = new SyncDataHandler();

  private final HashMap<String, SyncResult> mLatestData = new HashMap<>();

  private final HashMap<String, List<SyncResult>> mSyncCombineSample = new HashMap<>();
  private final HashMap<String, List<RawSleepData>> mSleepInput = new HashMap<>();

  private boolean mRequestSleepAtSync;

  public static void init() {
    DeviceManager deviceManager = DeviceManager.getDeviceManager();
    SyncManager syncManager = deviceManager.getSyncManager();
    syncManager.setSyncDelegate(sInstance);
    syncManager.setFileSyncDelegate(sInstance);
  }

  public static SyncDataHandler getInstance() {
    return sInstance;
  }

  public void setRequestSleepAtSync(boolean requestSleepAtSync) {
    mRequestSleepAtSync = requestSleepAtSync;
  }

  public SyncResult getLatestSyncData(String deviceAddress) {
    return mLatestData.get(deviceAddress);
  }

  public RawSleepData getCombinedRawData(String deviceAddress) {
    return RawSleepData.combineRawSleepData(mSleepInput.get(deviceAddress));
  }

  @Override
  public int getSyncInterval(Device device) {
    return SyncDelegate.DEFAULT_INTERVAL_SECONDS;
  }

  @Override
  public int getHeartRateInterval(Device device) {
    return SyncDelegate.DEFAULT_HEARTRATE_SECONDS;
  }

  @Override
  public void onSyncResult(Device device, SyncResult results) {
    Log.d(TAG, "onSyncResult()");
    if (results == null) {
      return;
    }
    //Data should be saved locally or to a server, for sample app save the latest results for display
    mLatestData.put(device.address(), results);

    try {
      sampleHandleSleepData(device.address(), results);

      sampleHandleStress(results);

      sampleHandleRawFileResult(results);
      //Sample sync combine, data will need to be persisted as desired.
      sampleHandleWellnessDailyTotals(device.address(), results);

    } catch (Exception e) {
      Log.d(TAG, "Failed to sample parsing", e);
    }
  }

  @Override
  public void fileSyncStarted(int fileCount) {
    Log.d(TAG, String.format("Beginning file sync, File count: %d", fileCount));
  }

  @Override
  public boolean onFile(FileInfo info, InputStream input) {
    Log.d(TAG, String.format("File synced, Key: {%s}, Fit Type: {%d}", info.fileKey(), info.fitType()));

    return true;
  }

  @Override
  public void fileSyncComplete() {
    Log.d(TAG, "File sync complete.");
  }

  @Override
  public void syncFailed(Throwable t) {
    Log.d(TAG, String.format("File sync failed with {%s}", t.getClass().getSimpleName()));
  }

  private void sampleHandleSleepData(String address, SyncResult results) {
    List<RawSleepData> previousResults = mSleepInput.get(address);
    if (previousResults == null) {
      previousResults = new ArrayList<>();
      mSleepInput.put(address, previousResults);
    }
    RawSleepData data = RawSleepData.createRawSleepData(results);
    //Sleep data can be saved and processed later once more/all data is available for a day
    if (!mRequestSleepAtSync) {
      //Multiple sync sample is not applicable without file deletion.
      if (GarminHealth.getTestModeConfiguration().keepData()) {
        previousResults.clear();
      }
      previousResults.add(data);
    }
    //Sleep request can be made on each sync
    else {
      GarminRequestManager requestManager = GarminRequestManager.getRequestManager();
      //Request is being made in singleton so request object isn't needed to provide cancel/state checking
      requestManager.requestSleepData(data, new SleepResultListener() {
        @Override
        public void onSuccess(SleepResult result) {
          if (result != null) {
            Log.i(TAG, "Successfully retrieved sleep results: " + result.getSleepSeconds());
          } else {
            Log.i(TAG, "No sleep data for supplied raw data");
          }
        }

        @Override
        public void onError(String errorMessage) {
          Log.i(TAG, "Failed to retrieve sleep results: " + errorMessage);
        }
      });
    }
  }

  private void sampleHandleStress(SyncResult results) {
    int stressDataPoints = 0;
    int stressTotal = 0;
    if (results.getStressList() != null) {
      for (Stress stress : results.getStressList()) {
        Integer stressLevelValue = stress.getStressLevelValue();
        //Out of range values indicate time when stress data wasn't available, because of off wrist or too active
        if (stressLevelValue != null && stressLevelValue >= 0 && stressLevelValue <= 100) {
          stressDataPoints++;
          stressTotal += stressLevelValue;
          Log.d(TAG, "Stress: " + new Date(stress.getTimestamp()) + " " + stressLevelValue + " " + stress.getAverageIntensity());
        }

      }
    }
    //Calculate average stress for the sync
    if (stressTotal > 0 && stressDataPoints > 0) {
      Log.d(TAG, "Avg Stress: " + stressTotal / stressDataPoints);
    }
  }

  private void sampleHandleWellnessDailyTotals(String key, SyncResult results) {
    if (results == null || results.getWellnessSyncSummaries() == null) {
      return;
    }

    //Multiple sync sample is not applicable without file deletion.
    if (GarminHealth.getTestModeConfiguration().keepData()) {
      return;
    }

    //Use local map for sample saving. Will work when app remains open over multiple syncs.
    List<SyncResult> previousResults = mSyncCombineSample.get(key);
    if (previousResults == null) {
      previousResults = new ArrayList<>();
      mSyncCombineSample.put(key, previousResults);
    }

    if (results.getWellnessSyncSummaries().size() == 0) {
      return;
    }

    //Multiple days can be processed in a single sync
    //See TabFragments for details on displaying data as it returns

    //The first and last day of a sync are likely to be partial days.
    //The last (current) day will be completed in a future sync.
    //The first day can be combined with previous sync results to get the complete totals for the day.
    WellnessSyncSummary wellnessSyncSummary = results.getWellnessSyncSummaries().get(0);
    Date date = wellnessSyncSummary.date();
    SampleSummary sample = new SampleSummary(date);

    //Combine all sync results from the day
    for (SyncResult result : previousResults) {
      List<WellnessSyncSummary> summaries = result.getWellnessSyncSummaries();
      for (WellnessSyncSummary summary : summaries) {
        //Data already exists for new date coming in
        if (summary.date().equals(date)) {
          sample.steps += summary.steps();
          sample.distance += summary.distance();
          sample.floorsClimbed += summary.floorsAscended();
        }
      }
    }

    //Add values from the most recent sync
    sample.steps += wellnessSyncSummary.steps();
    sample.distance += wellnessSyncSummary.distance();
    sample.floorsClimbed += wellnessSyncSummary.floorsAscended();

    //Save most recent sync results for future combination if day is still incomplete
    previousResults.add(results);

    //Totals for the day. Will be complete for days that have been fully synced. For current day, will be total so far.
    Log.i(TAG, "Totals for day:" + sample.date);
    Log.i(TAG, "Steps:" + sample.steps);
    Log.i(TAG, "Distance:" + sample.distance);
    Log.i(TAG, "Meters:" + sample.floorsClimbed);
  }

  private void sampleHandleRawFileResult(SyncResult results) {
    if (results == null || results.getRawFilePaths() == null) {
      return;
    }

    for (int i = 0; i < results.getRawFilePaths().size(); i++) {
      File file = new File(results.getRawFilePaths().get(i));
      // Save file somewhere else for more permanent storage
      // For sample read text from file and Log it
      StringBuilder text = new StringBuilder();
      try {

        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;

        while ((line = br.readLine()) != null) {
          text.append(line);
          text.append('\n');
        }
        br.close();
      } catch (IOException e) {
        Log.e("RAW", "Failed", e);
      }
      Log.i("RAW", i + ":" + text.toString());
    }
  }
}
