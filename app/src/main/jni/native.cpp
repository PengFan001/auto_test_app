#define LOG_TAG "Survive_Jni"
#include "JNIHelp.h"

#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <fcntl.h>
#include <dirent.h>
#include <unistd.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <linux/netlink.h>
#include <sys/time.h>
//#include <asm/page.h>
#include <termios.h>
#include <cutils/sockets.h>

#include <utils/Log.h>
#include <utils/RefBase.h>
#include <utils/Looper.h>
#include <android_runtime/AndroidRuntime.h>
#include "android_os_MessageQueue.h"

#undef ALOGD
#define ALOGD(...) ALOGD_IF(mDebugLevel > 1, __VA_ARGS__);

#ifdef __cplusplus
extern "C" {
#endif

extern int QueryEncryptStatus(void);

#ifdef __cplusplus
}
#endif

//using namespace android;
#define DUMP_TRANSFER_DATA		1

static JavaVM* g_vm                         = NULL;
static JNIEnv* JniEnv                       = NULL;
static jclass  JniClass                     = NULL;
static jobject JniObject                    = NULL;

jmethodID  recvRespPacketCbId               = NULL;
jmethodID  recvUnsolPacketCbId              = NULL;
static const char *classPathName            = "com/xt/keytosurvive/AtSender";

static int sSolFd = -1;
static int sUnsolFd = -1;

#define DATA_BUFFER_SIZE 4096
static char sRespDataBuffer[DATA_BUFFER_SIZE] = {0};
static int sRespDataLen = 0;
static char sParsedRespStrings[256] = {0};

static char sUnsolDataBuffer[DATA_BUFFER_SIZE] = {0};
static int sUnsolDataLen = 0;
static char sParsedUnsolStrings[256] = {0};

static int mDebugLevel = 2;

void recvOnePacketRespDataHandler(char *atStrigs, int length)
{
    int status;
    JNIEnv *env;
    int IsAttached = 0;

    ALOGD("recvOneRespPacket : ++++++");
    ALOGD("recvOneRespPacket : %s", atStrigs);
    ALOGD("recvOneRespPacket : ------");

    status = g_vm->GetEnv((void**)&env, JNI_VERSION_1_4);
    if (status < 0){
        ALOGD("Status < 0 Is Native Thread \n");
        status =  g_vm->AttachCurrentThread(&env, NULL);
        if (status < 0){
              ALOGE("java_callback failed to attach current thread \n");
              return;
        }

        IsAttached = 1;
    }

    // Malloc Memory To Data.
    jbyteArray jarray = env->NewByteArray(length);
    if (jarray == NULL){
        ALOGE(" %s %s %d", __FILE__, __FUNCTION__, __LINE__);
        if (IsAttached){
            g_vm->DetachCurrentThread();
        }
        return;
    }

    env->SetByteArrayRegion(jarray, 0, length, (jbyte *)atStrigs);

    // Take jbyteArray Memory First Address.
    /*jbyte* bytes = env->GetByteArrayElements(jarray, NULL);
    if (bytes == NULL){
        ALOGE(" %s %s %d", __FILE__, __FUNCTION__, __LINE__);
        if (IsAttached){
            g_vm->DetachCurrentThread();
        }
        return ;
    }

    // Copy Data To 'array'.
   // memcpy(bytes, atStrigs, length);
    for(i = 0; i < length; i++) {
        ALOGD("atStrings[%d] = %d,  bytes[%d] = %d ", i, atStrigs[i], i, bytes[i]);
    }*/
    // Invoke Java Method, And Pass Data To Java App.
    env->CallVoidMethod(JniObject, recvRespPacketCbId, jarray, length);

    //    env->ReleaseByteArrayElements(jarray, bytes, 0);

    // ALOGE(" %s %s %d ", __FILE__, __FUNCTION__, __LINE__, uri_jarray);
    if (IsAttached){
        g_vm->DetachCurrentThread();
    }
}

