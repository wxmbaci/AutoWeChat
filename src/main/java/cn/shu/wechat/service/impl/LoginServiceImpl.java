package cn.shu.wechat.service.impl;

import cn.shu.wechat.api.ContactsTools;
import cn.shu.wechat.api.DownloadTools;
import cn.shu.wechat.configuration.WechatConfiguration;
import cn.shu.wechat.core.Core;
import cn.shu.wechat.core.MsgCenter;
import cn.shu.wechat.enums.*;
import cn.shu.wechat.enums.parameters.BaseParaEnum;
import cn.shu.wechat.enums.parameters.LoginParaEnum;
import cn.shu.wechat.enums.parameters.StatusNotifyParaEnum;
import cn.shu.wechat.enums.parameters.UUIDParaEnum;
import cn.shu.wechat.mapper.AttrHistoryMapper;
import cn.shu.wechat.pojo.dto.msg.sync.AddMsgList;
import cn.shu.wechat.pojo.dto.msg.sync.WebWxSyncMsg;
import cn.shu.wechat.pojo.entity.Contacts;
import cn.shu.wechat.service.LoginService;
import cn.shu.wechat.swing.utils.AvatarUtil;
import cn.shu.wechat.utils.CommonTools;
import cn.shu.wechat.utils.ExecutorServiceUtil;
import cn.shu.wechat.utils.HttpUtil;
import cn.shu.wechat.utils.SleepUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.log4j.Log4j2;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;

/**
 * 登陆服务实现类
 *
 * @author SXS
 * @version 1.1
 * @date 创建时间：2017年5月13日 上午12:09:35
 */
@Log4j2
@Component
public class LoginServiceImpl implements LoginService {

    @Resource
    private WechatConfiguration config;

    @Resource
    private AttrHistoryMapper attrHistoryMapper;

    @Resource
    private MsgCenter msgCenter;


    private final Set<String> msgIds = new HashSet<>();

    @Override
    public boolean login(LoginCallBack callBack) throws Exception {

        boolean isLogin = false;
        // 组装参数和URL
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair(LoginParaEnum.LOGIN_ICON.para(), LoginParaEnum.LOGIN_ICON.value()));
        params.add(new BasicNameValuePair(LoginParaEnum.UUID.para(), Core.getUuid()));
        params.add(new BasicNameValuePair(LoginParaEnum.TIP.para(), LoginParaEnum.TIP.value()));

