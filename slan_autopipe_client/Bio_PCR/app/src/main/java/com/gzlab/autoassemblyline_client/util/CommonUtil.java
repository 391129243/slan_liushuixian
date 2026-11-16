package com.gzlab.autoassemblyline_client.util;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.core.content.FileProvider;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.hzmct.enjoysdk.api.EnjoySDK;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


public class CommonUtil {
    /**
     * 获取设备的序列号SN
     */
    public static String getDeviceSerialNum(){
        String serialNum = null;
        try {

            Class<?> c = Class.forName("android.os.SystemProperties");

            Method get =c.getMethod("get", String.class);

            serialNum = (String)get.invoke(c, "ro.serialno");

        } catch (Exception e) {

            e.printStackTrace();

        }

        return  serialNum;
    }

	/***
	 * 获取版本号
	 * versionCode
	 */
	public static int getAppVersionCode(Context context){
		int versionCode = 0;
		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(context.getApplicationContext().getPackageName(), 0);
			versionCode = pi.versionCode;
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		return versionCode;
	}
	
	/**
	 * 获取版本名称
	 * versionName
	 */
	public static String getAppVersionName(Context context){
		String versionName = "";
		try {
			PackageManager pm = context.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
			versionName = pi.versionName;
			if(null == versionName || versionName.length()<=0){
				versionName = "";
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return versionName;
	}
	

	
	   //判断是否是字母
    public static boolean isLetter(String str) {
        String wordStr = "abcdefghijklmnopqrstuvwxyz";
        return wordStr.contains(str.toLowerCase());
    }


    public static boolean isNumeric(String str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }


    /**
     * 禁止Edittext弹出软件盘，光标依然正常显示,并且能正常选取光标
     */
    public static void disableShowSoftInput(EditText editText) {
        Class<EditText> cls = EditText.class;
        Method method;
        try {
            method = cls.getMethod("setShowSoftInputOnFocus", boolean.class);
            method.setAccessible(true);
            method.invoke(editText, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * 显示键盘
     * */
    public static void showKeyboard(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                view.requestFocus();
                imm.showSoftInput(view, 0);
            }
        }
    }

    /**
     * 隐藏软键盘
     */
    public static void hideKeyboard(Context context) {
        View view = ((Activity) context).getWindow().peekDecorView();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }


    /**
     * 获取实际内容高度
     *
     * @param context
     * @return
     */
    public static int getContentHeight(Context context) {
        int screenh_nonavbar = 0;
        DisplayMetrics dMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (windowManager != null) {
            Display display = windowManager.getDefaultDisplay();
            display.getMetrics(dMetrics);
            screenh_nonavbar = dMetrics.heightPixels;

            int ver = Build.VERSION.SDK_INT;

            // 新版本的android 系统有导航栏，造成无法正确获取高度
            if (ver == 13) {
                try {
                    Method mt = display.getClass().getMethod("getRealHeight");
                    screenh_nonavbar = (Integer) mt.invoke(display);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (ver > 13) {
//                try {
//                    Method mt = display.getClass().getMethod("getRawHeight");
//                    screenh_nonavbar = (Integer) mt.invoke(display);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
            }
        }
        return screenh_nonavbar - getStatusBarHeight(context);
    }
    
    /**
     * 电量栏高度
     *
     * @return
     */
    public static int getStatusBarHeight(Context context) {
        Class<?> c = null;
        Object obj = null;
        Field field = null;
        int x = 0,
                sbar = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            sbar = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        return sbar;
    }

    public static void checkNull(Object object, String info) {
        if (object == null) {
            throw new NullPointerException(info);
        }
    }

    public static boolean checkIsApk(String filename){
        boolean isApkFile = false;
       // 判断是否以.apk结尾
        String fileEnd = filename.substring(filename.lastIndexOf(".")+1,filename.length()).toLowerCase();
        if(fileEnd.equals("apk")){
            isApkFile = true;
        }else{
            isApkFile = false;
        }
        return  isApkFile;
    }

    public static String getUPath(Context context){
        StorageManager mStorageManager = (StorageManager) context.getSystemService(Activity.STORAGE_SERVICE);
        Class<?> volumeInfoClazz = null;
        Method getVolumes = null;
        Method isMountedReadable = null;
        Method getType = null;
        Method getPath = null;
        List<?> volumes = null;
        try {
            Class storeManagerClazz = Class.forName("android.os.storage.StorageManager");
            volumeInfoClazz = Class.forName("android.os.storage.VolumeInfo");
            getVolumes = StorageManager.class.getMethod("getVolumes");
            isMountedReadable = volumeInfoClazz.getMethod("isMountedReadable");

            getType = volumeInfoClazz.getMethod("getType");
            getPath = volumeInfoClazz.getMethod("getPath");
            volumes = (List<?>) getVolumes.invoke(mStorageManager);
            if (volumes.size()==0){
                return null;
            }
            for (Object vol : volumes) {

                if (vol != null && (Boolean)isMountedReadable.invoke(vol) && (Integer) getType.invoke(vol) == 0) {
                    File path2 = (File) getPath.invoke(vol);
                    String p2 = path2.getPath();
                    Log.i("CommonUtil","找到U盘路径:%s\n"+p2);
                    return p2;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


    public static String getAppProcessName(Context context) {
        //当前应用pid
        int pid = android.os.Process.myPid();
        //任务管理类
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        //遍历所有应用
        List<ActivityManager.RunningAppProcessInfo> infos = manager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo info : infos) {
            if (info.pid == pid)//得到当前应用
                return info.processName;//返回包名
        }
        return "";
    }

    public static Uri getUriForFile(Context context, File file) {
        if (context == null || file == null) {
            throw new NullPointerException();
        }
        Uri uri;
        if (Build.VERSION.SDK_INT >= 24) {
            uri = FileProvider.getUriForFile(context, CommonUtil.getAppProcessName(context) + ".fileProvider", file);
        } else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }


    /**
     * 剩余SDCARD的空间
     */

    public static long getSDcardTotalSize(){
        File directory = Environment.getExternalStorageDirectory();
        StatFs statFs = new StatFs(directory.getAbsolutePath());
        //获取硬盘分的块的数量
        int blockCount = statFs.getBlockCount();
        //每块的大小
        long blockSize = statFs.getBlockSize();
        //可用块的数量
        int availableBlocks = statFs.getAvailableBlocks();
        //sdcard的总容量   以字节为单位
        long sdcardSize = blockCount*blockSize;

        long availableSdcardSize = blockSize*availableBlocks;
        //格式化 转换sdcard大小的单位
        return sdcardSize;
    }

    /**
     * 剩余的sdcard容量
     */
    public static long getAvailableSDcardSize(){
        File directory = Environment.getExternalStorageDirectory();
        StatFs statFs = new StatFs(directory.getAbsolutePath());
        //获取硬盘分的块的数量
        int blockCount = statFs.getBlockCount();
        //每块的大小
        long blockSize = statFs.getBlockSize();
        //可用块的数量
        int availableBlocks = statFs.getAvailableBlocks();
        //sdcard的总容量   以字节为单位

        long availableSdcardSize = blockSize*availableBlocks;
        //格式化 转换sdcard大小的单位
        return availableSdcardSize;
    }

    /**
     * 获取U盘真正的根目录
     */
    public static String getUSBRealRootDirectory(String usbTempRootDirectory) {
        String realUSBRootDirectory = "";
        File dir = new File(usbTempRootDirectory);
        File[] files = dir.listFiles();

        /**
         * 注意：
         * 经测试，
         * TV 直接是 usbTempRootDirectory 作为 U 盘的根目录，例如：/storage/577F-85CA
         * 一体机会在 U 盘的根目录（usbTempRootDirectory=/mnt/usb_storage/USB_DISK4）下再创建多个包含 "udisk" 的目录，然后其中一个作为 U 盘的根目录，例如：/mnt/usb_storage/USB_DISK4/udisk0
         */
        if (files != null) {
            for (File file : files) {
                //如果根目录下还有包含 "udisk" 的目录，则该包含 "udisk" 的目录才是 U 盘真正的根目录
                if (file.isDirectory() && file.list().length > 0 && file.getAbsolutePath().contains("udisk")) {
                    realUSBRootDirectory = file.getAbsolutePath();
                    break;
                } else { // 如果根目录下没有包含 "udisk" 的目录，说明 dir 就是根目录
                    realUSBRootDirectory = dir.getAbsolutePath();
                }
            }
        }
        return realUSBRootDirectory;
    }

    public static boolean createPdf(Context context ,View view,String file_path,int pageNumber){
        if(android.os.Build.VERSION.SDK_INT>= android.os.Build.VERSION_CODES.KITKAT) {

            FileOutputStream outputStream =null;

            try {
                outputStream = new FileOutputStream(new File(file_path));

                PdfDocument document =new PdfDocument();

                PdfDocument.PageInfo pageInfo =new PdfDocument.PageInfo.Builder(

                        (int)view.getWidth(),(int)view.getHeight(), pageNumber).create();
                /*PrintAttributes.Builder builder = new PrintAttributes.Builder();
                PrintAttributes pdfAttributes = builder.setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .build();
                PrintedPdfDocument printedPdfDocument = new PrintedPdfDocument(context, pdfAttributes);
                PdfDocument.PageInfo pageInfo =new PdfDocument.PageInfo.Builder(
                        pdfAttributes.getMediaSize().getWidthMils(),
                        pdfAttributes.getMediaSize().getHeightMils(),
                        pageNumber).create();*/

                PdfDocument.Page page = document.startPage(pageInfo);

//此处是与官方的不同之处，官方的view.draw(page.getCanvas()) 无非也是在page返回的canvas对象上绘图，那么我们可以获取SurfaceView对象的Bitmap后再绘制到page的//canvas上即可          

                view.draw(page.getCanvas());

                document.finishPage(page);
                document.writeTo(outputStream);
                document.close();

                return true;
            }catch (Exception e){
                e.getMessage();
                return false;
            }
        }
        return false;
    }

    public static Bitmap generateBarcodeBitmap(String barcodeContent) {
        BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
        try {
            // 设置条形码格式和宽度/高度（例如：宽度为400，高度为200）
            Bitmap bitmap = barcodeEncoder.encodeBitmap(barcodeContent, BarcodeFormat.CODE_128, 400, 200);
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 加密
     */
    public static String encryptAEC(String sourceData)throws Exception {
        byte[] key = "1234567890abcdef".getBytes("UTF-8");
        SecretKey secretKey = new SecretKeySpec(key,"AES"); // 生成256位长度的密钥
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(sourceData.getBytes());
        return Base64.getEncoder().encodeToString(encrypted); // 将加密后的字节数组转换为Base64字符串以便于存储或传输
    }


    public static String decryptAEC(String encryptedData) throws Exception {
        byte[] key = "1234567890abcdef".getBytes("UTF-8");
        SecretKey secretKey = new SecretKeySpec(key,"AES"); // 生成256位长度的密钥
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE,secretKey);
        byte[] original = cipher.doFinal(Base64.getDecoder().decode(encryptedData)); // 将Base64字符串解码为字节数组后再解密
        return new String(original);
    }


   /* public static void printDocument(Context context,String filename) {
        // 准备要打印的位图列表
        List<Bitmap> pages = new ArrayList<>();

        // 添加页面（这里只是示例，你需要根据实际情况创建位图）
        try {
            // 从assets加载PDF并转换为位图
            PdfRenderer pdfRenderer = new PdfRenderer(getSeekableFileDescriptor(context,filename));
            for (int i = 0; i < pdfRenderer.getPageCount(); i++) {
                PdfRenderer.Page page = pdfRenderer.openPage(i);

                Bitmap bitmap = Bitmap.createBitmap(
                        page.getWidth(),
                        page.getHeight(),
                        Bitmap.Config.ARGB_8888);

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
                pages.add(bitmap);

                page.close();
            }
            pdfRenderer.close();

            // 创建打印助手并开始打印
            PdfPrintHelper printHelper = new PdfPrintHelper(context);
            printHelper.printPdf(pages, "打印文档.pdf");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 从assets获取PDF文件的FileDescriptor
    private static ParcelFileDescriptor getSeekableFileDescriptor(Context context, String assetName) throws IOException {
        File tempFile = new File(context.getCacheDir(), assetName);
        // 复制assets文件到缓存
        copyAssetToCache(context,assetName, tempFile);

        return ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private static void copyAssetToCache(Context context,String assetName, File outFile) throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = context.getAssets().open(assetName);
            out = new FileOutputStream(outFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }
*/

    /**
     * 设置系统时间
     * 优先使用 EnjoySDK（脉冲平板），失败后降级使用 AlarmManager
     *
     * @param time yyyy-MM-dd HH:mm:ss 格式的时间字符串
     */
    public static void setSystemTime(String time , Context context) {
        if (time == null || time.trim().isEmpty()) {
            Log.e("CommonUtil", "setSystemTime: time is empty");
            return;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(time);
            if (date == null) {
                Log.e("CommonUtil", "setSystemTime: parse failed, time=" + time);
                return;
            }

            // 优先尝试 EnjoySDK（脉冲平板专用）
            boolean enjoySuccess = false;
            try {
                EnjoySDK enjoySDK = new EnjoySDK(context);
                enjoySDK.switchAutoDateAndTime(false);

                int year = date.getYear() + 1900; // getYear() 返回的是从 1900 年开始的年份
                int month = date.getMonth();      // getMonth() 返回的是从 0 开始的月份
                int day = date.getDate();
                int retDate = enjoySDK.setDate(year, month, day);

                int hourOfDay = date.getHours();
                int minute = date.getMinutes();
                int second = date.getSeconds();
                int millisecond = 0; // Java Date 对象没有毫秒信息，设置为0
                int retTime = enjoySDK.setTime(hourOfDay, minute, second, millisecond);

                // 判断返回值：0 表示成功
                if (retDate == 0 && retTime == 0) {
                    enjoySuccess = true;
                    Log.i("CommonUtil", "setSystemTime: EnjoySDK成功设置时间 " + time);
                } else {
                    Log.w("CommonUtil", "setSystemTime: EnjoySDK失败 retDate=" + retDate + ", retTime=" + retTime + ", 尝试降级方案");
                }
            } catch (Exception e) {
                Log.w("CommonUtil", "setSystemTime: EnjoySDK异常，尝试降级方案", e);
            }

            // 降级方案：使用 AlarmManager（需要系统签名权限）
            if (!enjoySuccess) {
                try {
                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    if (alarmManager == null) {
                        Log.e("CommonUtil", "setSystemTime: AlarmManager unavailable");
                        return;
                    }
                    // 需要 android.permission.SET_TIME 权限（系统签名应用）
                    alarmManager.setTime(date.getTime());
                    Log.i("CommonUtil", "setSystemTime: AlarmManager成功设置时间 " + time);
                } catch (Exception e) {
                    Log.e("CommonUtil", "setSystemTime: AlarmManager失败", e);
                }
            }
        } catch (Exception e) {
            Log.e("CommonUtil", "setSystemTime: invalid time format, expect yyyy-MM-dd HH:mm:ss, time=" + time, e);
        }
    }

    /**
     * 重启本 App：通过 AlarmManager 延迟拉起 MainActivity，然后杀掉当前进程
     * 用于释放 slansdk 泄露的内存（进程重建后内存全部回收）
     *
     * @param delayMs 杀进程后多久拉起 MainActivity
     */
    public static void restartApp(Context context, long delayMs) {
        Context appContext = context.getApplicationContext();
        try {
            Intent intent = new Intent(appContext, com.gzlab.autoassemblyline_client.MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            int flags = PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            int requestCode = (int) (System.currentTimeMillis() & 0x7fffffff);
            PendingIntent restartIntent = PendingIntent.getActivity(appContext, requestCode, intent, flags);
            AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + delayMs, restartIntent);
                Log.i("CommonUtil", "restartApp: 已调度重启，" + delayMs + "ms 后启动 MainActivity");
            } else {
                Log.w("CommonUtil", "restartApp: AlarmManager 不可用，仅杀进程");
            }
        } catch (Exception e) {
            Log.e("CommonUtil", "restartApp: 调度重启失败", e);
        }
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }


}
