package buildcraft.lib.net;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MessageContainerClientHandler {
    @Nullable
    public static AbstractContainerMenu getClientContainerMenu() {
        return Minecraft.getInstance().player == null ? null : Minecraft.getInstance().player.containerMenu;
    }
}