        while1:
        while (!isLogin) {

            long millis = System.currentTimeMillis();
            params.add(new BasicNameValuePair(LoginParaEnum.R.para(), String.valueOf(millis / 1579L)));
            params.add(new BasicNameValuePair(LoginParaEnum._.para(), String.valueOf(millis)));
            HttpEntity entity = HttpUtil.doGet(URLEnum.LOGIN_URL.getUrl(), params, true, null);

            try {
                String result = EntityUtils.toString(entity);
                String status = checklogin(result);
                if (status == null) {
                    continue;
                }
                CheckLoginResultEnum byCode = CheckLoginResultEnum.getByCode(status);

                switch (byCode) {

                    case SUCCESS: {
                        String loginInfo = processLoginInfo(result);
                        if (loginInfo == null) {
                            isLogin = true;
                            Core.setAlive(true);
                            callBack.CallBack(byCode.getMsg());
                        } else {
                            callBack.CallBack(loginInfo);
                        }
                        break while1;
                    }
                    case WAIT_CONFIRM:
                        log.info(byCode.getMsg());
                        String avatar = getUserAvatar(result);
                        callBack.avatar(avatar);
                        callBack.CallBack(byCode.getMsg());
                        break;
                    case WAIT_SCAN: {
                        log.info(byCode.getMsg());
                        callBack.CallBack(byCode.getMsg());
                        break;
                    }
                    case NONE: {
                        log.info(byCode.getMsg());
                        break;
                    }

                }

            } catch (Exception e) {
                e.printStackTrace();
                log.error("微信登陆异常：{}", e.getMessage());
            }
        }
        return isLogin;
    }


    @Override
    public String getUuid() {
        // 组装参数和URL
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair(UUIDParaEnum.APP_ID.para(), UUIDParaEnum.APP_ID.value()));
        params.add(new BasicNameValuePair(UUIDParaEnum.FUN.para(), UUIDParaEnum.FUN.value()));
        params.add(new BasicNameValuePair(UUIDParaEnum.LANG.para(), UUIDParaEnum.LANG.value()));
        params.add(new BasicNameValuePair(UUIDParaEnum._.para(), String.valueOf(System.currentTimeMillis())));

        HttpEntity entity = HttpUtil.doGet(URLEnum.UUID_URL.getUrl(), params, true, null);

        try {
            String result = EntityUtils.toString(entity);
            String regEx = "window.QRLogin.code = (\\d+); window.QRLogin.uuid = \"(\\S+?)\";";
            Matcher matcher = CommonTools.getMatcher(regEx, result);
            if (matcher.find()) {
                if ((CheckLoginResultEnum.SUCCESS.getCode().equals(matcher.group(1)))) {
                    Core.setUuid(matcher.group(2));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return Core.getUuid();
    }

    @Override
    public boolean getQR(String qrPath) {

        String qrUrl = URLEnum.QRCODE_URL.getUrl() + Core.getUuid();
        HttpEntity entity = HttpUtil.doGet(qrUrl, null, true, null);
        try {
            //下载二维码图片
            OutputStream out = new FileOutputStream(qrPath);
            byte[] bytes = EntityUtils.toByteArray(entity);
            out.write(bytes);
            out.flush();
            out.close();
            //二维码地址
            String qrUrl2 = URLEnum.cAPI_qrcode.getUrl() + Core.getUuid();
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public BufferedImage getQR() {
        String qrUrl = URLEnum.QRCODE_URL.getUrl() + Core.getUuid();
        HttpEntity entity = HttpUtil.doGet(qrUrl, null, true, null);
        try {
            BufferedImage image = ImageIO.read(entity.getContent());
            return image;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public boolean webWxInit() {
        Core.setAlive(true);
        Core.setLastNormalRetCodeTime(System.currentTimeMillis());
        // 组装请求URL和参数
        String url = String.format(URLEnum.INIT_URL.getUrl(),
                Core.getLoginInfoMap().get(StorageLoginInfoEnum.url.getKey()),
                System.currentTimeMillis() / 3158L,
                Core.getLoginInfoMap().get(StorageLoginInfoEnum.pass_ticket.getKey()));

        Map<String, Object> paramMap = Core.getParamMap();

        // 请求初始化接口
        HttpEntity entity = HttpUtil.doPost(url, JSON.toJSONString(paramMap));
        try {
            String result = EntityUtils.toString(entity, Consts.UTF_8);
            JSONObject obj = JSON.parseObject(result);

            JSONObject user = obj.getJSONObject(StorageLoginInfoEnum.User.getKey());
            JSONObject syncKey = obj.getJSONObject(StorageLoginInfoEnum.SyncKey.getKey());

            Core.getLoginInfoMap().put(StorageLoginInfoEnum.InviteStartCount.getKey(),
                    obj.getInteger(StorageLoginInfoEnum.InviteStartCount.getKey()));
            Core.getLoginInfoMap().put(StorageLoginInfoEnum.SyncKey.getKey(), syncKey);

            JSONArray syncArray = syncKey.getJSONArray("List");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < syncArray.size(); i++) {
                sb.append(syncArray.getJSONObject(i).getString("Key")).append("_").append(syncArray.getJSONObject(i).getString("Val")).append("|");
            }
            // 1_661706053|2_661706420|3_661706415|1000_1494151022|
            String synckey = sb.toString();

            // 1_661706053|2_661706420|3_661706415|1000_1494151022
            // 1_656161336|2_656161626|3_656161313|11_656159955|13_656120033|201_1492273724|1000_1492265953|1001_1492250432|1004_1491805192
            Core.getLoginInfoMap().put(StorageLoginInfoEnum.synckey.getKey(), synckey.substring(0, synckey.length() - 1));
            Core.setUserName(user.getString("UserName"));
            Core.setNickName(user.getString("NickName"));
            Contacts me = JSON.parseObject(JSON.toJSONString(obj.getJSONObject("User")), Contacts.class);
            Core.setUserSelf(me);
            Core.getMemberMap().put(user.getString("UserName"), me);
            //初始化列表的联系人
            //最近聊天的联系人
            JSONArray contactList = obj.getJSONArray("ContactList");
            List<Contacts> contactsList = JSON.parseArray(JSON.toJSONString(contactList), Contacts.class);
            Set<String> recentContacts = Core.getRecentContacts();
            for (Contacts contacts : contactsList) {
                //下载头像
                ExecutorServiceUtil.getHeadImageDownloadExecutorService().submit(() -> {
                    AvatarUtil.putUserAvatarCache(contacts.getUsername(), DownloadTools.downloadHeadImgByRelativeUrl(contacts.getHeadimgurl()));
                });
                addContacts(contacts, false);
                recentContacts.add(contacts.getUsername());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void wxStatusNotify() {
        // 组装请求URL和参数
        String url = String.format(URLEnum.STATUS_NOTIFY_URL.getUrl(),
                Core.getLoginInfoMap().get(StorageLoginInfoEnum.pass_ticket.getKey()));

        Map<String, Object> paramMap = Core.getParamMap();
        paramMap.put(StatusNotifyParaEnum.CODE.para(), StatusNotifyParaEnum.CODE.value());
        paramMap.put(StatusNotifyParaEnum.FROM_USERNAME.para(), Core.getUserName());
        paramMap.put(StatusNotifyParaEnum.TO_USERNAME.para(), Core.getUserName());
        paramMap.put(StatusNotifyParaEnum.CLIENT_MSG_ID.para(), System.currentTimeMillis());
        String paramStr = JSON.toJSONString(paramMap);

        try {
            HttpEntity entity = HttpUtil.doPost(url, paramStr);
            EntityUtils.toString(entity, Consts.UTF_8);
        } catch (Exception e) {
            log.error("微信状态通知接口失败！", e);
        }

    }

    /**
     * 处理成功消息
     *
     * @param selector 类型
     */
    private void processSuccessMsg(String selector) {
        // 最后收到正常报文时间
        Core.setLastNormalRetCodeTime(System.currentTimeMillis());
        //消息同步
        //JSONObject msgObj = webWxSync();
        WebWxSyncMsg webWxSyncMsg = webWxSync();
        if (webWxSyncMsg == null) {
            return;
        }
        switch (SyncCheckSelectorEnum.getByCode(selector)) {
            case NORMAL:
                break;
            case MOD_CONTACT:
            case ADD_OR_DEL_CONTACT:
            case NEW_MSG:

                //新消息
                for (AddMsgList msg : webWxSyncMsg.getAddMsgList()) {
                    if (msgIds.contains(msg.getMsgId())) {
                        log.warn("消息重复：{}", msg);
                        continue;
                    }
                    msgIds.add(msg.getMsgId());
                    ExecutorServiceUtil.getGlobalExecutorService().execute(() -> {
                        msgCenter.handleNewMsg(msg);
                    });
                }
                //联系人修改
                msgCenter.handleModContact(webWxSyncMsg.getModContactList());
                for (Contacts contacts : webWxSyncMsg.getDelContactList()) {
                    log.info("联系人删除：{}", contacts);
                }

                break;

            case ENTER_OR_LEAVE_CHAT:
                webWxSync();
                break;

            case A:
                log.info("未知消息：{}", webWxSyncMsg);
                break;
            default:
                break;

        }
    }

    @Override
    public void startReceiving() {
        Core.setAlive(true);
        Runnable runnable = () -> {
            while (Core.isAlive()) {
                try {

                    //检测是否有新消息
                    Map<String, String> resultMap = syncCheck();
                    String retcode = resultMap.get("retcode");
                    SyncCheckRetCodeEnum syncCheckRetCodeEnum = SyncCheckRetCodeEnum.getByCode(retcode);
                    switch (syncCheckRetCodeEnum) {

                        case SUCCESS: {
                            processSuccessMsg(resultMap.get("selector"));
                            break;
                        }
                        case UNKOWN: {
                            log.info(syncCheckRetCodeEnum.getType());
                            continue;
                        }
                        case LOGIN_OUT:
                        case LOGIN_OTHERWHERE:
                        case MOBILE_LOGIN_OUT: {
                            log.warn(syncCheckRetCodeEnum.getType());
                            //重启客户端
                            //WeChatStater.restart();
                            break;
                        }
                        case TICKET_ERROR:
                        case PARAM_ERROR:
                        case NOT_LOGIN_WARN:
                        case LOGIN_ENV_ERROR:
                        case TOO_OFEN: {
                            log.error(syncCheckRetCodeEnum.getType());
                            Core.setAlive(false);
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("消息同步错误：{}", e.getMessage());
                    SleepUtils.sleep(1000);
                }

            }
        };
        ExecutorServiceUtil.getReceivingExecutorService().execute(runnable);
    }

    @Override
    public void webWxGetContact() {
        String url = String.format(URLEnum.WEB_WX_GET_CONTACT.getUrl(),
                Core.getLoginInfoMap().get(StorageLoginInfoEnum.url.getKey()));
        Map<String, Object> paramMap = Core.getParamMap();
        HttpEntity entity = HttpUtil.doPost(url, JSON.toJSONString(paramMap));
        if (entity == null) {
            return;
        }
        try {
            String result = EntityUtils.toString(entity, Consts.UTF_8);
            JSONObject fullFriendsJsonList = JSON.parseObject(result);
            // 查看seq是否为0，0表示好友列表已全部获取完毕，若大于0，则表示好友列表未获取完毕，当前的字节数（断点续传）
            long seq = 0;
            long currentTime = 0L;
            List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
            if (fullFriendsJsonList.get("Seq") != null) {
                seq = fullFriendsJsonList.getLong("Seq");
                currentTime = System.currentTimeMillis();
            }
            JSONArray member = fullFriendsJsonList.getJSONArray(StorageLoginInfoEnum.MemberList.getKey());
            // 循环获取seq直到为0，即获取全部好友列表
            // ==0：好友获取完毕
            // >0：好友未获取完毕，此时seq为已获取的字节数
            while (seq > 0) {
                // 设置seq传参
                params.add(new BasicNameValuePair("r", String.valueOf(currentTime)));
                params.add(new BasicNameValuePair("seq", String.valueOf(seq)));
                entity = HttpUtil.doGet(url, params, false, null);

                params.remove(new BasicNameValuePair("r", String.valueOf(currentTime)));
                params.remove(new BasicNameValuePair("seq", String.valueOf(seq)));

                result = EntityUtils.toString(entity, Consts.UTF_8);
                fullFriendsJsonList = JSON.parseObject(result);

                if (fullFriendsJsonList.get("Seq") != null) {
                    seq = fullFriendsJsonList.getLong("Seq");
                    currentTime = System.currentTimeMillis();
                }

                // 累加好友列表
                member.addAll(fullFriendsJsonList.getJSONArray(StorageLoginInfoEnum.MemberList.getKey()));
            }
            for (Object value : member) {
                JSONObject o = (JSONObject) value;
                Contacts contacts = JSON.parseObject(JSON.toJSONString(o), Contacts.class);
                addContacts(contacts, true);
            }
            if (!Core.getMemberMap().containsKey("filehelper")) {
                Core.getMemberMap().put("filehelper",
                        Contacts.builder().username("filehelper").displayname("文件传输助手")
                                .type(Contacts.ContactsType.ORDINARY_USER).build());
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 添加联系人
     *
     * @param contacts
     */
    private void addContacts(Contacts contacts, boolean compare) {

        contacts.setIscontacts(true);
        String userName = contacts.getUsername();
        String nickName = contacts.getNickname();
        //保存之前的群信息 方便compare
        if (Core.getMemberMap().containsKey(contacts.getUsername())) {
            contacts.setMemberlist(Core.getMemberMap().get(contacts.getUsername()).getMemberlist());
        }
        Core.getMemberMap().put(userName, contacts);

        if ((contacts.getVerifyflag() & 8) != 0) {
            // 公众号/服务号
            if (!Core.getPublicUsersMap().containsKey(userName)) {
                log.info("新增公众号/服务号：{}", nickName);
            }
            Core.getPublicUsersMap().put(userName, contacts);
            contacts.setType(Contacts.ContactsType.PUBLIC_USER);
        } else if (config.getSpecialUser().contains(userName)) {
            // 特殊账号
            if (!Core.getSpecialUsersMap().containsKey(userName)) {
                log.info("新增特殊账号：{}", nickName);
            }
            Core.getSpecialUsersMap().put(userName, contacts);
            contacts.setType(Contacts.ContactsType.SPECIAL_USER);
        } else if (userName.startsWith("@@")) {
            // 群聊
            if (!Core.getGroupIdSet().contains(userName)) {
                log.info("新增群聊：{}", nickName);
                Core.getGroupIdSet().add(userName);
            }
            contacts.setType(Contacts.ContactsType.GROUP_USER);
        } else {
            contacts.setType(Contacts.ContactsType.ORDINARY_USER);
            //比较上次差异
            if (compare) {
                Contacts old = Core.getContactMap().get(userName);
                ContactsTools.compareContacts(old, contacts);
            }

            // 普通联系人
            Core.getContactMap().put(userName, contacts);
        }
    }

    @Override
    public void WebWxBatchGetContact() {
        String url = String.format(URLEnum.WEB_WX_BATCH_GET_CONTACT.getUrl(),
                Core.getLoginInfoMap().get(StorageLoginInfoEnum.url.getKey()), new Date().getTime(),
                Core.getLoginInfoMap().get(StorageLoginInfoEnum.pass_ticket.getKey()));
        Map<String, Object> paramMap = Core.getParamMap();
        paramMap.put("Count", Core.getGroupIdSet().size());
        List<Map<String, String>> list = new ArrayList<Map<String, String>>(Core.getGroupIdSet().size());
        for (String s : Core.getGroupIdSet()) {
            HashMap<String, String> map = new HashMap<String, String>(2);
            map.put("UserName", s);
            map.put("EncryChatRoomId", "");
            list.add(map);
        }
        paramMap.put("List", list);
        HttpEntity entity = HttpUtil.doPost(url, JSON.toJSONString(paramMap));
        try {
            String text = EntityUtils.toString(entity, Consts.UTF_8);
            JSONObject obj = JSON.parseObject(text);
            //群列表
            JSONArray contactList = obj.getJSONArray("ContactList");
            for (int i = 0; i < contactList.size(); i++) {
                // 群好友
                JSONObject groupObject = contactList.getJSONObject(i);
                Contacts group = JSON.parseObject(JSON.toJSONString(groupObject), Contacts.class);
                String userName = group.getUsername();
                if (userName.startsWith("@@")) {
                    //以上接口返回的成员属性不全，以下的接口获取群成员详细属性
                    JSONArray memberArray = WebWxBatchGetContactDetail(group);
                    List<Contacts> memberList = JSON.parseArray(JSON.toJSONString(memberArray), Contacts.class);
                    group.setMemberlist(memberList);

                    //比较群成员信息
                    Contacts old = Core.getGroupMap().get(userName);
                    //比较上次差异
                    ContactsTools.compareGroup(old, group);

                    Core.getMemberMap().put(userName, group);
                    Core.getGroupMap().put(userName, group);
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }

    @Override
    public List<Contacts> WebWxBatchGetContact(String groupName) {

        log.info("加载群成员开始：" + groupName);
        String url = String.format(URLEnum.WEB_WX_BATCH_GET_CONTACT.getUrl(),
                Core.getLoginInfoMap().get(StorageLoginInfoEnum.url.getKey()), new Date().getTime(),
                Core.getLoginInfoMap().get(StorageLoginInfoEnum.pass_ticket.getKey()));
        Map<String, Object> paramMap = Core.getParamMap();
        paramMap.put("Count", 1);
        List<Map<String, String>> list = new ArrayList<Map<String, String>>(1);
        HashMap<String, String> map = new HashMap<String, String>(2);
        map.put("UserName", groupName);
        map.put("EncryChatRoomId", "");
        list.add(map);
        paramMap.put("List", list);
        HttpEntity entity = null;
        synchronized ((groupName + "WebWxBatchGetContact").intern()) {
            entity = HttpUtil.doPost(url, JSON.toJSONString(paramMap));
        }
        try {
            String text = EntityUtils.toString(entity, Consts.UTF_8);
            JSONObject obj = JSON.parseObject(text);
            //群列表
            JSONArray contactList = obj.getJSONArray("ContactList");
            for (int i = 0; i < contactList.size(); i++) {
                // 群好友
                JSONObject groupObject = contactList.getJSONObject(i);
                Contacts group = JSON.parseObject(JSON.toJSONString(groupObject), Contacts.class);
                group.setType(Contacts.ContactsType.GROUP_USER);
                String userName = group.getUsername();
                Core.getMemberMap().put(userName, group);
                if (userName.startsWith("@@")) {
                    //以上接口返回的成员属性不全，以下的接口获取群成员详细属性
                    JSONArray memberArray = WebWxBatchGetContactDetail(group);
                    List<Contacts> memberList = JSON.parseArray(JSON.toJSONString(memberArray), Contacts.class);
                    group.setMemberlist(memberList);
                    Core.getGroupMap().put(userName, group);
                    Core.getMemberMap().put(userName, group);
                    log.info("加载群成员结束：" + Core.getMemberMap().get(groupName).getMemberlist().size());
                    return memberList;
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
        log.info("加载群成员结束：0");
        return new ArrayList<>();
    }

    @Override
    public JSONArray WebWxBatchGetContactDetail(Contacts group) {
        String url = String.format(URLEnum.WEB_WX_BATCH_GET_CONTACT.getUrl(),
                Core.getLoginInfoMap().get(StorageLoginInfoEnum.url.getKey()), System.currentTimeMillis(),
                Core.getLoginInfoMap().get(StorageLoginInfoEnum.pass_ticket.getKey()));
        Map<String, Object> paramMap = Core.getParamMap();

        //保存获取的群成员详细信息
        ArrayList<Contacts> groupContactsList = new ArrayList<>();
        JSONArray memberArray = new JSONArray();
        //保存需要获取详细资料的群成员username
        List<Map<String, String>> list = new ArrayList<Map<String, String>>(group.getMemberlist().size());
        for (Contacts o : group.getMemberlist()) {
            //遍历群成员
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("UserName", o.getUsername());
            map.put("EncryChatRoomId", group.getUsername());
            list.add(map);
        }
        if (list.isEmpty()) {
            return memberArray;
        }
        //每次请求50个
        int ceil = (int) (Math.ceil((list.size() / 50.0)));

        for (int i = 0; i < ceil; i++) {
            List<Map<String, String>> subList = null;
            if (i < ceil - 1) {
                subList = list.subList(i * 50, i * 50 + 50);
            } else {
                subList = list.subList(i * 50, list.size());
            }
            paramMap.put("Count", subList.size());
            paramMap.put("List", subList);
            HttpEntity entity = null;
            synchronized ((group.getUsername() + "WebWxBatchGetContact").intern()) {
                entity = HttpUtil.doPost(url, JSON.toJSONString(paramMap));
            }
            try {
                String text = EntityUtils.toString(entity, Consts.UTF_8);
                JSONObject obj = JSON.parseObject(text);
                JSONArray contactListArray = obj.getJSONArray("ContactList");
                memberArray.addAll(contactListArray);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return memberArray;

    }

    /**
     * 检查登陆状态
     *
     * @param result
     * @return
     */
    public String checklogin(String result) {
        String regEx = "window.code=(\\d+)";
        Matcher matcher = CommonTools.getMatcher(regEx, result);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 检查登录人的头像
     *
     * @param result
     * @return
     */
    public String getUserAvatar(String result) {
        String regEx = "window.userAvatar\\s*=\\s*'data:img/jpg;base64,(.+)'";
        Matcher matcher = CommonTools.getMatcher(regEx, result);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 处理登陆信息
     *
     * @param loginContent
     * @author SXS
     * @date 2017年4月9日 下午12:16:26
     */
    private String processLoginInfo(String loginContent) {
        String regEx = "window.redirect_uri=\"(\\S+)\";";
        Matcher matcher = CommonTools.getMatcher(regEx, loginContent);
        if (matcher.find()) {
            String originalUrl = matcher.group(1);
            // https://wx2.qq.com/cgi-bin/mmwebwx-bin
            String url = originalUrl.substring(0, originalUrl.lastIndexOf('/'));
            Core.getLoginInfoMap().put("url", url);
            Map<String, List<String>> possibleUrlMap = this.getPossibleUrlMap();
            Iterator<Entry<String, List<String>>> iterator = possibleUrlMap.entrySet().iterator();
            Map.Entry<String, List<String>> entry;
            String fileUrl;
            String syncUrl;
            while (iterator.hasNext()) {
                entry = iterator.next();
                String indexUrl = entry.getKey();
                fileUrl = "https://" + entry.getValue().get(0) + "/cgi-bin/mmwebwx-bin";
                syncUrl = "https://" + entry.getValue().get(1) + "/cgi-bin/mmwebwx-bin";
                if (Core.getLoginInfoMap().get("url").toString().contains(indexUrl)) {
                    Core.getLoginInfoMap().put("fileUrl", fileUrl);
                    Core.getLoginInfoMap().put("syncUrl", syncUrl);
                    break;
                }
            }
            if (Core.getLoginInfoMap().get("fileUrl") == null && Core.getLoginInfoMap().get("syncUrl") == null) {
                Core.getLoginInfoMap().put("fileUrl", url);
                Core.getLoginInfoMap().put("syncUrl", url);
            }
            Core.getLoginInfoMap().put("deviceid", "e" + String.valueOf(new Random().nextLong()).substring(1, 16)); // 生成15位随机数
            Core.getLoginInfoMap().put("BaseRequest", new ArrayList<String>());
            String text = "";

            try {
                HttpEntity entity = HttpUtil.doGet(originalUrl, null, false, null);
                text = EntityUtils.toString(entity);
            } catch (Exception e) {
                log.error(e.getMessage());
                return e.getMessage();
            }
            //add by 默非默 2017-08-01 22:28:09
            //如果登录被禁止时，则登录返回的message内容不为空，下面代码则判断登录内容是否为空，不为空则退出程序
            String msg = getLoginMessage(text);
            if (!"".equals(msg)) {
                log.error(msg);
                return msg;
            }
            Document doc = CommonTools.xmlParser(text);
            if (doc != null) {
                Core.getLoginInfoMap().put(StorageLoginInfoEnum.skey.getKey(),
                        doc.getElementsByTagName(StorageLoginInfoEnum.skey.getKey()).item(0).getFirstChild()
                                .getNodeValue());
                Core.getLoginInfoMap().put(StorageLoginInfoEnum.wxsid.getKey(),
                        doc.getElementsByTagName(StorageLoginInfoEnum.wxsid.getKey()).item(0).getFirstChild()
                                .getNodeValue());
                Core.getLoginInfoMap().put(StorageLoginInfoEnum.wxuin.getKey(),
                        doc.getElementsByTagName(StorageLoginInfoEnum.wxuin.getKey()).item(0).getFirstChild()
                                .getNodeValue());
                Core.getLoginInfoMap().put(StorageLoginInfoEnum.pass_ticket.getKey(),
                        doc.getElementsByTagName(StorageLoginInfoEnum.pass_ticket.getKey()).item(0).getFirstChild()
                                .getNodeValue());
            }

        }
        return null;
    }

    private Map<String, List<String>> getPossibleUrlMap() {
        Map<String, List<String>> possibleUrlMap = new HashMap<String, List<String>>();
        possibleUrlMap.put("wx.qq.com", new ArrayList<String>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                add("file.wx.qq.com");
                add("webpush.wx.qq.com");
            }
        });

        possibleUrlMap.put("wx2.qq.com", new ArrayList<String>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                add("file.wx2.qq.com");
                add("webpush.wx2.qq.com");
            }
        });
        possibleUrlMap.put("wx8.qq.com", new ArrayList<String>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                add("file.wx8.qq.com");
                add("webpush.wx8.qq.com");
            }
        });

        possibleUrlMap.put("web2.wechat.com", new ArrayList<String>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                add("file.web2.wechat.com");
                add("webpush.web2.wechat.com");
            }
        });
        possibleUrlMap.put("wechat.com", new ArrayList<String>() {
            /**
             *
             */
            private static final long serialVersionUID = 1L;

            {
                add("file.web.wechat.com");
                add("webpush.web.wechat.com");
            }
        });
        return possibleUrlMap;
    }

    /**
     * 同步消息 sync the messages
     *
     * @return
     * @author SXS
     * @date 2017年5月12日 上午12:24:55
     */
    private WebWxSyncMsg webWxSync() {
        String url = String.format(URLEnum.WEB_WX_SYNC_URL.getUrl(),
                Core.getLoginInfoMap().get(StorageLoginInfoEnum.url.getKey()),
                Core.getLoginInfoMap().get(StorageLoginInfoEnum.wxsid.getKey()),
                Core.getLoginInfoMap().get(StorageLoginInfoEnum.skey.getKey()),
                Core.getLoginInfoMap().get(StorageLoginInfoEnum.pass_ticket.getKey()));
        Map<String, Object> paramMap = Core.getParamMap();
        paramMap.put(StorageLoginInfoEnum.SyncKey.getKey(),
                Core.getLoginInfoMap().get(StorageLoginInfoEnum.SyncKey.getKey()));
        paramMap.put("rr", -System.currentTimeMillis() / 1000);
        String paramStr = JSON.toJSONString(paramMap);
        try {
            HttpEntity entity = HttpUtil.doPost(url, paramStr);
            String text = EntityUtils.toString(entity, Consts.UTF_8);
            WebWxSyncMsg webWxSyncMsg = JSON.parseObject(text, WebWxSyncMsg.class);
            if (webWxSyncMsg.getBaseResponse().getRet() != 0) {
                return null;
            } else {
                Core.getLoginInfoMap().put(StorageLoginInfoEnum.SyncKey.getKey(), webWxSyncMsg.getSyncCheckKey());
                StringBuilder sb = new StringBuilder();
                for (cn.shu.wechat.pojo.dto.msg.sync.List list : webWxSyncMsg.getSyncCheckKey().getList()) {
                    sb.append(list.getKey()).append("_").append(list.getVal()).append("|");
                }
                String synckey = sb.toString();
                Core.getLoginInfoMap().put(StorageLoginInfoEnum.synckey.getKey(),
                        synckey.substring(0, synckey.length() - 1));
            }
            return webWxSyncMsg;
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return null;
    }

    /**
     * 检查是否有新消息 check whether there's a message
     *
     * @return
     * @author SXS
     * @date 2017年4月16日 上午11:11:34
     */
    private Map<String, String> syncCheck() {
        Map<String, String> resultMap = new HashMap<String, String>();
        // 组装请求URL和参数
        String url = Core.getLoginInfoMap().get(StorageLoginInfoEnum.syncUrl.getKey()) + URLEnum.SYNC_CHECK_URL.getUrl();
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        for (BaseParaEnum baseRequest : BaseParaEnum.values()) {
            params.add(new BasicNameValuePair(baseRequest.para().toLowerCase(),
                    Core.getLoginInfoMap().get(baseRequest.value()).toString()));
        }
        params.add(new BasicNameValuePair("r", String.valueOf(System.currentTimeMillis())));
        params.add(new BasicNameValuePair("synckey", (String) Core.getLoginInfoMap().get("synckey")));
        params.add(new BasicNameValuePair("_", String.valueOf(System.currentTimeMillis())));
        SleepUtils.sleep(7);
        try {
            Long start = System.currentTimeMillis();
            //log.info("开始syncCheck-params：{}",params.toString());
            HttpEntity entity = HttpUtil.doGetOfReceive(url, params, true, null);
            if (entity == null) {
                resultMap.put("retcode", "9999");
                resultMap.put("selector", "9999");
                return resultMap;
            }
            String text = EntityUtils.toString(entity);
            Long end = System.currentTimeMillis();
            //log.info("结束syncCheck({}s)结束-----------------------result：{}",    ((double)(end-start))/1000,text);
            String regEx = "window.synccheck=\\{retcode:\"(\\d+)\",selector:\"(\\d+)\"\\}";
            Matcher matcher = CommonTools.getMatcher(regEx, text);
            if (!matcher.find() || matcher.group(1).equals("2")) {
                log.error(String.format("Unexpected sync check result: %s", text));
            } else {
                resultMap.put("retcode", matcher.group(1));
                resultMap.put("selector", matcher.group(2));
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
        return resultMap;
    }

    /**
     * 解析登录返回的消息，如果成功登录，则message为空
     *
     * @param result
     * @return
     */
    public String getLoginMessage(String result) {
        String[] strArr = result.split("<message>");
        String[] rs = strArr[1].split("</message>");
        if (rs.length > 1) {
            return rs[0];
        }
        return "";
    }


}
