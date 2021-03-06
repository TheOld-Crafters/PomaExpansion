package me.poma123.pomaexpansion;

import dev.j3fftw.extrautils.utils.Utils;
import dev.j3fftw.litexpansion.Items;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.interfaces.InventoryBlock;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.cscorelib2.blocks.BlockPosition;
import me.mrCookieSlime.Slimefun.cscorelib2.item.CustomItem;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class AdvancedMassFabricator extends SlimefunItem implements InventoryBlock, EnergyNetComponent {

        public static final RecipeType RECIPE_TYPE = new RecipeType(
                new NamespacedKey(PomaExpansion.getInstance(), "advanced_mass_fabricator"), PomaExpansion.ADVANCED_MASS_FABRICATOR_MACHINE
        );

        private static final int[] BORDER = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 12, 14, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26};
        public static final int ENERGY_CONSUMPTION = 200;
        public static final int CAPACITY = 1024;

        private static final int[] INPUT_SLOTS = new int[] {10, 11};
        private static final int OUTPUT_SLOT = 15;
        private static final int PROGRESS_SLOT = 13;
        private static final int PROGRESS_AMOUNT = 90; // Divide by 2 for seconds it takes

        private static final Map<BlockPosition, Integer> progress = new HashMap<>();

        private static final CustomItem progressItem = new CustomItem(Items.UU_MATTER.getType(), "&7Progress");

        public AdvancedMassFabricator() {
            super(PomaExpansion.category, PomaExpansion.ADVANCED_MASS_FABRICATOR_MACHINE, RecipeType.ENHANCED_CRAFTING_TABLE, new ItemStack[] {
                    Items.REINFORCED_GLASS, Items.MAG_THOR, Items.REINFORCED_GLASS,
                    Items.ADVANCED_MACHINE_BLOCK, Items.MASS_FABRICATOR_MACHINE, Items.ADVANCED_MACHINE_BLOCK,
                    Items.REINFORCED_GLASS, Items.MAG_THOR, Items.REINFORCED_GLASS
            });
            setupInv();

            PomaExpansion.getInstance().registerRecipe(Items.UU_MATTER, Items.SCRAP, AdvancedMassFabricator.RECIPE_TYPE);
        }

    private void setupInv() {
        createPreset(this, "&4Advanced Mass Fabricator", blockMenuPreset -> {
            for (int i : BORDER) {
                blockMenuPreset.addItem(i, ChestMenuUtils.getBackground(), ChestMenuUtils.getEmptyClickHandler());
            }

            Utils.putOutputSlot(blockMenuPreset, OUTPUT_SLOT);

            blockMenuPreset.addItem(PROGRESS_SLOT, progressItem);
            blockMenuPreset.addMenuClickHandler(PROGRESS_SLOT, ChestMenuUtils.getEmptyClickHandler());

        });
    }

    @Override
    public void preRegister() {
        this.addItemHandler(new BlockTicker() {
            public void tick(Block b, SlimefunItem sf, Config data) {
                AdvancedMassFabricator.this.tick(b);
            }

            public boolean isSynchronized() {
                return false;
            }
        });
    }

    private void tick(@Nonnull Block b) {
        @Nullable final BlockMenu inv = BlockStorage.getInventory(b);
        if (inv == null) {
            return;
        }

        // yes this is ugly shush
        @Nullable ItemStack input = inv.getItemInSlot(INPUT_SLOTS[0]);
        @Nullable ItemStack input2 = inv.getItemInSlot(INPUT_SLOTS[1]);
        @Nullable final ItemStack output = inv.getItemInSlot(OUTPUT_SLOT);
        if (output != null && (output.getType() != Items.UU_MATTER.getType()
                || output.getAmount() == output.getMaxStackSize()
                || !Items.UU_MATTER.getItem().isItem(output))) {
            return;
        }

        if (!Items.SCRAP.getItem().isItem(input)) {
            input = null;
        }
        if (!Items.SCRAP.getItem().isItem(input2)) {
            input2 = null;
        }
        if (input == null && input2 == null) {
            return;
        }

        final BlockPosition pos = new BlockPosition(b.getWorld(), b.getX(), b.getY(), b.getZ());
        int currentProgress = progress.getOrDefault(pos, 0);

        if (!takePower(b)) {
            return;
        }

        // Process first tick - remove an input and put it in map.
        if (currentProgress != PROGRESS_AMOUNT) {
            if (input != null)
                inv.consumeItem(INPUT_SLOTS[0]);
            else
                inv.consumeItem(INPUT_SLOTS[1]);
            progress.put(pos, ++currentProgress);
            ChestMenuUtils.updateProgressbar(inv, PROGRESS_SLOT, PROGRESS_AMOUNT - currentProgress,
                    PROGRESS_AMOUNT, progressItem);
        } else {
            if (output != null && output.getAmount() > 0) {
                output.setAmount(output.getAmount() + 1);
            } else {
                inv.replaceExistingItem(OUTPUT_SLOT, Items.UU_MATTER.clone());
            }
            progress.remove(pos);
            ChestMenuUtils.updateProgressbar(inv, PROGRESS_SLOT, PROGRESS_AMOUNT, PROGRESS_AMOUNT, progressItem);
        }
    }

    private boolean takePower(@Nonnull Block b) {
        if (getCharge(b.getLocation()) < ENERGY_CONSUMPTION) {
            return false;
        }
        removeCharge(b.getLocation(), ENERGY_CONSUMPTION);
        return true;
    }

    @Nonnull
    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    @Override
    public int getCapacity() {
        return CAPACITY;
    }

    @Override
    public int[] getInputSlots() {
        return INPUT_SLOTS;
    }

    @Override
    public int[] getOutputSlots() {
        return new int[] {OUTPUT_SLOT};
    }
}
