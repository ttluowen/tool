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
		
		try {
			StatuscodeMap sm = StatuscodeMap.parse(HttpUtil.get(Common.DOMAIN + "api/base/version/notification"));

			if (sm.getCode() == Statuscode.SUCCESS) {
				Logger.log("通知服务器发布版本成功");
			} else {
				System.out.println("通知服务器发布版本失败，" + sm.getDescription());
			}
		} catch (Exception e) {
			Logger.printStackTrace(e);
		}
	}
}
