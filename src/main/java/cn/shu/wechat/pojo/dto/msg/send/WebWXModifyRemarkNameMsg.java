package cn.shu.wechat.pojo.dto.msg.send;

import cn.shu.wechat.enums.WXSendMsgCodeEnum;

/**
 * @作者 舒新胜
 * @项目 AutoWeChat
 * @创建时间 3/10/2021 2:49 PM
 * <p>
 * 文本消息
 */

public class WebWXModifyRemarkNameMsg  extends WebWXSendingMsg {

   public Byte CmdId;
   public String RemarkName;
   public String UserName;
}
