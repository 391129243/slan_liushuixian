package com.gzlab.autoassemblyline_client.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.Log;

import androidx.core.content.FileProvider;


import com.gzlab.autoassemblyline_client.R;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.jahnen.libaums.core.fs.UsbFile;

public class FileUtils {

	private static final String TAG = "FileUtils";
	public static final String PCR_MODULE_PATH =  Environment.getExternalStorageDirectory().getAbsolutePath()
			+ "/PCR_CONTROL";
	public static final String LOG_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
			+ "/PCR_CONTROL/Log/";
	public static final String UPDATE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
			+ "/PCR_CONTROL/Update/";
	public static final String RESULT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
			+ "/PCR_CONTROL/result/";
	public static final String RESULT_EXPORT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
			+ "/PCR_CONTROL/result_export/";



	/**
	 * 创建的配置的核酸文件夹目录
	 */

	public static final String USB_PATH_LU = "/storage/usbdisk/";
	
	public static final String EXCEL_SUFFIX = ".xls";


	public static final String FULL_PROCESS_TEMPLATE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
			+ "/PCR_CONTROL/Template/fullProcess";
	public static final String SINGLE_PCR_TEMPLATE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
			+ "/PCR_CONTROL/Template/singlePCR";
	public static final String SINGLE_RNA_TEMPLATE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
			+ "/PCR_CONTROL/Template/singleRNA";

	public static String COMMON_HEX_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
			+ "/PCR_CONTROL/commonHex_v5/";




    public static final String INFO_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/PCR_CONTROL/info/";
	public static final String JSON_SUFFIX = ".json";
	public static final String TXT_SUFFIX = ".txt";


	
	public static boolean fileIsExists(String path){
        try{
            File f=new File(path);
            if(!f.exists()){
                return false;
            }
         
        }catch (Exception e) {
            // TODO: handle exception
            return false;
        }
        return true;
    }

	/**
	 * 根据文件路径删除文件
	 */
	public static boolean deleteByPath(String filePath){
		File file = new File(filePath);
		if(file.exists()){
			if(file.isFile()){
				return file.delete();
			}
		}
		return false;
	}

	/**
	 * 在U盘上创建目录
	 */
	public static File createUdiskDir(String usb_path,String dirName){
		File dir = new File(usb_path + dirName);
		if (!dir.exists()|| !dir.isDirectory()) {  
			dir.mkdir();
        }
		return dir;
	}
	
