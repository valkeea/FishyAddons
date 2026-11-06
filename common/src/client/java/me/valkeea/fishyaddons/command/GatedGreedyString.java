package me.valkeea.fishyaddons.command;

import java.util.Set;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

public final class GatedGreedyString implements ArgumentType<String> {
    private static final SimpleCommandExceptionType RESERVED_WORD =
        new SimpleCommandExceptionType(new LiteralMessage("Reserved word"));

    private final Set<String> reserved;

    private GatedGreedyString(Set<String> reserved) {
        this.reserved = reserved;
    }

    /** Creates a greedy string argument that rejects exact matches of the given reserved words. */
    public static GatedGreedyString excluding(Set<String> reserved) {
        return new GatedGreedyString(Set.copyOf(reserved));
    }

    /**
     * Parses the remaining input as a greedy string.
     * Throws a {@link CommandSyntaxException} if the input matches a reserved word.
     */
    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String remaining = reader.getRemaining();
        reader.setCursor(reader.getTotalLength());
        if (reserved.contains(remaining)) {
            throw RESERVED_WORD.create();
        }
        return remaining;
    }
}
