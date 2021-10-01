package com.direwolf20.buildinggadgets.common.tainted.inventory;

import com.direwolf20.buildinggadgets.common.tainted.inventory.materials.MaterialList;
import com.direwolf20.buildinggadgets.common.tainted.inventory.materials.objects.UniqueItem;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;

import java.util.Iterator;

/**
 * Represents the Items available in Creative Mode: everything. All queries will succeed. Always.
 */
public final class CreativeItemIndex implements IItemIndex {
    @Override
    public Multiset<UniqueItem> insert(Multiset<UniqueItem> items, boolean simulate) {
        return items;
    }

    @Override
    public void reIndex() {

    }

    @Override
    public MatchResult tryMatch(MaterialList list) {
        Iterator<ImmutableMultiset<UniqueItem>> it = list.iterator();
        ImmutableMultiset<UniqueItem> chosen = it.hasNext() ? it.next() : ImmutableMultiset.of();
        return MatchResult.success(list, chosen, chosen);
    }

    @Override
    public boolean applyMatch(MatchResult result) {
        return result.isSuccess();
    }
}
