package Client;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;

public class Client {

    private String host;
    private int port;
    private String clientName;
    private Socket sck;
    private DataInputStream dis;
    private DataOutputStream dos;
    private JPanel cards;
    final static String CONNECTPANEL = "ConnectPanel";
    final static String CHATPANEL = "ChatPanel";
    private static Boolean isConnecting = false;

    private JTree treeDirectory;
    private FileMonitor monitor = null;
    private Thread watcherThread = null;
    private static String watcherMessage = "";

    private static JTextArea txaArea = new JTextArea(10, 30);
    private static JTextField txfChatBox = new JTextField(30);
    private static JButton btnDisconnect = new JButton("DISCONNECT");
    private static JButton btnSend = new JButton("SEND");

    public void specifyConnectInfo(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void specifyName(String name) {
        this.clientName = name;
    }

    public void Connect() throws IOException {
        if (this.sck != null) {
            this.sck.close();
        }
        this.sck = new Socket(this.host, this.port);
        JOptionPane.showMessageDialog(null, "Successfully connect to server!", "NOTIFICATION",
                JOptionPane.INFORMATION_MESSAGE);
        this.dis = new DataInputStream(this.sck.getInputStream());
        this.dos = new DataOutputStream(this.sck.getOutputStream());

        // Notification
        txfChatBox.setEditable(false);
        btnDisconnect.setEnabled(false);
        btnSend.setEnabled(false);
        txaArea.setText("Try to connecting to server...\n");

        isConnecting = true;
        CardLayout cl = (CardLayout) (cards.getLayout());
        cl.show(cards, CHATPANEL);

        // Write clientName to server(ClientHandler)
        if (this.clientName.equals("")) {
            this.clientName = sck.getLocalAddress().getHostAddress();
        }
        this.dos.writeUTF(this.clientName);

        // Send current connection time
        this.dos.writeUTF("INFO");
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        this.dos.writeUTF(this.clientName + " has connected to server at: " + formatter.format(date));

        // Create a new thread to sending directory tree requests
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    sendDirectoryTree();
                    // Notification after sending directory tree
                    txaArea.setText(txaArea.getText() + "Connected!\n");
                    txfChatBox.setEditable(true);
                    btnDisconnect.setEnabled(true);
                    btnSend.setEnabled(true);
                } catch (IOException exc) {
                    //
                }
            }
        }).start();

        while (isConnecting) {
            System.out.println("I'm here to receive your command");
            String receivedString = this.dis.readUTF();
            System.out.println("I'm here to execute your command");
            if (receivedString.equals("DISCONNECT")) {
                // Close connection
                isConnecting = false;
                txaArea.setText(null);
                // Change layout
                cl.show(cards, CONNECTPANEL);
                break;
            } else if (receivedString.equals("CHAT")) {
                receivedString = this.dis.readUTF();
                txaArea.setText(txaArea.getText() +
                        "Server: " + receivedString + "\n");
            } else if (receivedString.equals("WATCHING")) {
                // Current watching dir
                receivedString = this.dis.readUTF();

                // If the watcherThread is already running, then interrupt
                // the previous watcherThread
                if (watcherThread != null) {
                    monitor = null;
                    watcherThread.interrupt();
                    watcherThread = null;
                    System.out.println("I'm interrupting the previous watcher");
                }
                // Running a new thread to see the changes in the directory
                try {
                    monitor = new FileMonitor(Paths.get(receivedString));
                    watcherThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("I'm here to monitoring");
                            while (isConnecting) {
                                try {

                                    // Don't know why when add this line,
                                    // the block of codes below is working,
                                    // it just printing the message to console screen :(
                                    System.out.println(monitor.getMonitorNotify());

                                    if (watcherMessage.compareTo(monitor.getMonitorNotify()) != 0) {
                                        watcherMessage = monitor.getMonitorNotify();
                                        dos.writeUTF("WATCHING");
                                        dos.writeUTF(watcherMessage);
                                    }
                                } catch (IOException exc) {
                                    //
                                }
                            }
                        }
                    });
                    watcherThread.start();
                } catch (AccessDeniedException exc) {
                    String msg = String.format("AccessDeniedException: Try monitoring another directory from %s!",
                            this.clientName);
                    dos.writeUTF("INFO");
                    dos.writeUTF(msg);
                }
            }
        }
    }

    public void sendDirectoryTree() throws IOException {
        this.treeDirectory = new ClientDirectory().getTree();
        this.dos.writeUTF("INFO");
        this.dos.writeUTF(this.clientName + " is sending directory tree...");

        this.dos.writeUTF("SEND");
        ObjectOutputStream oos = new ObjectOutputStream(sck.getOutputStream());
        oos.writeObject(this.treeDirectory);

        this.dos.writeUTF("INFO");
        this.dos.writeUTF(this.clientName + " has sent directory tree!");
    }

    public void addComponentToPane(Container pane) {

        JPanel chatCard = new JPanel(new BorderLayout());
        JPanel connectCard = new JPanel(new BorderLayout());

        // ConnectPanel

        GridLayout gridLayout = new GridLayout(3, 2);
        gridLayout.setHgap(10);
        gridLayout.setVgap(10);

        JPanel infoPane = new JPanel(gridLayout);
        infoPane.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel lbName = new JLabel("Name");
        JLabel lbIP = new JLabel("IP");
        JLabel lbPort = new JLabel("Port");
        JTextField txfName = new JTextField();
        JTextField txfIP = new JTextField();
        JTextField txfPort = new JTextField();

        JButton btnSubmit = new JButton("Connect");

        btnSubmit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                specifyName(txfName.getText());
                specifyConnectInfo(txfIP.getText(), Integer.parseInt(txfPort.getText()));
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

        infoPane.add(lbName);
        infoPane.add(txfName);
        infoPane.add(lbIP);
        infoPane.add(txfIP);
        infoPane.add(lbPort);
        infoPane.add(txfPort);

        connectCard.add(infoPane, BorderLayout.NORTH);
        connectCard.add(btnSubmit, BorderLayout.SOUTH);

        // ChatPanel
        JPanel titlePane = new JPanel();
        JPanel logPane = new JPanel();
        JPanel botPane = new JPanel(new BorderLayout());

        JLabel lbTitle = new JLabel("Chat");
        lbTitle.setHorizontalAlignment(JLabel.CENTER);

        JScrollPane scpScroller = new JScrollPane(txaArea);
        txaArea.setEditable(false);
        // txaArea.setPreferredSize(new Dimension(200,400));

        btnSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    // Send command
                    dos.writeUTF("CHAT");
                    // Get message from chatbox and send it to the server
                    String msg = txfChatBox.getText();
                    dos.writeUTF(msg);
                    // Update text area
                    txaArea.setText(txaArea.getText() + clientName + ": " + msg + "\n");
                    txfChatBox.setText("");
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        });

        txfChatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                btnSend.doClick();
            }
        });

        btnDisconnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                try {
                    // Send DISCONNECT command
                    dos.writeUTF("DISCONNECT");
                    // Close connection
                    isConnecting = false;
                    txaArea.setText(null);
                    // Change layout
                    CardLayout cl = (CardLayout) (cards.getLayout());
                    cl.show(cards, CONNECTPANEL);
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        });

        titlePane.add(lbTitle);
        logPane.add(scpScroller);
        botPane.add(txfChatBox, BorderLayout.NORTH);
        botPane.add(btnSend, BorderLayout.EAST);
        botPane.add(btnDisconnect, BorderLayout.CENTER);

        chatCard.add(titlePane, BorderLayout.NORTH);
        chatCard.add(logPane, BorderLayout.CENTER);
        chatCard.add(botPane, BorderLayout.SOUTH);

        // Config CardLayout
        cards = new JPanel(new CardLayout());
        cards.add(connectCard, CONNECTPANEL);
        cards.add(chatCard, CHATPANEL);

        pane.add(cards);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("ClientChat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        Client clientprog = new Client();
        clientprog.addComponentToPane(frame.getContentPane());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                // Do something
                try {
                    if (isConnecting) {
                        clientprog.dos.writeUTF("DISCONNECT");
                        isConnecting = false;
                        clientprog.sck.close();
                    }
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        });

        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}