package net.ceziak.trailbound.item;

import net.ceziak.trailbound.component.ModDataComponents;
import net.ceziak.trailbound.data.TeaType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class TeaCupItem extends Item {

    public TeaCupItem(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    public Component getName(
            ItemStack stack
    ) {
        return getTeaType(stack).displayName();
    }

    public static TeaType getTeaType(
            ItemStack stack
    ) {
        String teaTypeId = stack.getOrDefault(
                ModDataComponents.TEA_TYPE.get(),
                TeaType.EMPTY.id()
        );

        return TeaType.byId(teaTypeId);
    }

    public static void setTeaType(
            ItemStack stack,
            TeaType teaType
    ) {
        stack.set(
                ModDataComponents.TEA_TYPE.get(),
                teaType.id()
        );
    }

    public static ItemStack create(
            TeaType teaType
    ) {
        ItemStack stack =
                new ItemStack(ModItems.TEA_CUP.get());

        setTeaType(stack, teaType);

        return stack;
    }
}