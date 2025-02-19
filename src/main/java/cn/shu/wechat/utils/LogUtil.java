package cn.shu.wechat.utils;

import cn.shu.wechat.api.ContactsTools;
import cn.shu.wechat.core.Core;
import cn.shu.wechat.pojo.dto.msg.sync.AddMsgList;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

/**
 * @作者 舒新胜
 * @项目 weixin
 * @创建时间 2/1/2021 2:28 PM
 */
@Log4j2
public class LogUtil {

    /**
     * 打印发给自己的消息
     *
     * @param msg  消息内容
     * @param path 消息为文件时的文件路径
     * @return 日志内容
     */
    public static String printFromMeg(AddMsgList msg, String path, String title) {
        String myUserName = Core.getUserName();
        String myNickName = Core.getNickName();
        //自己发的消息
        String fromUser = "";
        String toUser = "";
        if (myUserName.equals(msg.getFromUserName())) {
            if (myUserName.equals(msg.getToUserName())) {
                //自己发给自己的消息
                toUser = myNickName;
                fromUser = myNickName;
            } else {
                fromUser = myNickName;
                if (msg.isGroupMsg()) {
                    toUser = ContactsTools.getContactDisplayNameByUserName(msg.getToUserName()) + "(Group)";
                } else {
                    toUser = ContactsTools.getContactDisplayNameByUserName(msg.getToUserName());
                }
            }

        } else {
            toUser = myNickName;
            if (msg.isGroupMsg()) {
                //群成员昵称
                String groupUserNickNameOfGroup = ContactsTools.getMemberNickNameOfGroup(msg.getFromUserName(), msg.getMemberName());
                fromUser = ContactsTools.getContactDisplayNameByUserName(msg.getFromUserName()) + "(" + groupUserNickNameOfGroup + ")";
            } else {
                fromUser = ContactsTools.getContactDisplayNameByUserName(msg.getFromUserName());

            }

        }
        return String.format(title + "【%s ->>>>>>> %s: %s】 ===%s", fromUser, toUser
                , StringUtils.isEmpty(path) ? (StringUtils.isEmpty(msg.getFilePath()) ? msg.getContent() : msg.getFilePath()) : path
                , "");
    }

    /**
     * 打印发给自己的消息
     *
     * @param msg 消息内容
     * @return 日志内容
     */
    public static String printFromMeg(AddMsgList msg, String title) {
        return printFromMeg(msg, "", title);
    }

    /**
     * 打印发给自己的消息
     *
     * @param msg 消息内容
     * @return 日志内容
     */
    public static String printFromMeg(AddMsgList msg, int title) {
        return printFromMeg(msg, "", title + "");
    }

    /**
     * 打印发给自己的消息
     *
     * @param fromUserName 发送者
     * @param content      发送内容
     * @return 日志内容
     */
    public static String printFromMeg(String fromUserName, String content) {
        String myNickName = Core.getUserSelf().getNickname();
        String fromRemarkName = ContactsTools.getContactDisplayNameByUserName(fromUserName);
        return String.format("【%s ->>>>>>> %s: %s】", fromRemarkName, myNickName, content);
    }

    /**
     * 打印发给别人的消息
     *
     * @param toUserName 发送者
     * @param content    发送内容
     * @return 日志内容
     */
    public static String printToMeg(String msgType, String toUserName, String content) {
        if (toUserName.startsWith("@@")) {
            toUserName = ContactsTools.getContactDisplayNameByUserName(toUserName) + "(Group)";
        } else {
            toUserName = ContactsTools.getContactDisplayNameByUserName(toUserName);
        }

        return String.format(msgType + "【%s ->>>>>>> %s】: %s", Core.getNickName(), toUserName, content);
    }
}
