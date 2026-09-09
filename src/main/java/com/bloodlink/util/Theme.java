package com.bloodlink.util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Single source of truth for the BloodLink palette: deep maroon chrome, a red
 * accent and white surfaces.
 */
public final class Theme {

    // Core palette
    public static final Color MAROON_DEEP = new Color(74, 12, 20);    // sidebar, table headers
    public static final Color MAROON      = new Color(109, 17, 28);   // hover, gradients
    public static final Color RED         = new Color(178, 27, 40);   // primary accent
    public static final Color RED_DARK    = new Color(139, 20, 32);
    public static final Color RED_SOFT    = new Color(250, 236, 237); // tinted fills

    // Surfaces and text
    public static final Color BG     = new Color(250, 246, 246);
    public static final Color CARD   = Color.WHITE;
    public static final Color TEXT   = new Color(38, 22, 25);
    public static final Color MUTED  = new Color(133, 110, 113);
    public static final Color BORDER = new Color(233, 220, 221);

    // Status colours
    public static final Color SUCCESS = new Color(45, 106, 79);
    public static final Color WARNING = new Color(176, 112, 18);
    public static final Color DANGER  = RED;

    private Theme() {}

    public static void apply() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        UIManager.put("Label.font", new Font("SansSerif", Font.PLAIN, 14));
        UIManager.put("Button.font", new Font("SansSerif", Font.BOLD, 13));
        UIManager.put("Table.font", new Font("SansSerif", Font.PLAIN, 13));
        UIManager.put("Table.rowHeight", 32);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("Table.selectionBackground", RED_SOFT);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("TextField.font", new Font("SansSerif", Font.PLAIN, 14));
        UIManager.put("PasswordField.font", new Font("SansSerif", Font.PLAIN, 14));
        UIManager.put("OptionPane.background", BG);
        UIManager.put("Panel.background", BG);
    }

    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 28));
        l.setForeground(MAROON_DEEP);
        return l;
    }

    public static JLabel subtitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 13));
        l.setForeground(MUTED);
        return l;
    }

    public static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(MAROON);
        return l;
    }

    public static JButton primary(String text) {
        JButton b = new JButton(text);
        b.setBackground(RED);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(10, 16, 10, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        hover(b, RED, MAROON);
        return b;
    }

    public static JButton secondary(String text) {
        JButton b = new JButton(text);
        b.setBackground(Color.WHITE);
        b.setForeground(MAROON_DEEP);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(9, 14, 9, 14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        hover(b, Color.WHITE, RED_SOFT);
        return b;
    }

    private static void hover(JButton b, Color normal, Color over) {
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(over); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { b.setBackground(normal); }
        });
    }

    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(16, 16, 16, 16)));
        return p;
    }

    /** Small coloured pill used for statuses in headers and cards. */
    public static JLabel chip(String text, Color colour) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(Color.WHITE);
        l.setOpaque(true);
        l.setBackground(colour);
        l.setBorder(new EmptyBorder(4, 9, 4, 9));
        return l;
    }

    /** Applies the maroon header treatment to a table. */
    public static JTable styleTable(JTable table) {
        JTableHeader header = table.getTableHeader();
        header.setBackground(MAROON_DEEP);
        header.setForeground(Color.WHITE);
        header.setFont(new Font("SansSerif", Font.BOLD, 12));
        header.setReorderingAllowed(false);
        table.setShowGrid(true);
        table.setGridColor(BORDER);
        table.setFillsViewportHeight(true);
        table.setRowHeight(32);
        return table;
    }

    /** Colour for a stock, bag or screening status word. */
    public static Color statusColour(String status) {
        return switch (status.toUpperCase().replace('-', '_').replace(' ', '_')) {
            case "LOW_STOCK", "CRITICAL", "EXPIRED", "DISCARDED", "REACTIVE" -> DANGER;
            case "WATCH", "QUARANTINED", "PENDING", "RESERVED" -> WARNING;
            case "HEALTHY", "AVAILABLE", "NON_REACTIVE", "ISSUED" -> SUCCESS;
            default -> MUTED;
        };
    }

    /** Renders a status cell in its status colour. */
    public static DefaultTableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                                     boolean focus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, selected, focus, row, column);
                String text = String.valueOf(value);
                c.setForeground(statusColour(text));
                c.setFont(new Font("SansSerif", "pending".equalsIgnoreCase(text) ? Font.PLAIN : Font.BOLD, 12));
                return c;
            }
        };
    }

    /** Applies the status renderer to the named columns of a table. */
    public static void colourStatusColumns(JTable table, String... columnNames) {
        for (String name : columnNames) {
            try {
                table.getColumn(name).setCellRenderer(statusRenderer());
            } catch (IllegalArgumentException ignored) {
                // Column not present in this table; nothing to colour.
            }
        }
    }
}
