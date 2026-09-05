package com.mycompany.chatserver;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/*
-~-~-~Explanation of layout~-~-~-
2 Panels:
Panel 1: Content Panel w/ all the like.. contents :p (contentPanel)
            -Uses GridbagLayout (to size elements well)
Panel 2: Server Bar Panel w/ the server scrollpane and whatever else I add later (serverPanel)
            -Uses BorderLayout for resizing of the server thingy when the button is pressed
 */
public class DihCordClientUI extends JFrame {

    private DihCordClient currentClient;

    public JButton sendButton;
    public JTextArea sendArea;
    public JLabel msgLabel;
    public String msg = "";

    final int COLLAPSE_SPEED = 20;

    public DihCordClientUI() {
        FlatDarkLaf.setup();

        SwingUtilities.invokeLater(() -> {
            JPanel mainPanel = new JPanel(new BorderLayout());

            //Panel With Server list + DM List
            JPanel serverPanel = new JPanel(new BorderLayout());
            serverPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            //Scrollpane with Server List
            JScrollPane serverScrollPane = new JScrollPane();                 //Server List ScrollPane
            serverScrollPane.setPreferredSize(new Dimension(50, 400));
            serverScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            serverScrollPane.getVerticalScrollBar().setUnitIncrement(16);
            serverPanel.add(serverScrollPane, BorderLayout.WEST);

            //Scrollpane with DM list
            JScrollPane messagesScrollPane = new JScrollPane();                 //Server List ScrollPane
            messagesScrollPane.setPreferredSize(new Dimension(200, 400));
            messagesScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            messagesScrollPane.getVerticalScrollBar().setUnitIncrement(16);
            serverPanel.add(messagesScrollPane, BorderLayout.EAST);

            //Panel with page contents
            JPanel contentPanel = new JPanel(new GridBagLayout());

            //Send Box + Msg Label
            sendButton = new JButton("Send");

            sendArea = new JTextArea();
            sendArea.setLineWrap(true);
            sendArea.setWrapStyleWord(true);

            JScrollPane sendPane = new JScrollPane(sendArea);

            msgLabel = new JLabel("gyat");
            msgLabel.setOpaque(true);
            msgLabel.setBackground(Color.white);
            msgLabel.setHorizontalAlignment(SwingConstants.LEFT);
            msgLabel.setVerticalAlignment(SwingConstants.TOP);

            JPanel msgPanel = new JPanel(new BorderLayout());
            msgPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            msgPanel.add(msgLabel);
            msgPanel.setBackground(Color.white);

            //Mode Button
            JButton visMode = new JButton();
            visMode.setPreferredSize(new Dimension(40, 40));

            //Show Server List
            JButton serverListBtn = new JButton(">");
            boolean sListOpen[] = {true};
            serverListBtn.setPreferredSize(new Dimension(40, 40));

            int preferredWidth[] = {270};
            serverListBtn.addActionListener((ActionEvent e) -> {
                serverListBtn.setEnabled(false);
                //Closes Server List
                if (sListOpen[0]) {
                    serverListBtn.setText("<");
                    System.out.println("CLOSING");
                    Timer t = new Timer(16, (ActionEvent e1) -> {
                        preferredWidth[0] -= COLLAPSE_SPEED;
                        serverPanel.setPreferredSize(new Dimension(preferredWidth[0], serverPanel.getHeight()));
                        mainPanel.revalidate();
                        mainPanel.repaint();
                        if (preferredWidth[0] <= 0) {
                            ((Timer) e1.getSource()).stop();
                            System.out.println("STOP");
                            serverListBtn.setEnabled(true);
                        }
                    });
                    t.start();

                    sListOpen[0] = false;
                } else { //Opens Server List
                    serverListBtn.setText(">");

                    Timer t = new Timer(16, (ActionEvent e1) -> {
                        preferredWidth[0] += COLLAPSE_SPEED;
                        serverPanel.setPreferredSize(new Dimension(preferredWidth[0], serverPanel.getHeight()));
                        mainPanel.revalidate();
                        mainPanel.repaint();
                        if (preferredWidth[0] >= 270) {
                            ((Timer) e1.getSource()).stop();
                            System.out.println("STOP");
                            serverListBtn.setEnabled(true);
                        }
                    });
                    t.start();

                    sListOpen[0] = true;
                }
            });

            //Sub-Panel to add to ensure that top bar doesnt resize cuz that pmo
            JPanel topBar = new JPanel(new BorderLayout());
            topBar.setPreferredSize(new Dimension(0, 50));
            topBar.setMinimumSize(new Dimension(0, 50));
            topBar.add(visMode, BorderLayout.EAST);
            topBar.add(serverListBtn, BorderLayout.WEST);

            //Making Server Lists
            //Setting GridBagConstraints
            //Send Button
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.LAST_LINE_END;
            c.gridx = 1;
            c.gridy = 2;
            c.weightx = 0;
            c.weighty = 0;
            c.ipady = 40;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(10, 10, 10, 10);
            contentPanel.add(sendButton, c);

            //Send Pane (text area to type)
            c = new GridBagConstraints();
            c.anchor = GridBagConstraints.LAST_LINE_START;
            c.gridx = 0;
            c.gridy = 2;
            c.weightx = 1;
            c.weighty = 0;
            c.ipady = 50;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(10, 10, 10, 10);
            contentPanel.add(sendPane, c);

            //Message Panel
            c = new GridBagConstraints();
            c.anchor = GridBagConstraints.PAGE_START;
            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 2;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(10, 10, 10, 10);
            contentPanel.add(msgPanel, c);

            //Top Bar
            c = new GridBagConstraints();
            c.anchor = GridBagConstraints.PAGE_START;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 0;
            c.weighty = 0;
            c.gridwidth = 2;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(10, 10, 10, 10);
            contentPanel.add(topBar, c);

            //delimiter for choosing name set to | for now because im soo lazy
            sendButton.addActionListener((ActionEvent e) -> {
                String[] fullTxt = sendArea.getText().split("\\|");
                String name = fullTxt[0].trim();
                msg = fullTxt[1].trim();

                sendArea.setText("");
                try {
                    currentClient.makeMessage(name, msg);
                } catch (Exception ex) {
                    Logger.getLogger(DihCordClientUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            });

            //<editor-fold desc="Message List Buttons & Pane">
            JPanel messageList = new JPanel();
            messageList.setLayout(new BoxLayout(messageList, BoxLayout.Y_AXIS));

            JTextField addChat = new JTextField("(+)");

            addChat.setPreferredSize(new Dimension(50, 50));
            addChat.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

            messageList.add(addChat);

            //JPanel serverListWrapper = new JPanel(new BorderLayout()); idk why i did this i dont think it does anything
            //serverListWrapper.add(serverList);
            messagesScrollPane.setViewportView(messageList);

            //for showing a new member 
            ArrayList[] names = {new ArrayList<String>()};
            ArrayList[] newNames = {new ArrayList<String>()};
            new Thread() {
                @Override
                public void run() {
                    while (true) {
                        if (currentClient == null) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(DihCordClientUI.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            continue;
                        }
                        //adds a new JButton with the coressponding members (ignoring the user themselves)
                        newNames[0] = currentClient.getMemberList();
                        for (String i : (ArrayList<String>) newNames[0]) { //check for new names added to add to
                            if (!names[0].contains(i) && !i.equals(currentClient.getUsername())) { //names list and add a button for it
                                names[0].add(i);
                                JButton btn = new JButton(i);

                                btn.setPreferredSize(new Dimension(200, 50));
                                btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

                                messageList.add(btn);
                                messagesScrollPane.revalidate();
                                messagesScrollPane.repaint(); //keep this cuz ill use it later to display all the members !
                            }
                        }

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(DihCordClientUI.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }.start();

            addChat.addActionListener((ActionEvent e) -> {
                try {
                } catch (Exception ex) {
                    Logger.getLogger(DihCordClientUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            //</editor-fold>

            //<editor-fold desc="Server List Buttons & Pane">
            JPanel serverList = new JPanel();
            serverList.setLayout(new BoxLayout(serverList, BoxLayout.Y_AXIS));

            JButton addServer = new JButton("(+)");

            addServer.setPreferredSize(new Dimension(50, 50));
            addServer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

            serverList.add(addServer);

            //JPanel serverListWrapper = new JPanel(new BorderLayout()); idk why i did this i dont think it does anything
            //serverListWrapper.add(serverList);
            serverScrollPane.setViewportView(serverList);

            //for adding a new server 
            addServer.addActionListener((ActionEvent e) -> {
                try {
                    ServerDialog d = new ServerDialog(this, true);
                    d.setVisible(true);

                    String ip = d.serverIP;
                    String port = d.serverPort;
                    String name = d.serverName;

                    DihCordClient cl = new DihCordClient(Integer.parseInt(port), ip);

                    JButton btn = new JButton(name) {
                        public String inIP = ip;
                        public String inPort = port;
                        DihCordClient current = cl;

                        //initializer of aic
                        {
                            this.addActionListener((ActionEvent e) -> {
                                setCurrentClient();
                            });
                        }

                        //setting current client to the one on the button
                        public void setCurrentClient() {
                            currentClient = current;
                            //resets member list
                            JPanel contentPanel = (JPanel) messagesScrollPane.getViewport().getView();
                            for (Component comp : contentPanel.getComponents()) {
                                if (comp instanceof JButton button) {
                                    contentPanel.remove(button);
                                }
                            }
                            names[0].clear();
                            //load new member list when client switches
                            newNames[0] = currentClient.getMemberList();
                            for (String i : (ArrayList<String>) newNames[0]) { //check for new names added to add to
                                if (!names[0].contains(i) && !i.equals(currentClient.getUsername())) { //names list and add a button for it
                                    names[0].add(i);
                                    JButton btn = new JButton(i);

                                    btn.setPreferredSize(new Dimension(200, 50));
                                    btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

                                    messageList.add(btn);
                                    messagesScrollPane.revalidate();
                                    messagesScrollPane.repaint(); //keep this cuz ill use it later to display all the members !
                                }
                            }

                            messagesScrollPane.repaint();
                            messagesScrollPane.revalidate();
                        }

                    };

                    btn.setPreferredSize(new Dimension(50, 50));
                    btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

                    serverList.add(btn);
                    serverScrollPane.revalidate();
                    serverScrollPane.repaint();
                } catch (Exception ex) {
                    Logger.getLogger(DihCordClientUI.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
            //</editor-fold>            

            //light or dark mode
            AtomicBoolean mode = new AtomicBoolean(true);
            visMode.addActionListener((ActionEvent e) -> {

                ImageIcon frames[] = new ImageIcon[10];
                for (int i = 0; i < frames.length; i++) {
                    ImageIcon icon = new ImageIcon(new ImageIcon(DihCordClientUI.class.getResource("/LightDarkButton/" + i + ".png")).getImage().getScaledInstance(60, 40, Image.SCALE_AREA_AVERAGING));

                    frames[i] = icon;
                }

                if (mode.get()) { //Light Mode Enable
                    mode.set(!mode.get());
                    FlatLightLaf.setup();
                    FlatLaf.updateUI();
                    msgPanel.setBackground(Color.white);
                    msgLabel.setBackground(Color.white);
                    msgLabel.setForeground(Color.black);

                    final int frame[] = new int[1];
                    Timer timer = new Timer(20, (ActionEvent e1) -> {
                        visMode.setIcon(frames[frame[0]]);
                        frame[0]++;
                        if (frame[0] >= frames.length) {
                            Timer t = (Timer) e1.getSource();
                            t.stop();
                        }
                    });
                    timer.start();

                } else { //Dark Mode Enable
                    mode.set(!mode.get());
                    FlatDarkLaf.setup();
                    FlatLaf.updateUI();
                    msgPanel.setBackground(new Color(76, 80, 82));
                    msgLabel.setBackground(new Color(76, 80, 82));
                    msgLabel.setForeground(Color.white);

                    final int frame[] = new int[1];
                    frame[0] = 9;
                    Timer timer = new Timer(20, (ActionEvent e1) -> {
                        visMode.setIcon(frames[frame[0]]);
                        frame[0]--;
                        if (frame[0] <= 0) {
                            Timer t = (Timer) e1.getSource();
                            t.stop();
                        }
                    });
                    timer.start();
                }
            });

            //make thread to listen for messages on current client
            new Thread() {
                @Override
                public void run() {
                    while (true) {
                        if (currentClient == null) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(DihCordClientUI.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            continue;
                        }
                        ArrayList<String> messages = currentClient.getMessages()[1];
                        ArrayList<String> names = currentClient.getMessages()[0];

                        String messagesStr = "<html>";
                        for (int i = 0; i < messages.size(); i++) {
                            String safeMessage = messages.get(i).replaceAll("<", "&lt;").replaceAll(">", "&gt;");
                            messagesStr += names.get(i) + ": " + safeMessage + "<br>";
                        }
                        msgLabel.setText(messagesStr + "</html>");

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(DihCordClientUI.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }.start();

            //Add 2 panels to main panel:
            mainPanel.add(serverPanel, BorderLayout.WEST);
            mainPanel.add(contentPanel, BorderLayout.CENTER);
            this.add(mainPanel);

            this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            this.setSize(500, 500);
            this.setVisible(true);

        });
    }

    public static void main(String[] args) {
        DihCordClientUI d = new DihCordClientUI();
    }

}