void recvOnePacketUnsolDataHandler(char *atStrigs, int length)
{
    int status;
    JNIEnv *env;
    int IsAttached = 0;

    ALOGD("recvOneUnsolPacket : ++++++");
    ALOGD("recvOneUnsolPacket : %s", atStrigs);
    ALOGD("recvOneUnsolPacket : ------");

    status = g_vm->GetEnv((void**)&env, JNI_VERSION_1_4);
    if (status < 0){
        ALOGD("Status < 0 Is Native Thread \n");
        status =  g_vm->AttachCurrentThread(&env, NULL);
        if (status < 0){
              ALOGE("java_callback failed to attach current thread \n");
              return;
        }

        IsAttached = 1;
    }

    // Malloc Memory To Data.
    jbyteArray jarray = env->NewByteArray(length);
    if (jarray == NULL){
        ALOGE(" %s %s %d", __FILE__, __FUNCTION__, __LINE__);
        if (IsAttached){
            g_vm->DetachCurrentThread();
        }
        return;
    }
    env->SetByteArrayRegion(jarray, 0, length, (jbyte *)atStrigs);

    // Take jbyteArray Memory First Address.
    /*
    jbyte* bytes = env->GetByteArrayElements(jarray, NULL);
    if (bytes == NULL){
        ALOGE(" %s %s %d", __FILE__, __FUNCTION__, __LINE__);
        if (IsAttached){
            g_vm->DetachCurrentThread();
        }
        return ;
    }

    // Copy Data To 'array'.
    memcpy(bytes, atStrigs, length);*/

    // Invoke Java Method, And Pass Data To Java App.
    env->CallVoidMethod(JniObject, recvUnsolPacketCbId, jarray, length);
    //env->ReleaseByteArrayElements(jarray, bytes, 0);

    // ALOGE(" %s %s %d ", __FILE__, __FUNCTION__, __LINE__, uri_jarray);
    if (IsAttached){
        g_vm->DetachCurrentThread();
    }
}

static int is_final_rsp(const char *at_ptr){
    static const char * final_rsp_ptr[] ={
        "CONNECT",
        "OK",
        "BUSY",
        "+CMS ERROR:",
        "+CME ERROR:",
        "ERROR",
        "NO CARRIER",
        "NO ANSWER",
        "NO DIALTONE",
        "ERROR",
    };
    
    int     count = sizeof(final_rsp_ptr) / sizeof(char *);
    int     i = 0;

    /* Check parameter. */
    if(!at_ptr){
        return 0;
    }

    for( i=0 ; i<count; i++ ){
        if(strstr(at_ptr, final_rsp_ptr[i])){
            return 1;
        }
    }

    return 0;
}

static int is_final_unsol(const char *at_ptr){
    static const char * final_unsol_ptr[] ={
        "+EMGSMS:"
    };

    int     count = sizeof(final_unsol_ptr) / sizeof(char *);
    int     i = 0;

    /* Check parameter. */
    if(!at_ptr){
        return 0;
    }

    for( i=0 ; i<count; i++ ){
        if(strstr(at_ptr, final_unsol_ptr[i])){
            return 1;
        }
    }

    return 0;
}

void checkResponseFrame()
{
	char* p;
	char* pStart = sRespDataBuffer;
	int remain;
	
	while((p = strstr(pStart, "\r\n")) != NULL){
		if(p == pStart){
			pStart += 2;
		}else{
			*p = 0;
			*(p + 1) = 0;
			strcat(sParsedRespStrings, pStart);
			if(is_final_rsp(pStart)){
				recvOnePacketRespDataHandler(sParsedRespStrings, strlen(sParsedRespStrings));
				sParsedRespStrings[0] = 0;
			} else if(is_final_unsol(pStart)){
			    recvOnePacketUnsolDataHandler(sParsedRespStrings, strlen(sParsedRespStrings));
				sParsedRespStrings[0] = 0;
			}else{
				strcat(sParsedRespStrings, "|");
			}
			pStart = p + 2;
		}
	}

	if(pStart < (sRespDataBuffer + sRespDataLen)){
		sRespDataLen -= (pStart - sRespDataBuffer);
		memmove(sRespDataBuffer, pStart, sRespDataLen);
	}else{
		sRespDataLen = 0;
	}

	sRespDataBuffer[sRespDataLen] = 0;
}

