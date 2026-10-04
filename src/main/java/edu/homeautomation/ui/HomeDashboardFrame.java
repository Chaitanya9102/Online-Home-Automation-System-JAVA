package edu.homeautomation.ui;

import edu.homeautomation.model.*;
import edu.homeautomation.persistence.HomeRepository;
import edu.homeautomation.service.DeviceService;
import edu.homeautomation.service.EnvironmentService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class HomeDashboardFrame extends JFrame {
    private static final Color BG = new Color(245, 248, 247);
    private static final Color NAVY = new Color(20, 39, 60);
    private static final Color GREEN = new Color(47, 131, 107);
    private final HomeRepository repository;
    private final DeviceService deviceService;
    private final EnvironmentService environment;
    private final boolean admin;
    private final CardLayout pages = new CardLayout();
    private final JPanel content = new JPanel(pages);
    private final JPanel deviceGrid = new JPanel(new GridLayout(0, 2, 14, 14));
    private final JComboBox<String> roomFilter = new JComboBox<>(new String[]{"All rooms", "Living Room", "Hallway", "Bedroom", "Entry", "Garage"});
    private JLabel temperatureValue;
    private JLabel securityValue;
    private JLabel timeValue;
    private JTable deviceTable;
    private JTable userTable;
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("h:mm a");

    public HomeDashboardFrame(HomeRepository repository, DeviceService deviceService, EnvironmentService environment, boolean admin) {
        super("Haven · " + (admin ? "Administration" : "Homeowner"));
        this.repository=repository; this.deviceService=deviceService; this.environment=environment; this.admin=admin;
        setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(1240, 790); setMinimumSize(new Dimension(1000, 680)); setLocationRelativeTo(null);
        build();
        environment.addListener(reading -> SwingUtilities.invokeLater(() -> updateEnvironment(reading)));
    }

    private void build() {
        content.removeAll();
        JPanel shell = new JPanel(new BorderLayout()); shell.setBackground(BG);
        shell.add(sidebar(), BorderLayout.WEST);
        JPanel body = new JPanel(new BorderLayout()); body.setBackground(BG);
        body.add(topbar(), BorderLayout.NORTH);
        content.setBackground(BG); content.add(homePage(), "home"); content.add(automationPage(), "automation"); content.add(adminPage(), "admin");
        body.add(content, BorderLayout.CENTER); shell.add(body, BorderLayout.CENTER); setContentPane(shell);
        refreshDevices();
        pages.show(content, "home");
    }

    private JPanel sidebar() {
        JPanel panel = new JPanel(); panel.setBackground(NAVY); panel.setPreferredSize(new Dimension(226, 0)); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(25, 18, 20, 18));
        JLabel logo = LoginFrame.label("⌂   HAVEN", 19, Color.WHITE, Font.BOLD); logo.setBorder(new EmptyBorder(3, 8, 34, 0)); panel.add(logo);
        panel.add(sectionLabel("YOUR HOME"));
        navButton(panel, "⌂   Overview", "home"); navButton(panel, "◷   Automations", "automation");
        if (admin) { panel.add(Box.createVerticalStrut(22)); panel.add(sectionLabel("MANAGEMENT")); navButton(panel, "⚙   Admin console", "admin"); }
        panel.add(Box.createVerticalGlue());
        JPanel online = new JPanel(new BorderLayout()); online.setBackground(new Color(31, 57, 77)); online.setBorder(new EmptyBorder(13, 12, 13, 12));
        JLabel dot = LoginFrame.label("●", 12, new Color(111, 205, 159), Font.BOLD); online.add(dot, BorderLayout.WEST);
        JLabel status = LoginFrame.label("  Home hub online<br>  All systems normal", 12, new Color(213, 226, 232), Font.PLAIN); online.add(status, BorderLayout.CENTER);
        panel.add(online); panel.add(Box.createVerticalStrut(18));
        JButton signout = new JButton("←  Sign out"); signout.setForeground(new Color(210, 221, 227)); signout.setBackground(NAVY); signout.setBorderPainted(false); signout.setFocusPainted(false); signout.setAlignmentX(Component.LEFT_ALIGNMENT); signout.addActionListener(e -> { new LoginFrame(repository,deviceService,environment).setVisible(true); dispose(); }); panel.add(signout);
        return panel;
    }

    private JLabel sectionLabel(String value) { JLabel l=LoginFrame.label(value,10,new Color(136,162,177),Font.BOLD); l.setBorder(new EmptyBorder(0,8,12,0)); return l; }
    private void navButton(JPanel parent, String text, String page) {
        JButton b=new JButton(text); b.setHorizontalAlignment(SwingConstants.LEFT); b.setFont(new Font("Segoe UI",Font.PLAIN,14)); b.setForeground(new Color(222,233,237)); b.setBackground(NAVY); b.setBorder(new EmptyBorder(12,12,12,10)); b.setFocusPainted(false); b.setMaximumSize(new Dimension(190,43)); b.setAlignmentX(Component.LEFT_ALIGNMENT); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); b.addActionListener(e -> { if ("admin".equals(page)) refreshAdmin(); pages.show(content,page); }); parent.add(b);
    }

    private JPanel topbar() {
        JPanel top=new JPanel(new BorderLayout()); top.setBackground(Color.WHITE); top.setBorder(new EmptyBorder(17,30,17,30));
        JLabel title=LoginFrame.label(admin?"Administration":"Good morning, Alex  ☀",22,NAVY,Font.BOLD);
        String sub=admin?"Manage accounts, devices and platform compatibility.":"Here’s what’s happening around your home.";
        JLabel stack=new JLabel("<html>"+title.getText().replaceAll("</?html>","")+"<br><span style='font-size:12px;color:#71818a;font-weight:normal'>"+sub+"</span></html>"); stack.setFont(new Font("Segoe UI",Font.BOLD,21)); top.add(stack,BorderLayout.WEST);
        JPanel profile=new JPanel(new FlowLayout(FlowLayout.RIGHT,12,3)); profile.setOpaque(false);
        JLabel avatar=new JLabel(admin?"AD":"AM",SwingConstants.CENTER); avatar.setOpaque(true); avatar.setBackground(new Color(226,240,235)); avatar.setForeground(GREEN); avatar.setFont(new Font("Segoe UI",Font.BOLD,12)); avatar.setPreferredSize(new Dimension(37,37));
        JLabel name=LoginFrame.label(admin?"Administrator":"Alex Morgan",13,NAVY,Font.BOLD); profile.add(avatar); profile.add(name); top.add(profile,BorderLayout.EAST);
        return top;
    }

    private JPanel pageBase() { JPanel p=new JPanel(new BorderLayout(0,18)); p.setBackground(BG); p.setBorder(new EmptyBorder(24,30,26,30)); return p; }
    private JPanel homePage() {
        JPanel page=pageBase(); JPanel metrics=new JPanel(new GridLayout(1,3,14,0)); metrics.setOpaque(false);
        temperatureValue=LoginFrame.label("22.4° C",25,NAVY,Font.BOLD); securityValue=LoginFrame.label("All secure",20,NAVY,Font.BOLD); timeValue=LoginFrame.label("Updated just now",11,new Color(119,135,144),Font.PLAIN);
        metrics.add(metricCard("INDOOR TEMPERATURE",temperatureValue,"Comfortable range · target 21–24°", "◉",new Color(232,242,250)));
        metrics.add(metricCard("HOME SECURITY",securityValue,"All entry points are protected", "⌑",new Color(232,245,238)));
        metrics.add(metricCard("CONNECTED DEVICES",LoginFrame.label(repository.findDevices().size()+" devices",25,NAVY,Font.BOLD),"Your home hub is responding", "⌘",new Color(244,239,250)));
        JPanel center=new JPanel(new BorderLayout(0,14)); center.setOpaque(false);
        JPanel head=new JPanel(new BorderLayout()); head.setOpaque(false); head.add(LoginFrame.label("Your devices",18,NAVY,Font.BOLD),BorderLayout.WEST);
        roomFilter.setFont(new Font("Segoe UI",Font.PLAIN,12)); roomFilter.addActionListener(e -> refreshDevices()); head.add(roomFilter,BorderLayout.EAST);
        deviceGrid.setOpaque(false); JScrollPane scroll=new JScrollPane(deviceGrid); scroll.setBorder(BorderFactory.createEmptyBorder()); scroll.getViewport().setBackground(BG); scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        center.add(head,BorderLayout.NORTH); center.add(scroll,BorderLayout.CENTER);
        page.add(metrics,BorderLayout.NORTH); page.add(center,BorderLayout.CENTER); return page;
    }

    private JPanel metricCard(String title,JLabel value,String detail,String symbol,Color tint) {
        JPanel card=new JPanel(new BorderLayout(10,8)); card.setBackground(Color.WHITE); card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(230,236,234)),new EmptyBorder(17,18,17,18)));
        JPanel labelRow=new JPanel(new BorderLayout()); labelRow.setOpaque(false); labelRow.add(LoginFrame.label(title,10,new Color(107,126,133),Font.BOLD),BorderLayout.WEST);
        JLabel icon=new JLabel(symbol,SwingConstants.CENTER); icon.setOpaque(true); icon.setBackground(tint); icon.setForeground(GREEN); icon.setFont(new Font("Segoe UI Symbol",Font.PLAIN,18)); icon.setPreferredSize(new Dimension(36,36)); labelRow.add(icon,BorderLayout.EAST);
        JPanel bottom=new JPanel(); bottom.setOpaque(false); bottom.setLayout(new BoxLayout(bottom,BoxLayout.Y_AXIS)); bottom.add(value); bottom.add(Box.createVerticalStrut(4)); bottom.add(LoginFrame.label(detail,11,new Color(112,129,136),Font.PLAIN));
        card.add(labelRow,BorderLayout.NORTH); card.add(bottom,BorderLayout.CENTER); return card;
    }

    private void refreshDevices() {
        if(deviceGrid==null) return;
        deviceGrid.removeAll();
        String selected=(String)roomFilter.getSelectedItem();
        List<SmartDevice> list=deviceService.devices();
        for(SmartDevice device:list) {
            if(selected!=null && !selected.equals("All rooms") && !selected.equals(device.getRoom())) continue;
            deviceGrid.add(deviceCard(device));
        }
        if(deviceGrid.getComponentCount()==0) deviceGrid.add(LoginFrame.label("No devices in this room yet.",14,new Color(115,130,137),Font.PLAIN));
        deviceGrid.revalidate(); deviceGrid.repaint();
    }

    private JPanel deviceCard(SmartDevice device) {
        JPanel card=new JPanel(new BorderLayout(12,10)); card.setBackground(Color.WHITE); card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(229,235,233)),new EmptyBorder(16,16,15,16)));
        JLabel badge=new JLabel(device.getKind().equals("Security")?"⌑":device.getKind().equals("Thermostat")?"♨":"☼",SwingConstants.CENTER); badge.setOpaque(true); badge.setBackground(new Color(232,242,238)); badge.setForeground(GREEN); badge.setFont(new Font("Segoe UI Symbol",Font.PLAIN,20)); badge.setPreferredSize(new Dimension(43,43)); card.add(badge,BorderLayout.WEST);
        JPanel middle=new JPanel(); middle.setOpaque(false); middle.setLayout(new BoxLayout(middle,BoxLayout.Y_AXIS)); middle.add(LoginFrame.label(device.getName(),14,NAVY,Font.BOLD)); middle.add(Box.createVerticalStrut(4)); middle.add(LoginFrame.label(device.getRoom()+"  ·  "+device.statusText(),11,new Color(112,129,136),Font.PLAIN)); card.add(middle,BorderLayout.CENTER);
        JToggleButton toggle=new JToggleButton(device.isOn()?"On":"Off",device.isOn()); toggle.setFocusPainted(false); toggle.setBackground(device.isOn()?new Color(225,242,235):new Color(242,245,244)); toggle.setForeground(device.isOn()?GREEN:new Color(108,121,127)); toggle.setFont(new Font("Segoe UI",Font.BOLD,12)); toggle.setBorderPainted(false); toggle.addActionListener(e -> {
            try { JOptionPane.showMessageDialog(this,deviceService.toggle(device)); }
            catch(HomeValidationException exception) { JOptionPane.showMessageDialog(this,exception.getMessage(),"Device unavailable",JOptionPane.WARNING_MESSAGE); }
            refreshDevices();
        }); card.add(toggle,BorderLayout.EAST);
        if(!device.isCompatible()) card.setToolTipText("Pending admin compatibility approval");
        return card;
    }

    private JPanel automationPage() {
        JPanel page=pageBase(); JPanel top=new JPanel(new BorderLayout()); top.setOpaque(false); top.add(LoginFrame.label("Your routines",20,NAVY,Font.BOLD),BorderLayout.WEST);
        JButton add=new JButton("＋  New routine"); stylePrimary(add); add.addActionListener(e -> addRule()); top.add(add,BorderLayout.EAST);
        JPanel list=new JPanel(); list.setBackground(BG); list.setLayout(new BoxLayout(list,BoxLayout.Y_AXIS));
        page.add(top,BorderLayout.NORTH); page.add(list,BorderLayout.CENTER); refreshRules(list); return page;
    }

    private void refreshRules(JPanel list) {
        list.removeAll();
        for(AutomationRule rule:repository.findRules()) {
            JPanel card=new JPanel(new BorderLayout(15,0)); card.setBackground(Color.WHITE); card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(229,235,233)),new EmptyBorder(18,18,18,18))); card.setMaximumSize(new Dimension(Integer.MAX_VALUE,96));
            JLabel icon=new JLabel("◷",SwingConstants.CENTER); icon.setOpaque(true); icon.setBackground(new Color(232,242,238)); icon.setForeground(GREEN); icon.setFont(new Font("Segoe UI",Font.PLAIN,20)); icon.setPreferredSize(new Dimension(44,44)); card.add(icon,BorderLayout.WEST);
            JPanel info=new JPanel(); info.setOpaque(false); info.setLayout(new BoxLayout(info,BoxLayout.Y_AXIS)); info.add(LoginFrame.label(rule.getName(),15,NAVY,Font.BOLD)); info.add(Box.createVerticalStrut(5)); info.add(LoginFrame.label(rule.getCondition()+"   →   "+rule.getAction(),12,new Color(108,125,133),Font.PLAIN)); card.add(info,BorderLayout.CENTER);
            JCheckBox enabled=new JCheckBox("Enabled",rule.isEnabled()); enabled.setOpaque(false); enabled.setForeground(rule.isEnabled()?GREEN:new Color(119,130,136)); enabled.addActionListener(e -> { rule.setEnabled(enabled.isSelected()); repository.updateRule(rule); }); card.add(enabled,BorderLayout.EAST);
            list.add(card); list.add(Box.createVerticalStrut(12));
        }
        list.revalidate(); list.repaint();
    }

    private void addRule() {
        JTextField name=new JTextField(22), condition=new JTextField(22), action=new JTextField(22);
        JPanel form=new JPanel(new GridLayout(0,2,8,9)); form.add(new JLabel("Routine name"));form.add(name);form.add(new JLabel("When / condition"));form.add(condition);form.add(new JLabel("Then / action"));form.add(action);
        if(JOptionPane.showConfirmDialog(this,form,"Create automation",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION) {
            try {
                String n=name.getText().trim(),c=condition.getText().trim(),a=action.getText().trim();
                if(n.isEmpty()||c.isEmpty()||a.isEmpty()) throw new HomeValidationException("Please complete all three fields.");
                repository.saveRule(new AutomationRule(n,c,a,true));
                JPanel list=(JPanel)((JPanel)content.getComponent(1)).getComponent(1); refreshRules(list);
            } catch(HomeValidationException ex) { JOptionPane.showMessageDialog(this,ex.getMessage(),"Check your routine",JOptionPane.WARNING_MESSAGE); }
        }
    }

    private JPanel adminPage() {
        JPanel page=pageBase(); JPanel header=new JPanel(new BorderLayout()); header.setOpaque(false); header.add(LoginFrame.label("Platform management",20,NAVY,Font.BOLD),BorderLayout.WEST);
        JLabel persistence=LoginFrame.label(repository.isPersistent()?"SQLite database connected":"Demo data · changes reset when app closes",12,new Color(106,124,131),Font.PLAIN); header.add(persistence,BorderLayout.EAST);
        JTabbedPane tabs=new JTabbedPane(); tabs.setFont(new Font("Segoe UI",Font.PLAIN,13));
        JPanel deviceTab=new JPanel(new BorderLayout(0,12)); deviceTab.setBackground(BG);
        JLabel intro=LoginFrame.label("Device compatibility review",14,NAVY,Font.BOLD); deviceTab.add(intro,BorderLayout.NORTH);
        String[] cols={"Device","Room","Type","Compatibility","Action"}; deviceTable=new JTable(adminDeviceRows(),cols); deviceTable.setRowHeight(42); deviceTable.setFont(new Font("Segoe UI",Font.PLAIN,13)); deviceTable.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,12)); deviceTable.setFillsViewportHeight(true); deviceTab.add(new JScrollPane(deviceTable),BorderLayout.CENTER);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT)); actions.setOpaque(false); JButton approve=new JButton("Approve / reject selected"); stylePrimary(approve); approve.addActionListener(e -> updateCompatibility(deviceTable.getSelectedRow())); actions.add(approve); deviceTab.add(actions,BorderLayout.SOUTH);
        tabs.addTab("Devices",deviceTab); tabs.addTab("Users",userTab()); tabs.addTab("System",systemTab());
        page.add(header,BorderLayout.NORTH); page.add(tabs,BorderLayout.CENTER); return page;
    }

    private Object[][] adminDeviceRows() {
        List<SmartDevice> ds=deviceService.devices(); Object[][] rows=new Object[ds.size()][5];
        for(int i=0;i<ds.size();i++){SmartDevice d=ds.get(i);rows[i]=new Object[]{d.getName(),d.getRoom(),d.getKind(),d.isCompatible()?"Approved":"Pending","Select row to review"};}
        return rows;
    }

    private void updateCompatibility(int selected) {
        if(selected<0){JOptionPane.showMessageDialog(this,"Select a device first.");return;}
        List<SmartDevice> ds=deviceService.devices(); if(selected>=ds.size())return;
        SmartDevice d=ds.get(selected); boolean approve=!d.isCompatible();
        deviceService.setCompatibility(d,approve); refreshAdmin();
        JOptionPane.showMessageDialog(this,d.getName()+" is now "+(approve?"approved":"pending review")+".");
    }

    private JPanel userTab() {
        JPanel panel=new JPanel(new BorderLayout(0,12)); panel.setBackground(BG); userTable=new JTable(userRows(),new String[]{"Name","Email","Role"}); userTable.setRowHeight(40); userTable.setFont(new Font("Segoe UI",Font.PLAIN,13));
        panel.add(new JScrollPane(userTable),BorderLayout.CENTER); JPanel buttons=new JPanel(new FlowLayout(FlowLayout.RIGHT)); buttons.setOpaque(false);
        JButton add=new JButton("＋ Add user"); stylePrimary(add); add.addActionListener(e->addUser()); JButton remove=new JButton("Remove selected"); remove.addActionListener(e->{int row=userTable.getSelectedRow();if(row<0){JOptionPane.showMessageDialog(this,"Select a user first.");return;}String email=(String)userTable.getValueAt(row,1);repository.deleteUser(email);refreshAdmin();});buttons.add(remove);buttons.add(add);panel.add(buttons,BorderLayout.SOUTH);return panel;
    }
    private Object[][] userRows(){List<AppUser> users=repository.findUsers();Object[][] rows=new Object[users.size()][3];for(int i=0;i<users.size();i++){AppUser u=users.get(i);rows[i]=new Object[]{u.name(),u.email(),u.role()};}return rows;}
    private void addUser(){JTextField name=new JTextField(),email=new JTextField();JComboBox<String> role=new JComboBox<>(new String[]{"HOMEOWNER","ADMIN"});JPanel form=new JPanel(new GridLayout(0,2,8,8));form.add(new JLabel("Name"));form.add(name);form.add(new JLabel("Email"));form.add(email);form.add(new JLabel("Role"));form.add(role);
        if(JOptionPane.showConfirmDialog(this,form,"Add platform user",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){String n=name.getText().trim(),em=email.getText().trim();if(n.isEmpty()||!em.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")){JOptionPane.showMessageDialog(this,"Enter a name and valid email address.");return;}repository.saveUser(new AppUser(n,em,(String)role.getSelectedItem()));refreshAdmin();}}
    private JPanel systemTab(){JPanel p=new JPanel(new GridLayout(0,1,8,8));p.setBorder(new EmptyBorder(22,22,22,22));p.setBackground(BG);p.add(LoginFrame.label("System status",16,NAVY,Font.BOLD));p.add(LoginFrame.label("Home hub: Online",14,GREEN,Font.PLAIN));p.add(LoginFrame.label("Database: "+(repository.isPersistent()?"SQLite connected":"In-memory classroom demo"),14,NAVY,Font.PLAIN));p.add(LoginFrame.label("Environment monitor: Active · refreshes every 5 seconds",14,NAVY,Font.PLAIN));p.add(LoginFrame.label("Connected devices: "+deviceService.devices().size(),14,NAVY,Font.PLAIN));return p;}
    private void refreshAdmin(){
        if(deviceTable!=null) replaceRows(deviceTable,adminDeviceRows());
        if(userTable!=null) replaceRows(userTable,userRows());
    }
    private void replaceRows(JTable table,Object[][] rows){
        DefaultTableModel model=(DefaultTableModel)table.getModel();
        model.setRowCount(0);
        for(Object[] row:rows) model.addRow(row);
    }

    private void updateEnvironment(EnvironmentReading reading){if(temperatureValue!=null)temperatureValue.setText(String.format("%.1f° C",reading.temperature()));if(timeValue!=null)timeValue.setText("Updated "+reading.recordedAt().format(timeFormat));}
    private void stylePrimary(JButton button){button.setBackground(GREEN);button.setForeground(Color.WHITE);button.setFont(new Font("Segoe UI",Font.BOLD,12));button.setFocusPainted(false);button.setBorderPainted(false);}
}
