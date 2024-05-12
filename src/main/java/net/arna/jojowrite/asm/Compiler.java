package net.arna.jojowrite.asm;

import net.arna.jojowrite.asm.instruction.*;

import java.util.*;
import java.util.stream.Stream;

import static net.arna.jojowrite.asm.instruction.Fragment.StaticFragment;
import static net.arna.jojowrite.asm.instruction.Fragment.VariableFragment;
import static net.arna.jojowrite.asm.instruction.Part.ArgumentType.*;

public class Compiler {
    private static final List<Instruction> instructions = new ArrayList<>();

    public static Stream<Instruction> getPossibleInstructions(String in) {
        return instructions.stream().filter(
                instruction -> instruction.getFormat().matches(in)
        );
    }

    public static String compileToHexString(Instruction instruction, String in) {
        return instruction.compileToHexString(instruction.getFormat().mapFragments(in));
    }

    public static void registerInstruction(Instruction i) throws IllegalStateException {
        if (instructions.contains(i))
            throw new IllegalStateException("Attempted to register the same instruction twice!");
        instructions.add(i);
    }

    static {
        registerInstruction(
                new Instruction(
                        List.of(StaticFragment((byte) 0), StaticFragment((byte) 0), StaticFragment((byte) 0), StaticFragment((byte) 9)),
                        new Format(Collections.singleton(Part.StaticPart("NOOP"))),
                        "Does nothing lol"
                )
        );

        List<Fragment> movFragments = List.of(
                StaticFragment((byte) 0b1110), VariableFragment('n'), VariableFragment('i'), VariableFragment('i')
        );
        registerInstruction(
                new Instruction(
                        movFragments,
                        new Format(
                                List.of(
                                Part.StaticPart("MOV #"),
                                Part.VariablePart(IMMEDIATE, List.of(movFragments.get(2), movFragments.get(3))),
                                Part.StaticPart(","),
                                Part.VariablePart(REGISTER, List.of(movFragments.get(1)))
                                )
                        ),
                        "imm → Sign extension → Rn"
                )
        );

        List<Fragment> movBFragments = List.of(
                StaticFragment((byte) 0b1100), StaticFragment((byte) 0), VariableFragment('d'), VariableFragment('d')
        );
        registerInstruction(
                new Instruction(
                        movBFragments,
                        new Format(
                                List.of(
                                        Part.StaticPart("MOV.B R0,@("),
                                        Part.VariablePart(IMMEDIATE, List.of(movBFragments.get(2), movBFragments.get(3))),
                                        Part.StaticPart(",GBR)")
                                )
                        ),
                        "R0 → (disp + GBR)"
                )
        );
    }
}
