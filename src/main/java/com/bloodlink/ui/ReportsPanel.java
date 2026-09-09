package com.bloodlink.ui;

import com.bloodlink.AppContext;
import com.bloodlink.model.*;
import com.bloodlink.util.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public final class ReportsPanel extends JPanel {

    private final AppContext ctx;
    private final JTextArea report = new JTextArea();

    public ReportsPanel(AppContext ctx) {
        this.ctx = ctx;
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));
        t.add(Theme.title("Reports and insights"));
        t.add(Box.createVerticalStrut(5));
        t.add(Theme.subtitle("Operational summary including shelf life, screening and wastage."));
        JButton refresh = Theme.primary("Refresh report");
        refresh.addActionListener(e -> refresh());
        h.add(t, BorderLayout.WEST);
        h.add(refresh, BorderLayout.EAST);
        add(h, BorderLayout.NORTH);

        JPanel c = Theme.card();
        c.setLayout(new BorderLayout());
        report.setEditable(false);
        report.setFont(new Font("Monospaced", Font.PLAIN, 13));
        report.setBackground(Color.WHITE);
        report.setForeground(Theme.TEXT);
        c.add(new JScrollPane(report), BorderLayout.CENTER);
        add(c, BorderLayout.CENTER);

        refresh();
    }

    private void refresh() {
        var inv = ctx.inventoryService;
        LocalDate today = LocalDate.now();
        StringBuilder b = new StringBuilder();

        b.append("BLOODLINK OPERATIONS REPORT\n");
        b.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))).append('\n');
        b.append("=".repeat(72)).append("\n\n");

        b.append("DONORS\n");
        b.append("  Total registered : ").append(ctx.donorService.all().size()).append('\n');
        b.append("  Available now    : ").append(ctx.donorService.availableCount()).append("\n\n");

        b.append("ISSUABLE STOCK BY GROUP\n");
        for (Map.Entry<BloodGroup, Integer> e : inv.stock().entrySet())
            b.append(String.format("  %-4s : %3d units %s%n", e.getKey(), e.getValue(), e.getValue() < 10 ? "[LOW]" : ""));
        b.append("  Total issuable   : ").append(inv.total()).append("\n\n");

        b.append("BAG LIFECYCLE\n");
        for (BagStatus s : BagStatus.values())
            b.append(String.format("  %-12s : %3d%n", s, inv.bagsWithStatus(s).size()));
        b.append("  Registered bags  : ").append(inv.allBags().size()).append("\n\n");

        b.append("SHELF LIFE\n");
        List<BloodBag> expiring = inv.expiringSoon();
        b.append("  Expiring within ").append(com.bloodlink.service.InventoryService.EXPIRY_WARNING_DAYS)
                .append(" days : ").append(expiring.size()).append('\n');
        for (BloodBag bag : expiring)
            b.append(String.format("    %-16s %-4s %-18s expires %s (%d day(s))%n",
                    bag.getBagCode(), bag.getBloodGroup(), bag.getComponent().getLabel(),
                    bag.getExpiryDate(), bag.daysToExpiry(today)));
        b.append('\n');

        b.append("SCREENING\n");
        b.append("  Awaiting TTI panel : ").append(ctx.screeningService.pendingCount()).append('\n');
        b.append("  Discarded reactive : ").append(inv.discardedCount()).append("\n\n");

        b.append("WASTAGE\n");
        b.append("  Expired          : ").append(inv.expiredCount()).append('\n');
        b.append("  Discarded        : ").append(inv.discardedCount()).append('\n');
        b.append("  Issued           : ").append(inv.issuedCount()).append('\n');
        b.append(String.format("  Wastage rate     : %.1f%% of all resolved bags%n%n", inv.wastagePercent()));

        b.append("REQUESTS\n");
        b.append("  Pending          : ").append(ctx.requestService.pending()).append('\n');
        b.append("  Active emergencies: ").append(ctx.requestService.emergencies()).append('\n');
        b.append("  Total            : ").append(ctx.requestService.all().size()).append("\n\n");

        b.append("APPOINTMENTS\n");
        b.append("  Scheduled or recorded : ").append(ctx.appointmentService.all().size()).append("\n\n");

        b.append("AUDIT\n");
        b.append("  Entries in trail : ").append(ctx.auditService.all().size()).append('\n');
        b.append("  Recorded today   : ").append(ctx.auditService.countToday()).append('\n');

        report.setText(b.toString());
        report.setCaretPosition(0);
    }
}
