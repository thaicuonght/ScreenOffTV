// IDisplayControl.aidl
package com.thaicuonght.screenofftv.shizuku;

import com.thaicuonght.screenofftv.ipc.result.UnitResult;

// Declare any non-default types here with import statements

interface IDisplayControl {

    UnitResult setPowerModeToSurfaceControl(int mode) = 2;
    UnitResult setBrightnessToSurfaceControl(float brightness) = 4;
    UnitResult setBrightnessToSetting(int brightness) = 5;
    UnitResult setBrightnessModeToSetting(int mode) = 6;

    UnitResult prepare() = 7;

    void destroy() = 16777114;
}