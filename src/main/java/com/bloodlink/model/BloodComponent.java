package com.bloodlink.model;

/**
 * A blood donation is separated into components, and each component has its own
 * shelf life. Shelf life is the single most important property here because it
 * drives expiry, FEFO issuing and wastage reporting.
 */
public enum BloodComponent {
    WHOLE_BLOOD("Whole Blood", 35),
    PRBC("Packed Red Cells", 42),
    PLATELETS("Platelets", 5),
    PLASMA("Fresh Frozen Plasma", 365);

    private final String label;
    private final int shelfLifeDays;

    BloodComponent(String label, int shelfLifeDays) {
        this.label = label;
        this.shelfLifeDays = shelfLifeDays;
    }

    public String getLabel() { return label; }
    public int getShelfLifeDays() { return shelfLifeDays; }

    @Override public String toString() { return label; }

    public static BloodComponent from(String value) {
        for (BloodComponent c : values())
            if (c.name().equalsIgnoreCase(value) || c.label.equalsIgnoreCase(value)) return c;
        throw new IllegalArgumentException("Unknown component: " + value);
    }
}
