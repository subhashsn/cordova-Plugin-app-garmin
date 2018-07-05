/*
 * Copyright (c) 2017 Garmin International. All Rights Reserved.
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
 * Created by johnsongar on 2/9/2017.
 */
package io.ionic.init;

import android.util.Log;

import com.garmin.health.Device;
import com.garmin.health.DeviceManager;
import com.garmin.health.sync.SyncListener;

/**
 * Manager responsible for listening to sync progress and status messages.
 * At least one SyncListener should always be registered to get sync start/complete/failure notifications.
 */
public class SyncManager implements SyncListener {

    protected final String TAG = getClass().getSimpleName();

    private static SyncManager sInstance = new SyncManager();

    public static void init() {
        DeviceManager devMgr = DeviceManager.getDeviceManager();
        com.garmin.health.sync.SyncManager syncMgr = devMgr.getSyncManager();
        syncMgr.addSyncListener(sInstance);
    }

    @Override
    public void onSyncStarted(final Device device) {
        Log.d(TAG, "onSyncStarted(device = " + device.address() + ")");
    }

    @Override
    public void onSyncProgress(Device device, int progress) {
        Log.d(TAG, String.format("onSyncProgress(device = %s, progress = %d)", device.address(), progress));
    }

    @Override
    public void onSyncComplete(Device device) {
        Log.d(TAG, "onSyncComplete(device = " + device.address() + ")");
    }

    @Override
    public void onSyncFailed(final Device device, Exception e) {
        Log.d(TAG, "onSyncFailed(device = " + device.address() + ")", e);
    }
}
