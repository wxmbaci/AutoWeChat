package cn.shu.wechat.swing.frames;

import cn.shu.wechat.api.DownloadTools;
import cn.shu.wechat.configuration.WechatConfiguration;
import cn.shu.wechat.core.Core;
import cn.shu.wechat.pojo.entity.Contacts;
import cn.shu.wechat.service.LoginService;
import cn.shu.wechat.swing.components.Colors;
import cn.shu.wechat.swing.components.GBC;
import cn.shu.wechat.swing.components.SizeAutoAdjustTextArea;
import cn.shu.wechat.swing.components.VerticalFlowLayout;
import cn.shu.wechat.swing.entity.RoomItem;
import cn.shu.wechat.swing.listener.AbstractMouseListener;
import cn.shu.wechat.swing.panels.left.tabcontent.ContactsPanel;
import cn.shu.wechat.swing.panels.left.tabcontent.RoomsPanel;
import cn.shu.wechat.swing.utils.FontUtil;
import cn.shu.wechat.swing.utils.IconUtil;
import cn.shu.wechat.swing.utils.OSUtil;
import cn.shu.wechat.utils.*;
import com.melloware.jintellitype.HotkeyListener;
import com.melloware.jintellitype.JIntellitype;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import sun.security.provider.MD5;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by 舒新胜 on 08/06/2017.
 */
@Log4j2
public final class LoginFrame extends JFrame {
    /**
     * 登陆服务实现类
     */
    private final LoginService loginService;

    /**
     * 微信配置类
     */
    private final WechatConfiguration wechatConfiguration;

    private static final int WINDOW_WIDTH = 300;
    private static final int WINDOW_HEIGHT = 450;

    private JPanel controlPanel;
    private JLabel closeLabel;
    private JPanel codePanel;
    private JLabel codeLabel;
    private JLabel refreshCodeBt;
    private SizeAutoAdjustTextArea statusLabel;

    private static final Point origin = new Point();

    public static volatile boolean cancelLogin;

    public LoginFrame() {
        super("微信-舒专用版");
        initComponents();
        initView();
        setLocationRelativeTo(null);
        setListeners();
        if (OSUtil.getOsType() == OSUtil.Windows) {
            registerHotKey();
        }
        loginService = SpringContextHolder.getBean(LoginService.class);
        wechatConfiguration = SpringContextHolder.getBean(WechatConfiguration.class);
    }


    private void initComponents() {
        Dimension windowSize = new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT);
        setMinimumSize(windowSize);
        setMaximumSize(windowSize);


        controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 5));


        closeLabel = new JLabel();
        closeLabel.setIcon(IconUtil.getIcon(this, "/image/close.png"));
        closeLabel.setHorizontalAlignment(JLabel.CENTER);
        closeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));


        statusLabel = new SizeAutoAdjustTextArea(280);
        statusLabel.setForeground(Colors.FONT_GRAY);
        statusLabel.setText("正在加载二维码...");
        statusLabel.setEditable(false);
        statusLabel.setVisible(true);
        StyledDocument doc = statusLabel.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);
    }

    private void initView() {
        JPanel contentPanel = new JPanel();
        contentPanel.setBorder(new LineBorder(Colors.LIGHT_GRAY));
        contentPanel.setLayout(new GridBagLayout());

        controlPanel.add(closeLabel);
        JPanel titleJPanel = new JPanel();
        JLabel titleJLabel = new JLabel(WechatConfiguration.getInstance().getLoginTitle());
        titleJLabel.setFont(FontUtil.getDefaultFont(14, Font.BOLD));
        titleJPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        titleJPanel.add(titleJLabel);
        if (OSUtil.getOsType() != OSUtil.Mac_OS) {
            setUndecorated(true);
            contentPanel.add(titleJPanel, new GBC(0, 0).setFill(GBC.BOTH).setWeight(1, 1).setInsets(5, 0, 0, 0));
            contentPanel.add(controlPanel, new GBC(0, 0).setFill(GBC.BOTH).setWeight(1, 1).setInsets(5, 0, 0, 0));
        }


        JPanel centerPanel = new JPanel(new BorderLayout(0, 0));
        centerPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        codeLabel = new JLabel();
        ImageIcon icon = IconUtil.getIcon(this, "/image/image_loading.gif");
        codeLabel.setHorizontalAlignment(JLabel.CENTER);
        codeLabel.setIcon(icon);
        centerPanel.add(codeLabel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new VerticalFlowLayout(true, false));

        //重新扫描按钮
        refreshCodeBt = new JLabel("刷新");
        refreshCodeBt.setHorizontalAlignment(JLabel.CENTER);
        refreshCodeBt.setBorder(new LineBorder(Color.GRAY));
        refreshCodeBt.setPreferredSize(new Dimension(150, 40));
        refreshCodeBt.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        //refreshCodeBt.setForeground(Colors.FONT_GRAY);
        refreshCodeBt.setFont(FontUtil.getDefaultFont());
        JPanel refreshCodeBtPanel = new JPanel();
        refreshCodeBtPanel.add(refreshCodeBt);
        bottomPanel.add(refreshCodeBtPanel);
        refreshCodeBt.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                //TODO 重新登录功能

                new SwingWorker<Object, Object>() {
                    private BufferedImage qr;

                    @Override
                    protected Object doInBackground() throws Exception {
                        qr = loginService.getQR();
                        return null;
                    }

                    @Override
                    protected void done() {
                        codeLabel.setIcon(new ImageIcon(qr.getScaledInstance(250, 250, Image.SCALE_SMOOTH)));
                        showMessage("请扫描二维码以登录");
                    }
                }.execute();
                super.mouseReleased(e);
            }
        });

        bottomPanel.add(statusLabel);
        bottomPanel.setBorder(new EmptyBorder(0, 0, 50, 0));
        centerPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(contentPanel);
        contentPanel.add(centerPanel, new GBC(0, 2).setFill(GBC.BOTH).setWeight(1, 10).setInsets(10, 10, 0, 10));
    }

    /**
     * 使窗口在屏幕中央显示
     */

    private void setListeners() {
        closeLabel.addMouseListener(new AbstractMouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                System.exit(1);
                super.mouseClicked(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                closeLabel.setBackground(Colors.LIGHT_GRAY);
                super.mouseEntered(e);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeLabel.setBackground(Colors.WINDOW_BACKGROUND);
                super.mouseExited(e);
            }
        });

        if (OSUtil.getOsType() != OSUtil.Mac_OS) {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    // 当鼠标按下的时候获得窗口当前的位置
                    origin.x = e.getX();
                    origin.y = e.getY();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    // 当鼠标拖动时获取窗口当前位置
                    Point p = LoginFrame.this.getLocation();
                    // 设置窗口的位置
                    LoginFrame.this.setLocation(p.x + e.getX() - origin.x, p.y + e.getY()
                            - origin.y);
                }
            });
        }


    }

    /**
     * 打开窗体
     */
    private void openMainFrame() {
        MainFrame frame = new MainFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        this.dispose();

    }

    /**
     * 显示消息
     */
    private void showMessage(String message) {
        if (!statusLabel.isVisible()) {
            statusLabel.setVisible(true);
        }

        statusLabel.setText(message);
        statusLabel.setToolTipText(message);
    }

    /**
     * 调用网页版微信登录
     *
     * @param dHImg 是否下载头像
     */
    public void login(boolean dHImg) {
        try { // 防止SSL错误
            System.setProperty("jsse.enableSNIExtension", "false");
            showMessage("获取UUID");
            getUUID();

            showMessage(" 获取登陆二维码图片");
            BufferedImage qr = loginService.getQR();
            codeLabel.setIcon(new ImageIcon(qr.getScaledInstance(250, 250, Image.SCALE_SMOOTH)));

            showMessage("请使用微信扫一扫以登录");

            loginService.login(new LoginService.LoginCallBack() {
                @Override
                public void CallBack(String loginInfo) {
                    showMessage(loginInfo);
                }

                @Override
                public void avatar(String avatarBase64) {
                    if (avatarBase64 == null) {
                        return;
                    }
                    try {
                        byte[] decode = Base64.getDecoder().decode(avatarBase64);
                        ByteArrayInputStream bais = new ByteArrayInputStream(decode);
                        BufferedImage read = ImageIO.read(bais);
                        codeLabel.setIcon(new ImageIcon(read));

                    } catch (IOException e) {
                        log.error(e.getMessage());
                        e.printStackTrace();
                    }

                }
            });
            refreshCodeBt.setVisible(false);
            //登录失败
            if (!Core.isAlive()) {
                return;
            }

            showMessage("登陆成功，微信初始化...");
            if (!loginService.webWxInit()) {
                showMessage(" 微信初始化异常");
                System.exit(0);
            }
            wechatConfiguration.setBasePath(wechatConfiguration.getBasePath() + File.separator + MD5Util.MD5(Core.getNickName())+ File.separator);


            log.info("开启微信状态通知");
            loginService.wxStatusNotify();

            //打开窗体
            log.info("登录成功");
            openMainFrame();

            //初始化聊天列表
            Set<String> recentContacts = Core.getRecentContacts();
            SwingUtilities.invokeLater(() -> {
                List<RoomItem> roomItems = recentContacts.stream()
                        .map(userId -> new RoomItem(Core.getMemberMap().get(userId), "", 0, false))
                        .collect(Collectors.toList());
                RoomsPanel.getContext().addRoom(roomItems);
            });


            new SwingWorker<Object, Object>() {

                @Override
                protected Object doInBackground() throws Exception {
                    log.info("获取联系人信息");
                    loginService.webWxGetContact();
                    return null;
                }

                @Override
                protected void done() {

                    log.info(" 开始接收消息");
                    loginService.startReceiving();
                    ContactsPanel.getContext().notifyDataSetChanged();

                    ExecutorServiceUtil.getGlobalExecutorService().submit(() -> {
                        log.info("9. 获取群好友及群好友列表");
                        loginService.WebWxBatchGetContact();
                        if (dHImg) {
                            downloadHeadImage();
                        }
                    });
                }
            }.execute();


        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
    }

    /**
     * 循环获取UUID
     */
    private void getUUID() {
        while (true) {
            log.info("1. 获取微信UUID");
            String uuid = loginService.getUuid();
            if (uuid != null) {
                break;
            }
            log.warn("1.1. 获取微信UUID失败，两秒后重新获取");
            SleepUtils.sleep(2000);
        }
    }

    /**
     * 注册截图快捷键
     */
    private void registerHotKey() {

        int SCREEN_SHOT_CODE = 10001;
        try {
            JIntellitype.getInstance().registerHotKey(SCREEN_SHOT_CODE, JIntellitype.MOD_ALT, 'S');

            JIntellitype.getInstance().addHotKeyListener(new HotkeyListener() {
                @Override
                public void onHotKey(int markCode) {
                    if (markCode == SCREEN_SHOT_CODE) {
                        screenShot();
                    }
                }
            });
        } catch (Exception e) {
            log.warn("注册截屏快捷键失败：" + e.getMessage());
        }
    }

    /**
     * 截图
     */
    private void screenShot() {
        ScreenShotFrame ssw = new ScreenShotFrame();
        ssw.setVisible(true);
    }

    /**
     * 下载头像
     */
    private void downloadHeadImage() {
        ExecutorServiceUtil.getHeadImageDownloadExecutorService().execute(() -> HeadImageUtil.deleteLoseEfficacyHeadImg(wechatConfiguration.getBasePath() + "/headimg/"));
        statusLabel.setText("11. 下载联系人头像");
        log.info("11. 下载联系人头像");
        Core.getMemberMap().entrySet().parallelStream()
                .forEach(contacts->{
                    Core.getContactHeadImgPath().put(contacts.getValue().getUsername(), DownloadTools.downloadBigHeadImg(contacts.getValue().getHeadimgurl(), contacts.getValue().getUsername()));
                    log.info("下载头像：({}):{}", contacts.getValue().getNickname(), contacts.getValue().getHeadimgurl());
                });
        /*  for (Map.Entry<String, Contacts> entry : Core.getMemberMap().entrySet()) {

          ExecutorServiceUtil.getHeadImageDownloadExecutorService().execute(

                    () -> {

                        Core.getContactHeadImgPath().put(entry.getValue().getUsername(), DownloadTools.downloadHeadImgBig(entry.getValue().getHeadimgurl(), entry.getValue().getUsername()));
                        log.info("下载头像：({}):{}", entry.getValue().getNickname(), entry.getValue().getHeadimgurl());
                    });

            ExecutorServiceUtil.getGlobalExecutorService().submit(new Runnable() {
                @Override
                public void run() {
                    AvatarUtil.putUserAvatarCache(entry.getValue().getUsername(), DownloadTools.downloadImage(entry.getValue().getHeadimgurl()));
                }
            });

        }*/
    }


}
