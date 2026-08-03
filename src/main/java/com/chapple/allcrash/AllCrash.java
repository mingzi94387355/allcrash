package com.chapple.allcrash;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Mod(AllCrash.MOD_ID)
public class AllCrash {
    public static final String MOD_ID = "allcrash";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    // 待验证玩家管理
    public static final Map<UUID, ScheduledFuture<?>> pendingPlayers = new ConcurrentHashMap<>();
    private static final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private static final long TIMEOUT_SECONDS = 5; // 5秒超时

    public AllCrash(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // 注册两个网络包
        CHANNEL.registerMessage(0, ConfigSyncPacket.class,
                ConfigSyncPacket::encode,
                ConfigSyncPacket::decode,
                ConfigSyncPacket::handle);

        CHANNEL.registerMessage(1, AckPacket.class,
                AckPacket::encode,
                AckPacket::decode,
                AckPacket::handle);

        modEventBus.addListener(this::onLoadComplete);
        MinecraftForge.EVENT_BUS.register(this);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    // 客户端本地检查（启动时）
    private void onLoadComplete(final FMLLoadCompleteEvent event) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            List<? extends String> list = Config.crash_mod.get();
            for (String name : list) {
                if (ModList.get().isLoaded(name)) {
                    throw new RuntimeException("Never gonna give you up, never gonna let you down, Never gonna run around and desert you.(" + name + " is loaded)");
                }
            }
            LOGGER.info("client:no crash mod");
        }
    }

    // 玩家登录时：发送配置列表，并开始超时计时
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();

            // 取消之前可能残留的任务
            ScheduledFuture<?> old = pendingPlayers.remove(uuid);
            if (old != null) old.cancel(false);

            // 发送配置包
            List<? extends String> serverList = Config.crash_mod.get();
            ConfigSyncPacket packet = new ConfigSyncPacket(List.copyOf(serverList));
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
            LOGGER.info("allcrash sent crash_mod list to player {}", player.getName().getString());

            // 安排超时任务
            ScheduledFuture<?> future = executor.schedule(() -> {
                if (pendingPlayers.containsKey(uuid)) {
                    // 超时未收到确认，踢出玩家
                    player.connection.disconnect(Component.literal("Verification timed out. Please try again."));
                    pendingPlayers.remove(uuid);
                    LOGGER.warn("Player {} timed out waiting for verification.", player.getName().getString());
                }
            }, TIMEOUT_SECONDS, TimeUnit.SECONDS);
            pendingPlayers.put(uuid, future);
        }
    }

    // 玩家登出时清理等待状态
    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ScheduledFuture<?> future = pendingPlayers.remove(player.getUUID());
            if (future != null) future.cancel(false);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    // 客户端专用事件
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}