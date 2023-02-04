package Server;

import java.net.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.*;

public class Server {

    private ServerSocket ss;
    private static HashMap<String, ServerThread> thread;
    private static DefaultListModel<String> listModel;
    private static JList<String> clientList;
    final static int PORT = 3200;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    private static JPanel screen;
    private static JPanel mainPanel;
    private static String appTitle = "REMOTE DIRECTORY MONITORING";
    private static JScrollPane activeClientScrollPanel;
    private static JTextArea messageContent;
    private static JTextField chosenDir;
    private static JLabel appTitleLabel;
    private static JPanel tablePanel;
    private static JScrollPane tableScrollPanel;
    private static JPanel activeClientPanel;
    private static JPanel dirPathPanel;
    private static JLabel chooseDir;
    private static JButton chooseDirButton;

    public void addComponentToPane(Container pane) {
        mainPanel = new JPanel(new BorderLayout());

        // start title panel
        appTitleLabel = new JLabel(appTitle);
        appTitleLabel.setHorizontalAlignment(JLabel.CENTER);
        appTitleLabel.setFont(new Font("Serif", Font.BOLD, 30));
        pane.add(appTitleLabel, BorderLayout.NORTH);
        // end title panel

        // start active client panel
        activeClientScrollPanel.setPreferredSize(new Dimension(200, 500));
        activeClientScrollPanel.setMaximumSize(activeClientScrollPanel.getPreferredSize());
        activeClientPanel = new JPanel(new BorderLayout());
        activeClientPanel.setBorder(new CompoundBorder(new TitledBorder("Active Client"), new EmptyBorder(5, 5, 5, 5)));
        activeClientPanel.add(activeClientScrollPanel, BorderLayout.CENTER);
        // end active client panel

        // start table panel
        tableScrollPanel = new JScrollPane(messageContent);
        messageContent.setEditable(false);
        messageContent.setLineWrap(true);
        messageContent.setWrapStyleWord(true);
        tableScrollPanel.setPreferredSize(new Dimension(800, 500));
        tableScrollPanel.setMaximumSize(activeClientScrollPanel.getPreferredSize());
        tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(new EmptyBorder(0, 20, 0, 20));
        tablePanel.setBorder(new CompoundBorder(new TitledBorder("Message Content"), new EmptyBorder(5, 5, 5, 5)));
        tablePanel.add(tableScrollPanel, BorderLayout.CENTER);
        // end table panel

        // start choose directory panel
        dirPathPanel = new JPanel();
        dirPathPanel = new JPanel();
        dirPathPanel.setBackground(Color.WHITE);
        chooseDir = new JLabel("Choose Directory:");
        chooseDirButton = new JButton("Choose");
        chosenDir.setEditable(false);
        // start choose directory panel

        chooseDirButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                JFrame browseFrame = new JFrame("Directory Tree");
                browseFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
                browseFrame.setResizable(false);

                String currentSelectedClient = clientList.getSelectedValue();
                if (currentSelectedClient == null) {
                    String msg = "Please select a client to monitor from the list";
                    JOptionPane.showMessageDialog(chooseDirButton, msg, "ERROR", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                ServerThread clientHandler = thread.get(currentSelectedClient);
                JTree cdTree = clientHandler.getDirectoryTree();

                JScrollPane dirScrollPane = new JScrollPane();
                dirScrollPane = new JScrollPane(cdTree);
                dirScrollPane.setPreferredSize(new Dimension(500, 500));
                dirScrollPane.setMaximumSize(dirScrollPane.getPreferredSize());

                JButton chooseButton = new JButton("CHOOSE");
                chooseButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        DefaultMutableTreeNode selectedElement = (DefaultMutableTreeNode) cdTree.getSelectionPath()
                                .getLastPathComponent();
                        String selectedDir = ((File) selectedElement.getUserObject()).getAbsolutePath();
                        chosenDir.setText(selectedDir);
                        messageContent.setText(messageContent.getText() + clientHandler.getUsername() + ": "
                                + "Selected folder to monitoring | " + selectedDir + "\"\n");
                        try {
                            DataOutputStream clientHandlerDOS = clientHandler.getDataOutputStream();
                            clientHandlerDOS.writeUTF("WATCHING");
                            clientHandlerDOS.writeUTF(selectedDir);
                        } catch (IOException exc) {
                            // TODO: handle exception
                            exc.printStackTrace();
                        }
                        browseFrame.dispatchEvent(new WindowEvent(browseFrame,
                                WindowEvent.WINDOW_CLOSING));
                    }
                });

                browseFrame.add(dirScrollPane, BorderLayout.CENTER);
                browseFrame.add(chooseButton, BorderLayout.SOUTH);

