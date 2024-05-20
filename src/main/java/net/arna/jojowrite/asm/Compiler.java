package net.arna.jojowrite.asm;

import javafx.scene.control.TextArea;
import net.arna.jojowrite.JJWUtils;
import net.arna.jojowrite.asm.instruction.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Stream;

/**
 * Receives and processes x16 RISC Assembly instructions (usually into machine code).
 * Also covers error logging via {@link Compiler#openErrorLog(int)} and {@link Compiler#raiseError(String)}.
 */
public class Compiler {
    private static final List<Instruction> instructions = new ArrayList<>();
    private static final Map<Integer, ArrayList<String>> errors = new HashMap<>();
    private static ArrayList<String> errorLog;
    private static TextArea errorOutputArea;

    public static Stream<Instruction> getPossibleInstructions(String addressStr, String instructionStr) {
        return instructions.stream().filter(
                instruction -> instruction.getFormat().matches(addressStr, instructionStr)
        );
    }

    /**
     * Compiles the specified instruction into a String Hex representation of the compiled machine code.
     */
    public static String compileToHexString(Instruction instruction, String addressStr, String instructionStr) {
        return instruction.compileToHexString(instruction.getFormat().mapFragments(addressStr, instructionStr));
    }

    public static void registerInstruction(Instruction i) throws IllegalStateException {
        if (instructions.contains(i))
            throw new IllegalStateException("Attempted to register the same instruction twice!");
        instructions.add(i);
    }

    public static void clearErrors() {
        errors.clear();
        errorOutputArea.clear();
    }

    /**
     * Opens a new error log for:
     * @param index
     */
    public static void openErrorLog(int index) {
        errorLog = new ArrayList<>();
        errors.put(index, errorLog);
    }

    /**
     * Appends a new error to the error log.
     */
    public static void raiseError(String err) {
        if (errorLog == null) return;
        errorLog.add(err);
    }

    public static void clearErrors(int index) {
        errors.remove(index);
    }

    public static void displayErrors() {
        errors.forEach(
                (key, value) -> value.forEach(
                        error -> errorOutputArea.appendText("Ln. " + (key + 1) + ": " + error + '\n')
                )
        );
    }

    public static void setErrorOutputArea(TextArea errorOutputArea) {
        Compiler.errorOutputArea = errorOutputArea;
    }

    static class Keyword {
        private final String format;
        private final String identifier;
        private final Part.ArgumentType type;
        private final int formatLength;

        Keyword(String format, String identifier, Part.ArgumentType type) {
            this.format = format;
            this.formatLength = format.length();
            this.identifier = identifier;
            this.type = type;
        }

        /**
         * Tests if the {@link StringBuilder} ends with this Keywords {@link Keyword#format}, then removes it from the StringBuilder.
         * If a match was detected, adds a new {@link Part#StaticPart(String)} and {@link Part#VariablePart(Part.ArgumentType, List)} to:
         * @param parts     the List of Parts to add to
         * @param fragments the List of Fragments to link the new Part to
         * @return whether the StringBuilder ended with the format.
         */
        public boolean findMatch(StringBuilder stringBuilder, List<Part> parts, List<Fragment> fragments) {
            if (stringBuilder.toString().endsWith(format)) {
                stringBuilder.setLength(stringBuilder.length() - formatLength);
                parts.add(Part.StaticPart(stringBuilder.toString()));
                stringBuilder.setLength(0);

                parts.add(Part.VariablePart(type, fragments.stream().filter(fragment ->
                                fragment.getType() == Fragment.FragmentType.VARIABLE && // 'd' might be 0x0D or disp
                                        fragment.asSingleChar().equals(identifier)
                                ).toList()));
                return true;
            }
            return false;
        }
    }

    public static final Set<Keyword> keywords = Set.of(
            new Keyword("Rm", "m", Part.ArgumentType.REGISTER),
            new Keyword("Rn", "n", Part.ArgumentType.REGISTER),
            new Keyword("imm", "i", Part.ArgumentType.IMMEDIATE),
            new Keyword("disp", "d", Part.ArgumentType.DISPLACEMENT),
            new Keyword("label", "d", Part.ArgumentType.LABEL)
    );

    public static void loadAssemblyDefinitions(InputStream asmdef) {
        try (InputStreamReader inputStreamReader = new InputStreamReader(asmdef)) {
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            while (bufferedReader.ready()) {
                String instructionDef = bufferedReader.readLine();
                String[] splitDef = instructionDef.split(" ");

                if (instructionDef.startsWith("//")) continue; // Comments

                if (splitDef.length < 2) {
                    System.out.println("Dropping invalid asmdef: " + instructionDef);
                    continue;
                }

                // Example: MOV #imm,Rn 1110nnnniiiiiiii imm → Sign extension → Rn 1 —
                String fragmentsStr = splitDef[2]; // Example: "1110nnnniiiiiiii"
                List<Fragment> fragments = new ArrayList<>();
                for (int i = 0; i < 16; i += 4) {
                    String fragmentStr = fragmentsStr.substring(i, i + 4);
                    try { // Valid binary fragment
                        byte fragmentValue = Integer.valueOf(fragmentStr, 2).byteValue();
                        fragments.add(Fragment.StaticFragment(fragmentValue));
                    } catch (NumberFormatException e) { // Parameterized fragment
                        char identifier = fragmentStr.charAt(0);
                        fragments.add(Fragment.VariableFragment(identifier));
                    }
                }
                //fragments.forEach(System.out::println);

                String parameterStr = splitDef[1]; // Example: "#imm,Rn"

                List<Part> parts = new ArrayList<>();
                if (parameterStr.isEmpty()) { // Parameterless instructions such ass SLEEP, DIV0U
                    parts.add(Part.StaticPart(splitDef[0]));
                } else { // Parameterized instructions (most)
                    String instructionStr = splitDef[0] + " " + parameterStr; // Example: "MOV" + " " + "#imm,Rn"
                    /*
                    mmmm: Source register
                    nnnn: Destination register
                    iiii: Immediate data
                    dddd: Displacement
                    label: Pointer
                     */
                    StringBuilder partStr = new StringBuilder();
                    while (!instructionStr.isEmpty()) {
                        partStr.append(instructionStr.charAt(0));
                        instructionStr = instructionStr.substring(1);

                        keywords.forEach(
                                keyword -> keyword.findMatch(partStr, parts, fragments)
                        );
                    }

                    if (!partStr.isEmpty())
                        parts.add(Part.StaticPart(partStr.toString()));
                }

                Format format = new Format(parts);
                if (splitDef[0].endsWith(".W") || parts.stream().anyMatch(part -> part.getArgumentType() == Part.ArgumentType.LABEL))
                    format.setDispMutation(DisplacementMutation.WORD);
                if (splitDef[0].endsWith(".L"))
                    format.setDispMutation(DisplacementMutation.LONG);

                Instruction newInstruction = new Instruction(fragments, format);
                registerInstruction(newInstruction);
                //System.out.println(newInstruction);
            }
        } catch (Exception e) {
            JJWUtils.printException(e, "Something went wrong while reading asmdef.txt!");
        }
    }
}
