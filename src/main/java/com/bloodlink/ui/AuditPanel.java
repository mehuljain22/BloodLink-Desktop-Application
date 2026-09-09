package com.bloodlink.ui;

import com.bloodlink.model.AuditAction;
import com.bloodlink.model.AuditEvent;
import com.bloodlink.service.AuditService;
import com.bloodlink.util.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Read only view of the audit trail. There is deliberately no edit or delete
 * action anywhere on this screen.
 */
public final class AuditPanel extends JPanel {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm:ss");

    private final AuditService service;
    private final JTextField search = new JTextField();
    private final JComboBox<String> category = new JComboBox<>(
            new String[]{"All activity", "Security", "Donors", "Inventory and screening", "Requests", "Appointments"});
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"When", "Who", "Role", "Action", "Record", "Details"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = Theme.styleTable(new JTable(model));
    private final JLabel count = Theme.subtitle(" ");

    public AuditPanel(AuditService service) {
        this.service = service;
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(24, 24, 24, 24));
        add(header(), BorderLayout.NORTH);

        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 12));
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);

        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(5).setPreferredWidth(420);
        refresh();
    }

    private JPanel header() {
        JPanel wrap = new JPanel(new BorderLayout(0, 12));
        wrap.setOpaque(false);

        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.add(Theme.title("Audit trail"));
        t.add(Box.createVerticalStrut(5));
        t.add(Theme.subtitle("Append only record of who did what, to which record, and when. Entries cannot be edited or removed."));
        t.add(Box.createVerticalStrut(4));
        t.add(count);
        wrap.add(t, BorderLayout.NORTH);

        JPanel controls = new JPanel(new BorderLayout(10, 0));
        controls.setOpaque(false);
        search.setPreferredSize(new Dimension(300, 38));
        search.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER), new EmptyBorder(8, 10, 8, 10)));
        search.addActionListener(e -> refresh());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        category.addActionListener(e -> refresh());
        JButton apply = Theme.secondary("Filter");
        apply.addActionListener(e -> refresh());
        JButton reload = Theme.primary("Refresh");
        reload.addActionListener(e -> { search.setText(""); category.setSelectedIndex(0); refresh(); });
        right.add(category);
        right.add(apply);
        right.add(reload);

        JPanel searchWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchWrap.setOpaque(false);
        searchWrap.add(Theme.subtitle("Search actor, action, record or details"));
        searchWrap.add(search);
        controls.add(searchWrap, BorderLayout.WEST);
        controls.add(right, BorderLayout.EAST);
        wrap.add(controls, BorderLayout.SOUTH);
        return wrap;
    }

    private void refresh() {
        String term = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        String choice = String.valueOf(category.getSelectedItem());

        List<AuditEvent> events = service.all().stream()
                .filter(e -> matchesCategory(e.getAction(), choice))
                .filter(e -> term.isEmpty()
                        || e.getActor().toLowerCase().contains(term)
                        || e.getAction().getLabel().toLowerCase().contains(term)
                        || String.valueOf(e.getEntityId()).toLowerCase().contains(term)
                        || String.valueOf(e.getDetails()).toLowerCase().contains(term))
                .collect(Collectors.toList());

        model.setRowCount(0);
        for (AuditEvent e : events)
            model.addRow(new Object[]{
                    e.getTimestamp().format(STAMP), e.getActor(), e.getActorRole(),
                    e.getAction().getLabel(), e.getEntityType() + " " + e.getEntityId(), e.getDetails()});

        count.setText(events.size() + " entr" + (events.size() == 1 ? "y" : "ies") + " shown, "
                + service.countToday() + " recorded today.");
    }

    private boolean matchesCategory(AuditAction action, String choice) {
        return switch (choice) {
            case "Security" -> action.isSecurityEvent();
            case "Donors" -> action.name().startsWith("DONOR_");
            case "Inventory and screening" -> action.name().startsWith("BAG_") || action == AuditAction.SCREENING_RECORDED;
            case "Requests" -> action.name().startsWith("REQUEST_");
            case "Appointments" -> action.name().startsWith("APPOINTMENT_");
            default -> true;
        };
    }
}
