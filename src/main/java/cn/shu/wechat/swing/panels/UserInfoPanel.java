package cn.shu.wechat.swing.panels;


import cn.shu.wechat.api.ContactsTools;
import cn.shu.wechat.core.Core;
import cn.shu.wechat.pojo.entity.Contacts;
import cn.shu.wechat.swing.components.*;
import cn.shu.wechat.swing.utils.AvatarUtil;
import cn.shu.wechat.swing.utils.ChatUtil;
import cn.shu.wechat.swing.utils.FontUtil;
import cn.shu.wechat.swing.utils.IconUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Created by 舒新胜 on 2017/6/15.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class UserInfoPanel extends ParentAvailablePanel {
    private JPanel contentPanel = new JPanel();
    private RCButton button= new RCButton();
    private JLabel avatarLabel = new JLabel();
    private JLabel sexLabel = new JLabel();
    private UserInfoDetailItemLabel username = new UserInfoDetailItemLabel("ID");
    private UserInfoDetailItemLabel nickname= new UserInfoDetailItemLabel("昵称");
    private UserInfoDetailItemLabel remarkName= new UserInfoDetailItemLabel("备注");

    private UserInfoDetailItemLabel signature = new UserInfoDetailItemLabel("签名");
    private UserInfoDetailItemLabel region= new UserInfoDetailItemLabel("地区");
    private JPanel titlePanel = new TitlePanel(this);

    private volatile String currUserId;

    private static UserInfoPanel context;
    public UserInfoPanel(JPanel parent) {
        super(parent);
        UserInfoPanel.context = this;
        initComponents();
        initView();
        setListeners();
        setContacts(Core.getUserSelf());
    }
    public static UserInfoPanel getContext() {
        return context;
    }
    public void setContacts(Contacts contacts){
        currUserId = contacts.getUsername();
        avatarLabel.setIcon(IconUtil.getIcon(this,"/image/image_loading.gif"));
        new SwingWorker<Object,Object>(){
            Image orLoadBigAvatar = null;
            private final String userId = contacts.getUsername();
            @Override
            protected Object doInBackground() throws Exception {
                orLoadBigAvatar = AvatarUtil.createOrLoadBigAvatar(contacts.getUsername(), contacts.getHeadimgurl());
                if (orLoadBigAvatar != null){
                    orLoadBigAvatar = orLoadBigAvatar.getScaledInstance(200,200,Image.SCALE_SMOOTH);
                }

                return null;
            }

            @Override
            protected void done() {
                if (orLoadBigAvatar!=null && userId.equals(currUserId)){
                    avatarLabel.setIcon(new ImageIcon(orLoadBigAvatar));
                }
                super.done();
            }
        }.execute();
        if (contacts.getSex() == null){
            sexLabel.setIcon(null);
        }else{
            if (contacts.getSex() == 2) {
                ImageIcon icon = IconUtil.getIcon(this, "/image/woman.png");
                sexLabel.setIcon(icon);
            }else if (contacts.getSex() == 1) {
                ImageIcon icon = IconUtil.getIcon(this, "/image/man.png");
                sexLabel.setIcon(icon);
            }else {
                sexLabel.setIcon(null);
            }
        }


        username.setValue(contacts.getUsername()==null?"":contacts.getUsername());

        String contactNickNameByUserName = ContactsTools.getContactNickNameByUserName(contacts);
        nickname.setValue(contactNickNameByUserName == null?"":contactNickNameByUserName);

        String contactRemarkNameByUserName = ContactsTools.getContactRemarkNameByUserName(contacts);
        if (contactRemarkNameByUserName == null){
            contactRemarkNameByUserName = contacts.getDisplayname();
        }
        remarkName.setValue(contactRemarkNameByUserName == null?"":contactRemarkNameByUserName);


        String signatureNameOfGroup = ContactsTools.getSignatureNameOfGroup(contacts);
        signature.setValue(signatureNameOfGroup == null?"":signatureNameOfGroup);

        region.setValue((contacts.getProvince()==null?"":contacts.getProvince())
                +" "+(contacts.getCity()==null?"":contacts.getCity()));
    }
    private void initComponents() {
        contentPanel = new JPanel();
        contentPanel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.CENTER, 10, 20, true, false));


        username.setFont(FontUtil.getDefaultFont(20));
        avatarLabel.setPreferredSize(new Dimension(200,200));
        avatarLabel.setBackground(Color.GRAY);
        avatarLabel.setHorizontalAlignment(JLabel.CENTER);
        button = new RCButton("发消息", Colors.MAIN_COLOR, Colors.MAIN_COLOR_DARKER, Colors.MAIN_COLOR_DARKER);
        button.setBackground(Colors.PROGRESS_BAR_START);
        button.setPreferredSize(new Dimension(200, 40));
        button.setFont(FontUtil.getDefaultFont(16));
    }

    private void initView() {
        this.setLayout(new GridBagLayout());


        JPanel infoPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.CENTER, 0, 10, true, false));
        //infoPanel.add(username);
        nickname.add(sexLabel);
        infoPanel.add(nickname);
        infoPanel.add(remarkName);
        infoPanel.add(signature);
        infoPanel.add(region);

        JPanel avatarInfoPanel = new JPanel();
        avatarInfoPanel.setLayout(new BorderLayout( 15, 0));
        avatarInfoPanel.add(avatarLabel,BorderLayout.WEST);
        avatarInfoPanel.add(infoPanel,BorderLayout.CENTER);
        contentPanel.add(avatarInfoPanel);
        contentPanel.add(button);
        add(titlePanel, new GBC(0, 0).setWeight(1, 1).setFill(GBC.BOTH).setAnchor(GBC.CENTER).setInsets(0, 0, 0, 0));
        add(contentPanel, new GBC(0, 1).setWeight(1, 1000).setAnchor(GBC.CENTER)
                .setInsets(0, 0, 250, 0));
    }


    private void setListeners() {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ChatUtil.openOrCreateDirectChat(currUserId);
                super.mouseClicked(e);
            }
        });
    }

    static class UserInfoDetailItemLabel extends JPanel{
        private final JLabel  nameLabel = new JLabel ();

        private final SizeAutoAdjustTextArea  valueLabel = new SizeAutoAdjustTextArea (200);
        public UserInfoDetailItemLabel(String name,String value){
            setNameAndValue(name,value);
        }
        public UserInfoDetailItemLabel(String name){
            this(name,"");
            initView();
        }
        public void setNameAndValue(String name,String value){
            this.nameLabel.setText(name);
            setValue(value);
        }
        private void initView(){
            setLayout(new FlowLayout(FlowLayout.LEFT,20,0));
            nameLabel.setForeground(Color.GRAY);
            valueLabel.setForeground(Color.BLACK);
            valueLabel.setEditable(true);
            valueLabel.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {
                    System.out.println("keyTyped");
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    System.out.println("keyPressed");
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    System.out.println("keyReleased");
                }
            });
            add(nameLabel);
            add(valueLabel);
        }
        public void setValue(String value){
            this.valueLabel.setText(value);
        }
    }


}
