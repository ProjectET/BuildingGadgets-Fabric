package com.direwolf20.buildinggadgets.common.tainted.inventory;

import com.direwolf20.buildinggadgets.common.tainted.inventory.materials.MaterialList;
import com.google.common.collect.Multiset;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;

/**
 * Represents Index for accessible Items. It allows for extraction/insertion into some kind of ItemVariant container(s).
 * An Implementation must also handle the options represented by a MaterialList correctly - test all available options until the first one matches, or no other is left
 * to search.
 * <p>
 *
 * @see PlayerItemIndex
 * @see CreativeItemIndex
 */
public interface IItemIndex {

    //returns the remaining items
    void insert(Multiset<ItemVariant> items, TransactionContext transaction);

    MatchResult tryMatch(MaterialList list, TransactionContext transaction);

    default MatchResult tryMatch(MaterialList list) {
        try (Transaction transaction = Transaction.openOuter()) {
            return tryMatch(list, transaction);
        }
    }

    default MatchResult tryMatch(Multiset<ItemVariant> items) {
        return tryMatch(MaterialList.of(items));
    }

    boolean applyMatch(MatchResult result, TransactionContext transaction);
}
