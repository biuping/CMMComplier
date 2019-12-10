package main.complierframe;

import main.TreeFrame;
import main.lexer.Error;
import main.lexer.Lexer;
import main.lexer.Token;
import main.parse.PError;
import main.parse.Parse;
import main.parse.TreeNode;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.event.*;
import java.io.*;
import java.awt.*;
import java.util.ArrayList;


public class ComplierFrame extends JFrame {

    private ArrayList<Token> tokens;
    private int lexerErrorCount = 0;

    private static JTextArea editPane;
    private final static JMenuBar MENUBAR = new JMenuBar();
    private final static JToolBar TOOLBAR = new JToolBar();

    private JTabbedPane tabbedPanel;
    public static JTextPane consoleArea = new JTextPane();
    /* 控制台和错误信息 */
    public static JTabbedPane proAndConPanel;
    /* 错误显示区 */
    public static JTextArea problemArea = new JTextArea();
    private FileDialog filedialog_save, filedialog_load;

    /* 编辑区字体 */
    private final static Font font = new Font("Courier New", Font.PLAIN, 15);
    private final static Font conAndErrFont = new Font("微软雅黑", Font.PLAIN, 14);
    private final static Font treeFont = new Font("微软雅黑", Font.PLAIN, 18);
    private final static Font lexerFont = new Font("黑体",Font.PLAIN,15);
    private final static Font LABELFONT = new Font("幼圆", Font.BOLD, 13);

    private JMenu fileMenu;
    private JMenu runMenu;
    private static FileDialog openDia;
    private FileDialog saveDia;// 定义"打开 保存"对话框
    private static File file;//定义文件

    private static JMenuItem openItem;
    private JMenuItem saveItem;
    private JMenuItem lexItem;
    private JMenuItem parseItem;
    private JMenuItem runItem;

    private JButton newButton;
    private JButton openButton;
    private JButton saveButton;
    private JButton runButton;
    private JButton lexButton;
    private JButton parseButton;

