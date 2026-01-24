#!/bin/bash

SOURCE="/Volumes/HDD/iOS/AndroidCLTDIY/AndroidApp/app/src/main/Icon-ios-marketing-1024pt-1x.png"
RES_DIR="/Volumes/HDD/iOS/AndroidCLTDIY/AndroidApp/app/src/main/res"

# Function to generate icon
generate_icon() {
    local folder=$1
    local size=$2
    local dest_dir="$RES_DIR/$folder"
    
    mkdir -p "$dest_dir"
    
    # Generate ic_launcher
    sips -z $size $size "$SOURCE" --out "$dest_dir/ic_launcher.png"
    
    # Generate ic_launcher_round (using same image for now as sips circle crop is complex)
    sips -z $size $size "$SOURCE" --out "$dest_dir/ic_launcher_round.png"
    
    echo "Generated icons for $folder ($size x $size)"
}

generate_icon "mipmap-mdpi" 48
generate_icon "mipmap-hdpi" 72
generate_icon "mipmap-xhdpi" 96
generate_icon "mipmap-xxhdpi" 144
generate_icon "mipmap-xxxhdpi" 192
