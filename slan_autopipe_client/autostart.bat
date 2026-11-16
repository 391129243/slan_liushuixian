::adb install app-debug.apk
adb shell setprop persist.sys.bootAppClass com.gzlab.autoassemblyline_client.MainActivity
adb shell setprop persist.sys.bootAppPack com.gzlab.autoassemblyline_client
pause