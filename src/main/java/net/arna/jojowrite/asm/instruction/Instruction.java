package net.arna.jojowrite.asm.instruction;

import java.util.*;

/**
 * Data structure that describes an Assembly instruction.
 * Every instruction is composed of 4 {@link Fragment}s and a {@link Format}.
 * Compiles into 2 bytes (via {@link Instruction#compileToHexString(Map)}).
 */
public final class Instruction {
    private final List<Fragment> fragments;
    private final Format format;
    private final String comment;
    private final int fragSize;

    public Instruction(List<Fragment> fragments, Format format, String comment) {
        if (fragments == null) {
            fragSize = 0;
        } else {
            fragSize = fragments.size();

            if (fragSize != 4)
                throw new RuntimeException("Attempted to create an Instruction with an invalid amount of fragments!");
        }

        this.fragments = fragments;
        this.format = format;
        this.comment = comment;
    }

    public Instruction(List<Fragment> fragments, Format format) {
        this(fragments, format, null);
    }

    public boolean hasVariants() {
        return fragments.stream().anyMatch(fragment -> fragment.getType() == Fragment.FragmentType.VARIABLE);
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        for (Fragment fragment : fragments) out.append(fragment.asSingleChar());
        return "Instruction@" + Integer.toHexString(hashCode()) + "{ " + format.toString() + " -> " + out + " }";
    }

    /**
     * @return Instruction@hash{ format seen in manual -> compiled bytemap }
     */
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

    // Yeah, I COULD compute this concisely, but I don't feel like it and hex digits won't be changing any time soon.
    private static final Map<Character, Byte> characterValueMap = new HashMap<>(
            Map.ofEntries(
                    Map.entry('0', (byte)0),
                    Map.entry('1', (byte)1),
                    Map.entry('2', (byte)2),
                    Map.entry('3', (byte)3),
                    Map.entry('4', (byte)4),
                    Map.entry('5', (byte)5),
                    Map.entry('6', (byte)6),
                    Map.entry('7', (byte)7),
                    Map.entry('8', (byte)8),
                    Map.entry('9', (byte)9),
                    Map.entry('A', (byte)0xA),
                    Map.entry('B', (byte)0xB),
                    Map.entry('C', (byte)0xC),
                    Map.entry('D', (byte)0xD),
                    Map.entry('E', (byte)0xE),
                    Map.entry('F', (byte)0XF),
                    Map.entry('a', (byte)0xa),
                    Map.entry('b', (byte)0xb),
                    Map.entry('c', (byte)0xc),
                    Map.entry('d', (byte)0xd),
                    Map.entry('e', (byte)0xe),
                    Map.entry('f', (byte)0Xf)
            )
    );
    public byte[] compileToBytes(Map<Fragment, Character> fragmentData) {
        byte[] out = new byte[2];
        byte[] outRaw = new byte[fragSize];

        for (int i = 0; i < fragSize; i++) {
            Fragment fragment = fragments.get(i);
            if (fragment.getType() == Fragment.FragmentType.STATIC) {
                outRaw[i] = fragment.getValue();
            } else if (fragment.getType() == Fragment.FragmentType.VARIABLE) {
                outRaw[i] = characterValueMap.get(fragmentData.get(fragment));
            }
        }

        for (int i = 0; i < fragSize; i += 2) {
            out[i / 2] = (byte) ((outRaw[i] << 4) + outRaw[i + 1]);
        }

        return out;
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
