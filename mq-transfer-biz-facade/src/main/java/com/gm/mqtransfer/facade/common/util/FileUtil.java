package com.gm.mqtransfer.facade.common.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public final class FileUtil {
	/**
	 * 读取文件内容为字符串
	 * @param filename String
	 * @return String
	 */
	public static String read(String filename,String charset) throws Exception {
		BufferedReader br = null;
		InputStream is = null;
		StringBuffer context = new StringBuffer();
		String tempStr;
		int i = 0;
		try {
			is =  ClassLoader.getSystemResourceAsStream(filename);
			if(is == null){
				is = new FileInputStream(filename);
			}
			br = new BufferedReader(new InputStreamReader(is,charset));
			while((tempStr = br.readLine()) != null) {
				if(i == 0) {
					i = 1;
				} else {
					context.append("\n");
				}

				context.append(tempStr);
			}
		} catch(FileNotFoundException ex) {
			throw new Exception(ex.getMessage());
		} catch(IOException ex) {
			throw new Exception(ex.getMessage());
		} finally {
			if(br != null){
				br.close();
			}
		}
		return context.toString();
	}
	
	public static String read(InputStream is, String charset) throws IOException
    {
        BufferedReader br = null;
        StringBuffer context = new StringBuffer();
        String tempStr;
        int i = 0;
        try {
            br = new BufferedReader(new InputStreamReader(is,charset));
            while((tempStr = br.readLine()) != null) {
                if(i == 0) {
                    i = 1;
                } else {
                    context.append("\n");
                }

                context.append(tempStr);
            }
        } finally {
            if(br != null){
                br.close();
            }
        }
        return context.toString();
    }
	public static InputStream getReadInputStream(String filename) throws Exception {
		InputStream is = ClassLoader.getSystemResourceAsStream(filename);
		if(is == null){
			is = new FileInputStream(filename);
		}
		return is;
	}
	
	/**
	 * 将文本写入到文件中
	 * @param filename String 文件名
	 * @param filecontext String 文件内容
	 * @param append boolean 是否将文件内容追加到文件尾部
	 * @param mkdir boolean 是否创建文件
	 * @throws IOException
	 */
	public static void write(String fileName, String context, boolean append,boolean mkdir,String charset) throws Exception {
		PrintWriter out = null;
		File file = new File(fileName);

		//如果文件目录不存在，是否生成文件目录
		try {
			File path = file.getParentFile(); //得到文件目录
			if(!path.exists()){
				if(mkdir) { //文件目录不存在，要创建目录
					path.mkdirs();
				}else{
					throw new Exception("文件目录不存在！");
				}
			}
			out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName,append),charset));
			out.write(context);
		} catch(IOException ex) {
			throw new Exception(ex.getMessage());
		} finally {
			if(out != null) {
				out.close();
			}
		}
	}
	
	public static void removeFile(File path)
	{
		if (!path.exists())
		{
			return;
		}
		if (path.isDirectory())
		{
			File[] child = path.listFiles();
			if ((child != null) && (child.length != 0))
			{
				for (int i = 0; i < child.length; i++)
				{
					removeFile(child[i]);
					child[i].delete();
				}
			}
		}
		path.delete();
	}
	
	/**
	 * 获取文件扩展名
	 * 
	 * @param filename
	 * @return
	 */
	public static String getExtend(String filename) {
		return getExtend(filename, "");
	}

	/**
	 * 获取文件扩展名
	 * 
	 * @param filename
	 * @return
	 */
	public static String getExtend(String filename, String defExt) {
		if ((filename != null) && (filename.length() > 0)) {
			int i = filename.lastIndexOf('.');

			if ((i > 0) && (i < (filename.length() - 1))) {
				return (filename.substring(i+1)).toLowerCase();
			}
		}
		return defExt.toLowerCase();
	}

	/**
	 * 获取文件名称[不含后缀名]
	 * 
	 * @param
	 * @return String
	 */
	public static String getFilePrefix(String fileName) {
		int splitIndex = fileName.lastIndexOf(".");
		return fileName.substring(0, splitIndex).replaceAll("\\s*", "");
	}
	
	/**
	 * 获取文件名称[不含后缀名]
	 * 不去掉文件目录的空格
	 * @param
	 * @return String
	 */
	public static String getFilePrefix2(String fileName) {
		int splitIndex = fileName.lastIndexOf(".");
		return fileName.substring(0, splitIndex);
	}
	/**
	 * 获取文件名称【包含后缀名、去掉文件目录】
	 * @param filePath
	 * @return
	 */
	public static String getFileName(String filePath)
	{
		if(filePath == null || filePath.trim().length() == 0)
		{
			return "";
		}
		filePath = filePath.trim().replaceAll("\\\\", "/");
		if(filePath.endsWith("/"))
		{
			filePath = filePath.substring(0, filePath.length() - 1);
		}
		int index = filePath.lastIndexOf("/");
		if(index != -1)
		{
			if(index == filePath.length())
			{
				return "";
			}
			return filePath.substring(index + 1);
		}
		else
		{
			return filePath;
		}
	}
	/**
	 * 获取文件名称【不包含后缀名、去掉文件目录】
	 * @param filePath
	 * @return
	 */
	public static String getFileNameWithoutExt(String filePath)
	{
		String fileName = getFileName(filePath);
		int splitIndex = fileName.lastIndexOf(".");
		if(splitIndex != -1)
		{
			return fileName.substring(0, splitIndex).trim();
		}
		else
		{
			return fileName;
		}
	}
	
	/**
	 * 文件复制
	 *方法摘要：这里一句话描述方法的用途
	 *@param
	 *@return void
	 */
	public static void copyFile(String inputFile,String outputFile) throws FileNotFoundException{
		File sFile = new File(inputFile);
		File tFile = new File(outputFile);
		if(inputFile.indexOf("*") != -1)//多文件匹配拷贝
		{
			String nameRegex = sFile.getName();
			final String regex = nameRegex.replaceAll("\\.", "\\\\.").replaceAll("\\*", "\\.\\*");
			File[] files = sFile.getParentFile().listFiles(new FilenameFilter(){
				@Override
				public boolean accept(File dir, String name)
				{
					Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE ); 
					Matcher matcher = pattern.matcher(name);
					return matcher.find();
				}});
			for(File file : files)
			{
				String newInputFile = file.getPath();
				String newOutputFile = tFile.getParentFile().getPath() + "/" + file.getName();
				copyFile(newInputFile, newOutputFile);
			}
			return;
		}
		else//单文件拷贝
		{
			if(!sFile.exists())
			{
				throw new FileNotFoundException("文件不存在-->" + inputFile);
			}
		}
		if(!tFile.exists())
		{
			if(tFile.isDirectory())
			{
				tFile.mkdirs();
				return;
			}
			else
			{
				tFile.getParentFile().mkdirs();
			}
		}
		FileInputStream fis = new FileInputStream(sFile);
		FileOutputStream fos = new FileOutputStream(tFile);
		int temp = 0;  
		byte[] buf = new byte[10240];
        try {  
        	while((temp = fis.read(buf))!=-1){   
        		fos.write(buf, 0, temp);   
            }   
        	fos.flush();
        } catch (IOException e) {  
        	throw new FileNotFoundException("文件存储异常-->" + inputFile);
        } finally{
            try {
            	fis.close();
            	fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        } 
	}
	public static void copyFile(InputStream is, String outputFile) throws Exception{
        OutputStream out = null; 
        try
        {
            out = new FileOutputStream(outputFile);
            byte[] chunk = new byte[1024];  
            int count;  
            while ((count = is.read(chunk)) >= 0) {  
                out.write(chunk, 0, count);  
            }
        }
        finally
        {
            if(out != null)
            {
                try
                {
                    out.close();
                }
                catch(Exception e){}
            }
            if(is != null)
            {
                try
                {
                    is.close();
                }
                catch(Exception e){}
            }
        }
    }
	/**
	 * 判断文件是否为图片<br>
	 * <br>
	 * 
	 * @param filename
	 *            文件名<br>
	 *            判断具体文件类型<br>
	 * @return 检查后的结果<br>
	 * @throws Exception
	 */
	public static boolean isPicture(String filename) {
		// 文件名称为空的场合
		if (StringUtils.isBlank(filename)) {
			// 返回不和合法
			return false;
		}
		// 获得文件后缀名
		//String tmpName = getExtend(filename);
		String tmpName = filename;
		// 声明图片后缀名数组
		String imgeArray[][] = { { "bmp", "0" }, { "dib", "1" },
				{ "gif", "2" }, { "jfif", "3" }, { "jpe", "4" },
				{ "jpeg", "5" }, { "jpg", "6" }, { "png", "7" },
				{ "tif", "8" }, { "tiff", "9" }, { "ico", "10" } };
		// 遍历名称数组
		for (int i = 0; i < imgeArray.length; i++) {
			// 判断单个类型文件的场合
			if (imgeArray[i][0].equals(tmpName.toLowerCase())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 判断文件是否为DWG<br>
	 * <br>
	 * 
	 * @param filename
	 *            文件名<br>
	 *            判断具体文件类型<br>
	 * @return 检查后的结果<br>
	 * @throws Exception
	 */
	public static boolean isDwg(String filename) {
		// 文件名称为空的场合
		if (StringUtils.isEmpty(filename)) {
			// 返回不和合法
			return false;
		}
		// 获得文件后缀名
		String tmpName = getExtend(filename);
		// 声明图片后缀名数组
		if (tmpName.equals("dwg")) {
			return true;
		}
		return false;
	}
	
	/**
	 * 删除指定的文件
	 * 
	 * @param strFileName
	 *            指定绝对路径的文件名
	 * @return 如果删除成功true否则false
	 */
	public static boolean delete(String strFileName) {
		File fileDelete = new File(strFileName);

		if (!fileDelete.exists() || !fileDelete.isFile()) {
			//org.jeecgframework.core.util.LogUtil.info("错误: " + strFileName + "不存在!");
			return false;
		}

		//org.jeecgframework.core.util.LogUtil.info("--------成功删除文件---------"+strFileName);
		return fileDelete.delete();
	}
	/**
	 * 删除文件夹
	 * @param fileDelete
	 * @return
	 */
	public static boolean deleteAll(File fileDelete) 
	{
		if(!fileDelete.exists())
		{
			return false;
		}
		if(fileDelete.isFile())
		{
			return fileDelete.delete();
		}
		else
		{
			File[] files = fileDelete.listFiles();
			for(File file : files)
			{
				deleteAll(file);
			}
			return fileDelete.delete();
		}
	}
	
	/**
	 * 获取文件摘要
	 * @param strFileName
	 * @return
	 */
	public static byte[] getFileMd5Digest(String strFileName) throws Exception{
		FileInputStream fis = null;
		byte[] digest = null;
		try{
			MessageDigest md = MessageDigest.getInstance("MD5");
			fis = new FileInputStream(strFileName); 
			byte[] buffer = new byte[2048]; 
			int length = -1;  
			while ((length = fis.read(buffer)) != -1) {  
			    md.update(buffer, 0, length);  
			}  
			digest = md.digest();
			System.out.println("md5摘要长度:" + digest.length);
		}finally{
			if(fis != null){
				try{
					fis.close();
				}catch(Exception e){
					
				}
			}
		}
		return digest;
	}
	/**
	 * 写文件
	 * 
	 * @param fileName
	 *            完整文件名(类似：/usr/a/b/c/d.txt)
	 * @param contentBytes
	 *            文件内容的字节数组
	 * @param autoCreateDir
	 *            目录不存在时，是否自动创建(多级)目录
	 * @param autoOverWrite
	 *            目标文件存在时，是否自动覆盖
	 * @return
	 * @throws IOException
	 */
	public static boolean write(String fileName, byte[] contentBytes,
			boolean autoCreateDir, boolean autoOverwrite) throws IOException {
		boolean result = false;
		if (autoCreateDir) {
			createDirs(fileName);
		}
		if (autoOverwrite) {
			delete(fileName);
		}
		File f = new File(fileName);
		FileOutputStream fs = new FileOutputStream(f);
		fs.write(contentBytes);
		fs.flush();
		fs.close();
		result = true;
		return result;
	}
	/**
	 * 创建(多级)目录
	 * 
	 * @param filePath
	 *            完整的文件名(类似：/usr/a/b/c/d.xml)
	 */
	public static void createDirs(String filePath) {
		File file = new File(filePath);
		File parent = file.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}

	}
	/**
	 * 下载远程文件到本地
	 * @param url
	 * @param fileSavePath
	 * @return
	 */
	public static boolean downloadFile(String url, String fileSavePath)
    {
        InputStream is = null;
        OutputStream os = null;
        try
        {
            File file = new File(fileSavePath);
            if(file.exists())
            {
                return false;
            }
            URL fileUrl = new URL(url);
            URLConnection conn = fileUrl.openConnection();
            is = conn.getInputStream();
            os = new FileOutputStream(fileSavePath);
            byte[] bys = new byte[1024];
            int len = 0;
            while((len = is.read(bys)) > 0)
            {
                os.write(bys, 0, len);
            }
            os.flush();
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
        finally
        {
            if(os != null)
            {
                try
                {
                    os.close();
                }catch(Exception e){}
            }
            if(is != null)
            {
                try
                {
                    is.close();
                }catch(Exception e){}
            }
        }
    }
	/**
	 * 创建目录
	 * @param directory
	 */
	public static boolean mkdirs(String directory) {
	    File file = new File(directory);
	    if (file.exists()) {
	        return true;
	    }
	    return file.mkdirs();
	}
	/**
	 * 获取文件大小
	 * @param filePath
	 * @return
	 */
	public static long getFileSize(String filePath) {
	    File file = new File(filePath);
        if (file.exists()) {
            return file.length();
        }
        return 0;
	}
	
}
