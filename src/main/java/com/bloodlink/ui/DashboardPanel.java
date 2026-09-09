package com.bloodlink.ui;

import com.bloodlink.AppContext;
import com.bloodlink.model.*;
import com.bloodlink.util.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class DashboardPanel extends JPanel {

    private final AppContext ctx;
    private final JPanel kpis = new JPanel(new GridLayout(2, 4, 12, 12));
    private final DefaultTableModel stockModel = new DefaultTableModel(
            new Object[]{"Blood group", "Issuable units", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel expiryModel = new DefaultTableModel(
            new Object[]{"Bag code", "Group", "Component", "Expires", "Days left"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    public DashboardPanel(AppContext ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(24, 24, 24, 24));
        add(header(), BorderLayout.NORTH);

        JPanel centre = new JPanel(new BorderLayout(0, 16));
        centre.setOpaque(false);
        kpis.setOpaque(false);
        kpis.setPreferredSize(new Dimension(0, 220));
        centre.add(kpis, BorderLayout.NORTH);

        JTable stockTable = Theme.styleTable(new JTable(stockModel));
        Theme.colourStatusColumns(stockTable, "Status");
        JTable expiryTable = Theme.styleTable(new JTable(expiryModel));
        Theme.colourStatusColumns(expiryTable, "Days left");

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                tableCard("BLOOD AVAILABILITY", stockTable),
                tableCard("EXPIRING WITHIN 7 DAYS", expiryTable));
        split.setResizeWeight(0.45);
        split.setBorder(null);
        split.setOpaque(false);
        centre.add(split, BorderLayout.CENTER);

        add(centre, BorderLayout.CENTER);
        refresh();
    }

    private JPanel header() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.add(Theme.title("Operations overview"));
        t.add(Box.createVerticalStrut(5));
        t.add(Theme.subtitle("Live donor, bag, screening and request status across BloodLink."));
        JButton refresh = Theme.primary("Refresh");
        refresh.addActionListener(e -> refresh());
        p.add(t, BorderLayout.WEST);
        p.add(refresh, BorderLayout.EAST);
        return p;
    }

    private JPanel tableCard(String heading, JTable table) {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 10));
        card.add(Theme.sectionLabel(heading), BorderLayout.NORTH);
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        return card;
    }

    private JPanel metric(String name, String value, String hint, Color accent) {
        JPanel p = Theme.card();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel n = new JLabel(name);
        n.setForeground(Theme.MUTED);
        n.setFont(new Font("SansSerif", Font.BOLD, 11));
        n.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel v = new JLabel(value);
        v.setForeground(accent);
        v.setFont(new Font("SansSerif", Font.BOLD, 30));
        v.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel h = new JLabel(hint);
        h.setForeground(Theme.MUTED);
        h.setFont(new Font("SansSerif", Font.PLAIN, 11));
        h.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(n);
        p.add(Box.createVerticalStrut(8));
        p.add(v);
        p.add(Box.createVerticalStrut(6));
        p.add(h);
        return p;
    }

    private void refresh() {
        var inv = ctx.inventoryService;
        List<BloodBag> expiring = inv.expiringSoon();

        kpis.removeAll();
        kpis.add(metric("TOTAL DONORS", String.valueOf(ctx.donorService.all().size()),
                ctx.donorService.availableCount() + " currently available", Theme.MAROON_DEEP));
        kpis.add(metric("ISSUABLE UNITS", String.valueOf(inv.total()),
                "Screened and released bags only", Theme.MAROON_DEEP));
        kpis.add(metric("IN QUARANTINE", String.valueOf(inv.quarantinedCount()),
                "Awaiting TTI screening", Theme.WARNING));
        kpis.add(metric("RESERVED", String.valueOf(inv.reservedCount()),
                "Held against approved requests", Theme.MAROON));
        kpis.add(metric("PENDING REQUESTS", String.valueOf(ctx.requestService.pending()),
                "Needs staff attention", Theme.MAROON_DEEP));
        kpis.add(metric("EMERGENCIES", String.valueOf(ctx.requestService.emergencies()),
                "Prioritised in request queue", Theme.RED));
        kpis.add(metric("EXPIRING SOON", String.valueOf(expiring.size()),
                "Within " + com.bloodlink.service.InventoryService.EXPIRY_WARNING_DAYS + " days, issue these first", Theme.WARNING));
        kpis.add(metric("WASTAGE", String.format("%.1f%%", inv.wastagePercent()),
                inv.expiredCount() + " expired, " + inv.discardedCount() + " discarded", Theme.RED));

        stockModel.setRowCount(0);
        for (Map.Entry<BloodGroup, Integer> e : inv.stock().entrySet()) {
            int units = e.getValue();
            stockModel.addRow(new Object[]{e.getKey(), units,
                    units < 10 ? "LOW STOCK" : units < 20 ? "WATCH" : "HEALTHY"});
        }

        LocalDate today = LocalDate.now();
        expiryModel.setRowCount(0);
        for (BloodBag b : expiring) {
            long days = b.daysToExpiry(today);
            expiryModel.addRow(new Object[]{b.getBagCode(), b.getBloodGroup(), b.getComponent().getLabel(),
                    b.getExpiryDate(), days <= 0 ? "today" : days + (days == 1 ? " day" : " days")});
        }

        revalidate();
        repaint();
    }
}
