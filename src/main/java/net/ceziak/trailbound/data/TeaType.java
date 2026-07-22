package net.ceziak.trailbound.data;

import net.minecraft.network.chat.Component;

import java.util.Locale;

public enum TeaType {

    /*
     * Alpha 00 makes the liquid overlay invisible.
     */
    EMPTY(
            "empty",
            0x00FFFFFF
    ),

    GREEN_TEA(
            "green_tea",
            0xFF91AC58
    ),

    BERRY_TEA(
            "berry_tea",
            0xFFB94F70
    ),

    HONEYCOMB_TEA(
            "honeycomb_tea",
            0xFFD59A3E
    );

    private final String id;
    private final int liquidColor;

    TeaType(
            String id,
            int liquidColor
    ) {
        this.id = id;
        this.liquidColor = liquidColor;
    }

    public String id() {
        return id;
    }

    public int liquidColor() {
        return liquidColor;
    }

    public Component displayName() {
        return Component.translatable(
                "item.trailbound.tea_cup." + id
        );
    }

    public static TeaType byId(
            String id
    ) {
        if (id == null || id.isBlank()) {
            return EMPTY;
        }

        String normalizedId =
                id.toLowerCase(Locale.ROOT);

        for (TeaType teaType : values()) {
            if (teaType.id.equals(normalizedId)) {
                return teaType;
            }
        }

        return EMPTY;
    }
}