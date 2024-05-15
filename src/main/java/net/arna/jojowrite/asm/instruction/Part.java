package net.arna.jojowrite.asm.instruction;

import net.arna.jojowrite.JJWUtils;
import net.arna.jojowrite.asm.Compiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Part {
    public enum ArgumentType {
        LABEL,
        // disp
        DISPLACEMENT,
        // #imm
        IMMEDIATE,
        // Rn
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
    private int dispMax = 0x00;
    private String dispZeroPadding = null;

    private Part(PartType type, String segment, ArgumentType argumentType, List<Fragment> fragments) {
        this.type = type;
        this.segment = segment;
        this.argumentType = argumentType;
        this.fragments = fragments;

        if (argumentType == ArgumentType.DISPLACEMENT || argumentType == ArgumentType.LABEL) {
            dispMax = ( 1 << (fragments.size() * 4) ) - 1;
            dispZeroPadding = "0".repeat(fragments.size());
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

    public record MatchData(boolean match, String remaining, Optional<Map<Fragment, String>> fragmentHexDigitMap) {}

    // Can't parse String by ref
    public MatchData matches(String in, Format.CompilationContext context) {
        if (type == PartType.STATIC) {
            if (in.startsWith(segment)) {
                return new MatchData(true, in.substring(segment.length()), Optional.empty());
            }  //else return raiseCompilerError(format, "Invalid instruction, expected: " + segment);
        }

        Map<Fragment, String> out = new HashMap<>();

        if (type == PartType.VARIABLE) {
            Format format = context.format();
            
            switch (argumentType) {
                case LABEL -> {
                    if (in.startsWith("$")) { in = in.substring(1); } // Optional $ at start of label

                    if (in.length() == 8) {
                        int pointerAddress = Integer.valueOf(in, 16);
                        // + 4 is a forced offset due to it being impractical to jump to the direct next instruction
                        int instructionAddress = Integer.valueOf(context.address(), 16) + 4;
                        if (instructionAddress > pointerAddress) {
                            return raiseCompilerError(format, "Non-referential branching instruction cannot jump back");
                        } else {
                            if (pointerAddress % 2 == 0) {
                                int offset = pointerAddress - instructionAddress;
                                offset /= context.displacementMutation().getModifier();
                                if (offset > dispMax) {
                                    return raiseCompilerError(format, "Cannot jump that far");
                                }
                                String offsetStr = Integer.toString(offset, 16);
                                offsetStr = (dispZeroPadding + offsetStr).substring(offsetStr.length());
                                for (int i = 0; i < fragments.size(); i++) {
                                    out.put(fragments.get(i), offsetStr.substring(i, i + 1));
                                }
                                return new MatchData(true, "", Optional.of(out));
                            } else {
                                return raiseCompilerError(format, "Cannot branch to unaligned address");
                            }
                        }
                    } else {
                        return raiseCompilerError(format, "Invalid pointer");
                    }
                }

                case DISPLACEMENT -> { // 1 or 2 bytes
                    if (in.startsWith("$")) { in = in.substring(1); } // Optional $ at start of displacement

                    int fragSize = fragments.size();
                    DisplacementMutation dispMutation = context.displacementMutation();

                    if (dispMutation == DisplacementMutation.NONE) { // Guaranteed 1 byte
                        if (in.length() >= fragSize) { // "8F" in $8F
                            for (int i = 0; i < fragSize; i++) {
                                String digit = String.valueOf(in.charAt(i));
                                if (!JJWUtils.isHexadecimal(digit)) {
                                    return raiseCompilerError(format, "Invalid character in Hex literal");
                                }
                                out.put(fragments.get(i), digit);
                            }
                            return new MatchData(true, in.substring(fragSize), Optional.of(out));
                        }
                    } else {
                        if (in.length() >= fragSize * 2) { // Possibly 2 bytes
                            String displacementStr = "";
                            for (int i = 0; i < fragSize; i++) {
                                String byteStr = in.substring(i * 2, i * 2 + 2);
                                if (!JJWUtils.isHexadecimal(byteStr)) {
                                    return raiseCompilerError(format, "Invalid character in Hex literal");
                                }
                                displacementStr += byteStr;
                            }

                            int dispValue = Integer.valueOf(displacementStr, 16);
                            if (dispValue % dispMutation.getModifier() != 0) {
                                return raiseCompilerError(format, "Invalid displacement, should be a multiple of " + dispMutation.getModifier());
                            }
                            dispValue /= dispMutation.getModifier();
                            if (dispValue > dispMax) {
                                return raiseCompilerError(format, "Displacement value too large");
                            }
                            String displacementValueStr = Integer.toString(dispValue, 16);
                            if (fragSize > 1) { // Append leading zeros
                                displacementValueStr = (dispZeroPadding + displacementValueStr).substring(displacementValueStr.length());
                            }
                            for (int i = 0; i < fragSize; i++) {
                                out.put(fragments.get(i), displacementValueStr.substring(i, i + 1));
                            }

                            return new MatchData(true, in.substring(fragSize * 2), Optional.of(out));
                        } else {
                            return raiseCompilerError(format, "Incorrect displacement length");
                        }
                    }
                }

                case IMMEDIATE -> {
                    int fragSize = fragments.size();
                    if (in.startsWith("$")) { // "$" in $8F
                        if (in.length() > fragSize) { // "8F" in $8F
                            for (int i = 0; i < fragSize; i++) {
                                String digit = String.valueOf(in.charAt(1 + i));
                                if (!JJWUtils.isHexadecimal(digit)) {
                                    return raiseCompilerError(format, "Invalid character in Hex literal");
                                }
                                out.put(fragments.get(i), digit);
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
                    if (in.startsWith("R")) { // "R" in R0
                        if (in.length() > 1) {
                            int cutoff = 2;
                            String registerNumber = in.substring(1, cutoff);
                            if (JJWUtils.isDecimal(registerNumber)) {
                                if (in.length() > 2) { // Detect second decimal digit
                                    String nextCharacter = in.substring(2, 3);
                                    if (JJWUtils.isDecimal(nextCharacter)) {
                                        registerNumber += nextCharacter;
                                        cutoff++;
                                    }
                                }
                                int registerId = Integer.valueOf(registerNumber, 10);
                                if (registerId > 0x0F) {
                                    return raiseCompilerError(format, "Invalid Register ID");
                                }
                                out.put(fragments.get(0), JJWUtils.HEX_DIGITS.substring(registerId, registerId + 1));
                                return new MatchData(true, in.substring(cutoff), Optional.of(out));
                            } else {
                                return raiseCompilerError(format, "Invalid character in Decimal literal");
                            }
                        }
                    } //else return raiseCompilerError(format, "Expected R at start of register reference");
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