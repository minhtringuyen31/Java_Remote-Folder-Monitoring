package Server;

import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.awt.*;
import java.awt.event.*;
import java.net.*;

public class UI {
    public static void main(String[] args) {

    }

    public String appTitle = "REMOTE DIRECTORY MONITORING";
    public JPanel appPanel;
    public JPanel appTitlePanel;
    public JLabel titleLabel;

    public JPanel tablePanel;
    public JPanel chooseDir;

    public void addComponentToPane(Container pane) {
        appPanel = new JPanel(new BorderLayout());

        appTitlePanel = new JPanel();
        titleLabel.setText(appTitle);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 30));
        appTitlePanel.add(titleLabel, CENTER_ALIGNMENT);

    }
}
