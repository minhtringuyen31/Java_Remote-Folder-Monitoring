package Client;

import java.io.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.border.CompoundBorder;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.nio.file.AccessDeniedException;
import java.nio.file.Paths;

public class Client {
    private String host;
    private int port;
    private static String deviceClientName;
    private Socket s;
    private DataInputStream dis;
    private DataOutputStream dos;
    private JPanel cards;
    private static Boolean isConnection = false;

    private JTree treeDirectory;
    private FolderMonitor watcher = null;
    private Thread watcherThread = null;
    private static String watcherMessage = "";

    public void specifyConnectInfo(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void specifyName(String name) {
        this.deviceClientName = name;
    }

    private static String clientTitle = "CONNECT TO SERVER";
    private static JLabel connectLabel;
    private static JPanel mainScreen;
    private static JPanel connectScreen;
    private static JPanel logPanel;
    private static JLabel nameLabel;
    private static JLabel ipLabel;
    private static JLabel portLabel;
    private static JTextField nameTextField;
    private static JTextField ipTextField;
    private static JTextField portTextField;
    private static JButton connectButton;

    private static JLabel deviceClientLabel;
    private static JPanel clientTitlePaneL;
    private static JPanel tablePanel;
    private static JPanel botPane;
    private static JLabel clientTitleLabel;
    private static JScrollPane tableScrollPanel;
    private static JTextArea messageContent = new JTextArea(10, 30);

    private static JButton disconnectButton = new JButton("DISCONNECT");
    final static String CONNECTPANEL = "CONNECTPANEL";
    final static String MAINPANEL = "MAINPANEL";

    public void addComponentToPane(Container pane) {

        mainScreen = new JPanel(new BorderLayout());
        connectScreen = new JPanel(new BorderLayout());

        // CONNECTPANEL
        connectLabel = new JLabel(clientTitle);
        connectLabel.setHorizontalAlignment(JLabel.CENTER);
        connectLabel.setFont(new Font("Serif", Font.BOLD, 30));
        connectScreen.add(connectLabel, BorderLayout.NORTH);

        GridLayout gridLayout = new GridLayout(3, 2);
        gridLayout.setHgap(10);
        gridLayout.setVgap(10);
        // Start logPanel
        logPanel = new JPanel(gridLayout);
        logPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        nameLabel = new JLabel("Client Name");
        ipLabel = new JLabel("IP");
        portLabel = new JLabel("Port");
        nameTextField = new JTextField();
        ipTextField = new JTextField();
        portTextField = new JTextField();
        connectButton = new JButton("Connect");

        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                specifyName(nameTextField.getText());
                specifyConnectInfo(ipTextField.getText(), Integer.parseInt(portTextField.getText()));
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        try {
                            Connect();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(null, "Connect Failure!", "ERROR", JOptionPane.ERROR_MESSAGE);

                        }
                    }
                };
                t.start();
            }
        });

        logPanel.add(nameLabel);
        logPanel.add(nameTextField);
        logPanel.add(ipLabel);
        logPanel.add(ipTextField);
        logPanel.add(portLabel);
        logPanel.add(portTextField);

        connectScreen.add(logPanel, BorderLayout.CENTER);
        connectScreen.add(connectButton, BorderLayout.SOUTH);

        // main screen
        clientTitlePaneL = new JPanel();
        botPane = new JPanel(new BorderLayout());
        // start title panel
        clientTitleLabel = new JLabel(clientTitle);
        clientTitleLabel.setHorizontalAlignment(JLabel.CENTER);
        clientTitleLabel.setFont(new Font("Serif", Font.BOLD, 30));
        pane.add(clientTitleLabel, BorderLayout.NORTH);
        // end title panel

        // start table panel
        deviceClientLabel = new JLabel(deviceClientName);
        messageContent = new JTextArea(5, 50);
        tableScrollPanel = new JScrollPane(messageContent);
        // messageContent.setPreferredSize(new Dimension(800, 500));
        // messageContent.setMaximumSize(messageContent.getPreferredSize());
        messageContent.setEditable(false);

        tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(new EmptyBorder(0, 20, 0, 20));
        tablePanel.setBorder(new CompoundBorder(new TitledBorder("Message Content"), new EmptyBorder(5, 5, 5, 5)));
        tablePanel.add(tableScrollPanel, BorderLayout.CENTER);
        // end table panel

        disconnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    dos.writeUTF("DISCONNECT");
                    isConnection = false;
                    messageContent.setText(null);
                    CardLayout cl = (CardLayout) (cards.getLayout());
                    cl.show(cards, CONNECTPANEL);
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        });

        clientTitlePaneL.add(clientTitleLabel);
        tablePanel.add(deviceClientLabel);
        tablePanel.add(tableScrollPanel);

        botPane.add(disconnectButton, BorderLayout.CENTER);

        mainScreen.add(clientTitlePaneL, BorderLayout.NORTH);
        mainScreen.add(tablePanel, BorderLayout.CENTER);
        mainScreen.add(botPane, BorderLayout.SOUTH);

        cards = new JPanel(new CardLayout());
        cards.add(connectScreen, CONNECTPANEL);
        cards.add(mainScreen, MAINPANEL);

        pane.add(cards);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Client" + deviceClientName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        Client client = new Client();
        client.addComponentToPane(frame.getContentPane());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                // Do something
                try {
                    if (isConnection) {
                        client.dos.writeUTF("DISCONNECT");
                        isConnection = false;
                        client.s.close();
                    }
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        });

        frame.pack();
        frame.setVisible(true);
    }

    public void Connect() throws IOException {
        this.s = new Socket(this.host, this.port);
        this.dis = new DataInputStream(this.s.getInputStream());
        this.dos = new DataOutputStream(this.s.getOutputStream());
        disconnectButton.setEnabled(false);
        messageContent.setText(this.deviceClientName + ": Connecting to server...\n");

        isConnection = true;
        CardLayout cl = (CardLayout) (cards.getLayout());
        cl.show(cards, MAINPANEL);

        if (this.deviceClientName.equals("")) {
            this.deviceClientName = s.getLocalAddress().getHostAddress();
        }
        this.dos.writeUTF(this.deviceClientName);

        // Create a new thread to sending directory tree requests
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sendDirectoryTree();
                    messageContent.setText(messageContent.getText() + "Connected to server!!!\n");
                    disconnectButton.setEnabled(true);
                } catch (IOException exc) {
                    // TODO: handle exception
                }
            }
        }).start();

        while (isConnection) {
            String receivedString = this.dis.readUTF();
            System.out.println(receivedString);
            if (receivedString.equals("DISCONNECT")) {
                isConnection = false;
                messageContent.setText(null);
                cl.show(cards, CONNECTPANEL);
                break;
            } else if (receivedString.equals("WATCHING")) {
                // Current watching dir
                receivedString = this.dis.readUTF();

                if (watcherThread != null) {
                    watcher = null;
                    watcherThread.interrupt();
                    watcherThread = null;
                }
                try {
                    watcher = new FolderMonitor(Paths.get(receivedString));
                    watcherThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("Monitoring");
                            System.out.println("isConnection: " + isConnection);
                            while (isConnection) {
                                try {
                                    System.out.println("watcher" + watcher.getWatcherMessage());
                                    if (watcherMessage.compareTo(watcher.getWatcherMessage()) != 0) {
                                        watcherMessage = watcher.getWatcherMessage();
                                        dos.writeUTF("WATCHING");
                                        System.out.println(watcherMessage);
                                        dos.writeUTF(watcherMessage);
                                        messageContent.setText(messageContent.getText() + watcherMessage + "\n");
                                    }
                                } catch (IOException exc) {
                                    // TODO: handle exception
                                }

                            }
                        }
                    });
                    watcherThread.start();
                } catch (AccessDeniedException exc) {
                    String msg = String.format("AccessDeniedException: Try monitoring another directory from %s!",
                            this.deviceClientName);
                    dos.writeUTF("INFO");
                    dos.writeUTF(msg);
                }
            }
        }
    }

    public void sendDirectoryTree() throws IOException {
        this.treeDirectory = new Directory().getTree();
        this.dos.writeUTF("SEND");
        ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
        oos.writeObject(this.treeDirectory);
        this.dos.writeUTF("INFO");
        this.dos.writeUTF(this.deviceClientName + ": Connected to server.");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}