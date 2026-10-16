package com.toto.zerocapes.client.mixin;

import com.toto.zerocapes.client.ZeroCapesClient;
import com.toto.zerocapes.client.config.ZeroCapesConfig;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void zerocapes$overrideCape(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer)(Object)this;
        if (ZeroCapesClient.isLocalPlayer(player)) {
            Identifier customCape = ZeroCapesConfig.getInstance().getCurrentCapeTexture();
            if (customCape != null) {
                PlayerSkin original = cir.getReturnValue();
                if (original != null) {
                    ClientAsset.Texture capeTexture = new ClientAsset.ResourceTexture(customCape, customCape);
                    ClientAsset.Texture elytraTexture = original.elytra() != null ? original.elytra() : capeTexture;
                    PlayerSkin modified = new PlayerSkin(
                        original.body(),
                        capeTexture,
                        elytraTexture,
                        original.model(),
                        original.secure()
                    );
                    cir.setReturnValue(modified);
                }
            }
        }
    }
}
