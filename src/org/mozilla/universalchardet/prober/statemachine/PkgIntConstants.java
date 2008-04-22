package org.mozilla.universalchardet.prober.statemachine;

public interface PkgIntConstants {
    public static final int INDEX_SHIFT_4BITS   = 3;
    public static final int INDEX_SHIFT_8BITS   = 2;
    public static final int INDEX_SHIFT_16BITS  = 1;

    public static final int SHIFT_MASK_4BITS    = 7;
    public static final int SHIFT_MASK_8BITS    = 3;
    public static final int SHIFT_MASK_16BITS   = 1;

    public static final int BIT_SHIFT_4BITS     = 2;
    public static final int BIT_SHIFT_8BITS     = 3;
    public static final int BIT_SHIFT_16BITS    = 4;

    public static final int UNIT_MASK_4BITS     = 0x0000000F;
    public static final int UNIT_MASK_8BITS     = 0x000000FF;
    public static final int UNIT_MASK_16BITS    = 0x0000FFFF;
}
