package com.bloodlink;

import com.bloodlink.ui.LoginFrame;
import com.bloodlink.util.Theme;

import javax.swing.*;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.apply();
            AppContext context = AppContext.bootstrap();
            new LoginFrame(context).setVisible(true);
        });
    }
}
