package com.yy.tool.publisher;

/**
 * 执行的操作列表。
 * 
 * @since 2018-03-06
 * @version 1.0
 * @author Luowen
 */
public class Actions {

	/** 是否进行打包操作。 */
	private boolean pack;
	/** 是否进行版本比较操作。 */
	private boolean compare;
	/** 是否进行上传更新操作。 */
	private boolean upload;
	/** 是否进行发布操作。 */
	private boolean publish;

	public boolean packable() {
		return pack;
	}

	public void setPack(boolean pack) {
		this.pack = pack;
	}

	public boolean compareable() {
		return compare;
	}

	public void setCompare(boolean compare) {
		this.compare = compare;
	}

	public boolean uploadable() {
		return upload;
	}

	public void setUpload(boolean upload) {
		this.upload = upload;
	}

	public boolean publishable() {
		return publish;
	}

	public void setPublish(boolean publish) {
		this.publish = publish;
	}
	
	
	public String toString() {
		
		return "pack[" + packable() + "], compare[" + compareable() + "], upload[" + uploadable() + "], publish[" + publishable() + "]";
	}
}
