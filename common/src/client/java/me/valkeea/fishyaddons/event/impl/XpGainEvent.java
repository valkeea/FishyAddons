package me.valkeea.fishyaddons.event.impl;

import me.valkeea.fishyaddons.event.BaseEvent;

public class XpGainEvent extends BaseEvent {
    public final String skill;
    public final String progress;

    public XpGainEvent(String skill, String progress) {
        this.skill = skill;
        this.progress = progress;
    }
}
