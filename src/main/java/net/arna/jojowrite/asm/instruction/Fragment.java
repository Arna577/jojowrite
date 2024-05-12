package net.arna.jojowrite.asm.instruction;

import net.arna.jojowrite.JJWUtils;

/**
 * Represents one half of a byte within a compiled Assembly instruction.
 */
public class Fragment {
    private final FragmentType type;
    private final Byte value;
    private final Character identifier;
    protected Fragment(FragmentType type, Byte value, Character identifier) {
        this.type = type;
        this.value = value;
        this.identifier = identifier;
    }

    public static Fragment StaticFragment(byte value) {
        return new Fragment(FragmentType.STATIC, value, null);
    }

    public static Fragment VariableFragment(char identifier) {
        return new Fragment(FragmentType.VARIABLE, null, identifier);
    }

    public FragmentType getType() {
        return type;
    }

    enum FragmentType {
        STATIC,
        VARIABLE,
    }

    @Override
    public String toString() {
        if (type == FragmentType.STATIC)
            return JJWUtils.HEX_DIGITS.substring(value, value + 1);
        if (type == FragmentType.VARIABLE)
            return identifier.toString();
        throw new IllegalStateException("Fragment type is invalid!");
    }
}
