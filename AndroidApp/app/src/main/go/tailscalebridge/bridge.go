package main

/*
#include <jni.h>
#include <stdlib.h>
#include <android/log.h>

static jstring newStringUTF(JNIEnv* env, const char* str) {
    if (str == NULL) return NULL;
    return (*env)->NewStringUTF(env, str);
}

static const char* getStringBytes(JNIEnv* env, jstring jstr) {
    if (jstr == NULL) return NULL;
    return (*env)->GetStringUTFChars(env, jstr, NULL);
}

static void releaseStringBytes(JNIEnv* env, jstring jstr, const char* str) {
    if (jstr == NULL || str == NULL) return;
    (*env)->ReleaseStringUTFChars(env, jstr, str);
}

static void android_log(const char* msg) {
    __android_log_write(ANDROID_LOG_DEBUG, "TailscaleBridge", msg);
}

__attribute__((constructor))
static void init_env() {
    setenv("TS_DEBUG_NO_NETLINK", "1", 1);
    setenv("TS_NO_EXTERNAL_CONFIG", "1", 1);
    setenv("TS_PORTABLE_NETSTACK", "1", 1);
    setenv("TS_NO_SYSNETWORK_CONNS", "1", 1);
    setenv("TS_DEBUG_NO_ROUTING", "1", 1);
    setenv("TS_DEBUG_IGNORE_NETLINK_ERRORS", "1", 1);
    setenv("TS_DEBUG_WITHOUT_NETMON", "1", 1);
    setenv("TS_DEBUG_ALWAYS_USE_NETSTACK", "1", 1);
    setenv("TS_DEBUG_NO_LINUX_NETLINK", "1", 1);
}
*/
import "C"

import (
	"context"
	"fmt"
	"net"
	"os"
	"sync"
	"time"
	"unsafe"

	"tailscale.com/net/netmon"
	"tailscale.com/net/socks5"
	"tailscale.com/tsnet"
)

// logToAndroid sends a formatted message to Android Logcat.
// This is the ONLY safe way to log from Go on Android — fmt.Printf will crash.
func logToAndroid(format string, args ...any) {
	msg := fmt.Sprintf(format, args...)
	cmsg := C.CString(msg)
	C.android_log(cmsg)
	C.free(unsafe.Pointer(cmsg))
}

// makeJString safely creates a JNI string from a Go string.
// Returns NULL jstring if the input is empty.
func makeJString(env *C.JNIEnv, s string) C.jstring {
	if s == "" {
		return 0
	}
	cs := C.CString(s)
	js := C.newStringUTF(env, cs)
	C.free(unsafe.Pointer(cs))
	return js
}

func init() {
	// This MUST be minimal. Only the netmon override.
	// All env vars are already set by the C constructor above.
	netmon.RegisterInterfaceGetter(func() ([]netmon.Interface, error) {
		// Return a fake active interface so Tailscale doesn't pause itself
		// thinking there's no internet connection.
		return []netmon.Interface{
			{
				Interface: &net.Interface{
					Index: 1,
					Name:  "fake0",
					Flags: net.FlagUp | net.FlagRunning,
				},
				AltAddrs: []net.Addr{
					&net.IPNet{IP: net.ParseIP("10.0.0.2"), Mask: net.CIDRMask(24, 32)},
				},
			},
		}, nil
	})
}


var (
	ts            *tsnet.Server
	tsLock        sync.Mutex
	socksListener net.Listener
)

//export Java_com_quantumproperty_qcai_native_TailscaleBridge_connect
func Java_com_quantumproperty_qcai_native_TailscaleBridge_connect(env *C.JNIEnv, obj C.jobject, authKey C.jstring, hostname C.jstring, stateDir C.jstring) C.jstring {
	// Catch any Go panic to prevent SIGSEGV killing the app
	defer func() {
		if r := recover(); r != nil {
			logToAndroid("PANIC in connect: %v", r)
		}
	}()

	goAuthKey := getString(env, authKey)
	goHostname := getString(env, hostname)
	goStateDir := getString(env, stateDir)

	logToAndroid("connect called: hostname=%s stateDir=%s authKeyLen=%d", goHostname, goStateDir, len(goAuthKey))

	err := connectInternal(goAuthKey, goHostname, goStateDir)
	if err != nil {
		logToAndroid("connect error: %v", err)
		return makeJString(env, err.Error())
	}

	logToAndroid("connect succeeded")
	return 0 // JNI null = success
}

//export Java_com_quantumproperty_qcai_native_TailscaleBridge_getIP
func Java_com_quantumproperty_qcai_native_TailscaleBridge_getIP(env *C.JNIEnv, obj C.jobject) C.jstring {
	defer func() {
		if r := recover(); r != nil {
			logToAndroid("PANIC in getIP: %v", r)
		}
	}()
	ip := getIPInternal()
	return makeJString(env, ip)
}