    public ComplierFrame(String title){
        super();
        setLayout(null);
        setTitle(title);
        setJMenuBar(MENUBAR);

        // 初始化菜单项
        fileMenu = new JMenu("文件(F)");
        runMenu = new JMenu("运行(R)");

        MENUBAR.add(fileMenu);
        MENUBAR.add(runMenu);

        openItem = new JMenuItem("打 开", new ImageIcon(getClass().getResource(
                "/open.png")));
        saveItem = new JMenuItem("保 存", new ImageIcon(getClass().getResource(
                "/save.png")));

        fileMenu.add(openItem);
        fileMenu.add(saveItem);

        openDia = new FileDialog(this, "打开", FileDialog.LOAD);
        saveDia = new FileDialog(this, "保存", FileDialog.SAVE);

        lexItem = new JMenuItem("词法分析", new ImageIcon(getClass().getResource(
                "/lex.png")));
        parseItem = new JMenuItem("语法分析", new ImageIcon(getClass().getResource(
                "/parse.png")));
        runItem = new JMenuItem("运    行", new ImageIcon(getClass().getResource(
                "/run.png")));
        runMenu.add(lexItem);
        runMenu.add(parseItem);
        runMenu.addSeparator();
        runMenu.add(runItem);

        newButton = new JButton(new ImageIcon(getClass().getResource(
                "/new.png")));
        newButton.setToolTipText("新建");
        openButton = new JButton(new ImageIcon(getClass().getResource(
                "/open.png")));
        openButton.setToolTipText("打开");
        saveButton = new JButton(new ImageIcon(getClass().getResource(
                "/save.png")));
        saveButton.setToolTipText("保存");
        lexButton = new JButton(new ImageIcon(getClass().getResource(
                "/lex.png")));
        lexButton.setToolTipText("词法分析");
        parseButton = new JButton(new ImageIcon(getClass().getResource(
                "/parse.png")));
        parseButton.setToolTipText("语法分析");
        runButton = new JButton(new ImageIcon(getClass().getResource(
                "/run.png")));
        runButton.setToolTipText("运行");

        TOOLBAR.setFloatable(false);
        TOOLBAR.add(newButton);
        TOOLBAR.add(openButton);
        TOOLBAR.add(saveButton);
        TOOLBAR.addSeparator();
        TOOLBAR.addSeparator();
        TOOLBAR.addSeparator();
        TOOLBAR.addSeparator();
        TOOLBAR.add(lexButton);
        TOOLBAR.add(parseButton);
        TOOLBAR.add(runButton);
        add(TOOLBAR);
        TOOLBAR.setBounds(0, 0, 1240, 33);
        TOOLBAR.setPreferredSize(getPreferredSize());

        filedialog_save = new FileDialog(this, "保存文件", FileDialog.SAVE);
        filedialog_save.setVisible(false);
        filedialog_load = new FileDialog(this, "打开文件", FileDialog.LOAD);
        filedialog_load.setVisible(false);
        filedialog_save.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                filedialog_save.setVisible(false);
            }
        });
        filedialog_load.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                filedialog_load.setVisible(false);
            }
        });

        editPane = new JTextArea();
        editPane.setFont(treeFont);
        JPanel editPanel = new JPanel(null);
        editPanel.setBackground(getBackground());
        editPanel.setForeground(new Color(250, 230, 192));
        JLabel editLabel = new JLabel("|CMM程序文本编辑区");
        JPanel editLabelPanel = new JPanel(new BorderLayout());
        editLabel.setFont(LABELFONT);
        editLabelPanel.add(editLabel, BorderLayout.WEST);
        editLabelPanel.setBackground(Color.LIGHT_GRAY);

        // 控制条和错误列表区
        consoleArea.setEditable(false);
        problemArea.setRows(6);
        problemArea.setEditable(false);
        consoleArea.setFont(font);
        problemArea.setFont(conAndErrFont);
        proAndConPanel = new JTabbedPane();
        proAndConPanel.setFont(treeFont);
        proAndConPanel.add(new JScrollPane(consoleArea), "控制台");
        proAndConPanel.add(new JScrollPane(problemArea), "错误列表");

        editPanel.add(editLabelPanel);
        editPanel.add(editPane);
        editPanel.add(proAndConPanel);
        editLabelPanel.setBounds(0, 0, 815, 15);
        editPane.setBounds(0, 15, 815, 462);
        proAndConPanel.setBounds(0, 475, 815, 160);
        add(editPanel);
        editPanel.setBounds(20, TOOLBAR.getHeight(), 815,
                768 - TOOLBAR.getHeight() - 5 - 98);

        // 词法分析结果显示区
        JScrollPane lexerPanel = new JScrollPane(null);
        JScrollPane parserPanel = new JScrollPane(null);
        tabbedPanel = new JTabbedPane(JTabbedPane.TOP,
                JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPanel.setFont(treeFont);
        tabbedPanel.add(lexerPanel, "词法分析");
        tabbedPanel.add(parserPanel, "语法分析");
        JPanel resultPanel = new JPanel(new BorderLayout());
        JLabel resultLabel = new JLabel("|分析结果显示区");
        JPanel resultLabelPanel = new JPanel(new BorderLayout());
        resultLabel.setFont(LABELFONT);
        resultLabelPanel.add(resultLabel, BorderLayout.WEST);
        resultLabelPanel.setBackground(Color.LIGHT_GRAY);
        resultPanel.add(resultLabelPanel, BorderLayout.NORTH);
        resultPanel.add(tabbedPanel, BorderLayout.CENTER);
        add(resultPanel);
        resultPanel.setBounds(20 + editPanel.getWidth(),
                TOOLBAR.getHeight(), 1200 - 20
                        - editPanel.getWidth() + 38, 768 - TOOLBAR.getHeight()
                        - 5 - 98);


        myEvent();

    }

    private  void myEvent(){
        openItem.addActionListener(e -> open());

        saveItem.addActionListener(e -> save());

        openButton.addActionListener(e -> open());

        saveButton.addActionListener(e -> save());

        lexButton.addActionListener(e->lexerRun());

        lexItem.addActionListener(e->lexerRun());

        parseButton.addActionListener(e->parseRun());

        parseItem.addActionListener(e->parseRun());

    }

    //打开文件
    private void open(){
        openDia.setVisible(true); //显得打开文件对话框
        String dirpath = openDia.getDirectory();//获取打开文件路径并且保存
        String fileName = openDia.getFile();//获取文件名并且保存

        if(dirpath == null ||  fileName == null) //判断路径和文件是否为空
            return ;
        else
            editPane.setText(null); //文件不为空 清楚原来文件内容
        file = new File(dirpath, fileName); //创建新的路径和名称
        try {
            BufferedReader bufr = new BufferedReader(new FileReader(file));//尝试从文件中读东西
            String line = null; //变量字符串初始化为空
            while((line = bufr.readLine())!= null)
                editPane.append(line+"\r\n"); //显示每一行内容
            bufr.close();//关闭文件

        }catch(FileNotFoundException e1) {
            e1.printStackTrace(); // 抛出文件路径找不到异常
        }catch(IOException e2){
            e2.printStackTrace();// 抛出IO异常
        }
    }

    //保存文件
    private void save(){
        if (file == null) {
            saveDia.setVisible(true);//显示保存文件对话框
            String dirpath = saveDia.getDirectory();//获取保存文件路径并保存到字符串中。
            String fileName = saveDia.getFile();////获取打保存文件名称并保存到字符串中

            if (dirpath == null || fileName == null)//判断路径和文件是否为空
                return;//空操作
            else
                file=new File(dirpath,fileName);//文件不为空，新建一个路径和名称
        }
        try {
            BufferedWriter bufw = new BufferedWriter(new FileWriter(file));
            String text = editPane.getText();//获取文本内容
            bufw.write(text);//将获取文本内容写入到字符输出流
            bufw.close();//关闭文件
        }catch (IOException e1) {
            e1.printStackTrace();//抛出IO异常
        }
    }

    //词法分析
    private void lexerRun(){
        tokens=null;
        lexerErrorCount=0;
        problemArea.setText("");
        String line = editPane.getText();
        String[] lines = line.split("\n");
        Lexer lexer = new Lexer();
        for (int i=0;i<lines.length;i++){
            lexer.analyze(lines[i],i+1);
        }
        tokens = lexer.getTokens();
        JTextArea textArea = new JTextArea();
        textArea.setFont(lexerFont);
        textArea.setBackground(new Color(45, 57, 73));
        for (Token token : tokens) {
            textArea.append(token.toString() + "\n");
        }
        tabbedPanel.setComponentAt(0,new JScrollPane(textArea));
        textArea.setEnabled(false);
        if (lexer.getErrors().size()>0){
            lexerErrorCount = lexer.getErrors().size();
            ArrayList<Error> errors = lexer.getErrors();
            problemArea.setText("**********词法分析出现"+errors.size()+"个错误**********\n");
            JOptionPane.showMessageDialog(null, "词法分析出现错误", "Error", JOptionPane.ERROR_MESSAGE);
            for (Error error : errors) {
                problemArea.append(error.toString() + "\n");
            }
        }

    }

    //语法分析
    private void parseRun(){
        if (tokens.isEmpty()){
            JOptionPane.showMessageDialog(null, "请先进行词法分析",
                    "提示", JOptionPane.INFORMATION_MESSAGE);
        }else if (lexerErrorCount>0){
            JOptionPane.showMessageDialog(null, "请先解决词法分析错误",
                    "提示", JOptionPane.WARNING_MESSAGE);
        }
        else {
            Parse parse = new Parse(tokens);
            TreeNode root = parse.runParse();
            DefaultTreeModel model = new DefaultTreeModel(root);
            JTree parserTree = new JTree(model);
            // 设置该JTree使用自定义的节点绘制器
            parserTree.setCellRenderer(new JTreeRenderer());

            // 设置是否显示根节点的“展开/折叠”图标,默认是false
            parserTree.setShowsRootHandles(true);
            // 设置节点是否可见,默认是true
            parserTree.setRootVisible(true);
            parserTree.setBackground(new Color(97, 157, 226));
            tabbedPanel.setComponentAt(1,new JScrollPane(parserTree));
            if (parse.getErrorCount()>0){
                ArrayList<PError> errors = parse.getErrors();
                problemArea.setText("**********语法分析出现"+errors.size()+"个错误**********\n");
                JOptionPane.showMessageDialog(null, "语法分析出现错误", "Error", JOptionPane.ERROR_MESSAGE);
                for (PError error : errors) {
                    problemArea.append(error.toString() + "\n");
                }
            }
        }

    }

    public static void main(String args[]){
        ComplierFrame frame = new ComplierFrame("CMM解释器");
        frame.setBounds(60, 0, 1240, 742);
        frame.setResizable(false);
        frame.setVisible(true);

    }
}

