package xyz.etcd.manager.form;

import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.lease.LeaseTimeToLiveResponse;
import xyz.etcd.manager.common.DisplayUtil;
import xyz.etcd.manager.service.EtcdClient;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

public class MainForm {
    private String m_etcdUrlStr;
    EtcdClient m_etcdClient;
    Watch.Watcher m_watcher;

    JFrame df;
    JPanel tp;
    JPanel cp;
    JTree tree;
    JScrollPane treePanel;
    DefaultTreeModel defaultTreeModel;
    DefaultMutableTreeNode root;
    JLabel keyLable;
    JTextField keyText;
    JTextArea contentText;
    JScrollPane contentTextPanel;
    JLabel ttlLable;
    JTextField ttlText;
    JButton saveBtn;
    JButton delBtn;
    JButton batchAddBtn;

    public MainForm(String etctUrl) {
        m_etcdUrlStr = etctUrl;
        m_etcdClient=new EtcdClient(m_etcdUrlStr);

        df = new JFrame();
        df.setResizable(false);   //禁止调整大小
        df.setLayout(null);
        df.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        df.setTitle("ETCD管理工具("+ m_etcdUrlStr +")");
        Rectangle managerRectangle = DisplayUtil.getDefaultDisplayCenterRectangle(900, 700);
        df.setBounds(managerRectangle);

        tp=new JPanel();
        tp.setLayout(null);
        tp.setBounds(0,0,300,df.getHeight());
        cp=new JPanel();
        cp.setLayout(null);
        cp.setBounds(300,0,df.getWidth()-300,df.getHeight());

        df.add(tp);
        df.add(cp);

        //加载Tree
        root=new DefaultMutableTreeNode("/");
        tree = new JTree(root);
        defaultTreeModel=(DefaultTreeModel) tree.getModel();
        tree.setEditable(false);
        //tree.setBounds(0, 0, tp.getWidth(), tp.getHeight());
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                int selRow = tree.getRowForLocation(mouseEvent.getX(), mouseEvent.getY());
                TreePath selPath = tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
                if(selRow!=-1){
                    if(mouseEvent.getClickCount()==1){
                        tree.setSelectionRow(selRow);
                    }
                    else if(mouseEvent.getClickCount()==2){
                        String path="";
                        for (int i = 1; i < selPath.getPath().length; i++) {
                            path+="/"+selPath.getPath()[i];
                        }

                        //查找ETCD的值
                        try {
                            KeyValue keyValue = m_etcdClient.get(path);
                            if(keyValue!=null){
                                contentText.setText(keyValue.getValue().toString(StandardCharsets.UTF_8));
                                //ttlText.setText(String.valueOf(keyValue.getLease()));
                                keyText.setText(path);

                                //查找ttl
                                LeaseTimeToLiveResponse lease = m_etcdClient.getLease(path);

                                String ttlStr="0";
                                if(lease.getTTl()>0){
                                    ttlStr=String.valueOf(lease.getTTl());
                                }

                                ttlText.setText(ttlStr);
                            }
                        }
                        catch (java.util.concurrent.ExecutionException exception){
                            if(exception.getMessage().contains("key is not provided")){
                                //忽略没找到键的查询异常
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        treePanel=new JScrollPane();
        treePanel.setViewportView(tree);
        treePanel.setBounds(0, 0, tp.getWidth(), tp.getHeight()-36);
        tp.add(treePanel);

        //添加或编辑的Key
        keyLable=new JLabel("KEY:");
        keyLable.setBounds(10,5,40,30);
        keyText=new JFormattedTextField();
        keyText.setBounds(40,5,540,30);
        cp.add(keyText);
        cp.add(keyLable);

        //内容
        contentTextPanel =new JScrollPane();
        contentTextPanel.setBounds(10,45,570,570);
        contentText=new JTextArea();
        contentText.setLineWrap(true);  //换行
        contentTextPanel.setViewportView(contentText);
        cp.add(contentTextPanel);

        //TTL文本框
        int ttly=45+contentTextPanel.getHeight()+10;
        ttlLable=new JLabel("TTL:");
        ttlLable.setBounds(10,ttly,40,30);
        ttlText=new JFormattedTextField();
        ttlText.setBounds(40,ttly,80,30);
        ttlText.setText("0");
        cp.add(ttlLable);
        cp.add(ttlText);

        //操作按钮
        int savey=45+contentTextPanel.getHeight()+10;
        saveBtn=new JButton("保存");
        saveBtn.setBounds(520,savey,60,30);
        saveBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if(!keyText.getText().startsWith("/")){
                    return;
                }

                int ttl=0;
                try {
                    ttl=Integer.valueOf(ttlText.getText());
                } catch (NumberFormatException e) {
                    ttlText.setText("0");
                    e.printStackTrace();
                }
                String key=keyText.getText();
                String value=contentText.getText();
                if(!key.equals("")){
                    try {
                        m_etcdClient.set(key,value,ttl);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                JOptionPane.showMessageDialog(null,"保存成功","提示",JOptionPane.INFORMATION_MESSAGE);
            }
        });
        cp.add(saveBtn);

        delBtn=new JButton("删除");
        delBtn.setBounds(450,savey,60,30);
        delBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if(!keyText.getText().startsWith("/")){
                    return;
                }
                String key=keyText.getText();

                String error="";

                try {
                    KeyValue keyValue = m_etcdClient.get(key);
                    if(keyValue==null){
                        error="没有在ETCD中找到该Key，可能ETCD中的节点已经过期。";
                    }
                } catch (Exception e) {
                    error=e.getMessage();
                }

                if(error.equals("")) {
                    if (!key.equals("")) {
                        try {
                            //无刷新操作树
                            updateTree(() -> {
                                m_etcdClient.del(key);
                                keyText.setText("");
                                contentText.setText("");
                                ttlText.setText("0");
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                if(error.equals("")) {
                    JOptionPane.showMessageDialog(null, "删除成功", "提示", JOptionPane.INFORMATION_MESSAGE);
                }
                else
                {
                    JOptionPane.showMessageDialog(null, error, "提示", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        cp.add(delBtn);

        //批量导入按钮
        int batchx = 450 - 120 - 10;
        batchAddBtn=new JButton("批量导入");
        batchAddBtn.setBounds(batchx,savey,120,30);
        batchAddBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Frame f=new JFrame();
                FileDialog openD=new FileDialog(f,"打开",FileDialog.LOAD);
                openD.setVisible(true);

                BufferedReader in=null;
                try {
                    in =new BufferedReader(new FileReader((openD.getDirectory()+openD.getFile())));
                    List<String> inStrList=new ArrayList<>();
                    String lineStr=null;
                    while ((lineStr=in.readLine())!=null){
                        inStrList.add(lineStr);
                    }

                    BatchAddWithText(inStrList);

                    in.close();
                    JOptionPane.showMessageDialog(null, "批量导入成功", "提示", JOptionPane.INFORMATION_MESSAGE);
                } catch (FileNotFoundException e) {
                    //e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        cp.add(batchAddBtn);

        df.setVisible(true);

        //初始化所有节点
        initTree();

        //发起对tree的监听
        Watch.Listener listener=Watch.listener(watchResponse -> {
            updateTree(()->{
                //直接重新加载比较容易;
                initTree();
            });
        });

        try {
            m_watcher=m_etcdClient.watch("/",listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void BatchAddWithText(List<String> stringList){
        if(stringList==null || stringList.size()<=0){
            return;
        }

        String prefix="";
        //解析前缀为：行为-prefix:开头的
        final String PREFIX_COUST="--prefix:";
        for (String s : stringList) {
            if(s.startsWith(PREFIX_COUST)){
                prefix=s.substring(PREFIX_COUST.length());
                continue;
            }

            if(s.startsWith("#") || s=="" || s.trim()=="" || !s.contains("=")){
                continue;
            }

            //暂时不支持TTL导入逻辑，因为不知道怎么定义格式比较优雅...
            int ttl=0;
            String[] splits=new String[2];
            String key = s.substring(0,s.indexOf("=")).trim();
            String value = s.substring(s.indexOf("=")+1).trim();
            try {
                if(key==""){
                    continue;
                }
                m_etcdClient.set(prefix+key,value,ttl);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void initTree() {
        try {
            root.removeAllChildren();
            List<KeyValue> range = m_etcdClient.getRange("/");
            if(range!=null){
                for (KeyValue keyValue : range) {
                    String key = keyValue.getKey().toString(StandardCharsets.UTF_8);
                    AddOneKey(key);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //增加树中的某个键
    private void AddOneKey(String key) {
        String[] split = key.split("/");
        TreeNode index=(TreeNode)tree.getModel().getRoot();
        for (int i = 1; i < split.length; i++) {
            index = AddTreeNode(split[i],index);
        }
    }

    private TreeNode AddTreeNode(String node,TreeNode parent){
        if(parent!=null){
            if(parent.getChildCount()>0){
                Enumeration children = parent.children();
                while(children.hasMoreElements()){
                    Object o = children.nextElement();
                    if(o.toString().equals(node)){
                        return (TreeNode) o;
                    }
                }
            }

            DefaultMutableTreeNode current = (DefaultMutableTreeNode) parent;
            DefaultMutableTreeNode addNode = new DefaultMutableTreeNode(node);
            current.add(addNode);
            return addNode;
        }

        return null;
    }

    /**  保持刷新前的树状态  */
    public interface Action{
        void action();
    }

    public void updateTree(Action action){
        Vector<TreePath> v=new Vector<TreePath>();
        getExpandNode(root, v);

        action.action();

        defaultTreeModel.reload();

        int n=v.size();
        for(int i=0;i<n;i++){
            Object[] objArr=v.get(i).getPath();
            Vector<Object> vec=new Vector<Object>();
            int len=objArr.length;
            for(int j=0;j<len;j++){
                vec.add(objArr[j]);
            }
            expandNode(tree,root,vec);
        }
    }

    public Vector<TreePath> getExpandNode(TreeNode node, Vector<TreePath> v){
        if (node.getChildCount() > 0) {
            TreePath treePath=new TreePath(defaultTreeModel.getPathToRoot(node));
            if(tree.isExpanded(treePath)) v.add(treePath);
            for (Enumeration e=node.children(); e.hasMoreElements(); ) {
                TreeNode n = (TreeNode)e.nextElement();
                getExpandNode(n,v);
            }
        }
        return v;
    }

    void expandNode(JTree myTree,DefaultMutableTreeNode currNode, Vector<Object> vNode){
        if(currNode.getParent()==null){
            vNode.removeElementAt(0);
        }
        if(vNode.size()<=0) return;

        int childCount = currNode.getChildCount();
        String strNode = vNode.elementAt(0).toString();
        DefaultMutableTreeNode child = null;
        boolean flag=false;
        for(int i=0; i<childCount; i++){
            child = (DefaultMutableTreeNode)currNode.getChildAt(i);
            if(strNode.equals(child.toString())){
                flag=true;
                break;
            }
        }
        if(child != null&&flag){
            vNode.removeElementAt(0);
            if(vNode.size()>0){
                expandNode(myTree,child, vNode);
            }else{
                myTree.expandPath(new TreePath(child.getPath()));
            }
        }
    }
    /**  状态保持结束  */
}
