package com.yy.tool.publisher.action;

import com.rt.log.Logger;
import com.rt.statuscode.Statuscode;
import com.rt.statuscode.StatuscodeMap;
import com.rt.util.http.HttpUtil;
import com.yy.tool.publisher.Common;

/**
 * 通知中心服务器发布当前版本。
 * 
 * @since 2018-03-06
 * @version 1.0
 * @author Luowen
 */
public class Publisher implements ActionInterface{

	@Override
	public void todo() {
		
		Logger.log("通知服务器版本发布");
		
		
		boolean success =
			updateVersion()
			&& notification()
		;


		Logger.log("通知服务器版本发布结束，发布" + (success ? "成功" : "失败"));
	}
	
	
	/**
	 * 更新服务器的最新版本号。
	 * 
	 * @return
	 */
	private boolean updateVersion() {
		
		try {
			String pack = Common.getPack();
			String api;

			if (pack.equals("big")) {
				api = "updateNextBigVersion";
			} else if (pack.equals("small")) {
				api = "updateNextSmallVersion";
			} else {
				api = "updateNextPatch";
			}


			StatuscodeMap sm = StatuscodeMap.parse(HttpUtil.get(Common.DOMAIN + "api/base/version/" + api));

			if (sm.getCode() == Statuscode.SUCCESS) {
				Logger.log("更新版本号成功");
				return true;
			} else {
				Logger.log("更新版本号失败，" + sm.getDescription());
				return false;
			}
		} catch (Exception e) {
			Logger.printStackTrace(e);
			return false;
		}
	}
	
	
	/**
	 * 通知服务器发布版本。
	 * 
	 * @return
	 */
	private boolean notification() {
		
		try {
			StatuscodeMap sm = StatuscodeMap.parse(HttpUtil.get(Common.DOMAIN + "api/base/version/notification"));

			if (sm.getCode() == Statuscode.SUCCESS) {
				Logger.log("通知服务器发布版本成功");
				return true;
			} else {
				Logger.log("通知服务器发布版本失败，" + sm.getDescription());
				return false;
			}
		} catch (Exception e) {
			Logger.printStackTrace(e);
			return false;
		}
	}
}
