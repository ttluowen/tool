package com.yy.tool.resizelistimage;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.imageio.ImageIO;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import com.yy.log.Logger;
import com.yy.util.number.NumberUtil;
import com.yy.util.proterty.PropertyUtil;
import com.yy.util.string.StringUtil;
import com.yy.web.config.SystemConfig;


/**
 * 重新生成本地 l(list) 图片的尺寸。
 * 
 * @since 2018-04-01
 * @version 1.0
 * @author Luowen
 */
@SuppressWarnings("restriction")
public class Main {
	
	private String[] root;
	private int width;
	
	
	/**
	 * 构造函数及执行。
	 */
	public Main() {
		
		initConfig();
	}
	

	/**
	 * 初始化配置信息。
	 * 
	 * @return
	 * @throws IOException 
	 */
	private void initConfig() {
	
		// 获取当前系统运行环境的磁盘路径，并统一将 / 目录转换成 \ 方式。
		String path = System.getProperty("user.dir").replace("/", "\\");
		
	
		// 初始化系统目录。
		SystemConfig.setSystemPath(path);
	
		// 设置日志目录。
		Logger.setSystemPath(path);
		Logger.log("System path：" + path);
	
	
		// 读取配置文件内容。
		Properties properties = PropertyUtil.read(path + "\\config.properties");
		root = properties.getProperty("root").split(",");
		
		width = NumberUtil.parseInt(properties.get("width"));
	}
	
	
	/**
	 * 开始执行。
	 */
	private void start() {
		
		for (String item : root) {
			if (!StringUtil.isEmpty(item)) {
				try {
					resizeDir(new File(item));
				} catch (Exception e) {
					Logger.printStackTrace(e);
				}
			}
		}
	}
	
	
	/**
	 * 根据指定目录。
	 * 
	 * @param root
	 */
	private void resizeDir(File root) {
		
		if (!root.exists()) {
			Logger.log("[" + root.getAbsolutePath() + "]不存在");
			return;
		}
		

		Logger.log("文件夹[" + root.getAbsolutePath() + "]");
		
		
		File[] files = root.listFiles();
		for (File file : files) {
			if (file.isFile()) {
				Logger.log("[" + file.getAbsolutePath() + "]");
				
				
				String name = file.getName();
				int lastPointIndex = name.lastIndexOf(".");

				if (lastPointIndex != -1) {
					String filename = name.substring(0, lastPointIndex);

					if ("ldrm".indexOf(filename.charAt(filename.length() - 1)) == -1) {
						resize(file);
					}
				}
			} else {
				resizeDir(file);
			}
		}
	}


	/**
	 * 重置原始文件的指定文件。
	 * 
	 * @param file
	 */
	private void resize(File file) {
		
		String name = file.getName();
		int lastPointIndex = name.lastIndexOf(".");
		String filename = name.substring(0, lastPointIndex);
		String ext = name.substring(lastPointIndex + 1);

		boolean toResize = false;

		File listFile = new File(file.getParent(), filename + "l." + ext);
		if (listFile.exists()) {
			try {
				Image imageFile = ImageIO.read(listFile);
				int imageWidth = imageFile.getWidth(null);

				if (imageWidth < width) {
					toResize = true;
				}
			} catch (Exception e) {
				Logger.printStackTrace(e);
			}
		} else {
			toResize = true;
		}


		if (toResize) {
			Logger.log("重置文件[" + file.getAbsolutePath() + "]");
			
			
			generateImages(file, listFile, width);
		}
	}

	
	/**
	 * 创建图片缩略图(等比缩放)
	 * 
	 * @param sourceFile 源图片文件完整路径
	 * @param targetFile 目标图片文件完整路径
	 * @param width 缩放的宽度
	 */
	private void generateImages(File sourceFile, File targetFile, int width) {
		
		try {
			BufferedImage image = ImageIO.read(sourceFile);
	
			// 获得缩放的比例
			double ratio = 1.0;
			// 判断如果宽都不大于设定值，则不处理
			if (image.getWidth() > width) {
				ratio = (double) width / (double) image.getWidth();
			}

			// 计算新的图面宽度和高度
			int newWidth = (int) (image.getWidth() * ratio);
			int newHeight = (int) (image.getHeight() * ratio);

			BufferedImage bfImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
			bfImage.getGraphics().drawImage(image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
	
			FileOutputStream os = new FileOutputStream(targetFile);
			JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(os);
			encoder.encode(bfImage);
			os.close();
		} catch (Exception e) {
			Logger.printStackTrace(e);
		}
	}

	
	/**
	 * 执行入口。
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		new Main().start();;
	}
}
