package com.leclowndu93150.twemoji.mixin;

import com.leclowndu93150.twemoji.client.EmojiRegistry;
import com.leclowndu93150.twemoji.client.ShapingSink;
import com.leclowndu93150.twemoji.client.ShapingTable;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StringDecomposer.class)
public class StringDecomposerMixin {

    @Inject(method = "iterate", at = @At("HEAD"), cancellable = true)
    private static void twemoji$iterate(String string, Style style, FormattedCharSink output, CallbackInfoReturnable<Boolean> cir) {
        ShapingTable table = EmojiRegistry.INSTANCE.shapingTable();
        if (table.isEmpty() || output instanceof ShapingSink) return;
        ShapingSink sink = new ShapingSink(output, table);
        boolean ok = StringDecomposer.iterate(string, style, sink);
        cir.setReturnValue(ok && sink.finish());
    }

    @Inject(method = "iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z", at = @At("HEAD"), cancellable = true)
    private static void twemoji$iterateFormatted(String string, int offset, Style currentStyle, Style resetStyle, FormattedCharSink output, CallbackInfoReturnable<Boolean> cir) {
        ShapingTable table = EmojiRegistry.INSTANCE.shapingTable();
        if (table.isEmpty() || output instanceof ShapingSink) return;
        ShapingSink sink = new ShapingSink(output, table);
        boolean ok = StringDecomposer.iterateFormatted(string, offset, currentStyle, resetStyle, sink);
        cir.setReturnValue(ok && sink.finish());
    }
}
