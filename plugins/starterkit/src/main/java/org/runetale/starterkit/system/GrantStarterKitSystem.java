package org.runetale.starterkit.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.starterkit.component.ReceivedStarterKitComponent;
import org.runetale.starterkit.domain.KitItem;
import org.runetale.starterkit.domain.StarterKitConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class GrantStarterKitSystem extends HolderSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final int GRANT_SUCCESS = 0;
    private static final int GRANT_NO_CONTAINER = 1;
    private static final int GRANT_SKIP = 2;

    private final ComponentType<EntityStore, ReceivedStarterKitComponent> receivedKitComponentType;
    private final StarterKitConfig config;
    private final Query<EntityStore> query;

    public GrantStarterKitSystem(
            @Nonnull ComponentType<EntityStore, ReceivedStarterKitComponent> receivedKitComponentType,
            @Nonnull StarterKitConfig config) {
        this.receivedKitComponentType = receivedKitComponentType;
        this.config = config;
        // InventoryComponent.Hotbar is required in addition to PlayerRef so that the system only
        // fires once inventory is fully initialized. For brand-new players, PlayerRef is attached
        // before inventory components, so without this guard the grant runs against null containers,
        // nothing is placed, but markReceived is called — permanently losing the kit.
        this.query = Query.and(
                PlayerRef.getComponentType(),
                InventoryComponent.Hotbar.getComponentType(),
                Query.not(receivedKitComponentType));
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    @Override
    public void onEntityAdd(@Nonnull Holder<EntityStore> holder, @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store) {
        PlayerRef playerRef = holder.getComponent(PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        if (!this.config.enabled()) {
            LOGGER.atFine().log("[StarterKit] Plugin disabled, marking player without granting items");
            markReceived(holder);
            return;
        }

        List<KitItem> items = this.config.items();
        if (items.isEmpty()) {
            LOGGER.atFine().log("[StarterKit] No items configured, marking player");
            markReceived(holder);
            return;
        }

        int granted = 0;
        int nullContainers = 0;
        for (KitItem kitItem : items) {
            int result = grantItem(holder, kitItem);
            if (result == GRANT_SUCCESS) {
                granted++;
            } else if (result == GRANT_NO_CONTAINER) {
                nullContainers++;
            }
        }

        if (nullContainers > 0 && granted == 0) {
            // Every item failed due to missing containers — inventory not ready despite query guard.
            // Skip markReceived so the system retries when the query re-matches.
            LOGGER.atWarning().log("[StarterKit] All containers unavailable, deferring kit grant (nullContainers=%d)", nullContainers);
            return;
        }

        markReceived(holder);
        LOGGER.atInfo().log("[StarterKit] Granted starter kit to player granted=%d/%d", granted, items.size());
    }

    @Override
    public void onEntityRemoved(@Nonnull Holder<EntityStore> holder, @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store) {
        // No cleanup required. Component lifecycle is handled by ECS persistence.
    }

    private int grantItem(
            @Nonnull Holder<EntityStore> holder,
            @Nonnull KitItem kitItem) {
        // Validate item exists in asset registry before attempting to grant
        Item item = Item.getAssetMap().getAsset(kitItem.itemId());
        if (item == null) {
            LOGGER.atWarning().log("[StarterKit] Unknown item id=%s, skipping", kitItem.itemId());
            return GRANT_SKIP;
        }

        ItemContainer container = resolveContainer(holder, kitItem.container());
        if (container == null) {
            LOGGER.atWarning().log("[StarterKit] Could not resolve container=%s, skipping item=%s",
                    kitItem.container(), kitItem.itemId());
            return GRANT_NO_CONTAINER;
        }

        for (short slot = 0; slot < container.getCapacity(); slot++) {
            ItemStack existing = container.getItemStack(slot);
            if (existing == null || ItemStack.isEmpty(existing)) {
                container.setItemStackForSlot(slot, new ItemStack(kitItem.itemId(), kitItem.quantity()));
                return GRANT_SUCCESS;
            }
        }

        LOGGER.atWarning().log("[StarterKit] Container=%s full, could not place item=%s",
                kitItem.container(), kitItem.itemId());
        return GRANT_SKIP;
    }

    @Nullable
    private static ItemContainer resolveContainer(
            @Nonnull Holder<EntityStore> holder,
            @Nonnull String containerName) {
        InventoryComponent component = switch (containerName) {
            case "hotbar" -> holder.getComponent(InventoryComponent.Hotbar.getComponentType());
            case "armour" -> holder.getComponent(InventoryComponent.Armor.getComponentType());
            case "tools" -> holder.getComponent(InventoryComponent.Tool.getComponentType());
            case "storage" -> holder.getComponent(InventoryComponent.Storage.getComponentType());
            case "backpack" -> holder.getComponent(InventoryComponent.Backpack.getComponentType());
            case "utility" -> holder.getComponent(InventoryComponent.Utility.getComponentType());
            default -> null;
        };
        return component != null ? component.getInventory() : null;
    }

    private void markReceived(@Nonnull Holder<EntityStore> holder) {
        ReceivedStarterKitComponent marker = new ReceivedStarterKitComponent(System.currentTimeMillis());
        holder.putComponent(this.receivedKitComponentType, marker);
    }
}
