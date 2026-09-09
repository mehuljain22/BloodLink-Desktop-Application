package com.bloodlink.ui;

import com.bloodlink.AppContext; import com.bloodlink.model.User; import com.bloodlink.util.Theme;
import javax.swing.*; import javax.swing.border.EmptyBorder; import java.awt.*; import java.util.Arrays;

public final class LoginFrame extends JFrame {
    private final AppContext context; private final JTextField email=new JTextField("admin@bloodlink.local"); private final JPasswordField password=new JPasswordField("Admin@123"); private final JLabel error=new JLabel(" ");
    public LoginFrame(AppContext context){
        super("BloodLink - Secure Sign In"); this.context=context; setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(980,620); setLocationRelativeTo(null); setMinimumSize(new Dimension(900,560)); build();
    }
    private void build(){
        JPanel root=new JPanel(new GridLayout(1,2)); root.add(brandPanel()); root.add(formPanel()); setContentPane(root); getRootPane().setDefaultButton(findLoginButton((JPanel)root.getComponent(1)));
    }
    private JButton findLoginButton(JPanel p){for(Component c:p.getComponents())if(c instanceof JButton b&&"Sign in securely".equals(b.getText()))return b;return null;}
    private JPanel brandPanel(){
        JPanel p=new JPanel(){protected void paintComponent(Graphics g){super.paintComponent(g);Graphics2D g2=(Graphics2D)g.create();g2.setPaint(new GradientPaint(0,0,Theme.MAROON_DEEP,getWidth(),getHeight(),Theme.RED));g2.fillRect(0,0,getWidth(),getHeight());g2.dispose();}}; p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setBorder(new EmptyBorder(70,55,70,55));
        JLabel icon=new JLabel("♥");icon.setForeground(Color.WHITE);icon.setFont(new Font("SansSerif",Font.BOLD,62));icon.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel name=new JLabel("BloodLink");name.setForeground(Color.WHITE);name.setFont(new Font("SansSerif",Font.BOLD,38));name.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tag=new JLabel("<html>Smart Blood Bank & Donor<br>Management System</html>");tag.setForeground(new Color(255,235,238));tag.setFont(new Font("SansSerif",Font.PLAIN,20));tag.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel features=new JLabel("<html><br>✓ Bag level inventory with expiry<br><br>✓ TTI screening and quarantine<br><br>✓ Emergency request workflow<br><br>✓ Full audit trail<br><br>✓ Compatibility and reporting</html>");features.setForeground(Color.WHITE);features.setFont(new Font("SansSerif",Font.PLAIN,15));features.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(icon);p.add(Box.createVerticalStrut(6));p.add(name);p.add(Box.createVerticalStrut(14));p.add(tag);p.add(Box.createVerticalStrut(35));p.add(features);return p;
    }
    private JPanel formPanel(){
        JPanel outer=new JPanel(new GridBagLayout());outer.setBackground(Theme.BG); JPanel p=new JPanel();p.setOpaque(false);p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));p.setPreferredSize(new Dimension(340,390));
        JLabel title=Theme.title("Welcome back");title.setAlignmentX(Component.LEFT_ALIGNMENT); JLabel sub=Theme.subtitle("Sign in to continue to BloodLink");sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(title);p.add(Box.createVerticalStrut(8));p.add(sub);p.add(Box.createVerticalStrut(32));
        p.add(label("Email address"));p.add(Box.createVerticalStrut(7));styleField(email);p.add(email);p.add(Box.createVerticalStrut(18));p.add(label("Password"));p.add(Box.createVerticalStrut(7));styleField(password);p.add(password);p.add(Box.createVerticalStrut(10));
        error.setForeground(Theme.RED);error.setFont(new Font("SansSerif",Font.PLAIN,12));error.setAlignmentX(Component.LEFT_ALIGNMENT);p.add(error);p.add(Box.createVerticalStrut(10));
        JButton login=Theme.primary("Sign in securely");login.setAlignmentX(Component.LEFT_ALIGNMENT);login.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));login.addActionListener(e->login());p.add(login);p.add(Box.createVerticalStrut(22));
        JLabel demo=Theme.subtitle("Demo: admin@bloodlink.local  •  Admin@123");demo.setAlignmentX(Component.LEFT_ALIGNMENT);p.add(demo); JLabel mode=Theme.subtitle("Mode: "+(context.databaseMode?"MySQL connected":"Demo / in-memory"));mode.setAlignmentX(Component.LEFT_ALIGNMENT);p.add(Box.createVerticalStrut(7));p.add(mode);
        outer.add(p);return outer;
    }
    private JLabel label(String t){JLabel l=new JLabel(t);l.setForeground(Theme.TEXT);l.setFont(new Font("SansSerif",Font.BOLD,13));l.setAlignmentX(Component.LEFT_ALIGNMENT);return l;}
    private void styleField(JTextField f){f.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));f.setPreferredSize(new Dimension(340,44));f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER),new EmptyBorder(8,11,8,11)));}
    private void login(){error.setText(" ");char[] pass=password.getPassword();try{var u=context.authService.login(email.getText(),pass);if(u.isPresent()){dispose();new DashboardFrame(context,u.get()).setVisible(true);}else error.setText("Invalid email or password.");}catch(Exception ex){error.setText("Sign in failed: "+ex.getMessage());}finally{Arrays.fill(pass,'\0');}}
}
