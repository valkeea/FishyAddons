package me.valkeea.fishyaddons.util.text;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public class FromText {

    /**
     * Finds the first literal text component with non-empty string
     */
    public static @Nullable Component firstLiteral(Component text) {
        if (!text.getString().trim().isEmpty()) {
            return text;
        }
        
        for (Component sibling : text.getSiblings()) {
            Component found = firstLiteral(sibling);
            if (found != null) {
                return found;
            }
        }
        return null;
    } 
    
    /**
     * Recursively search for text with the specified color
     */
    public static @Nullable Component findNodeWithColor(Component text, ChatFormatting targetColor) {
        Style style = text.getStyle();
        var targetTextColor = targetColor.getColor();
        var textColor = style.getColor();
        if (textColor != null && targetTextColor != null 
            && textColor.getValue() == targetTextColor) {
            return text;
        }
        
        for (Component sibling : text.getSiblings()) {
            Component found = findNodeWithColor(sibling, targetColor);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Find the first TextColor that passes the provided condition, be it from the root or any sibling
     */
    public static @Nullable TextColor findFirstTextColor(
        Component text, @Nullable Predicate<TextColor> condition
    ) {

        Style style = text.getStyle();
        var textColor = style.getColor();
        if (textColor != null) {
            if (condition == null || condition.test(textColor)) {
                return textColor;
            }
        }

        for (Component sibling : text.getSiblings()) {
            TextColor found = findFirstTextColor(sibling, condition);
            if (found != null) return found;
        }
        
        return null;
    }

    /** 
     * Returns the first ShowText hoverevent or null if none found 
     */
    public static @Nullable Component findShowText(Component text) {

        if (text.getStyle().getHoverEvent() instanceof ShowText textEvent) {
            return textEvent.value();
        }

        for (Component sibling : text.getSiblings()) {
            Component found = findShowText(sibling);
            if (found != null) {
                return found;
            }
        }
        
        return null;
    }

    /** 
     * Returns the first RunCommand clickevent or null if none found
     */
    public static @Nullable String findCommand(Component text) {

        var event = text.getStyle().getClickEvent();
        if (event != null && event instanceof RunCommand runnable) {
            return runnable.command();
        }

        for (Component sibling : text.getSiblings()) {
            String found = findCommand(sibling);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private FromText() {}

}
