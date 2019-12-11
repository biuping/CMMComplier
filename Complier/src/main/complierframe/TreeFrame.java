package main.complierframe;

import main.parse.TreeNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TreeFrame extends JFrame {
    private static final int W=600;private static final int H=500;
    private JButton addBtn,delBtn;
    private DefaultTreeModel model;
    public TreeFrame( TreeNode treeNode) {
        model = new DefaultTreeModel(treeNode);
        setTitle("Tree");
        setSize(W, H);
        JTree tree = new JTree(model);
        tree.setShowsRootHandles(true);
        tree.setRootVisible(true);
        tree.setEditable(true);
        //tree.putClientProperty("JTree.lineStyle", "None");
        this.getContentPane().add(new JScrollPane(tree));
//        addBtn = new JButton("添加");
//        addBtn.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                DefaultMutableTreeNode select = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
//                if (select == null) return;
//                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) select.getParent();
//                if (parent == null) return;
//                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode("NewNode");
//                int selectedIndex = parent.getIndex(select);
//                model.insertNodeInto(newNode, parent, selectedIndex + 1);
//                //展开路径
//                TreeNode[] treeNode = (TreeNode[]) model.getPathToRoot(newNode);
//                TreePath treePath = new TreePath(treeNode);
//                tree.scrollPathToVisible(treePath);
//            }
//        });
//        this.getContentPane().add(addBtn, BorderLayout.WEST);
//        delBtn = new JButton("删除");
//        this.getContentPane().add(delBtn, BorderLayout.EAST);
//        delBtn.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//                DefaultMutableTreeNode select = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
//                if (select != null)
//                    model.removeNodeFromParent(select);
//            }
//        });
    }
}
