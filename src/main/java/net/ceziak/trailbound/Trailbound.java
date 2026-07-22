package net.ceziak.trailbound;

import com.mojang.logging.LogUtils;
import net.ceziak.trailbound.item.ModCreativeModeTabs;
import net.ceziak.trailbound.item.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;
import net.ceziak.trailbound.loot.ModLootModifiers;
import net.ceziak.trailbound.block.ModBlocks;
import net.ceziak.trailbound.block.entity.ModBlockEntities;
import net.ceziak.trailbound.recipe.ModRecipes;

@Mod(Trailbound.MOD_ID)
public class Trailbound {
    public static final String MOD_ID = "trailbound";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Trailbound(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);

        ModCreativeModeTabs.register(modEventBus);

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        ModRecipes.register(modEventBus);

        ModLootModifiers.register(modEventBus);

        modEventBus.addListener(this::addCreative);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }
}
