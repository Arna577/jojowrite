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

    private Part(PartType type, String segment, ArgumentType argumentType, List<Fragment> fragments) {
        this.type = type;
        this.segment = segment;
        this.argumentType = argumentType;
        this.fragments = fragments;
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
    public MatchData matches(String in, DisplacementMutation dispMutation) {
        if (type == PartType.STATIC) {
            if (in.startsWith(segment)) {
                return new MatchData(true, in.substring(segment.length()), Optional.empty());
            }  //else return raiseCompilerError("Invalid instruction, expected: " + segment);
        }

        Map<Fragment, String> out = new HashMap<>();

        if (type == PartType.VARIABLE) {
            switch (argumentType) {
                case DISPLACEMENT -> { // 1 or 2 bytes
                    int fragSize = fragments.size();
                    if (dispMutation == DisplacementMutation.NONE) { // Guaranteed 1 byte
                        if (in.length() >= fragSize) { // "8F" in $8F
                            for (int i = 0; i < fragSize; i++) {
                                String digit = String.valueOf(in.charAt(i));
                                if (!JJWUtils.isHexadecimal(digit)) {
                                    return raiseCompilerError("Invalid character in Hex literal");
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
                                    return raiseCompilerError("Invalid character in Hex literal");
                                }
                                displacementStr += byteStr;
                            }

                            int dispValue = Integer.valueOf(displacementStr, 16);
                            if (dispValue % dispMutation.getModifier() != 0) {
                                return raiseCompilerError("Invalid displacement, should be a multiple of " + dispMutation.getModifier());
                            }
                            dispValue /= dispMutation.getModifier();
                            if (dispValue > 0xFF) {
                                return raiseCompilerError("Displacement value too large");
                            }
                            String displacementValueStr = Integer.toString(dispValue, 16);
                            if (fragSize > 1) { // Append leading zeros
                                displacementValueStr = ("0".repeat(fragSize) + displacementValueStr).substring(displacementValueStr.length());
                            }
                            for (int i = 0; i < fragSize; i++) {
                                out.put(fragments.get(i), displacementValueStr.substring(i, i + 1));
                            }

                            return new MatchData(true, in.substring(fragSize * 2), Optional.of(out));
                        } else {
                            return raiseCompilerError("Incorrect displacement length");
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
                                    return raiseCompilerError("Invalid character in Hex literal");
                                }
                                out.put(fragments.get(i), digit);
                            }
                            return new MatchData(true, in.substring(1 + fragSize), Optional.of(out));
                        } else {
                            return raiseCompilerError("Incorrect immediate value length");
                        }
                    } else {
                        return raiseCompilerError("Expected $ at start of immediate value");
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
                                    return raiseCompilerError("Invalid Register ID");
                                }
                                out.put(fragments.get(0), JJWUtils.HEX_DIGITS.substring(registerId, registerId + 1));
                                return new MatchData(true, in.substring(cutoff), Optional.of(out));
                            } else {
                                return raiseCompilerError("Invalid character in Decimal literal");
                            }
                        }
                    } //else return raiseCompilerError("Expected R at start of register reference");
                }
            }
        }

        return fail;
    }

    private static final MatchData fail = new MatchData(false, null, null);
    public MatchData raiseCompilerError(String error) {
        Compiler.raiseError(error);
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