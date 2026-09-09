package com.bloodlink.ui;

import com.bloodlink.model.*;
import com.bloodlink.service.ScreeningService;
import com.bloodlink.util.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * The quarantine worklist. Bags sit here from collection until every TTI marker
 * has a result, and this screen is the only way they leave.
 */
public final class ScreeningPanel extends JPanel {

    private final ScreeningService service;
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel summary = Theme.subtitle(" ");
    private List<BloodBag> current;

    public ScreeningPanel(ScreeningService service) {
        this.service = service;

        Object[] columns = new Object[3 + TtiMarker.values().length];
        columns[0] = "Bag code";
        columns[1] = "Group";
        columns[2] = "Collected";
        int i = 3;
        for (TtiMarker m : TtiMarker.values()) columns[i++] = m.name();

        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = Theme.styleTable(new JTable(model));

        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(24, 24, 24, 24));
        add(header(), BorderLayout.NORTH);

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        card.add(Theme.sectionLabel("QUARANTINE WORKLIST"), BorderLayout.NORTH);
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        card.add(legend(), BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);

        for (TtiMarker m : TtiMarker.values()) Theme.colourStatusColumns(table, m.name());
        refresh();
    }

    private JPanel header() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.add(Theme.title("TTI screening"));
        t.add(Box.createVerticalStrut(5));
        t.add(Theme.subtitle("No unit reaches the shelf until all five markers are non-reactive."));
        t.add(Box.createVerticalStrut(4));
        t.add(summary);

        JPanel a = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        a.setOpaque(false);
        JButton single = Theme.secondary("Record one marker");
        single.addActionListener(e -> recordSingle());
        JButton clear = Theme.primary("Mark panel non-reactive");
        clear.addActionListener(e -> recordPanel());
        a.add(single);
        a.add(clear);

        p.add(t, BorderLayout.WEST);
        p.add(a, BorderLayout.EAST);
        return p;
    }

    private JPanel legend() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        p.setOpaque(false);
        p.add(Theme.subtitle("Outcome:"));
        p.add(Theme.chip("ALL NON-REACTIVE  ->  RELEASED TO SHELF", Theme.SUCCESS));
        p.add(Theme.chip("ANY REACTIVE  ->  BAG DISCARDED, DONOR DEFERRED", Theme.DANGER));
        return p;
    }

    private void refresh() {
        current = service.pending();
        model.setRowCount(0);
        for (BloodBag b : current) {
            Object[] row = new Object[3 + TtiMarker.values().length];
            row[0] = b.getBagCode();
            row[1] = b.getBloodGroup();
            row[2] = b.getCollectionDate();
            int i = 3;
            for (TtiMarker m : TtiMarker.values()) {
                TestResult r = b.resultFor(m);
                row[i++] = r == TestResult.PENDING ? "pending" : r == TestResult.NON_REACTIVE ? "non-reactive" : "REACTIVE";
            }
            model.addRow(row);
        }
        summary.setText(current.size() + " bag(s) in quarantine awaiting screening.");
    }

    private BloodBag selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a bag from the worklist first.");
            return null;
        }
        return current.get(row);
    }

    private void recordSingle() {
        BloodBag bag = selected();
        if (bag == null) return;

        JComboBox<TtiMarker> marker = new JComboBox<>(TtiMarker.values());
        JComboBox<String> result = new JComboBox<>(new String[]{"Non-reactive", "Reactive"});
        Object[] fields = {"Bag", new JLabel(bag.describe()), "Marker", marker, "Result", result};

        if (JOptionPane.showConfirmDialog(this, fields, "Record screening result",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        TestResult chosen = "Reactive".equals(result.getSelectedItem()) ? TestResult.REACTIVE : TestResult.NON_REACTIVE;
        apply(bag, () -> service.record(bag, (TtiMarker) marker.getSelectedItem(), chosen));
    }

    private void recordPanel() {
        BloodBag bag = selected();
        if (bag == null) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Record all five markers as non-reactive for " + bag.describe() + "?\n"
                        + "This releases the bag to issuable stock.",
                "Confirm release", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        apply(bag, () -> service.recordFullPanelNonReactive(bag));
    }

    private void apply(BloodBag bag, Runnable action) {
        try {
            action.run();
            refresh();
            String message = switch (bag.getStatus()) {
                case AVAILABLE -> bag.describe() + " cleared screening and is now issuable.\nExpires " + bag.getExpiryDate() + ".";
                case DISCARDED -> bag.describe() + " was reactive.\nThe bag has been discarded and the donor deferred pending counselling.";
                default -> "Result recorded. " + bag.describe() + " remains in quarantine until the panel is complete.";
            };
            JOptionPane.showMessageDialog(this, message, "Screening",
                    bag.getStatus() == BagStatus.DISCARDED ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Screening", JOptionPane.ERROR_MESSAGE);
        }
    }
}
