package xyz.etcd.manager.form;

import xyz.etcd.manager.common.DisplayUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class LoginForm {
    public LoginForm(){
//        JFrame.setDefaultLookAndFeelDecorated(true);
        JFrame df=new JFrame();
        df.setResizable(false);
        df.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        df.setTitle("ETCD连接");

        //获取屏幕的尺寸
        int frameWidth = 500;
        int frameHeight = 300;

        df.setBounds(DisplayUtil.getDefaultDisplayCenterRectangle(frameWidth,frameHeight));

        //构建一个Plane
        JPanel jPanel=new JPanel();
        jPanel.setLayout(null);
        jPanel.setBounds(0,0,frameWidth,frameHeight);
        df.add(jPanel);

        //标题
        JLabel label=new JLabel("ETCD地址：");
        label.setBounds(5,100,80,30);
        jPanel.add(label);

        //构建一个文本框
        JTextField linkUrl=new JFormattedTextField();
        linkUrl.setText("http://127.0.0.1:2379");
        int urlBoxWidth = 400;
        int urlBoxHeight = 50;
        linkUrl.setBounds(80,100,400,30);
        jPanel.add(linkUrl);

        JButton enter=new JButton("进入");
        enter.setBounds(220,140,80,30);
        jPanel.add(enter);
        enter.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                df.setVisible(false);

                //处理网址
                if (!linkUrl.getText().startsWith("http://")) {
                    linkUrl.setText("http://"+linkUrl.getText());
                }

                MainForm mainForm=new MainForm(linkUrl.getText());
            }
        });

        df.setVisible(true);

        //获取默认的ETCD配置

    }
}
