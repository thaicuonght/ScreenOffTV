// IEventsListener.aidl
package com.thaicuonght.screenofftv.shizuku;

// Declare any non-default types here with import statements
import com.thaicuonght.screenofftv.ipc.result.EventResult;

interface IEventsListener {
    void onEvent(in EventResult event);
}