package net.arna.jojowrite.asm.instruction;

/**
 * An enum that informs the compiler of the type of changes that have to be applied to a displacement to get its correct compiled value.
 */
public enum DisplacementMutation {
    NONE(1),
    WORD(2),
    LONG(4);

    private final int modifier;
    DisplacementMutation(int modifier) {
        this.modifier = modifier;
    }

    /**
     * @return The int to divide a displacement by to get its compiled value.
     */
    public int getModifier() {
        return modifier;
    }
}
