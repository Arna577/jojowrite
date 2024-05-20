package net.arna.jojowrite.asm.instruction;

import net.arna.jojowrite.JJWUtils;
import net.arna.jojowrite.asm.Compiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A segment of an {@link Instruction}s {@link Format}.
 * Used for pattern-matching against an input String which ultimately decides whether a String is a valid Instruction.
 */
public final class Part {
    public enum ArgumentType {
        /**
         * Represents a pointer. Length: 4 bytes, Example: 06280000
         */
        LABEL,
        /**
         * Represents an offset. Length: 1-3 bytes, Example: $03FC
         */
        DISPLACEMENT,
        /**
         * Represents a direct value. Length: 1 byte, Example: #$40
         */
        IMMEDIATE,
        /**
         * Represents a system register. Length: 1 fragment, Example: R12
         */
        REGISTER,
    }

    public enum PartType {
        STATIC,
        VARIABLE
    }

    private final PartType type;
    private final String segment;
    private final ArgumentType argumentType;
    private final List<Fragment> fragments;
    private final int fragSize;
    private int dispMax = 0x00;
    private String dispZeroPadding = null;

    private Part(PartType type, String segment, ArgumentType argumentType, List<Fragment> fragments) {
        this.type = type;
        this.segment = segment;
        this.argumentType = argumentType;

        this.fragments = fragments;

        if (fragments == null) {
            fragSize = 0;
        } else {
            fragSize = fragments.size();

            if (argumentType == ArgumentType.REGISTER && fragSize != 1)
                throw new IllegalArgumentException("Parts of ArgumentType.REGISTER must have only one fragment!");

            if (argumentType == ArgumentType.DISPLACEMENT || argumentType == ArgumentType.LABEL) {
                dispMax = ( 1 << (fragSize * 4) ) - 1;
                dispZeroPadding = "0".repeat(fragSize);
            }
        }
    }

    /**
     * @param segment The identifiable string of this Part.
     * @return A new text-based Part.
     */
    public static Part StaticPart(String segment) {
        return new Part(PartType.STATIC, segment, null, null);
    }

    /**
     * @param fragments The fragments this Part refers to.
     * @return A new parametric Part.
     */
    public static Part VariablePart(ArgumentType argumentType, List<Fragment> fragments) {
        // Sanity checks
        String argumentChar = "";
        for (Fragment fragment : fragments) {
            if (fragment.getType() == Fragment.FragmentType.STATIC) {
                throw new IllegalArgumentException("The fragments that a Part refers to must all be VARIABLE!");
            }

            if (argumentChar.equals("")) {
                argumentChar = fragment.toString();
            } else if (!argumentChar.equals(fragment.toString())) {
                throw new IllegalArgumentException("The fragments that a Part refers to must all refer to the same argument!");
            }
        }

        if (argumentType == ArgumentType.REGISTER && fragments.size() != 1) {
            throw new IllegalArgumentException("Parts of ArgumentType.REGISTER must have exactly one fragment!");
        }

        return new Part(PartType.VARIABLE, null, argumentType, fragments);
    }

    /**
     * Return type for {@link Part#matches(String, Format.CompilationContext)}.
     * Holds:
     * @param match Whether the input strings start matches this Part.
     * @param remaining The unprocessed remains of the input string.
     * @param fragmentHexDigitMap A map of which fragments within the greater {@link Instruction} to assign upon compilation.
     */
    public record MatchData(boolean match, String remaining, Optional<Map<Fragment, Character>> fragmentHexDigitMap) {}

