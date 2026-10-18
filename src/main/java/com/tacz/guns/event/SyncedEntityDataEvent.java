package com.tacz.guns.event;

import com.tacz.guns.entity.sync.core.*;
import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageUpdateEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@EventBusSubscriber
public final class SyncedEntityDataEvent {
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!event.getEntity().level().isClientSide()) {
            Entity entity = event.getTarget();
            DataHolder holder = SyncedEntityData.instance().getDataHolder(entity);
            if (holder != null) {
                List<DataEntry<?, ?>> entries = holder.gatherAll();
                entries.removeIf(entry -> !entry.getKey().syncMode().isTracking());
                if (!entries.isEmpty()) {
                    NetworkHandler.sendToClientPlayer(new ServerMessageUpdateEntityData(entity.getId(), entries), event.getEntity());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoinWorld(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player && !event.getLevel().isClientSide()) {
            DataHolder holder = SyncedEntityData.instance().getDataHolder(player);
            if (holder != null) {
                List<DataEntry<?, ?>> entries = holder.gatherAll();
                if (!entries.isEmpty()) {
                    NetworkHandler.sendToClientPlayer(new ServerMessageUpdateEntityData(player.getId(), entries), player);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        DataHolder oldHolder = SyncedEntityData.instance().getDataHolder(original);
        if (oldHolder == null) {
            return;
        }
        Player player = event.getEntity();
        DataHolder newHolder = SyncedEntityData.instance().getDataHolder(player);
        if (newHolder == null) {
            return;
        }
        Map<SyncedDataKey<?, ?>, DataEntry<?, ?>> dataMap = new HashMap<>(oldHolder.dataMap);
        if (event.isWasDeath()) {
            dataMap.entrySet().removeIf(entry -> !entry.getKey().persistent());
        }
        newHolder.dataMap = dataMap;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        SyncedEntityData instance = SyncedEntityData.instance();
 
        if (!instance.isDirty()) {
            return;
        }
        List<Entity> dirtyEntities = instance.getDirtyEntities();
        if (dirtyEntities.isEmpty()) {
            instance.setDirty(false);
            return;
        }
        for (Entity entity : dirtyEntities) {
            DataHolder holder = instance.getDataHolder(entity);
            if (holder == null || !holder.isDirty()) {
                continue;
            }
            List<DataEntry<?, ?>> entries = holder.gatherDirty();
            if (entries.isEmpty()) {
                continue;
            }
            List<DataEntry<?, ?>> selfEntries = entries.stream().filter(entry -> entry.getKey().syncMode().isSelf()).collect(Collectors.toList());
            if (!selfEntries.isEmpty() && entity instanceof ServerPlayer) {
                NetworkHandler.sendToClientPlayer(new ServerMessageUpdateEntityData(entity.getId(), selfEntries), (ServerPlayer) entity);
            }
            List<DataEntry<?, ?>> trackingEntries = entries.stream().filter(entry -> entry.getKey().syncMode().isTracking()).collect(Collectors.toList());
            if (!trackingEntries.isEmpty()) {
                NetworkHandler.sendToTrackingEntity(new ServerMessageUpdateEntityData(entity.getId(), trackingEntries), entity);
            }
            holder.clean();
        }
        dirtyEntities.clear();
        instance.setDirty(false);
    }
}