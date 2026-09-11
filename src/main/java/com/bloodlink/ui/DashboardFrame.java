package com.bloodlink.ui;

import com.bloodlink.AppContext;
import com.bloodlink.model.Role;
import com.bloodlink.model.User;
import com.bloodlink.util.FlatButton;
import com.bloodlink.util.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DashboardFrame extends JFrame {

    private final AppContext context;
    private final User user;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, JButton> nav = new LinkedHashMap<>();

    public DashboardFrame(AppContext context, User user) {
        super("BloodLink - " + user.dashboardTitle());
        this.context = context;
        this.user = user;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1320, 800);
        setMinimumSize(new Dimension(1150, 700));
        setLocationRelativeTo(null);
        build();
    }

    private void build() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(sidebar(), BorderLayout.WEST);
        root.add(mainArea(), BorderLayout.CENTER);
        setContentPane(root);
        showPage("Dashboard");
    }

    private JPanel sidebar() {
        JPanel side = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, Theme.MAROON_DEEP, 0, getHeight(), Theme.MAROON));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        side.setPreferredSize(new Dimension(235, 0));
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(26, 18, 22, 18));

        JLabel brand = new JLabel("\u2665  BloodLink");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("SansSerif", Font.BOLD, 22));
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(brand);
        side.add(Box.createVerticalStrut(30));

        for (String name : new String[]{"Dashboard", "Donors", "Inventory", "Screening", "Requests",
                "Appointments", "Compatibility", "Reports", "Audit trail"}) {
            JButton b = navButton(name);
            nav.put(name, b);
            side.add(b);
            side.add(Box.createVerticalStrut(6));
        }

        side.add(Box.createVerticalGlue());

        JLabel mode = new JLabel(context.databaseMode ? "\u25CF MySQL Connected" : "\u25CF Demo Mode");
        mode.setForeground(context.databaseMode ? new Color(150, 226, 180) : new Color(255, 205, 160));
        mode.setFont(new Font("SansSerif", Font.BOLD, 12));
        mode.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(mode);
        side.add(Box.createVerticalStrut(14));

        JButton logout = navButton("Sign out");
        logout.addActionListener(e -> {
            dispose();
            context.auditService.setActor(null);
            new LoginFrame(context).setVisible(true);
        });
        side.add(logout);
        return side;
    }

    private JButton navButton(String text) {
        FlatButton b = Theme.navItem(text, false);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        b.addActionListener(e -> showPage(text));
        return b;
    }

    private JPanel mainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(Theme.BG);
        main.add(topBar(), BorderLayout.NORTH);

        content.setBackground(Theme.BG);
        content.add(new DashboardPanel(context), "Dashboard");
        content.add(new DonorPanel(context.donorService), "Donors");
        content.add(new InventoryPanel(context.inventoryService), "Inventory");
        content.add(new ScreeningPanel(context.screeningService), "Screening");
        content.add(new RequestPanel(context.requestService), "Requests");
        content.add(new AppointmentPanel(context.appointmentService), "Appointments");
        content.add(new CompatibilityPanel(context.inventoryService), "Compatibility");
        content.add(new ReportsPanel(context), "Reports");
        content.add(new AuditPanel(context.auditService), "Audit trail");

        main.add(content, BorderLayout.CENTER);
        return main;
    }

    private JPanel topBar() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, Theme.RED), new EmptyBorder(14, 24, 14, 24)));

        JLabel crumb = new JLabel("Blood Bank Operations Center");
        crumb.setForeground(Theme.MAROON);
        crumb.setFont(new Font("SansSerif", Font.BOLD, 13));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JLabel who = new JLabel(user.getName());
        who.setForeground(Theme.TEXT);
        who.setFont(new Font("SansSerif", Font.BOLD, 13));
        right.add(who);
        right.add(Theme.chip(user.getRole().name(), user.getRole() == Role.ADMIN ? Theme.MAROON_DEEP : Theme.RED));

        p.add(crumb, BorderLayout.WEST);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private void showPage(String page) {
        if (!nav.containsKey(page)) return;
        cards.show(content, page);
        for (Map.Entry<String, JButton> e : nav.entrySet()) {
            boolean active = e.getKey().equals(page);
            JButton b = e.getValue();
            if (b instanceof FlatButton f) f.setActive(active);
            b.repaint();
        }
        content.revalidate();
        content.repaint();
    }
}
