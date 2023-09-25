package dev.murad.shipping.entity.custom;

import net.minecraftforge.items.ItemStackHandler;

import java.util.Optional;

/**
 * A train inventory provider is a barge or a car that provides inventories
 * to other barges or cars ahead of it.
 */
public interface TrainInventoryProvider {

    // TODO: fluid provider

    // TODO: energy provider

    default Optional<ItemStackHandler> getTrainInventoryHandler() {
        return Optional.empty();
    }
}
