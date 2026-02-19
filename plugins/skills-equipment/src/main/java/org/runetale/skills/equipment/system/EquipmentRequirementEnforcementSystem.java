package org.runetale.skills.equipment.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.DelayedSystem;
import com.hypixel.hytale.protocol.ItemArmorSlot;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.inventory.transaction.MoveTransaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.runetale.skills.api.SkillsRuntimeApi;
import org.runetale.skills.config.EquipmentConfig;
import org.runetale.skills.domain.SkillRequirement;
import org.runetale.skills.equipment.domain.EquipmentLocation;
import org.runetale.skills.equipment.service.EquipmentGateNotificationService;
import org.runetale.skills.equipment.service.EquipmentRequirementTagService;

import javax.annotation.Nonnull;

public class EquipmentRequirementEnforcementSystem extends DelayedSystem<EntityStore> {

    private final SkillsRuntimeApi runtimeApi;
    private final EquipmentConfig equipmentConfig;
    private final EquipmentRequirementTagService requirementTagService;
    private final EquipmentGateNotificationService notificationService;

    public EquipmentRequirementEnforcementSystem(
            @Nonnull SkillsRuntimeApi runtimeApi,
            @Nonnull EquipmentConfig equipmentConfig,
            @Nonnull EquipmentRequirementTagService requirementTagService,
            @Nonnull EquipmentGateNotificationService notificationService) {
        super(equipmentConfig.armorScanTickSeconds());
        this.runtimeApi = runtimeApi;
        this.equipmentConfig = equipmentConfig;
        this.requirementTagService = requirementTagService;
        this.notificationService = notificationService;
    }

    @Override
    public void delayedTick(float deltaTime, int systemIndex, @Nonnull Store<EntityStore> store) {
        if (!this.equipmentConfig.enforceArmor() && !this.equipmentConfig.enforceActiveHand()) {
            return;
        }

        World world = store.getExternalData().getWorld();
        for (PlayerRef playerRef : world.getPlayerRefs()) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref == null || !ref.isValid()) {
                continue;
            }

            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                continue;
            }

            if (this.equipmentConfig.enforceArmor()) {
                enforceArmor(store, ref, playerRef, player);
            }
            if (this.equipmentConfig.enforceActiveHand()) {
                enforceActiveHand(store, ref, playerRef, player);
            }
        }
    }

    private void enforceArmor(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull Player player) {
        Inventory inventory = player.getInventory();
        ItemContainer armor = inventory.getArmor();

        for (int slotIndex = 0; slotIndex < armor.getCapacity(); slotIndex++) {
            ItemStack equipped = armor.getItemStack((short) slotIndex);
            if (equipped == null || ItemStack.isEmpty(equipped)) {
                continue;
            }

            ItemArmorSlot armorSlot = ItemArmorSlot.fromValue(slotIndex);
            EquipmentLocation location = EquipmentLocation.fromArmorSlot(armorSlot);
            SkillRequirement requirement = this.requirementTagService.getRequirementForLocation(equipped.getItem(), location);
            if (requirement == null) {
                continue;
            }

            int currentLevel = this.runtimeApi.getSkillLevel(store, ref, requirement.skillType());
            if (currentLevel >= requirement.requiredLevel()) {
                continue;
            }

            MoveTransaction<ItemStackTransaction> transaction = armor.moveItemStackFromSlot(
                    (short) slotIndex,
                    equipped.getQuantity(),
                    inventory.getCombinedBackpackStorageHotbar());

            if (transaction.succeeded()) {
                this.notificationService.sendBlockedEquipNotice(
                        playerRef,
                        requirement,
                        currentLevel,
                        equipped.getItem().getId(),
                        location);
            }
        }
    }

    private void enforceActiveHand(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull Player player) {
        Inventory inventory = player.getInventory();
        enforceActiveSection(store, ref, playerRef, inventory, this.equipmentConfig.activeSectionHotbar());
        enforceActiveSection(store, ref, playerRef, inventory, this.equipmentConfig.activeSectionTools());
    }

    private void enforceActiveSection(
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull Inventory inventory,
            int sectionId) {
        byte activeSlot;
        try {
            activeSlot = inventory.getActiveSlot(sectionId);
        } catch (IllegalArgumentException ignored) {
            return;
        }
        if (activeSlot < 0) {
            return;
        }

        ItemContainer section = inventory.getSectionById(sectionId);
        if (section == null) {
            return;
        }

        ItemStack equipped = section.getItemStack(activeSlot);
        if (equipped == null || ItemStack.isEmpty(equipped)) {
            return;
        }

        SkillRequirement requirement = this.requirementTagService.getRequirementForLocation(equipped.getItem(), EquipmentLocation.MAINHAND);
        if (requirement == null) {
            return;
        }

        int currentLevel = this.runtimeApi.getSkillLevel(store, ref, requirement.skillType());
        if (currentLevel >= requirement.requiredLevel()) {
            return;
        }

        try {
            inventory.setActiveSlot(sectionId, Inventory.INACTIVE_SLOT_INDEX);
        } catch (IllegalArgumentException ignored) {
            return;
        }
        this.notificationService.sendBlockedEquipNotice(
                playerRef,
                requirement,
                currentLevel,
                equipped.getItem().getId(),
                EquipmentLocation.MAINHAND);
    }
}
