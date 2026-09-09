package com.bloodlink.model;

import java.util.*;

public enum BloodGroup {
    A_POS("A+"), A_NEG("A-"), B_POS("B+"), B_NEG("B-"),
    AB_POS("AB+"), AB_NEG("AB-"), O_POS("O+"), O_NEG("O-");

    private final String label;
    BloodGroup(String label) { this.label = label; }
    @Override public String toString() { return label; }

    public static BloodGroup from(String value) {
        for (BloodGroup g : values()) if (g.label.equalsIgnoreCase(value) || g.name().equalsIgnoreCase(value)) return g;
        throw new IllegalArgumentException("Unknown blood group: " + value);
    }

    public Set<BloodGroup> compatibleDonors() {
        return switch (this) {
            case O_NEG -> EnumSet.of(O_NEG);
            case O_POS -> EnumSet.of(O_NEG, O_POS);
            case A_NEG -> EnumSet.of(O_NEG, A_NEG);
            case A_POS -> EnumSet.of(O_NEG, O_POS, A_NEG, A_POS);
            case B_NEG -> EnumSet.of(O_NEG, B_NEG);
            case B_POS -> EnumSet.of(O_NEG, O_POS, B_NEG, B_POS);
            case AB_NEG -> EnumSet.of(O_NEG, A_NEG, B_NEG, AB_NEG);
            case AB_POS -> EnumSet.allOf(BloodGroup.class);
        };
    }
}
