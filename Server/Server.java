package Server;

import java.net.ServerSocket;
import java.net.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class Server {
    private ServerSocket s;
    private static HashMap<String, ClientHandler> serverThread;

    private static DefaultListModel<String> listModel;
    private static JList<String> list_client;
    private static JScrollPane scpClients;
    private static JTextArea txaLogs;
    private static JTextField txfDirectory;
    private JButton btn_start;
    private JPanel bCards;
    final static String HOMEPANEL = "HomePanel";
    final static String MAINPANEL = "MainPanel";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    public void addComponentToPane(Container pane) {

        JPanel homeCard = new JPanel();
        JPanel mainCard = new JPanel(new BorderLayout());

        JPanel functionPane = new JPanel(new BorderLayout());
        JPanel botPane = new JPanel();
        JLabel lbBottomText = new JLabel("File Monitoring by 20127605");
        lbBottomText.setHorizontalAlignment(JLabel.CENTER);
        botPane.add(lbBottomText);

        JLabel lbTitle = new JLabel("SERVER CONTROL PANEL");
        lbTitle.setHorizontalAlignment(JLabel.CENTER);
        lbTitle.setFont(new Font("MV Boli", Font.PLAIN, 35));
        lbTitle.setHorizontalAlignment(JLabel.CENTER);

        JPanel logsPane = new JPanel(new BorderLayout());
        logsPane.setBorder(new EmptyBorder(0, 10, 0, 10));

        JScrollPane scpLogs = new JScrollPane(txaLogs);
        txaLogs.setEditable(false);
        txaLogs.setLineWrap(true);
        txaLogs.setWrapStyleWord(true);

        JTextField txfChatBox = new JTextField(20);

        scpClients.setPreferredSize(new Dimension(150, 400));
        scpClients.setMaximumSize(scpClients.getPreferredSize());

        homeCard.add(btn_start, BorderLayout.CENTER);

        JLabel lbLogsTitle = new JLabel("LOGS");
        lbLogsTitle.setHorizontalAlignment(JLabel.CENTER);

        logsPane.add(lbLogsTitle, BorderLayout.NORTH);
        logsPane.add(scpLogs, BorderLayout.CENTER);
        JPanel chatPane = new JPanel();
        JButton btnSend = new JButton("SEND");
        btnSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    // Get selected client username
                    String currentSelectedClient = list_client.getSelectedValue();
                    if (currentSelectedClient == null) {
                        String msg = "Please select a client to chat from the list";
                        JOptionPane.showMessageDialog(btnSend, msg, "ERROR", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    ClientHandler clh = serverThread.get(currentSelectedClient);
                    if (clh == null) {
                        // No client
                        return;
                    }
                    DataOutputStream dos = clh.getDOS();
                    dos.writeUTF("CHAT");

                    String msg = txfChatBox.getText();
                    dos.writeUTF(msg);
                    dos.flush();
                    txfChatBox.setText("");

                    // Update text area
                    txaLogs.setText(txaLogs.getText() + "Server send to " + clh.getUsername() + ": " + msg + "\n");

                } catch (IOException exc) {
                    exc.getStackTrace();
                }
            }
        });

        chatPane.add(txfChatBox);
        chatPane.add(btnSend);
        logsPane.add(chatPane, BorderLayout.SOUTH);

        JPanel upperPane = new JPanel();

        JPanel browsePane = new JPanel();
        browsePane.setBackground(Color.WHITE);

        JLabel lbMonitoring = new JLabel("Monitoring directory:");
        JButton btnBrowse = new JButton("Choose dir");

        txfDirectory.setEditable(false);

        btnBrowse.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                JFrame browseFrame = new JFrame("ServerControlPanel");
                browseFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                browseFrame.setResizable(false);

                // JPanel browsePanel = new JPanel(new BorderLayout());
                JLabel lbBrowseTitle = new JLabel("CHOOSE A DIRECTORY FOR MONITORING");
                lbBrowseTitle.setHorizontalAlignment(JLabel.CENTER);

                String currentSelectedClient = list_client.getSelectedValue();
                if (currentSelectedClient == null) {
                    String msg = "Please select a client to monitor from the list";
                    JOptionPane.showMessageDialog(btnBrowse, msg, "ERROR", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                ClientHandler clh = serverThread.get(currentSelectedClient);
                JTree cdTree = clh.getDirectoryTree();

                JScrollPane scpDirectory = new JScrollPane();
                scpDirectory = new JScrollPane(cdTree);
                scpDirectory.setPreferredSize(new Dimension(400, 300));
                scpDirectory.setMaximumSize(scpDirectory.getPreferredSize());

                JButton btnChoose = new JButton("CHOOSE");
                btnChoose.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        DefaultMutableTreeNode selectedElement = (DefaultMutableTreeNode) cdTree.getSelectionPath()
                                .getLastPathComponent();
                        String selectedDir = ((File) selectedElement.getUserObject()).getAbsolutePath();
                        txfDirectory.setText(selectedDir);
                        txaLogs.setText(txaLogs.getText() + "CHANGE_DIRECTORY: Monitoring " + selectedDir
                                + " from \"" + clh.getUsername() + "\"\n");
                        try {
                            DataOutputStream clhDOS = clh.getDOS();
                            clhDOS.writeUTF("WATCHING");
                            clhDOS.writeUTF(selectedDir);
                        } catch (IOException exc) {
                            exc.printStackTrace();
                        }
                    }
                });

                browseFrame.add(lbBrowseTitle, BorderLayout.NORTH);
                browseFrame.add(scpDirectory, BorderLayout.CENTER);
                browseFrame.add(btnChoose, BorderLayout.SOUTH);

                browseFrame.pack();
                browseFrame.setVisible(true);
            }
        });

        upperPane.add(lbMonitoring);
        browsePane.add(btnBrowse);
        browsePane.add(txfDirectory);
        upperPane.add(browsePane);

        JPanel listClientPane = new JPanel(new BorderLayout());
        JLabel lbListTitle = new JLabel("AVAILABLE CLIENT(S)");
        lbListTitle.setHorizontalAlignment(JLabel.CENTER);
        listClientPane.add(lbListTitle, BorderLayout.NORTH);
        listClientPane.add(scpClients, BorderLayout.CENTER);
        functionPane.add(listClientPane, BorderLayout.WEST);
        functionPane.add(logsPane, BorderLayout.CENTER);
        functionPane.add(upperPane, BorderLayout.NORTH);

        mainCard.add(functionPane, BorderLayout.CENTER);

        // Initialize CardLayout
        bCards = new JPanel(new CardLayout());
        bCards.add(homeCard, HOMEPANEL);
        bCards.add(mainCard, MAINPANEL);

        pane.add(lbTitle, BorderLayout.NORTH);
        pane.add(bCards, BorderLayout.CENTER);
        pane.add(botPane, BorderLayout.SOUTH);

        txfChatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                btnSend.doClick();
            }
        });

    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("ServerControlPanel");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        ServerProgram serverprog = new ServerProgram();
        serverprog.addComponentToPane(frame.getContentPane());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                // Send server disconnect command
                try {
                    for (String username : serverThread.keySet()) {
                        ClientHandler clh = serverThread.get(username);
                        clh.getDOS().writeUTF("DISCONNECT");
                    }
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        });

        frame.pack();
        frame.setVisible(true);

        CardLayout cl = (CardLayout) (serverprog.bCards.getLayout());
        cl.show(serverprog.bCards, HOMEPANEL);

        serverprog.btn_start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    serverprog.ssck = new ServerSocket(3000);

                    // running new thread to handle connecting socket
                    new Thread() {
                        public void run() {
                            while (true) {
                                try {
                                    // Wating for client to connect
                                    Socket sck = serverprog.ssck.accept();

                                    ClientHandler clh = serverprog.new ClientHandler(sck);

                                    String clientUsername = clh.getUsername();

                                    // Suppose client usernames are unique
                                    serverThread.put(clientUsername, clh);
                                    listModel.addElement(clientUsername);
                                    clh.start();

                                } catch (IOException exc) {
                                    exc.printStackTrace();
                                }
                            }
                        }
                    }.start();

                    CardLayout cl = (CardLayout) (serverprog.bCards.getLayout());
                    cl.show(serverprog.bCards, MAINPANEL);

                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        });
    }

    public ServerProgram() {
}