void receivedResponseData(char* data, int byteLen)
{
	int ret, i;

	if(sRespDataLen + byteLen < DATA_BUFFER_SIZE){
		memcpy(&sRespDataBuffer[sRespDataLen], data, byteLen);
		sRespDataLen += byteLen;
		sRespDataBuffer[sRespDataLen] = 0;
	}

	ALOGD(">>resp recv %d bytes : %s", sRespDataLen, sRespDataBuffer);
	if(sRespDataLen >= 2) {
		checkResponseFrame();
	}
}

void *readResponseThread(void *param)
{
    long fd = (long)(uintptr_t)param;
    int ret, length;
    fd_set rfds;
    char buffer[64];
    struct timeval tv;

    ALOGD("readResponseThread entry fd = %d", fd);

    for (;;) {
    	FD_ZERO(&rfds);
    	FD_SET(fd, &rfds);

        tv.tv_sec = 10;
        tv.tv_usec = 0;
        ret = select(fd+1, &rfds, NULL, NULL, &tv);
        if (ret > 0) {
            length = read(fd, buffer, 63);
            if(length > 0){
                buffer[length] = 0;
                receivedResponseData(buffer, length);
            }else if(length == 0){
                //ALOGD("read 0 byte...");
                continue;
            }else{
                if(errno == EINTR || errno == EAGAIN){
                    ALOGE("read EINTR EAGAIN");
                    continue;
                }else{
                    ALOGE("read error:%d,errno=%d", length, errno);
                    break;
                }
            }
        }else if(ret == 0){
            //ALOGD("select 0 byte...");
            continue;
        }else{
            ALOGE("select error:%d,errno=%d", ret, errno);
            break;
        }
    }

    sSolFd = -1;
    ALOGE("read response thread exit!!!");
    return NULL;
}

void checkUnsolFrame()
{
	char* p;
	char* pStart = sUnsolDataBuffer;
	int remain;
	
	while((p = strstr(pStart, "\r\n")) != NULL){
		if(p == pStart){
			pStart += 2;
		}else{
			*p = 0;
			*(p + 1) = 0;
			strcpy(sParsedUnsolStrings, pStart);
            recvOnePacketUnsolDataHandler(sParsedUnsolStrings, strlen(sParsedUnsolStrings));
			pStart = p + 2;
		}
	}

	if(pStart < (sUnsolDataBuffer + sUnsolDataLen)){
		sUnsolDataLen -= (pStart - sUnsolDataBuffer);
		memmove(sUnsolDataBuffer, pStart, sUnsolDataLen);
	}else{
		sUnsolDataLen = 0;
	}

	sUnsolDataBuffer[sUnsolDataLen] = 0;
}
void receivedUnsolData(char* data, int byteLen)
{
	int ret, i;

	if(sUnsolDataLen + byteLen < DATA_BUFFER_SIZE){
		memcpy(&sUnsolDataBuffer[sUnsolDataLen], data, byteLen);
		sUnsolDataLen += byteLen;
		sUnsolDataBuffer[sUnsolDataLen] = 0;
	}

	ALOGD(">>unsol recv %d bytes : %s", sUnsolDataLen, sUnsolDataBuffer);
	if(sUnsolDataLen >= 2) {
		checkUnsolFrame();
	}
}

