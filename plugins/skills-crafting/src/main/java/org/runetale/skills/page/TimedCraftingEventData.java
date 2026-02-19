package org.runetale.skills.page;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nullable;
import java.util.function.Supplier;

abstract class TimedCraftingEventData {

	static final String KEY_ACTION = "Action";
	static final String KEY_TIER = "Tier";
	static final String KEY_QUANTITY = "Quantity";
	static final String KEY_QUANTITY_INPUT = "@QuantityInput";
	static final String KEY_RECIPE_ID = "RecipeId";

	@Nullable
	String action;

	@Nullable
	String tier;

	@Nullable
	String quantity;

	@Nullable
	String quantityInput;

	@Nullable
	String recipeId;

	static <T extends TimedCraftingEventData> BuilderCodec<T> createCodec(
			Class<T> type,
			Supplier<T> supplier) {
		return BuilderCodec
				.builder(type, supplier)
				.append(new KeyedCodec<>(KEY_ACTION, Codec.STRING), (entry, value) -> entry.action = value, entry -> entry.action)
				.add()
				.append(new KeyedCodec<>(KEY_TIER, Codec.STRING), (entry, value) -> entry.tier = value, entry -> entry.tier)
				.add()
				.append(new KeyedCodec<>(KEY_QUANTITY, Codec.STRING), (entry, value) -> entry.quantity = value, entry -> entry.quantity)
				.add()
				.append(new KeyedCodec<>(KEY_QUANTITY_INPUT, Codec.STRING), (entry, value) -> entry.quantityInput = value, entry -> entry.quantityInput)
				.add()
				.append(new KeyedCodec<>(KEY_RECIPE_ID, Codec.STRING), (entry, value) -> entry.recipeId = value, entry -> entry.recipeId)
				.add()
				.build();
	}
}
