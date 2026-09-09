package com.bloodlink.ui;

import com.bloodlink.model.*;
import com.bloodlink.service.InventoryService;
import com.bloodlink.util.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Inventory now has two views: the familiar per group summary, and the bag
 * register underneath it, where every physical unit is visible with its own
 * barcode, expiry and status.
 */
public final class InventoryPanel extends JPanel {

    private final InventoryService service;
    private final JPanel grid = new JPanel(new GridLayout(2, 4, 12, 12));
    private final JComboBox<String> filter = new JComboBox<>(
            new String[]{"All bags", "Available", "Quarantined", "Reserved", "Expiring soon", "Issued", "Expired", "Discarded"});
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Bag code", "Group", "Component", "Donor", "Collected", "Expires", "Days left", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = Theme.styleTable(new JTable(model));
    private List<BloodBag> current;

    public InventoryPanel(InventoryService service) {
        this.service = service;
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        add(header(), BorderLayout.NORTH);

        JPanel centre = new JPanel(new BorderLayout(0, 16));
        centre.setOpaque(false);
        grid.setOpaque(false);
        grid.setPreferredSize(new Dimension(0, 250));
        centre.add(grid, BorderLayout.NORTH);
        centre.add(register(), BorderLayout.CENTER);
        add(centre, BorderLayout.CENTER);

        refresh();
    }

    private JPanel header() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.add(Theme.title("Blood inventory"));
        t.add(Box.createVerticalStrut(5));
        t.add(Theme.subtitle("Every unit is tracked as an individual bag with its own expiry date. Issuing is first expired, first out."));

        JPanel a = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        a.setOpaque(false);
        JButton sweep = Theme.secondary("Run expiry sweep");
        sweep.addActionListener(e -> sweep());
        JButton discard = Theme.secondary("Discard selected");
        discard.addActionListener(e -> discard());
        JButton issue = Theme.secondary("Issue units");
        issue.addActionListener(e -> issue());
        JButton collect = Theme.primary("+ Record collection");
        collect.addActionListener(e -> collect());
        a.add(sweep);
        a.add(discard);
        a.add(issue);
        a.add(collect);