void *readUnsolThread(void *param)
{
    long fd = (long)(uintptr_t)param;
    int ret, length;
    fd_set rfds;
    char buffer[64];
    struct timeval tv;

    ALOGD("readUnsolThread entry fd = %d", fd);
    for (;;) {
    	FD_ZERO(&rfds);
    	FD_SET(fd, &rfds);

        tv.tv_sec = 10;
        tv.tv_usec = 0;
        ret = select(fd+1, &rfds, NULL, NULL, &tv);
        if (ret > 0) {
            length = read(fd, buffer, 63);
            if(length > 0){
                buffer[length] = 0;
                receivedUnsolData(buffer, length);
            }else if(length == 0){
                //ALOGD("read 0 byte...");
                continue;
            }else{
                if(errno == EINTR || errno == EAGAIN){
                    ALOGE("read EINTR EAGAIN");
                    continue;
                }else{
                    ALOGE("read error:%d,errno=%d", length, errno);
                    break;
                }
            }
        }else if(ret == 0){
            //ALOGD("select 0 byte...");
            continue;
        }else{
            ALOGE("select error:%d,errno=%d", ret, errno);
            break;
        }
    }

    sUnsolFd = -1;
    ALOGE("read unsol thread exit!!!");
    return NULL;
}

int createReadResponseThread(int fd){
    int ret;
    pthread_t tid;

    ALOGD("createReadResponseThread fd = %d", fd);

    ret = pthread_create(&tid, NULL, readResponseThread, (void *)(uintptr_t)fd);

    return 0;
}

int createReadUnsolThread(int fd){
    int ret;
    pthread_t tid;

    ALOGD("createReadUnsolThread");
	ret = pthread_create(&tid, NULL, readUnsolThread, (void *)(uintptr_t)fd);

    return 0;
}
static int open_device(const char* tty_name)
{
    int serial_fd;
    
    ALOGD("open_device:%s", tty_name);
    serial_fd = open(tty_name, O_RDWR | O_NOCTTY | O_NDELAY);
    if (serial_fd < 0) {
        perror("open");
        return -1;
    }

    return serial_fd;
}

static int send_at_command(const char* command)
{
	if (sSolFd >= 0) {
		int ret, length, sentLengh;
		char *sendPtr;
		ALOGD(">>Send string (%s)", command);

		char buffer[256];
		sprintf(buffer, "%s\r\n", command);
		sendPtr = (char*) buffer;
		length = strlen(buffer);
		sentLengh = 0;

		while (length > 0) {
			ret = write(sSolFd, sendPtr, length);
			ALOGD("send string length=%d", ret);
			if (ret > 0) {
				length -= ret;
				sendPtr += ret;
				sentLengh += ret;
			} else {
				ALOGE("send string error:%d", ret);
				break;
			}
		}

        return sentLengh;
    }else{
        return -1;
    }
}

jboolean init_javacb(JNIEnv *env, jobject thiz)
{
    ALOGD("Enter %s:%s()", __FILE__, __FUNCTION__);
    JniEnv    = env;
    JniObject = env->NewGlobalRef(thiz);

    // Get the Class.
    JniClass = env->FindClass(classPathName);
    if (!JniClass)
    {
        ALOGE("JniClass Init Failed. %s %s %d", __FILE__, __FUNCTION__, __LINE__);
        return JNI_FALSE;
    }

    recvRespPacketCbId = env->GetMethodID(JniClass, "jniReceivedOneRespPacket", "([BI)V");
    if (!recvRespPacketCbId)
    {
        ALOGE("recvRespPacketCbId Init Failed. %s %s %d", __FILE__, __FUNCTION__, __LINE__);
        return JNI_FALSE;
    }

    recvUnsolPacketCbId = env->GetMethodID(JniClass, "jniReceivedOneUnsolPacket", "([BI)V");
    if (!recvUnsolPacketCbId)
    {
        ALOGE("recvUnsolPacketCbId Init Failed. %s %s %d", __FILE__, __FUNCTION__, __LINE__);
        return JNI_FALSE;
    }

    ALOGD("Exit %s:%s", __FILE__, __FUNCTION__);
    return JNI_TRUE;
}

