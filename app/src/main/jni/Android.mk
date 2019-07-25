###
### JNI libary for libsurvive
###

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_PRELINK_MODULE := false

LOCAL_C_INCLUDES += \
    $(JNI_H_INCLUDE) \
    $(common_C_INCLUDES) \
    frameworks/base/core/jni 

LOCAL_SRC_FILES := native.cpp 

LOCAL_MODULE := libsurvive

LOCAL_SHARED_LIBRARIES := libc liblog libcutils libutils libandroid_runtime libnativehelper libencrypt_client

include $(BUILD_SHARED_LIBRARY)
