package com.jiaze.callback;

/**
 * =========================================
 * The Project:AutoTestApp
 * the Package:com.jiaze.callback
 * Create by jz-pf
 * on 2019/8/15
 * =========================================
 */
public interface NetworkTestCallback {
    void testResultCallback(boolean isFinished, int netInServiceTime, int netOutServiceTime, int netEmergencyTime, int netPowerOffTime);
}
