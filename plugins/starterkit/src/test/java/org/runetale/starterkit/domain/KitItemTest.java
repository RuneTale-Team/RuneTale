package org.runetale.starterkit.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KitItemTest {

    @Test
    void validKitItemIsCreated() {
        KitItem item = new KitItem("hotbar", "Weapon_Sword_Steel_Rusty", 1);

        assertThat(item.container()).isEqualTo("hotbar");
        assertThat(item.itemId()).isEqualTo("Weapon_Sword_Steel_Rusty");
        assertThat(item.quantity()).isEqualTo(1);
    }

    @Test
    void blankContainerThrows() {
        assertThatThrownBy(() -> new KitItem("", "Weapon_Sword_Steel_Rusty", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("container");
    }

    @Test
    void blankItemIdThrows() {
        assertThatThrownBy(() -> new KitItem("hotbar", " ", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("itemId");
    }

    @Test
    void zeroQuantityThrows() {
        assertThatThrownBy(() -> new KitItem("hotbar", "Food_Bread", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity");
    }

    @Test
    void negativeQuantityThrows() {
        assertThatThrownBy(() -> new KitItem("hotbar", "Food_Bread", -5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quantity");
    }

    @Test
    void unknownContainerThrows() {
        assertThatThrownBy(() -> new KitItem("banana", "Food_Bread", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("container");
    }

    @Test
    void allValidContainersAreAccepted() {
        for (String container : new String[]{"hotbar", "armour", "utility", "tools", "storage", "backpack"}) {
            KitItem item = new KitItem(container, "Food_Bread", 1);
            assertThat(item.container()).isEqualTo(container);
        }
    }
}
