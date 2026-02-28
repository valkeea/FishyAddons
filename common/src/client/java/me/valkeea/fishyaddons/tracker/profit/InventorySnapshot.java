package me.valkeea.fishyaddons.tracker.profit;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import me.valkeea.fishyaddons.event.impl.FaEvents;
import me.valkeea.fishyaddons.event.impl.GuiChangeEvent;
import me.valkeea.fishyaddons.feature.item.safeguard.SlotHandler;

/**
 * Record inventory contents on sack open and compare with next snapshot to detect negative changes.
 */
public class InventorySnapshot {
    private InventorySnapshot() {}
    private static Map<Integer, SlotContent> lastSnapshot = Map.of();

    public static void init() {
        FaEvents.GUI_CHANGE.register(InventorySnapshot::onGuiOpen);
    }

    private static void onGuiOpen(GuiChangeEvent event) {
        if (!SackDropParser.isEnabled()) return;

        if (event.titleContains("sack")) {

            var newSlotContent = scan(event);
            compare(newSlotContent);
            lastSnapshot = newSlotContent;
        }
    }

    private static Map<Integer, SlotContent> scan(GuiChangeEvent event) {

        var screenHandler = event.screen.getScreenHandler();

        return IntStream.range(0, screenHandler.slots.size())
            .filter(i -> {
                int idx = SlotHandler.remap(event.screen, i);
                return idx != -1 && screenHandler.slots.get(i).hasStack();
            })
            .boxed()
            .collect(Collectors.toMap(
                i -> SlotHandler.remap(event.screen, i),
                i -> new SlotContent(
                    screenHandler.slots.get(i).getStack().getName().getString(),
                    screenHandler.slots.get(i).getStack().getCount()
                )
            ));
    }

    public static void compare(Map<Integer, SlotContent> newSnapshot) {
        if (lastSnapshot.isEmpty()) return;
        
        lastSnapshot.forEach((i, slot) -> {

            if (!newSnapshot.containsKey(i)) {
                SackDropParser.registerChatDrop(slot.itemName, slot.count);

            } else {
                var current = newSnapshot.get(i);
                if (current.itemName.equals(slot.itemName) && current.count < slot.count) {
                    SackDropParser.registerChatDrop(slot.itemName, slot.count - current.count);
                }
            }
        });
    }

    private static class SlotContent {
        String itemName;
        int count;

        public SlotContent(String itemName, int count) {
            this.itemName = itemName;
            this.count = count;
        }
    }
}
