package net.arna.jojowrite.asm.instruction;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public final class Instruction {
    private final Collection<Fragment> fragments;
    private final Format format;
    private final String comment;

    public Instruction(Collection<Fragment> fragments, Format format, String comment) {
        if (fragments.size() != 4)
            throw new RuntimeException("Attempted to create an Instruction with an invalid amount of fragments!");

        this.fragments = fragments;
        this.format = format;
        this.comment = comment;
    }

    public Instruction(Collection<Fragment> fragments, Format format) {
        this(fragments, format, null);
    }

    public boolean hasVariants() {
        return fragments.stream().anyMatch(fragment -> fragment.getType() == Fragment.FragmentType.VARIABLE);
    }

    /**
     * @return Instruction@hash{ format seen in manual -> compiled bytemap }
     */
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        for (Fragment fragment : fragments) out.append(fragment.asSingleChar());
        return "Instruction@" + Integer.toHexString(hashCode()) + "{ " + format.toString() + " -> " + out + " }";
    }

    public String compileToHexString(Map<Fragment, Character> fragmentData) {
        StringBuilder out = new StringBuilder();
        for (Fragment fragment : fragments) {
            if (fragment.getType() == Fragment.FragmentType.STATIC) {
                out.append(fragment.asSingleChar());
            } else if (fragment.getType() == Fragment.FragmentType.VARIABLE) {
                out.append(fragmentData.get(fragment));
            }
        }
        return out.toString();
    }

    public Format getFormat() {
        return format;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Instruction) obj;
        return Objects.equals(this.fragments, that.fragments) &&
                Objects.equals(this.format, that.format) &&
                Objects.equals(this.comment, that.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fragments, format, comment);
    }
}
