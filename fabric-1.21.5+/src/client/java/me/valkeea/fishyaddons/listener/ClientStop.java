package me.valkeea.fishyaddons.listener;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class ClientStop {

    public static void init () {
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            System.out.println("ClientStop: Saving all tracker data before shutdown...");
            
            try {
                if (me.valkeea.fishyaddons.tracker.DianaStats.loaded()) {
                    me.valkeea.fishyaddons.tracker.DianaStats.getInstance().save();
                    System.out.println("ClientStop: DianaStats saved successfully");
                }
            } catch (Exception e) {
                System.err.println("ClientStop: Failed to save DianaStats: " + e.getMessage());
            }

            try {
                if (me.valkeea.fishyaddons.tracker.fishing.ScStats.isEnabled()) {
                    me.valkeea.fishyaddons.tracker.fishing.ScStats.getInstance().save();
                    System.out.println("ClientStop: ScStats saved successfully");
                }
            } catch (Exception e) {
                System.err.println("ClientStop: Failed to save ScStats: " + e.getMessage());
            }
            
            try {
                if (me.valkeea.fishyaddons.tracker.fishing.ScData.isEnabled()) {
                    me.valkeea.fishyaddons.tracker.fishing.ScData.getInstance().save();
                    System.out.println("ClientStop: ScData saved successfully");
                }
            } catch (Exception e) {
                System.err.println("ClientStop: Failed to save ScData: " + e.getMessage());
            }
            
            System.out.println("ClientStop: Shutdown save sequence completed");
        });
   }

    private ClientStop() {}
}