	public static File createUdiskFile(String fileName){
		 File file = new File(USB_PATH_LU + fileName);
	     try {
			file.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return file;
	}

	public static boolean createDir(String dirName){
		File dir = new File(dirName);
		if (!dir.exists()|| !dir.isDirectory()) {
			return dir.mkdir();
		}
		return false;
	}

	/**
	 * 删除文件目录以及里面的文件
	 * @param deleteFile
	 * @return
	 */
	public static void remove(File deleteFile) {
		File[] files = deleteFile.listFiles();//将file子目录及子文件放进文件数组
		if (files != null) {//如果包含文件进行删除操作
			for (int i = 0; i < files.length; i++) {
				if (files[i].isFile()) {//删除子文件
					files[i].delete();
				} else if (files[i].isDirectory()) {//通过递归方法删除子目录的文件
					remove(files[i]);
				}
				files[i].delete();//删除子目录
			}
		}
		deleteFile.delete();
	}




	public static File createDir(File dirName, String subDirName){
		File dir = new File(dirName , subDirName);		
		if (!dir.exists()|| !dir.isDirectory()) {  
			dir.mkdir();
        }
		return dir;
	}

	public static boolean iscreateDir(File dirName, String subDirName){
		boolean result = false;
		File dir = new File(dirName , subDirName);
		if (!dir.exists()|| !dir.isDirectory()) {
			result = dir.mkdir();
		}
		return result;
	}
	
	/**
	 * 检查外部存储器是否可写
	 * @author xuxiaoshan
	 * @version 1.5.11
	 */
	public static boolean isExternalStorageWritable(){
		String state = Environment.getExternalStorageState();
		if(Environment.MEDIA_MOUNTED.equals(state)){
			return true;
		}
		return false;		
	}
	
	/**
	 * 只读检查
	 */
	public boolean isExternalStorageReadable(){
		String state = Environment.getExternalStorageState();
		if(Environment.MEDIA_MOUNTED.equals(state)||
			Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)){
			return true;
			
		}
		return false;
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
					Log.i("FileUtils","找到U盘路径:%s\n"+p2);
					return p2;
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	/**
	 * 读取(raw/deviceinfo)文件
	 * @return
	 */
/*	public static String getFromRaw(){
		InputStreamReader inputReader = null;
		BufferedReader bufReader = null;
		try {
			inputReader = new InputStreamReader(PcrApplication.getAppContext().getResources()
					.openRawResource(R.raw.deviceinfo));
			bufReader = new BufferedReader(inputReader);
			String line = "";
			String Result = "";
			while ((line = bufReader.readLine()) != null)
				Result += line;
			return Result;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (bufReader != null) {
					bufReader.close();
				}
				if (inputReader != null){
					inputReader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e){
				e.printStackTrace();
			}
		}
		return null;
	}*/

	public static boolean copyFile(String oldPathName, String newPathName) {
		try {
			File oldFile = new File(oldPathName);
			if (!oldFile.exists()) {
				Log.e("copyFile", "copyFile:  oldFile not exist.");
				return false;
			} else if (!oldFile.isFile()) {
				Log.e("copyFile", "copyFile:  oldFile not file.");
				return false;
			} else if (!oldFile.canRead()) {
				Log.e("copyFile", "copyFile:  oldFile cannot read.");
				return false;
			}
			FileInputStream fileInputStream = new FileInputStream(oldPathName);
			FileOutputStream fileOutputStream = new FileOutputStream(newPathName);
			byte[] buffer = new byte[1024];
			int byteRead;
			while (-1 != (byteRead = fileInputStream.read(buffer))) {
				fileOutputStream.write(buffer, 0, byteRead);
			}
			fileInputStream.close();
			fileOutputStream.flush();
			fileOutputStream.close();
			return true;
		} catch (Exception e) {
			Log.e("copyFile", e.getMessage());
			return false;
		}
	}


	public static void saveHexToResultDir(String rootTempName, String subFirName){
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(1000);

					File dir = new File(FileUtils.RESULT_PATH + subFirName);

					if(!dir.exists()){
						dir.mkdirs();
					}

					String templatePath_FullProcess = FileUtils.FULL_PROCESS_TEMPLATE_PATH + File.separator +  rootTempName;

					File file = new File(templatePath_FullProcess);

					String savePath = FileUtils.RESULT_PATH + subFirName + File.separator;

					if(file.isDirectory()){
						File[] files = file.listFiles();
						for (File item: files) {
							if(item.getName().toLowerCase().contains("first.hex")){

								File newFile = new File(savePath + item.getName());

								FileUtils.copyFile(item,newFile);
							}
						}
					}
				} catch (Exception e) {
					Log.e(TAG, "saveHexToResultDir:" + e.getMessage());
				}
			}
		}).start();
	}


	public static boolean copyFile(File oldFile, File newPathName) {
		try {
			if(!newPathName.exists()){
				newPathName.createNewFile();
			}
			if (!oldFile.exists()) {
				Log.e("copyFile", "copyFile:  oldFile not exist.");
				return false;
			} else if (!oldFile.isFile()) {
				Log.e("copyFile", "copyFile:  oldFile not file.");
				return false;
			} else if (!oldFile.canRead()) {
				Log.e("copyFile", "copyFile:  oldFile cannot read.");
				return false;
			}
			FileInputStream fileInputStream = new FileInputStream(oldFile);
					//new FileInputStream(oldPathName);
			FileOutputStream fileOutputStream = new FileOutputStream(newPathName.getAbsolutePath());
			byte[] buffer = new byte[1024];
			int byteRead;
			while (0 < (byteRead = fileInputStream.read(buffer))) {
				fileOutputStream.write(buffer, 0, byteRead);
			}
			fileInputStream.close();
			fileOutputStream.flush();
			fileOutputStream.close();
			return true;
		} catch (Exception e) {
			Log.e("copyFile", e.getMessage());
			return false;
		}
	}


	/**
	 * 读取文件
	 * @param filePath
	 * @return
	 */
	public static String getStringFromSDFile(String filePath) {
		InputStreamReader inputReader = null;
		BufferedReader bufReader = null;
		try {
			File file = new File(filePath);
			if (!file.exists()) {
				return null;
			} else if (!file.isFile()) {
				return null;
			} else if (!file.canRead()) {
				return null;
			}
			inputReader = new InputStreamReader(new FileInputStream(file));
			bufReader = new BufferedReader(inputReader);
			String line = "";
			String Result = "";
			while ((line = bufReader.readLine()) != null)
				Result += line;
			return Result;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (bufReader != null) {
					bufReader.close();
					bufReader = null;
				}
				if (inputReader != null) {
					inputReader.close();
					inputReader = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		return null;

	}

	public static boolean dumpDeviceInfo(String initInfo,String name){
		if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			return false;
		}
		File dir = new File(INFO_PATH);
		if(!dir.exists()){
			dir.mkdirs();
		}else{
			File[] files = dir.listFiles();
			if(null != files && files.length>0){
				for(File file :files){
					file.delete();
				}
			}
		}
		try{
			File file = new File (INFO_PATH + name+ TXT_SUFFIX);
			if(!file.exists()){
				file.createNewFile();
			}
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(initInfo.getBytes());
			fos.close();
			return true;

		}catch (Exception e){
			return  false;
		}

	}

	public static File dumpDeviceInfo(String initInfo,String fileName,File dir){
		if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			return null;
		}
		if(!dir.exists()){
			dir.mkdirs();
		}else{
			File[] files = dir.listFiles();
			if(null != files && files.length>0){
				for(File file :files){
					if(file.getName().equals(fileName)){
						file.delete();
					}
				}
			}
		}
		try{
			File file = new File(dir,fileName);

			if(!file.exists()){
				file.createNewFile();
			}
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(initInfo.getBytes());
			fos.close();
			return file;

		}catch (Exception e){
			return  null;
		}

	}

	public static boolean saveFileInSdcard(String s,String dirPath,String fileName,String suffix){
		if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			return false;
		}
		File dir = new File(dirPath);
		if(!dir.exists()){
			dir.mkdirs();
		}
		try{
			File file = new File (dirPath + fileName + suffix);
			if(!file.exists()){
				file.createNewFile();
			}
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(s.getBytes());
			fos.close();
			return true;

		}catch (Exception e){
			return false;
		}

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
	 * 找到单重判别模板
	 * ".ct"
	 * @param localList
	 * @return
	 */
	public static File getFile(List<File> localList,String end_suffex){
		if(null != localList && localList.size()>0){
			for(File file : localList){
				if(file.getName().toLowerCase().endsWith(end_suffex)){
					return file;
				}
			}
		}
		return null;
	}

	/**
	 * 复制assert文件到制定的目录
	 * @param context
	 * @param assetsPath
	 * @param storagePath
	 */
	private void copyFileFromAsserts(Context context, String assetsPath, String storagePath){
		String temp = "";
		if(StringUtil.isEmpty(storagePath)){
			return;
		}else if(storagePath.equals(File.separator)){
			storagePath = storagePath.substring(0, storagePath.length() - 1);
		}

		AssetManager assetManager = context.getAssets();
		try {

			File file = new File(storagePath);
			if (!file.exists()) {
				file.mkdirs();
			}
			//获取assert下所有文件夹以及目录名称
			String[] fileNames = assetManager.list(assetsPath);
			if (fileNames.length > 0) {
				//如果是目录 apk
				for (String fileName : fileNames){
					if (!StringUtil.isEmpty(assetsPath)){
						temp = assetsPath + File.separator + fileName;//补全assets资源路径


						String[] childFileNames = assetManager.list(temp);
						if(!StringUtil.isEmpty(temp) && childFileNames.length ==0){
							InputStream inputStream = assetManager.open(temp);
							readInputStream(storagePath + File.separator + fileName, inputStream);
						}
					}

				}


			}

		}catch (IOException e){
			e.printStackTrace();
		}

	}

	/**
	 * 读取输入流中的数据写入输出流
	 *
	 * @param storagePath 目标文件路径
	 * @param inputStream 输入流
	 */
	public static void readInputStream(String storagePath, InputStream inputStream){
		File file = new File(storagePath);
		try{
			if (!file.exists()){
				FileOutputStream fos = new FileOutputStream(file);
				// 2.定义存储空间
				byte[] buffer = new byte[inputStream.available()];
				// 3.开始读文件
				int lenght = 0;
				while ((lenght = inputStream.read(buffer)) != -1) // 循环从输入流读取buffer字节
					// 将Buffer中的数据写到outputStream对象中
					fos.write(buffer, 0, lenght);

				fos.flush();// 刷新缓冲区
				// 4.关闭流
				fos.close();
				inputStream.close();
			}
				// 1.建立通道对象

		} catch (FileNotFoundException e){
			e.printStackTrace();
		} catch (IOException e){
			e.printStackTrace();
		}

	}

	public static String fileToStr(InputStream inputStream) throws IOException {
		ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
		// 数组长度
		byte[] buffer = new byte[1024];
		// 初始长度
		int len = 0;
		// 循环
		while ((len = inputStream.read(buffer)) != -1) {
			arrayOutputStream.write(buffer, 0, len);
//            return arrayOutputStream.toString();
		}
		//Log.i(TAG, arrayOutputStream.toString());
		return arrayOutputStream.toString();
	}

	public static boolean deleteDirectory(UsbFile dirFile){
		if(dirFile != null){
			if(dirFile.isDirectory()){
				//文件夹
				boolean flag = true;
				try{
					// 删除文件夹中的所有文件包括子目录
					UsbFile[] files = dirFile.listFiles();
					for (int i = 0; i < files.length; i++) {
						// 删除子目录
						if (files[i].isDirectory()) {
							flag = deleteDirectory(files[i]);
							if (!flag)
								break;
						}else{
							//子文件
							files[i].delete();
							if (!flag)
								break;
						}
					}
				}catch (Exception e){
					return false;
				}
				try{
					dirFile.delete();
					return true;
				}catch (Exception e){
					return false;
				}
			}else{
				try{
					dirFile.delete();
					return true;
				}catch (Exception e){
					return false;
				}
			}
		}else {
			return false;
		}
	}

	public static boolean removeAll(File dirFile){
		// 如果dir对应的文件不存在，或者不是一个目录，则退出
		if ((!dirFile.exists()) || (!dirFile.isDirectory())) {
			Log.e("FileUtils","删除目录失败：" + dirFile.isFile() + "不存在！");
			return false;
		}
		boolean flag = true;
		File[] files = dirFile.listFiles();
		if(null != files){
			for(int i=0;i<files.length;i++){
				if(files[i].isFile()){
					 flag = files[i].delete();
					 if(!flag){
						 break;
					 }
					//删除文件
				}else if(files[i].isDirectory()){
					//文件夹
					Log.e("FileUtils","目录：" +files[i].getAbsolutePath());
					flag = removeAll(files[i]);
					if(!flag){
						break;
					}
				}
			}
		}
		return flag;
	}


	/*
	 *删除文件夹
	 */
	public static boolean deleteDirectory(String dir) {
		// 如果dir不以文件分隔符结尾，自动添加文件分隔符
		if (!dir.endsWith(File.separator))
			dir = dir + File.separator;
		File dirFile = new File(dir);
		// 如果dir对应的文件不存在，或者不是一个目录，则退出
		if ((!dirFile.exists()) || (!dirFile.isDirectory())) {
			Log.e("FileUtils","删除目录失败：" + dir + "不存在！");
			return false;
		}
		boolean flag = true;
		// 删除文件夹中的所有文件包括子目录
		File[] files = dirFile.listFiles();
		for (int i = 0; i < files.length; i++) {
			// 删除子文件
			if (files[i].isFile()) {
				flag = files[i].delete();
				if (!flag)
					break;
			}
			// 删除子目录
			else if (files[i].isDirectory()) {
				flag = deleteDirectory(files[i]
						.getAbsolutePath());
				if (!flag)
					break;
			}
		}
		if (!flag) {
			Log.e("FileUtils","删除目录失败！");
			return false;
		}
		// 删除当前目录
		if (dirFile.delete()) {
			Log.e("FileUtils","删除目录" + dir + "成功！");
			return true;
		} else {
			return false;
		}
	}

	public static boolean deleteDirByFile(File dirFile) {
		// 如果dir不以文件分隔符结尾，自动添加文件分隔符
		// 如果dir对应的文件不存在，或者不是一个目录，则退出
		if ((!dirFile.exists()) || (!dirFile.isDirectory())) {
			Log.e("FileUtils","删除目录失败：" + dirFile + "不存在！");
			return false;
		}
		boolean flag = true;
		// 删除文件夹中的所有文件包括子目录
		File[] files = dirFile.listFiles();
		for (int i = 0; i < files.length; i++) {
			// 删除子文件
			if (files[i].isFile()) {
				flag = files[i].delete();
				if (!flag)
					break;
			}
			// 删除子目录
			else if (files[i].isDirectory()) {
				flag = deleteDirectory(files[i]
						.getAbsolutePath());
				if (!flag)
					break;
			}
		}
		if (!flag) {
			Log.e("FileUtils","删除目录失败！");
			return false;
		}
		// 删除当前目录
		if (dirFile.delete()) {
			Log.e("FileUtils","删除目录" + dirFile + "成功！");
			return true;
		} else {
			return false;
		}
	}




	public static File writeAssetsToFile(Context context, String assetPath) throws IOException {
		String fileName = assetPath.substring(assetPath.lastIndexOf("/") + 1);
		File resultFile = checkCacheSize(context, assetPath);
		if (!resultFile.exists()) {
			boolean createResult = resultFile.createNewFile();
			if (!createResult) {
				return null;
			}
		}
		InputStream is = context.getAssets().open(fileName);
		FileOutputStream fos = null;
		try {
			byte[] data = new byte[2048];
			int readBuffer;
			fos = new FileOutputStream(resultFile);
			while ((readBuffer = is.read(data)) > -1) {
				fos.write(data, 0, readBuffer);
			}
		} catch (Exception ex) {
			Log.e("Exception ","Exception:" + ex);
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
		return resultFile;
	}


	private static File checkCacheSize(Context context, String url) {
		String folderPath = context.getFilesDir().getAbsolutePath() + "/pdf/";
		File folder = new File(folderPath);
		if (!folder.exists()) {
			folder.mkdir();
		}
		File[] cacheList = folder.listFiles();
		if (cacheList != null && cacheList.length >= 10) {
			for (File childFile : cacheList) {
				childFile.delete();
			}
		}
		return new File(folderPath, url.substring(url.lastIndexOf("/") + 1));
	}

	public static List<File> getFileList(String path){
		List<File> configFiles = new ArrayList<>();
		File[] files = new File(path).listFiles();//文件夹下的所有文件
		if(files != null && files.length>0) {
			for (int i = 0; i < files.length; i++) {
				if (null != configFiles) {
					configFiles.add(files[i]);
					Collections.sort(configFiles);
				}
			}
		}
		return configFiles;
	}

	public static void copyFileByFileChannel(File source, File dest) throws IOException {
		try (FileInputStream inStream = new FileInputStream(source);
			 FileOutputStream outStream = new FileOutputStream(dest);
			 FileChannel inChannel = inStream.getChannel();
			 FileChannel outChannel = outStream.getChannel()) {

			inChannel.transferTo(0, inChannel.size(), outChannel);
		}
	}
}