//export Java_com_quantumproperty_qcai_native_TailscaleBridge_getState
func Java_com_quantumproperty_qcai_native_TailscaleBridge_getState(env *C.JNIEnv, obj C.jobject) C.jstring {
	defer func() {
		if r := recover(); r != nil {
			logToAndroid("PANIC in getState: %v", r)
		}
	}()
	state := getStateInternal()
	return makeJString(env, state)
}

//export Java_com_quantumproperty_qcai_native_TailscaleBridge_disconnect
func Java_com_quantumproperty_qcai_native_TailscaleBridge_disconnect(env *C.JNIEnv, obj C.jobject) {
	defer func() {
		if r := recover(); r != nil {
			logToAndroid("PANIC in disconnect: %v", r)
		}
	}()
	disconnectInternal()
}

func getString(env *C.JNIEnv, jstr C.jstring) string {
	if jstr == 0 {
		return ""
	}
	charPtr := C.getStringBytes(env, jstr)
	if charPtr == nil {
		return ""
	}
	defer C.releaseStringBytes(env, jstr, charPtr)
	return C.GoString(charPtr)
}

func connectInternal(authKey string, hostname string, stateDir string) error {
	tsLock.Lock()
	if ts != nil {
		tsLock.Unlock()
		return nil
	}

	// stateDir MUST be provided by the Android caller (context.filesDir)
	if stateDir == "" {
		tsLock.Unlock()
		return fmt.Errorf("stateDir is required")
	}

	// Create the directory FIRST, then tell Tailscale about it
	if err := os.MkdirAll(stateDir, 0700); err != nil {
		tsLock.Unlock()
		return fmt.Errorf("failed to create stateDir: %v", err)
	}

	logToAndroid("stateDir created/verified: %s", stateDir)

	if hostname == "" {
		hostname = "qcai-android"
	}

	logToAndroid("creating tsnet.Server: hostname=%s dir=%s", hostname, stateDir)

	// Fix: "no safe place found to store log state"
	// These MUST be set BEFORE ts.Start(), and the directory MUST exist
	os.Setenv("TS_LOGS_DIR", stateDir)
	os.Setenv("TS_LOG_TARGET", "")    // disable remote log uploading
	os.Setenv("HOME", stateDir)       // many Go packages check HOME
	os.Setenv("TMPDIR", stateDir)     // Android has no /tmp

	if authKey != "" {
		logToAndroid("AuthKey provided, forcing login")
		os.Setenv("TSNET_FORCE_LOGIN", "1")
	}

	ts = &tsnet.Server{
		Hostname:   hostname,
		AuthKey:    authKey,
		Dir:        stateDir,
		ControlURL: "https://controlplane.tailscale.com",
		Ephemeral:  true,
		Logf:       logToAndroid,
		UserLogf:   logToAndroid,
	}

	logToAndroid("calling ts.Start()...")

	if err := ts.Start(); err != nil {
		ts = nil
		tsLock.Unlock()
		return fmt.Errorf("failed to start tsnet: %v", err)
	}

	logToAndroid("ts.Start() succeeded, starting SOCKS5 listener...")

	go func() {
		defer func() {
			if r := recover(); r != nil {
				logToAndroid("PANIC in SOCKS5 goroutine: %v", r)
			}
		}()
		server := &socks5.Server{
			Logf:   logToAndroid,
			Dialer: ts.Dial,
		}
		ln, err := net.Listen("tcp", "127.0.0.1:18791")
		if err != nil {
			logToAndroid("SOCKS5 listen error: %v", err)
			return
		}
		tsLock.Lock()
		socksListener = ln
		tsLock.Unlock()
		logToAndroid("SOCKS5 listening on 127.0.0.1:18789")
		server.Serve(ln)
	}()

	tsLock.Unlock()

	logToAndroid("waiting for Tailscale IP assignment...")

	for i := 0; i < 40; i++ {
		if ip := getIPInternal(); ip != "" {
			logToAndroid("got IP: %s", ip)
			return nil
		}
		time.Sleep(500 * time.Millisecond)
	}

	state := getStateInternal()
	return fmt.Errorf("timeout waiting for IP. State: %s", state)
}

func getIPInternal() string {
	tsLock.Lock()
	defer tsLock.Unlock()
	if ts == nil {
		return ""
	}
	lc, err := ts.LocalClient()
	if err != nil {
		return ""
	}
	status, err := lc.Status(context.Background())
	if err != nil || status == nil || len(status.TailscaleIPs) == 0 {
		return ""
	}
	return status.TailscaleIPs[0].String()
}

func getStateInternal() string {
	tsLock.Lock()
	defer tsLock.Unlock()
	if ts == nil {
		return "Disconnected"
	}
	lc, err := ts.LocalClient()
	if err != nil {
		return "Error"
	}
	status, err := lc.StatusWithoutPeers(context.Background())
	if err != nil || status == nil {
		return "Unknown"
	}
	return status.BackendState
}

func disconnectInternal() {
	tsLock.Lock()
	defer tsLock.Unlock()
	if ts == nil {
		return
	}
	if socksListener != nil {
		socksListener.Close()
		socksListener = nil
	}
	ts.Close()
	ts = nil
}

func main() {}
