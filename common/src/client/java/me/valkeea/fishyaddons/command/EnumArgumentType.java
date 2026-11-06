package me.valkeea.fishyaddons.command;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

public final class EnumArgumentType<E extends Enum<E>> implements ArgumentType<E> {
    private static final DynamicCommandExceptionType INVALID_VALUE = new DynamicCommandExceptionType(
        word -> new LiteralMessage("Unknown value: " + word)
    );

    private final E[] values;

    private EnumArgumentType(E[] values) {
        this.values = values;
    }

    /** Creates an argument type accepting only the constants of the given enum, by name. */
    public static <E extends Enum<E>> EnumArgumentType<E> of(Class<E> enumClass) {
        return new EnumArgumentType<>(enumClass.getEnumConstants());
    }

    @Override
    public E parse(StringReader reader) throws CommandSyntaxException {
        String word = reader.readUnquotedString();
        for (E value : values) {
            if (value.name().equalsIgnoreCase(word)) {
                return value;
            }
        }
        throw INVALID_VALUE.create(word);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        for (E value : values) {
            String name = value.name().toLowerCase(Locale.ROOT);
            if (name.startsWith(remaining)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return Arrays.stream(values)
            .map(value -> value.name().toLowerCase(Locale.ROOT))
            .collect(Collectors.toList());
    }
}
