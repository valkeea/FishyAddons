package me.valkeea.fishyaddons.vconfig.ui.model;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.vconfig.core.UICategory;
import me.valkeea.fishyaddons.vconfig.ui.control.UIControl;
import net.minecraft.text.Text;

/**
 * Represents a configuration entry in VCScreen.
 * <br>Uses UIControl composition pattern for all rendering and interaction logic.
 * 
 * <h4>Each entry consists of:</h4>
 * <ul>
 *   <li>Name and description</li>
 *   <li>Controls - list of UIControl instances</li>
 *   <li>TooltipText - optional extra info rendered on hover</li>
 *   <li>SubEntries/parent - optional hierarchical structure</li>
 * </ul>
 */
public class VCEntry {
    public final String name;
    public final String cleanName;
    public final String[] description;
    public final List<UIControl> controls;
    public final List<Text> tooltipText;
    public final UICategory category;    
    public final @Nullable List<VCEntry> subEntries;
    public final @Nullable VCEntry parent;
    
    private VCEntry(Builder builder) {
        this.name = builder.name;
        this.cleanName = clean(builder.name);
        this.description = builder.description;
        this.controls = builder.controls != null ? List.copyOf(builder.controls) : List.of();
        this.tooltipText = builder.tooltipText != null ? List.copyOf(builder.tooltipText) : null;
        this.category = builder.category;        
        this.subEntries = builder.subEntries;
        this.parent = builder.parent;
    }

    private String clean(String name) {
        return name.replace("*", "");
    }
    
    public static class Builder {
        private final String name;
        private final String[] description;
        private List<UIControl> controls = null;
        private List<Text> tooltipText = null;
        private UICategory category = null;
        private List<VCEntry> subEntries = null;        
        private VCEntry parent = null;
        
        public Builder(String name, String[] description) {
            this.name = name;
            this.description = description;
        }
        
        public Builder controls(List<UIControl> controls) {
            this.controls = controls;
            return this;
        }
        
        public Builder controls(UIControl... controls) {
            this.controls = List.of(controls);
            return this;
        }
        
        public Builder tooltipText(List<Text> tooltipText) {
            this.tooltipText = tooltipText;
            return this;
        }

        public Builder category(UICategory category) {
            this.category = category;
            return this;
        }        
        
        public Builder subEntries(List<VCEntry> subEntries) {
            this.subEntries = subEntries;
            return this;
        }

        public Builder parent(VCEntry parent) {
            this.parent = parent;
            return this;
        }
        
        public VCEntry build() {
            return new VCEntry(this);
        }
    }
    
    /**
     * Returns whether this entry has sub-entries (is expandable).
     */
    public boolean hasSubEntries() {
        return subEntries != null && !subEntries.isEmpty();
    }
    
    /**
     * Returns the list of sub-entries, or an empty list if none.
     */
    public List<VCEntry> getSubEntries() {
        return subEntries != null ? subEntries : new ArrayList<>();
    }
    
    /**
     * Returns whether this entry has any controls.
     */
    public boolean hasControls() {
        return controls != null && !controls.isEmpty();
    }
    
    /**
     * Returns the number of controls in this entry.
     */
    public int getControlCount() {
        return controls != null ? controls.size() : 0;
    }
    
    /**
     * Returns whether this entry is a header
     */
    public boolean isHeader() {
        return !hasControls() && !hasSubEntries();
    }
    
    /**
     * Create a header entry
     */
    public static VCEntry header(String name) {
        return new Builder(name, new String[0]).build();
    }
    
    /**
     * Create a header entry with category metadata
     */
    public static VCEntry header(UICategory category) {
        return new Builder(category.toString(), new String[0])
            .category(category)
            .build();
    }

    /**
     * Create an entry with the specified controls
     */
    public static VCEntry of(String name, String[] description, List<UIControl> controls) {
        return new Builder(name, description)
            .controls(controls)
            .build();
    }
    
    // --- Hierarchy management ---

    public VCEntry withSubEntries(List<VCEntry> subEntries) {
        return new Builder(this.name, this.description)
            .controls(this.controls)
            .tooltipText(this.tooltipText)
            .subEntries(subEntries)
            .category(this.category)
            .parent(this.parent)
            .build();
    }

    public VCEntry withParent(VCEntry parent) {
        return new Builder(this.name, this.description)
            .controls(this.controls)
            .tooltipText(this.tooltipText)
            .subEntries(this.subEntries)
            .category(this.category)
            .parent(parent)
            .build();
    }
}
