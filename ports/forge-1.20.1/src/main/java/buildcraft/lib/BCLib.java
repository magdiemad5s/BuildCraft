/* Copyright (c) 2016 SpaceToad and the BuildCraft team
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/. */
package buildcraft.lib;

import buildcraft.api.BCModules;
import buildcraft.api.core.BCLog;
import buildcraft.lib.block.VanillaRotationHandlers;
import buildcraft.lib.expression.ExpressionDebugManager;
import buildcraft.lib.list.VanillaListHandlers;
import buildcraft.lib.marker.MarkerCache;
import buildcraft.lib.migrate.LegacyMissingMappings;
import buildcraft.lib.misc.ExpressionCompat;
import buildcraft.lib.net.MessageManager;
import buildcraft.lib.net.cache.BuildCraftObjectCaches;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BCLib.MODID)
public class BCLib {
    public static final String MODID = "buildcraftlib";
    public static final String VERSION = resolveVersion();
    public static final String MC_VERSION = "1.20.1";
    public static final String GIT_BRANCH = "${git_branch}";
    public static final String GIT_COMMIT_HASH = "${git_commit_hash}";
    public static final String GIT_COMMIT_MSG = "${git_commit_msg}";
    public static final String GIT_COMMIT_AUTHOR = "${git_commit_author}";

    public static final boolean DEV = Boolean.getBoolean("buildcraft.dev") || "development".equals(VERSION);

    private static String resolveVersion() {
        String configuredVersion = System.getProperty("buildcraft.version");
        if (configuredVersion != null && !configuredVersion.isBlank()) {
            return configuredVersion;
        }
        String implementationVersion = BCLib.class.getPackage().getImplementationVersion();
        return implementationVersion == null || implementationVersion.isBlank()
            ? "development"
            : implementationVersion;
    }

    public BCLib() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::init);
        modEventBus.addListener(this::postInit);
        modEventBus.addListener(this::gatherData);

        try {
            BCLog.logger.info("");
        } catch (NoSuchFieldError e) {
            throw throwBadClass(e, BCLog.class);
        }
        BCLog.logger.info("Starting BuildCraft " + BCLib.VERSION);
        BCLog.logger.info("Copyright (c) the BuildCraft team, 2011-2018");
        BCLog.logger.info("https://www.mod-buildcraft.com");
        if (!GIT_COMMIT_HASH.startsWith("${")) {
            BCLog.logger.info("Detailed Build Information:");
            BCLog.logger.info("  Branch " + GIT_BRANCH);
            BCLog.logger.info("  Commit " + GIT_COMMIT_HASH);
            BCLog.logger.info("    " + GIT_COMMIT_MSG);
            BCLog.logger.info("    committed by " + GIT_COMMIT_AUTHOR);
        }
        BCLog.logger.info("");
        BCLog.logger.info("Loaded Modules:");
        for (BCModules module : BCModules.VALUES) {
            if (module.isLoaded()) {
                BCLog.logger.info("  - " + module.lowerCaseName);
            }
        }
        BCLog.logger.info("Missing Modules:");
        for (BCModules module : BCModules.VALUES) {
            if (!module.isLoaded()) {
                BCLog.logger.info("  - " + module.lowerCaseName);
            }
        }
        BCLibRegistries.fmlPreInit();

        // Register library network messages during mod construction, before any sided setup event
        // can attempt to replace their client handlers.
        BCLibProxy.MessageRegistry();

        ExpressionDebugManager.logger = BCLog.logger::info;
        ExpressionCompat.setup();

        
        
        
        BCLibItems.registry(modEventBus);

        BuildCraftObjectCaches.fmlPreInit();
//        NetworkRegistry.INSTANCE.registerGuiHandler(INSTANCE, BCLibProxy.getProxy());

//        MinecraftForge.EVENT_BUS.register(MigrationManager.INSTANCE);
  //      MinecraftForge.EVENT_BUS.register(FluidManager);
        //TODO
        // Set max chunk limit for quarries: 1 chunk for quarry itself and 5 * 5 chunks square for working area
//        ForgeChunkManager.getConfig().get(MODID, "maximumChunksPerTicket", 26);
 //       ForgeChunkManager.syncConfigDefaults();
 //      ForgeChunkManager.setForcedChunkLoadingCallback(BCLib.MODID, ChunkLoaderManager::rebindTickets);

        ExpressionDebugManager.logger = BCLog.logger::info;
        ExpressionCompat.setup();

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(LegacyMissingMappings::onMissingMappings);
        MinecraftForge.EVENT_BUS.register(BCLibEventDist.class);
        
    }

    public void gatherData(GatherDataEvent event) {
        var output = event.getGenerator().getPackOutput();
        var lookupProvider = event.getLookupProvider();
        var existingFiles = event.getExistingFileHelper();
        event.getGenerator().addProvider(event.includeServer(), new BCTagsProvider.BlockTag(output, lookupProvider, existingFiles));
        event.getGenerator().addProvider(event.includeServer(), new BCTagsProvider.FluidTag(output, lookupProvider, existingFiles));
        event.getGenerator().addProvider(event.includeServer(), new BCTagsProvider.BiomeTag(output, lookupProvider, existingFiles));
    }

    public void init(final FMLCommonSetupEvent event) {
    	BCLibRegistries.fmlInit();
    	VanillaListHandlers.fmlInit();
  //  	VanillaPaintHandlers.fmlInit();
        VanillaRotationHandlers.fmlInit();
    }
    
    public void postInit(FMLLoadCompleteEvent evt) {
//        ReloadableRegistryManager.loadAll();

//        VanillaListHandlers.fmlPostInit();
        MarkerCache.postInit();
    	BuildCraftObjectCaches.fmlPostInit();
    	MessageManager.fmlPostInit();
    }

    public static Error throwBadClass(Error e, Class<?> cls) throws Error {
        throw new Error(
            "Bad " + cls + " loaded from " + cls.getClassLoader() + " domain: " + cls.getProtectionDomain(), e
        );
    }

}
