package com.jiaze.callback;

/**
 * =========================================
 * The Project:AutoTestApp
 * the Package:com.jiaze.callback
 * Create by jz-pf
 * on 2019/8/15
 * =========================================
 */
public interface SimTestCallback {
    void testResultCallback(boolean isFinished, int simAbsentTime, int simUnknownTime, int simPinRequiredTime, int simPukRequiredTime, int simReadyTime, int simNetworkLockTime);
}
