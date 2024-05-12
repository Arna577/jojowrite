package net.arna.jojowrite.asm.instruction;

import javafx.util.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper for a Collection that represents individual parts of an instruction, in the order they appear.
 */
public class Format {
    private final int variableParts;
    private final Collection<Part> parts;

    public Format(Collection<Part> parts) {
        this.parts = parts;
        variableParts = parts.stream().filter(Part::isVariable).mapToInt(e -> 1).sum();
    }

    public boolean matches(String in) {
        for (Part part : parts) {
            // Procedurally consumes the string as the pattern check continues.
            Part.MatchData matchData = part.matches(in);
            if (!matchData.match()) return false;
            in = matchData.remaining();
            if (in.equals("")) return true; // Partial match
        }
        return true; // Full match
    }

    public Map<Fragment, String> mapFragments(String in) {
        Map<Fragment, String> out = new HashMap<>();
        for (Part part : parts) {
            // Procedurally consumes the string as the pattern check continues.
            Part.MatchData matchData = part.matches(in);
            if (!matchData.match()) throw new IllegalStateException("Trying to mapFragments for non-matching instruction!");
            in = matchData.remaining();
            matchData.fragmentHexDigitMap().ifPresent(out::putAll);
            if (in.equals("")) return out; // Partial match
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
}