static jint jni_init(JNIEnv *env, jobject obj)
{
	if (!init_javacb(env, obj))
    {
        ALOGE("ERROR: C Invoke Java CallBack Init Failed.");
        return -1;
    }

    return 0;
}

static jint jni_open_at_device(JNIEnv *env, jobject obj, jstring solDevName, jstring unsolDevName)
{
    if(sSolFd < 0){
        const char *sol_dev = env->GetStringUTFChars(solDevName, NULL);
        sSolFd = open_device(sol_dev);
        env->ReleaseStringUTFChars(solDevName, sol_dev);
        if(sSolFd >= 0){
            createReadResponseThread(sSolFd);
        }else{
            return -1;
        }
    }

    if(sUnsolFd < 0){
        const char *unsol_dev = env->GetStringUTFChars(unsolDevName, NULL);
        sUnsolFd = open_device(unsol_dev);
        env->ReleaseStringUTFChars(unsolDevName, unsol_dev);
        if(sUnsolFd >= 0){
            createReadUnsolThread(sUnsolFd);
        }else{
            return -1;
        }
    }

    return 0;
}

static jint jni_close_at_device(JNIEnv *env, jobject obj)
{
    if(sSolFd >= 0){
        close(sSolFd);
        sSolFd = -1;

        sRespDataLen = 0;
        memset(sRespDataBuffer, 0, DATA_BUFFER_SIZE);
    }

    if(sUnsolFd >= 0){
        close(sUnsolFd);
        sUnsolFd = -1;
        memset(sUnsolDataBuffer, 0, DATA_BUFFER_SIZE);
        sUnsolDataLen = 0;
    }

    return 0;
}

static jint jni_send_at_command(JNIEnv *env, jobject obj, jstring str)
{	
    if(sSolFd >= 0){
        int ret, length, sentLengh;
        const char *command = env->GetStringUTFChars(str, NULL);

		ret = send_at_command(command);

        env->ReleaseStringUTFChars(str, command);

        return ret;
    }else{
        return -1;
    }
}

static JNINativeMethod methods[] = {
    //Java Invoke C.
    {"jni_init",                             "()I",                                     (void*)jni_init},
    {"jni_open_at_device",                   "(Ljava/lang/String;Ljava/lang/String;)I", (void*)jni_open_at_device},
    {"jni_close_at_device",                  "()I",                                     (void*)jni_close_at_device},
    {"jni_send_at_command",                  "(Ljava/lang/String;)I",                   (void*)jni_send_at_command},
};

static int registerNativeMethods(JNIEnv* env, const char* className, JNINativeMethod* gMethods, int numMethods)
{
    ALOGD("Enter %s:%s()", __FILE__, __FUNCTION__);
    jclass clazz;

    clazz = env->FindClass(className);
    if (clazz == NULL)
    {
        ALOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }

    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0)
    {
        ALOGE("RegisterNatives failed for '%s'", className);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

static int registerNatives(JNIEnv* env)
{
    ALOGD("Enter %s:%s()", __FILE__, __FUNCTION__);
    if (!registerNativeMethods(env, classPathName, methods, sizeof(methods) / sizeof(methods[0])))
    {
        ALOGE(" %s %s %d", __FILE__, __FUNCTION__, __LINE__);
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    ALOGD("Enter %s:%s()", __FILE__, __FUNCTION__);
    jint result = -1;
    void* env = NULL;
    g_vm = vm;

    if (vm->GetEnv(&env, JNI_VERSION_1_4) != JNI_OK)
    {
        ALOGE("ERROR: GetEnv failed %s %s %d", __FILE__, __FUNCTION__, __LINE__);
        goto bail;
    }

    if (registerNatives((JNIEnv*)env) != JNI_TRUE)
    {
        ALOGE("ERROR: registerNatives failed %s %s %d", __FILE__, __FUNCTION__, __LINE__);
        goto bail;
    }

    result = JNI_VERSION_1_4;

bail:
    return result;
}



