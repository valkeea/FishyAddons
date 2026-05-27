package me.valkeea.fishyaddons.vconfig.ui.factory;

import static me.valkeea.fishyaddons.vconfig.ui.factory.UIGenerator.PRIMARY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import me.valkeea.fishyaddons.util.text.StringUtils;
import me.valkeea.fishyaddons.vconfig.core.ConfigRegistry;
import me.valkeea.fishyaddons.vconfig.core.ConfigRegistry.FieldInfo;
import me.valkeea.fishyaddons.vconfig.core.ConfigRegistry.ModuleInfo;
import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.core.UIMetadata;
import me.valkeea.fishyaddons.vconfig.ui.control.ColorControl;
import me.valkeea.fishyaddons.vconfig.ui.control.UIControl;
import me.valkeea.fishyaddons.vconfig.ui.model.VCEntry;
import me.valkeea.fishyaddons.vconfig.util.VCLogger;

public final class UIFactory {

    // --- Entry Generation ---
    
    /**
     * Creates VCEntry list for all registered config fields, organized by category.
     * @param colorOpener Callback for opening color picker controls
     * @param stateProvider Provider for expandable state (from VCScreen)
     * @return List of VCEntry objects with parent-child relationships
     */
    public static List<VCEntry> generateEntries(
        Consumer<ColorControl> colorOpener, ExpandableStateProvider stateProvider
    ) {

        var colorCallback = colorOpener;
        List<VCEntry> entries = new ArrayList<>();
        
        var categories = ConfigRegistry.getModules().stream()
            .map(ModuleInfo::getCategory)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        
        for (var cat : categories) {
            List<VCEntry> categoryEntries = createEntriesForCategory(cat, colorCallback, stateProvider);
            if (!categoryEntries.isEmpty()) {
                entries.add(VCEntry.header(cat));
                entries.addAll(categoryEntries);
            }
        }
        
        return entries;
    }
    
    public static List<VCEntry> createEntriesForCategory(
        @NotNull UICategory category, Consumer<ColorControl> colorOpenerCallback,
        ExpandableStateProvider stateProvider
    ) {

        var fields = ConfigRegistry.getUiFieldsByCategory(category);

        Map<String, VCEntry> entriesByName = new HashMap<>();
        Map<String, List<VCEntry>> childrenByParent = new HashMap<>();
        Map<String, String> subcategoryByEntry = new HashMap<>();
        
        for (var fieldInfo : fields) {

            try {
                VCEntry e = createEntryFromField(fieldInfo, colorOpenerCallback, stateProvider, category);
                if (e != null) {
                    entriesByName.put(e.name, e);
                    
                    if (fieldInfo.hasUIAnnotation()) {
                        UIMetadata meta = fieldInfo.getMetadata();
                        subcategoryByEntry.put(e.name, meta.subcategory());
                        if (!meta.parent().isEmpty()) {
                            childrenByParent.computeIfAbsent(meta.parent(), k -> new ArrayList<>()).add(e);
                        }
                    }
                }

            } catch (Exception e) {
                VCLogger.error( 
                    UIFactory.class, e,
                    "Error creating UI entry for field: " +
                    fieldInfo.getField().getName()
                );
            }
        }
        
        return buildHierarchy(entriesByName, childrenByParent, subcategoryByEntry);
    }

    private static List<VCEntry> buildHierarchy(
        Map<String, VCEntry> entriesByName, Map<String, List<VCEntry>> childrenByParent,
        Map<String, String> subcategoryByEntry
    ) {

        List<VCEntry> topLevelEntries = new ArrayList<>();
        
        for (VCEntry e : entriesByName.values()) {
            var children = childrenByParent.get(e.name);

            if (children != null && !children.isEmpty()) {

                var childrenWithHeaders = groupBySubcategory(children, subcategoryByEntry);
                var attachedChildren = new ArrayList<>(childrenWithHeaders);
                VCEntry container = e.withSubEntries(attachedChildren);

                for (int i = 0; i < attachedChildren.size(); i++) {
                    attachedChildren.set(i, attachedChildren.get(i).withParent(container));
                }
                topLevelEntries.add(container);

            } else {
                boolean isChild = childrenByParent.values().stream().anyMatch(childList -> childList.contains(e));
                if (!isChild) topLevelEntries.add(e);
            }
        }
        return topLevelEntries;
    }
    
    /**
     * Group entries by subcategory and insert header separators between groups.
     * @param entries The list of entries to group
     * @param subcategoryByEntry Map of entry name to subcategory
     * @return List of entries with subcategory headers inserted
     */
    private static List<VCEntry> groupBySubcategory(
        List<VCEntry> entries, Map<String, String> subcategoryByEntry
    ) {
        Map<String, List<VCEntry>> groups = new java.util.LinkedHashMap<>();
        for (VCEntry entry : entries) {
            String subcategory = subcategoryByEntry.getOrDefault(entry.name, "");
            groups.computeIfAbsent(subcategory, k -> new ArrayList<>()).add(entry);
        }
        
        List<VCEntry> result = new ArrayList<>();
        
        for (var groupEntry : groups.entrySet()) {
            String subcategory = groupEntry.getKey();
            List<VCEntry> groupEntries = groupEntry.getValue();
            
            if (!subcategory.isEmpty()) {
                result.add(VCEntry.header(styleCategory(subcategory)));
            }
            result.addAll(groupEntries);
        }
        
        return result;
    }
    
    private static VCEntry createEntryFromField(
        @NotNull FieldInfo fieldInfo, Consumer<ColorControl> colorOpenerCallback,
        ExpandableStateProvider stateProvider, UICategory category
    ) {

        var field = fieldInfo.getField();
        if (!fieldInfo.hasUIAnnotation()) return null;
        
        var meta = fieldInfo.getMetadata();
        var name = meta.name().isEmpty() ? formatFieldName(field.getName()) : meta.name();
        var desc = meta.description();
        var cat = meta.category() != UICategory.NONE ? meta.category() : category;
        
        List<UIControl> controls = createControlsForField(
            fieldInfo, meta, colorOpenerCallback, stateProvider
        );

        if (controls.isEmpty()) return null;

        return new VCEntry.Builder(name, desc)
            .controls(controls)
            .category(cat)
            .build();
    }
    
    private static List<UIControl> createControlsForField(
        FieldInfo fieldInfo, UIMetadata meta,
        Consumer<ColorControl> colorOpenerCallback,
        ExpandableStateProvider stateProvider
    ) {
        var annotation = meta.getAnnotation();
        var handler = PRIMARY.get(annotation.annotationType());
        
        if (handler != null) {
            List<UIControl> controls = handler.handle(fieldInfo, meta, colorOpenerCallback, stateProvider);
            if (controls != null) {
                return controls;
            }
        }
        
        throw new IllegalArgumentException("Cannot determine control type for: " + fieldInfo.getField().getName());
    }
    
    // --- Util ---

    private static String styleCategory(String category) {
        return StringUtils.capitalize(category);
    }
    
    private static String formatFieldName(String fieldName) {
        return fieldName.replaceAll("([A-Z])", " $1")
            .substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    /**
     * Access expandable state in VCScreen.
     */
    @FunctionalInterface
    public interface ExpandableStateProvider {
        boolean isExpanded(String entryName);
        
        /**
         * Toggle the expanded state of an entry
         * @param entryName The unique name of the entry
         */
        default void toggleExpanded(String entryName) {
            // Default
        }
    }

    private UIFactory() {}    
}