                browseFrame.pack();
                browseFrame.setVisible(true);
            }
        });

        dirPathPanel.add(chooseDir);
        dirPathPanel.add(chosenDir);
        dirPathPanel.add(chooseDirButton);

        mainPanel.add(activeClientPanel, BorderLayout.EAST);
        mainPanel.add(tablePanel, BorderLayout.CENTER);
        mainPanel.add(dirPathPanel, BorderLayout.SOUTH);

        screen = new JPanel(new CardLayout());
        screen.add(mainPanel);

        pane.add(screen, BorderLayout.CENTER);
    }

    private static void createAndShowGUI() {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JFrame frame = new JFrame("Server-Port:3200");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        Server server = new Server();
        server.addComponentToPane(frame.getContentPane());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                try {
                    for (String deviceClientName : thread.keySet()) {
                        ServerThread clientHandler = thread.get(deviceClientName);
                        clientHandler.getDataOutputStream().writeUTF("DISCONNECT");
                    }
                } catch (IOException exc) {
                    exc.printStackTrace();
                }
            }
        });

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Start Server
        try {
            server.ss = new ServerSocket(PORT);

            new Thread() {
                public void run() {
                    while (true) {
                        try {
                            // Wating for client to connect
                            System.out.println("Waiting for a Client");
                            messageContent.setText("Server Started\n");
                            Socket s = server.ss.accept();

                            ServerThread clientHandler = server.new ServerThread(s);

                            String clientUsername = clientHandler.getUsername();

                            thread.put(clientUsername, clientHandler);
                            listModel.addElement(clientUsername);
                            clientHandler.start();

                        } catch (IOException exc) {
                            exc.printStackTrace();
                        }
                    }
                }
            }.start();

        } catch (IOException exc) {
            exc.printStackTrace();
        }
    }

    public Server() {
        thread = new HashMap<String, ServerThread>();
        listModel = new DefaultListModel<String>();
        clientList = new JList<String>(listModel);
        activeClientScrollPanel = new JScrollPane(clientList);
        messageContent = new JTextArea(10, 30);
        chosenDir = new JTextField(20);
    }

    public class ServerThread extends Thread {
        private Socket s;
        private DataInputStream dis;
        private DataOutputStream dos;
        private String deviceClientName;

        private JTree directoryTree;
        private Boolean isConnection;

        public ServerThread(Socket s) throws IOException {
            this.setSocket(s);
            this.setUsername(this.dis.readUTF());
            isConnection = true;
        }

        public void setSocket(Socket s) {
            this.s = s;
            try {
                this.dis = new DataInputStream(s.getInputStream());
                this.dos = new DataOutputStream(s.getOutputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void closeSocket() {
            if (this.s != null) {
                try {
                    isConnection = false;
                    this.s.close();
                    this.s = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public Boolean getStatus() {
            return isConnection;
        }

        public DataOutputStream getDataOutputStream() {
            return this.dos;
        }

        public DataInputStream getDataInputStream() {
            return this.dis;
        }

        public String getUsername() {
            return this.deviceClientName;
        }

        public Boolean setUsername(String deviceClientName) {
            if (!deviceClientName.equals("")) {
                this.deviceClientName = deviceClientName;
                return true;
            }
            return false;
        }

        public void receivedDirectoryTree() throws IOException, ClassNotFoundException {
            ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
            directoryTree = (JTree) ois.readObject();
        }

        public JTree getDirectoryTree() {
            return directoryTree;
        }

        @Override
        public void run() {
            while (isConnection) {
                try {
                    String message = null;

                    // read user request
                    message = dis.readUTF();

                    // Receive DISCONNECT message from client
                    if (message.equals("DISCONNECT")) {
                        this.closeSocket();
                        thread.remove(this.getUsername());
                        listModel.removeElement(this.getUsername());
                        chosenDir.setText(null);
                        messageContent.setText(messageContent.getText() + this.getUsername() + " : "
                                + "Disconnected to server." + "\n");
                        break;
                    }

                    // Receive directory tree
                    else if (message.equals("SEND")) {
                        receivedDirectoryTree();
                    }
                    // Receive changes from monitoring directory
                    else if (message.equals("WATCHING")) {
                        String receivedMessage = dis.readUTF();
                        messageContent.setText(
                                messageContent.getText() + this.getUsername() + " : " + receivedMessage + "\n");
                    }
                    // Receive other message from client
                    else if (message.equals("INFO")) {
                        String receivedMessage = dis.readUTF();
                        messageContent.setText(messageContent.getText() + receivedMessage + "\n");
                    }
                } catch (IOException exc) {
                    System.out.println(exc.getMessage());
                } catch (ClassNotFoundException exc) {
                    System.out.println(exc.getMessage());
                }
            }
        }

    }
}
