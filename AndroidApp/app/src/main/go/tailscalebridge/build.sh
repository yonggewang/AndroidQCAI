#!/bin/bash
NDK_PATH="/Users/yonwang/Library/Android/sdk/ndk/30.0.14904198"
TOOLCHAIN="$NDK_PATH/toolchains/llvm/prebuilt/darwin-x86_64"
TARGET="aarch64-linux-android30"
CLANG="$TOOLCHAIN/bin/${TARGET}-clang"

export CGO_ENABLED=1
export GOOS=android
export GOARCH=arm64
export CC="$CLANG"

echo "Building Tailscale Bridge for Android ARM64..."
go build -buildmode=c-shared -o libtailscalebridge.so bridge.go
if [ $? -eq 0 ]; then
    echo "Build Succeeded!"
    # Move to the project's jniLibs
    mkdir -p ../../jniLibs/arm64-v8a
    mv libtailscalebridge.so ../../jniLibs/arm64-v8a/
    rm libtailscalebridge.h
else
    echo "Build Failed!"
    exit 1
fi
