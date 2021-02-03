package shu.cn.weichat.beans;

import java.io.Serializable;

/**
 * AppInfo
 * 
 * @author SXS
 * @date 创建时间：2017年7月3日 下午10:38:14
 * @version 1.1
 *
 */
public class AppInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int type;
	private String appId;

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

}