        p.add(t, BorderLayout.WEST);
        p.add(a, BorderLayout.EAST);
        return p;
    }

    private JPanel register() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));

        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        JLabel heading = Theme.sectionLabel("BAG REGISTER");
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(Theme.subtitle("Show"));
        filter.addActionListener(e -> refreshTable());
        right.add(filter);
        bar.add(heading, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);

        card.add(bar, BorderLayout.NORTH);
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        Theme.colourStatusColumns(table, "Status", "Days left");
        return card;
    }

    private void refresh() {
        grid.removeAll();
        Map<BloodGroup, Integer> stock = service.stock();
        for (BloodGroup g : BloodGroup.values()) {
            int units = stock.getOrDefault(g, 0);
            String status = units < 10 ? "LOW STOCK" : units < 20 ? "WATCH" : "HEALTHY";

            JPanel c = Theme.card();
            c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));

            JLabel group = new JLabel(g.toString());
            group.setForeground(Theme.RED);
            group.setFont(new Font("SansSerif", Font.BOLD, 26));
            group.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel u = new JLabel(units + (units == 1 ? " unit" : " units"));
            u.setForeground(Theme.TEXT);
            u.setFont(new Font("SansSerif", Font.BOLD, 19));
            u.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            chipRow.setOpaque(false);
            chipRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            chipRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
            chipRow.add(Theme.chip(status, Theme.statusColour(status)));

            c.add(group);
            c.add(Box.createVerticalStrut(10));
            c.add(u);
            c.add(Box.createVerticalStrut(10));
            c.add(chipRow);
            grid.add(c);
        }
        refreshTable();
        revalidate();
        repaint();
    }

    private void refreshTable() {
        String choice = String.valueOf(filter.getSelectedItem());
        current = switch (choice) {
            case "Available" -> service.bagsWithStatus(BagStatus.AVAILABLE);
            case "Quarantined" -> service.bagsWithStatus(BagStatus.QUARANTINED);
            case "Reserved" -> service.bagsWithStatus(BagStatus.RESERVED);
            case "Issued" -> service.bagsWithStatus(BagStatus.ISSUED);
            case "Expired" -> service.bagsWithStatus(BagStatus.EXPIRED);
            case "Discarded" -> service.bagsWithStatus(BagStatus.DISCARDED);
            case "Expiring soon" -> service.expiringSoon();
            default -> service.allBags();
        };

        LocalDate today = LocalDate.now();
        model.setRowCount(0);
        for (BloodBag b : current) {
            long days = b.daysToExpiry(today);
            model.addRow(new Object[]{
                    b.getBagCode(), b.getBloodGroup(), b.getComponent().getLabel(), b.getDonorName(),
                    b.getCollectionDate(), b.getExpiryDate(),
                    days < 0 ? "expired" : days + (days == 1 ? " day" : " days"),
                    b.getStatus()
            });
        }
    }

    private BloodBag selected() {
        int row = table.getSelectedRow();
        return row < 0 ? null : current.get(row);
    }

    private void collect() {
        JComboBox<BloodGroup> group = new JComboBox<>(BloodGroup.values());
        JComboBox<BloodComponent> component = new JComboBox<>(BloodComponent.values());
        JTextField donorId = new JTextField("1001");
        JTextField donorName = new JTextField();
        JTextField date = new JTextField(LocalDate.now().toString());
        JSpinner count = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));

        Object[] fields = {
                "Blood group", group,
                "Component", component,
                "Donor ID", donorId,
                "Donor name", donorName,
                "Collection date (YYYY-MM-DD)", date,
                "Number of bags", count
        };

        if (JOptionPane.showConfirmDialog(this, fields, "Record collection",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        try {
            BloodComponent chosen = (BloodComponent) component.getSelectedItem();
            LocalDate collected = LocalDate.parse(date.getText().trim());
            int bags = (Integer) count.getValue();
            for (int i = 0; i < bags; i++)
                service.collect((BloodGroup) group.getSelectedItem(), chosen,
                        Integer.parseInt(donorId.getText().trim()), donorName.getText().trim(), collected);

            refresh();
            JOptionPane.showMessageDialog(this,
                    bags + " bag(s) recorded and placed in quarantine.\n"
                            + "Shelf life " + chosen.getShelfLifeDays() + " days, expiring "
                            + collected.plusDays(chosen.getShelfLifeDays()) + ".\n"
                            + "Complete TTI screening to release them for issue.",
                    "Collection recorded", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Collection", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void issue() {
        JComboBox<BloodGroup> group = new JComboBox<>(BloodGroup.values());
        JSpinner units = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
        JTextField reason = new JTextField("Over the counter issue");
        Object[] fields = {"Blood group", group, "Units", units, "Reason", reason};

        if (JOptionPane.showConfirmDialog(this, fields, "Issue units (FEFO)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        try {
            List<BloodBag> issued = service.issueDirect((BloodGroup) group.getSelectedItem(),
                    (Integer) units.getValue(), reason.getText().trim());
            refresh();
            StringBuilder sb = new StringBuilder("Issued nearest expiry first:\n\n");
            for (BloodBag b : issued) sb.append("  ").append(b.getBagCode()).append("  expires ").append(b.getExpiryDate()).append('\n');
            JOptionPane.showMessageDialog(this, sb.toString(), "Units issued", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Issue units", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void discard() {
        BloodBag bag = selected();
        if (bag == null) { JOptionPane.showMessageDialog(this, "Select a bag in the register first."); return; }

        String reason = JOptionPane.showInputDialog(this,
                "Reason for discarding " + bag.describe() + ":", "Discard bag", JOptionPane.QUESTION_MESSAGE);
        if (reason == null || reason.isBlank()) return;

        try {
            service.discard(bag, reason.trim());
            refresh();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Discard bag", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sweep() {
        List<BloodBag> swept = service.runExpirySweep();
        refresh();
        JOptionPane.showMessageDialog(this,
                swept.isEmpty() ? "No bags were past their expiry date."
                        : swept.size() + " bag(s) moved to EXPIRED and removed from issuable stock.",
                "Expiry sweep", JOptionPane.INFORMATION_MESSAGE);
    }
}
