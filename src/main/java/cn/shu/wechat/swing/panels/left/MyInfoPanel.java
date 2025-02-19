package cn.shu.wechat.swing.panels.left;

import cn.shu.wechat.core.Core;
import cn.shu.wechat.pojo.entity.Contacts;
import cn.shu.wechat.swing.components.Colors;
import cn.shu.wechat.swing.components.GBC;
import cn.shu.wechat.swing.components.message.MainOperationPopupMenu;
import cn.shu.wechat.swing.frames.MainFrame;
import cn.shu.wechat.swing.frames.SystemConfigDialog;
import cn.shu.wechat.swing.listener.AbstractMouseListener;
import cn.shu.wechat.swing.panels.ParentAvailablePanel;
import cn.shu.wechat.swing.utils.AvatarUtil;
import cn.shu.wechat.swing.utils.FontUtil;
import cn.shu.wechat.swing.utils.IconUtil;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

/**
 * Created by 舒新胜 on 17-5-29.
 */
public class MyInfoPanel extends ParentAvailablePanel {
    private static MyInfoPanel context;

    private JLabel avatar;
    private JLabel username;
    private JLabel menuIcon;

    MainOperationPopupMenu mainOperationPopupMenu;


    public MyInfoPanel(JPanel parent) {
        super(parent);
        context = this;

        initComponents();
        setListeners();
        initView();
    }


    private void initComponents() {


        Contacts userSelf = Core.getUserSelf();
        String headImage = Core.getContactHeadImgPath().get(userSelf.getUsername());
        avatar = new JLabel();
        if (StringUtils.isEmpty(headImage)) {
            avatar.setIcon(AvatarUtil.createOrLoadUserAvatar(userSelf.getUsername()));
        } else {
            try {
                avatar.setIcon(new ImageIcon(ImageIO.read(new File(headImage)).getScaledInstance(50, 50, Image.SCALE_SMOOTH)));
            } catch (IOException e) {
                avatar.setIcon(AvatarUtil.createOrLoadUserAvatar(userSelf.getNickname()));
                e.printStackTrace();
            }

        }

        avatar.setPreferredSize(new Dimension(50, 50));
        avatar.setCursor(new Cursor(Cursor.HAND_CURSOR));


        username = new JLabel();
        username.setText(userSelf.getNickname());
        username.setFont(FontUtil.getDefaultFont(16));
        username.setForeground(Colors.DARK);


        menuIcon = new JLabel();
        menuIcon.setIcon(IconUtil.getIcon(this,"/image/options.png"));
        menuIcon.setForeground(Colors.DARK);
        menuIcon.setCursor(new Cursor(Cursor.HAND_CURSOR));


        mainOperationPopupMenu = new MainOperationPopupMenu();
    }

    private void setListeners() {
        menuIcon.addMouseListener(new AbstractMouseListener() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    Component component = e.getComponent();
                    mainOperationPopupMenu.show(component, -112, 50);
                    super.mouseClicked(e);
                }

            }
        });

        avatar.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    SystemConfigDialog dialog = new SystemConfigDialog(MainFrame.getContext(), true);
                    dialog.setVisible(true);
                    super.mouseClicked(e);
                }
            }
        });
    }

    private void initView() {
        this.setBackground(Colors.WINDOW_BACKGROUND);
        this.setLayout(new GridBagLayout());

        add(avatar, new GBC(0, 0).setFill(GBC.NONE).setWeight(2, 1));
        add(username, new GBC(1, 0).setFill(GBC.BOTH).setWeight(7, 1));
        add(menuIcon, new GBC(2, 0).setFill(GBC.BOTH).setWeight(1, 1));
    }

    public void reloadAvatar() {
        // currentUsername = currentUserService.findAll().get(0).getUsername();
        //Image image = AvatarUtil.createOrLoadAvatar(currentUsername);
        //avatar.setImage(image);
        avatar.setIcon(AvatarUtil.createOrLoadUserAvatar(Core.getUserSelf().getUsername()));


        avatar.revalidate();
        avatar.repaint();
    }

    public static MyInfoPanel getContext() {
        return context;
    }
}
