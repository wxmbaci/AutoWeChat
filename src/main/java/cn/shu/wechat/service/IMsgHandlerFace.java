package cn.shu.wechat.service;

import cn.shu.wechat.pojo.dto.msg.sync.AddMsgList;
import cn.shu.wechat.pojo.entity.Message;

import java.util.List;

/**
 * 消息处理接口
 *
 * @author ShuXinSheng
 * @version 1.1
 * @date 创建时间：2017年4月20日 上午12:13:49
 */
public interface IMsgHandlerFace {
    /**
     * @param msg
     * @return
     * @author ShuXinSheng
     * @date 2017年4月20日 上午12:15:00
     */
    public List<Message> textMsgHandle(AddMsgList msg);

    /**
     * 处理图片消息
     *
     * @param msg
     * @return
     * @author ShuXinSheng
     * @date 2017年4月21日 下午11:07:06
     */
    public List<Message> picMsgHandle(AddMsgList msg);

    /**
     * 处理声音消息
     *
     * @param msg
     * @return
     * @author ShuXinSheng
     * @date 2017年4月22日 上午12:09:44
     */
    public List<Message> voiceMsgHandle(AddMsgList msg);

    /**
     * 处理小视频消息
     *
     * @param msg
     * @return
     * @author ShuXinSheng
     * @date 2017年4月23日 下午12:19:50
     */
    public List<Message> videoMsgHandle(AddMsgList msg);

    /**
     * 处理名片消息
     *
     * @param msg
     * @return
     * @author ShuXinSheng
     * @date 2017年5月1日 上午12:50:50
     */
    public List<Message> nameCardMsgHandle(AddMsgList msg);

    /**
     * 处理撤回消息
     *
     * @param msg
     * @return
     * @author ShuXinSheng
     * @date 2017年5月1日 上午12:50:50
     */
    public List<Message> undoMsgHandle(AddMsgList msg);

    /**
     * 处理好友确认消息
     *
     * @param msg
     * @return
     * @author ShuXinSheng
     * @date 2017年5月1日 上午12:50:50
     */
    public List<Message> addFriendMsgHandle(AddMsgList msg);

    /**
     * 处理好友确认消息
     *
     * @param msg
     * @return
     * @author ShuXinSheng
     * @date 2017年5月1日 上午12:50:50
     */
    public List<Message> systemMsgHandle(AddMsgList msg);

    /**
     * 表情处理消息
     *
     * @param msg
     * @return
     */
    public List<Message> emotionMsgHandle(AddMsgList msg);

    /**
     * 分享链接信息
     *
     * @param msg
     * @return
     */
    public List<Message> appMsgHandle(AddMsgList msg);

    /**
     * map消息
     *
     * @param msg
     * @return
     */
    public List<Message> mapMsgHandle(AddMsgList msg);

    /**
     * 处理确认添加好友消息
     *
     * @param msg
     * @return
     * @date 2017年6月28日 下午10:15:30
     */
    public List<Message> verifyAddFriendMsgHandle(AddMsgList msg);


}
