#!/usr/bin/env bash

# Adapted from https://gitlab.com/connect2x/tammy/-/blob/4f2c41cd758cd6f6205a2046a3a8472104c62dec/fastlane/start_screenshot_emulators.sh

ANDROID_PLATFORM=${ANDROID_PLATFORM:-35}

echo "create emulators"
avdmanager create avd -n Default -k "system-images;android-${ANDROID_PLATFORM};google_apis;x86_64" -d pixel_7_pro

echo "start emulators"
emulator -avd Default -port 5554 -no-window -no-audio -no-boot-anim >> emulator.log 2>&1 &

explain() {
	if [[ "$1" =~ "not found" ]]; then
		printf "device not found"
	elif [[ "$1" =~ "offline" ]]; then
		printf "device offline"
	elif [[ "$1" =~ "running" ]]; then
		printf "booting"
	else
		printf "$1"
	fi
}

wait_for_emulator() {
    local adb_port=$1
    local sec=0
    local timeout=300

    adb -s "emulator-${adb_port}" wait-for-device
    adb -s "emulator-${adb_port}" devices

    while true; do
        if [[ $sec -ge $timeout ]]; then
            echo "Timeout (${timeout} seconds) reached - Failed to start emulator on port ${adb_port}"
            exit 1
        fi
        out=$(adb -s "emulator-${adb_port}" shell getprop init.svc.bootanim 2>&1 | grep -v '^\*')
        if [[ "$out" =~ "command not found" ]]; then
            echo "$out"
            exit 1
        fi
        if [ "$(adb -s "emulator-${adb_port}" shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; then
            break
        fi
        let "r = sec % 5"
        if [[ $r -eq 0 ]]; then
            echo "Waiting for emulator on port ${adb_port} to start: $(explain "$out")"
        fi
        sleep 1
        let "sec++"
    done
    echo "Emulator on port ${adb_port} is ready (took ${sec} seconds)"
}

echo "wait for emulators to fully start"
wait_for_emulator 5554