    /**
     * The main method which drives compilation.
     * Receives a string, and determines whether the start of it matches this Part.
     */
    public MatchData matches(String in, Format.CompilationContext context) {
        if (type == PartType.STATIC) {
            if (in.startsWith(segment)) {
                return new MatchData(true, in.substring(segment.length()), Optional.empty());
            }  //else return raiseCompilerError(format, "Invalid instruction, expected: " + segment);
        }

        if (type == PartType.VARIABLE) {
            Format format = context.format();
            Map<Fragment, Character> out = new HashMap<>();
            
            switch (argumentType) {
                case LABEL -> {
                    if (in.startsWith("$")) { in = in.substring(1); } // Optional $ at start of label

                    if (in.length() == 8) {
                        try {
                            int pointerAddress = Integer.valueOf(in, 16);
                            // + 4 is a forced offset due to it being impractical to jump to the direct next instruction
                            int instructionAddress = Integer.valueOf(context.address(), 16) + 4;
                            if (instructionAddress > pointerAddress) {
                                return raiseCompilerError(format, "Non-referential branching instruction cannot jump back");
                            } else {
                                if (pointerAddress % 2 == 0) {
                                    // Convert absolute address to valid relative offset
                                    int offset = pointerAddress - instructionAddress;
                                    offset /= context.displacementMutation().getModifier();
                                    if (offset > dispMax) {
                                        return raiseCompilerError(format, "Cannot jump that far");
                                    }
                                    String offsetStr = Integer.toString(offset, 16);
                                    offsetStr = (dispZeroPadding + offsetStr).substring(offsetStr.length());
                                    for (int i = 0; i < fragSize; i++) {
                                        out.put(fragments.get(i), offsetStr.charAt(i));
                                    }
                                    return new MatchData(true, "", Optional.of(out));
                                } else {
                                    return raiseCompilerError(format, "Cannot branch to unaligned address");
                                }
                            }
                        } catch (NumberFormatException e) {
                            return raiseCompilerError(format, "Invalid character in Hex literal");
                        }
                    } else {
                        return raiseCompilerError(format, "Invalid pointer");
                    }
                }

                case DISPLACEMENT -> { // 1 or 2 bytes
                    if (in.startsWith("$")) { in = in.substring(1); } // Optional $ at start of displacement

                    DisplacementMutation dispMutation = context.displacementMutation();

                    if (dispMutation == DisplacementMutation.NONE) { // Guaranteed 1 byte due to instruction length limitations
                        if (in.length() >= fragSize) { // "8F" in $8F
                            for (int i = 0; i < fragSize; i++) {
                                char digit = in.charAt(i);
                                if (JJWUtils.isHexadecimal(digit)) {
                                    out.put(fragments.get(i), digit);
                                } else {
                                    return raiseCompilerError(format, "Invalid character in Hex literal");
                                }
                            }
                            return new MatchData(true, in.substring(fragSize), Optional.of(out));
                        }
                    } else {
                        if (in.length() >= fragSize * 2) { // Possibly 2 bytes
                            String displacementStr = "";
                            for (int i = 0; i < fragSize; i++) {
                                String byteStr = in.substring(i * 2, i * 2 + 2);
                                if (JJWUtils.isHexadecimal(byteStr)) {
                                    displacementStr += byteStr;
                                } else {
                                    return raiseCompilerError(format, "Invalid character in Hex literal");
                                }
                            }

                            int dispValue = Integer.valueOf(displacementStr, 16);
                            if (dispValue % dispMutation.getModifier() != 0) {
                                return raiseCompilerError(format, "Invalid displacement, should be a multiple of " + dispMutation.getModifier());
                            }

                            // Account for memory alignment
                            dispValue /= dispMutation.getModifier();
                            if (dispValue > dispMax) {
                                return raiseCompilerError(format, "Displacement value too large");
                            }

                            // Append leading zeros
                            String displacementValueStr = Integer.toString(dispValue, 16);
                            if (fragSize > 1) {
                                displacementValueStr = (dispZeroPadding + displacementValueStr).substring(displacementValueStr.length());
                            }

                            for (int i = 0; i < fragSize; i++) {
                                out.put(fragments.get(i), displacementValueStr.charAt(i));
                            }

                            return new MatchData(true, in.substring(fragSize * 2), Optional.of(out));
                        } else {
                            return raiseCompilerError(format, "Incorrect displacement length");
                        }
                    }
                }

                case IMMEDIATE -> { // Always 1 byte long
                    if (in.startsWith("$")) { // "$" in $8F
                        if (in.length() > fragSize) { // "8F" in $8F
                            for (int i = 0; i < fragSize; i++) {
                                char digit = in.charAt(i + 1);
                                if (JJWUtils.isHexadecimal(digit)) {
                                    out.put(fragments.get(i), digit);
                                } else {
                                    return raiseCompilerError(format, "Invalid character in Hex literal");
                                }
                            }
                            return new MatchData(true, in.substring(1 + fragSize), Optional.of(out));
                        } else {
                            return raiseCompilerError(format, "Incorrect immediate value length");
                        }
                    } else {
                        return raiseCompilerError(format, "Expected $ at start of immediate value");
                    }
                }

                case REGISTER -> {
                    if (in.length() > 1) {
                        if (in.startsWith("R")) { // "R" in R0
                            int cutoff = 2;
                            String registerNumber = in.substring(1, cutoff);
                            try {
                                if (in.length() > 2) { // Detect second decimal digit
                                    String nextCharacter = in.substring(2, 3);
                                    if (JJWUtils.isDecimal(nextCharacter)) {
                                        registerNumber += nextCharacter;
                                        cutoff++;
                                    }
                                } else {
                                    raiseCompilerError(format, "Expected decimal index after registry specifier");
                                }
                                int registerId = Integer.valueOf(registerNumber, 10);
                                if (registerId > 0x0F) {
                                    return raiseCompilerError(format, "Invalid Register ID");
                                }
                                out.put(fragments.get(0), JJWUtils.HEX_DIGITS.charAt(registerId));
                                return new MatchData(true, in.substring(cutoff), Optional.of(out));
                            } catch (NumberFormatException e) {
                                return raiseCompilerError(format, "Invalid character in Decimal literal");
                            }
                        } //else return raiseCompilerError(format, "Expected R at start of register reference");
                    }
                }
            }
        }

        return fail;
    }

    private static final MatchData fail = new MatchData(false, null, null);
    public MatchData raiseCompilerError(Format format, String error) {
        Compiler.raiseError(error + " - format: " + format);
        return fail;
    }

    @Override
    public String toString() {
        if (type == PartType.STATIC) {
            return segment;
        }

        if (type == PartType.VARIABLE) {
            StringBuilder out = new StringBuilder();

            switch (argumentType) {
                case IMMEDIATE -> out.append("$imm");
                case DISPLACEMENT -> out.append("disp");
                case LABEL -> out.append("label");
                case REGISTER -> {
                    out.append("R");
                    out.append(fragments.get(0).asSingleChar());
                }
            }

            return out.toString();
        }

        throw new RuntimeException("Reading toString() of invalid Part!");
    }

    public boolean isVariable() {
        return type == PartType.VARIABLE;
    }

    public ArgumentType getArgumentType() {
        return this.argumentType;
    }
}