package com.leclowndu93150.twemoji.client.config;

import com.leclowndu93150.twemoji.client.registry.EmojiRegistry;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class TwemojiConfigScreen {

    private TwemojiConfigScreen() {}

    public static Screen create(Screen parent) {
        EmojiConfig config = EmojiConfig.get();
        return YetAnotherConfigLib.createBuilder()
                .title(Component.translatable("twemoji.config.title"))
                .category(ConfigCategory.createBuilder()
                        .name(Component.translatable("twemoji.config.category.general"))
                        .option(toggle("emoticons", config::isEmoticonsEnabled, config::setEmoticonsEnabled, true))
                        .option(toggle("suggestions", config::isSuggestionsEnabled, config::setSuggestionsEnabled, true))
                        .option(toggle("tooltips", config::isHoverTooltipsEnabled, config::setHoverTooltipsEnabled, true))
                        .option(toggle("picker_button", config::isPickerButtonVisible, config::setPickerButtonVisible, true))
                        .option(Option.<Boolean>createBuilder()
                                .name(Component.translatable("twemoji.config.flags"))
                                .description(OptionDescription.of(Component.translatable("twemoji.config.flags.desc")))
                                .binding(false, config::isFlagsEnabled, value -> {
                                    config.setFlagsEnabled(value);
                                    EmojiRegistry.INSTANCE.refreshShapingTable();
                                })
                                .controller(option -> BooleanControllerBuilder.create(option).coloured(true).onOffFormatter())
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Component.translatable("twemoji.config.skin_tone"))
                                .description(OptionDescription.of(Component.translatable("twemoji.config.skin_tone.desc")))
                                .binding(0, config::getSkinTone, config::setSkinTone)
                                .controller(option -> IntegerSliderControllerBuilder.create(option).range(0, 5).step(1))
                                .build())
                        .build())
                .build()
                .generateScreen(parent);
    }

    private static Option<Boolean> toggle(String key, Supplier<Boolean> getter, Consumer<Boolean> setter, boolean defaultValue) {
        return Option.<Boolean>createBuilder()
                .name(Component.translatable("twemoji.config." + key))
                .description(OptionDescription.of(Component.translatable("twemoji.config." + key + ".desc")))
                .binding(defaultValue, getter, setter)
                .controller(option -> BooleanControllerBuilder.create(option).coloured(true).onOffFormatter())
                .build();
    }
}
