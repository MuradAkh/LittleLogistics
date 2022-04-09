package dev.murad.shipping.util;

import dev.murad.shipping.setup.ModTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

public class InteractionUtil {
    public static boolean doConfigure(Player player, InteractionHand hand) {
        return  (player.getPose().equals(Pose.CROUCHING) && player.getItemInHand(hand).isEmpty()) ||
                (!player.getPose().equals(Pose.CROUCHING) && player.getItemInHand(hand).getTags().anyMatch(ModTags.Items.WRENCHES::equals));
    }
}
