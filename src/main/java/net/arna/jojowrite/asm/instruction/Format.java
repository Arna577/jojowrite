package net.arna.jojowrite.asm.instruction;

import javafx.util.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper for a Collection that represents individual parts of an instruction, in the order they appear.
 */
public class Format {
    private DisplacementMutation dispMutation = DisplacementMutation.NONE;
    private final int variableParts;
    private final Collection<Part> parts;

    public Format(Collection<Part> parts) {
        this.parts = parts;
        variableParts = parts.stream().filter(Part::isVariable).mapToInt(e -> 1).sum();
    }

    /**
     * Self-explanatory. Provides the Parts with:
     * @param format a reference to this Format
     * @param displacementMutation its DisplacementMutation
     * @param address the address of the instruction being compiled.
     */
    public record CompilationContext(Format format, DisplacementMutation displacementMutation, String address) {}

    public boolean matches(String addressStr, String instructionStr) {
        CompilationContext ctx = new CompilationContext(this, dispMutation, addressStr);

        for (Part part : parts) {
            // Procedurally consumes the string as the pattern check continues.
            Part.MatchData matchData = part.matches(instructionStr, ctx);
            if (!matchData.match()) return false;
            instructionStr = matchData.remaining();
            if (instructionStr.equals("")) return true; // Partial match
        }
        return true; // Full match
    }

    public Map<Fragment, String> mapFragments(String addressStr, String instructionStr) {
        CompilationContext ctx = new CompilationContext(this, dispMutation, addressStr);
        Map<Fragment, String> out = new HashMap<>();

        for (Part part : parts) {
            // Procedurally consumes the string as the pattern check continues.
            Part.MatchData matchData = part.matches(instructionStr, ctx);
            if (!matchData.match()) throw new IllegalStateException("Trying to mapFragments for non-matching instruction!");
            instructionStr = matchData.remaining();
            matchData.fragmentHexDigitMap().ifPresent(out::putAll);
            if (instructionStr.equals("")) return out; // Partial match
        }
        return out;
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        parts.forEach(
                part -> out.append(part.toString())
        );
        return out.toString();
    }

    public void setDispMutation(DisplacementMutation dispMutation) {
        this.dispMutation = dispMutation;
    }
}
