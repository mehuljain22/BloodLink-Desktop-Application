package com.bloodlink.util;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A button that paints its own background.
 *
 * Swing's platform look-and-feels (notably the Windows one) ignore
 * setBackground() on buttons and paint the native skin instead, while still
 * honouring setForeground(). The result is white text on a pale native button,
 * which is invisible. Painting the fill ourselves removes that whole class of
 * problem, so BloodLink looks identical on Windows, macOS and Linux.
 */
public final class FlatButton extends JButton {

    private Color fill;
    private final Color hoverFill;
    private final Color outline;
    private boolean hovered;

    public FlatButton(String text, Color fill, Color hoverFill, Color textColour, Color outline) {
        super(text);
        this.fill = fill;
        this.hoverFill = hoverFill;
        this.outline = outline;

        setForeground(textColour);
        setBackground(fill == null ? new Color(0, 0, 0, 0) : fill);
        // Stop the look-and-feel from painting anything of its own.
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
        });
    }

    /** Sidebar use: an active item gets a solid fill, an inactive one is transparent. */
    public void setActive(boolean active) {
        this.fill = active ? Theme.RED : null;
        setForeground(Color.WHITE);
        repaint();
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color paint = hovered && hoverFill != null ? hoverFill : fill;
        if (paint != null) {
            g2.setColor(paint);
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
        if (outline != null) {
            g2.setColor(outline);
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
        g2.dispose();
        super.paintComponent(g);   // draws the label text only
    }
}
