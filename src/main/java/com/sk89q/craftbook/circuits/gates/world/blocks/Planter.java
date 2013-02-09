package com.sk89q.craftbook.circuits.gates.world.blocks;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.bukkit.util.BukkitUtil;
import com.sk89q.craftbook.circuits.ic.AbstractIC;
import com.sk89q.craftbook.circuits.ic.AbstractICFactory;
import com.sk89q.craftbook.circuits.ic.ChipState;
import com.sk89q.craftbook.circuits.ic.IC;
import com.sk89q.craftbook.circuits.ic.ICFactory;
import com.sk89q.craftbook.util.ICUtil;
import com.sk89q.craftbook.util.ItemUtil;
import com.sk89q.craftbook.util.LocationUtil;
import com.sk89q.craftbook.util.RegexUtil;
import com.sk89q.craftbook.util.SignUtil;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.blocks.ItemID;

/**
 * Sapling planter Hybrid variant of MCX206 and MCX203 chest collector When there is a sapling or seed item drop in
 * range it will auto plant it above
 * the IC.
 *
 * @authors Drathus, Me4502
 */
public class Planter extends AbstractIC {

    public Planter(Server server, ChangedSign block, ICFactory factory) {

        super(server, block, factory);
    }

    ItemStack item;
    Block target;
    Block onBlock;
    Vector offset;
    Vector radius;

    @Override
    public void load() {

        item = ICUtil.getItem(getLine(2));
        if (item == null) item = new ItemStack(295, 1);

        onBlock = SignUtil.getBackBlock(BukkitUtil.toSign(getSign()).getBlock());

        radius = ICUtil.parseRadius(getSign(), 3);
        try {
            try {
                String[] loc = RegexUtil.COLON_PATTERN.split(RegexUtil.EQUALS_PATTERN.split(getSign().getLine(3))[1]);
                offset = new Vector(Integer.parseInt(loc[0]), Integer.parseInt(loc[1]), Integer.parseInt(loc[2]));
                if (offset.getX() > 16) offset.setX(16);
                if (offset.getY() > 16) offset.setY(16);
                if (offset.getZ() > 16) offset.setZ(16);

                if (offset.getX() < -16) offset.setX(-16);
                if (offset.getY() < -16) offset.setY(-16);
                if (offset.getZ() < -16) offset.setZ(-16);
            } catch (Exception e) {
                offset = new Vector(0, 2, 0);
            }

        } catch (Exception e) {
            offset = new Vector(0, 2, 0);
        }

        target = onBlock.getRelative(offset.getBlockX(), offset.getBlockY(), offset.getBlockZ());
    }

    @Override
    public String getTitle() {

        return "Planter";
    }

    @Override
    public String getSignTitle() {

        return "PLANTER";
    }

    @Override
    public void trigger(ChipState chip) {

        if (chip.getInput(0)) chip.setOutput(0, plant());
    }

    public boolean plant() {

        if (!plantableItem(item.getTypeId())) return false;

        if (onBlock.getRelative(0, 1, 0).getTypeId() == BlockID.CHEST) {

            Chest c = (Chest) onBlock.getRelative(0, 1, 0).getState();
            for (ItemStack it : c.getInventory().getContents()) {

                if (!ItemUtil.isStackValid(it)) continue;

                if (it.getTypeId() != item.getTypeId()) continue;

                if (item.getDurability() != 0 && it.getDurability() != item.getDurability()) continue;

                Block b = null;

                if ((b = searchBlocks(it)) != null) {
                    if (c.getInventory().removeItem(new ItemStack(it.getTypeId(), 1, it.getDurability())).isEmpty()) {
                        b.setTypeIdAndData(getBlockByItem(item.getTypeId()), (byte) item.getDurability(), true);
                        return true;
                    }
                }
            }
        } else {
            for (Entity ent : target.getChunk().getEntities()) {
                if (!(ent instanceof Item)) continue;

                Item itemEnt = (Item) ent;

                if (!ItemUtil.isStackValid(itemEnt.getItemStack())) continue;

                if (itemEnt.getItemStack().getTypeId() == item.getTypeId()
                        && (item.getDurability() == 0 || itemEnt.getItemStack().getDurability() == item.getDurability
                        () || itemEnt.getItemStack()
                        .getData().getData() == item.getData().getData())) {
                    Location loc = itemEnt.getLocation();

                    if (LocationUtil.isWithinRadius(target.getLocation(), loc, radius)) {

                        Block b = null;

                        if ((b = searchBlocks(itemEnt.getItemStack())) != null) {
                            if (ItemUtil.takeFromEntity(itemEnt)) {
                                b.setTypeIdAndData(getBlockByItem(item.getTypeId()), (byte) item.getDurability(), true);
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    public Block searchBlocks(ItemStack stack) {

        for (int x = -radius.getBlockX() + 1; x < radius.getBlockX(); x++) {
            for (int y = -radius.getBlockY() + 1; y < radius.getBlockY(); y++) {
                for (int z = -radius.getBlockZ() + 1; z < radius.getBlockZ(); z++) {
                    int rx = target.getX() - x;
                    int ry = target.getY() - y;
                    int rz = target.getZ() - z;
                    Block b = BukkitUtil.toSign(getSign()).getWorld().getBlockAt(rx, ry, rz);

                    if (b.getTypeId() != 0) continue;

                    if (itemPlantableOnBlock(item.getTypeId(), b.getRelative(0, -1, 0).getTypeId())) {

                        return b;
                    }
                }
            }
        }
        return null;
    }

    protected boolean plantableItem(int itemId) {

        switch (itemId) {
            case BlockID.SAPLING:
            case ItemID.SEEDS:
            case ItemID.NETHER_WART_SEED:
            case ItemID.MELON_SEEDS:
            case ItemID.PUMPKIN_SEEDS:
            case BlockID.CACTUS:
            case ItemID.POTATO:
            case ItemID.CARROT:
            case BlockID.RED_FLOWER:
            case BlockID.YELLOW_FLOWER:
            case BlockID.RED_MUSHROOM:
            case BlockID.BROWN_MUSHROOM:
                return true;
            default:
                return false;
        }
    }

    protected boolean itemPlantableOnBlock(int itemId, int blockId) {

        switch (itemId) {
            case BlockID.SAPLING:
            case BlockID.RED_FLOWER:
            case BlockID.YELLOW_FLOWER:
                return blockId == BlockID.DIRT || blockId == BlockID.GRASS;
            case ItemID.SEEDS:
            case ItemID.MELON_SEEDS:
            case ItemID.PUMPKIN_SEEDS:
            case ItemID.POTATO:
            case ItemID.CARROT:
                return blockId == BlockID.SOIL;
            case ItemID.NETHER_WART_SEED:
                return blockId == BlockID.SLOW_SAND;
            case BlockID.CACTUS:
                return blockId == BlockID.SAND;
            case BlockID.RED_MUSHROOM:
            case BlockID.BROWN_MUSHROOM:
                return !BlockType.canPassThrough(blockId);
        }
        return false;
    }

    protected int getBlockByItem(int itemId) {

        switch (itemId) {
            case ItemID.SEEDS:
                return BlockID.CROPS;
            case ItemID.MELON_SEEDS:
                return BlockID.MELON_STEM;
            case ItemID.PUMPKIN_SEEDS:
                return BlockID.PUMPKIN_STEM;
            case BlockID.SAPLING:
                return BlockID.SAPLING;
            case ItemID.NETHER_WART_SEED:
                return BlockID.NETHER_WART;
            case BlockID.CACTUS:
                return BlockID.CACTUS;
            case ItemID.POTATO:
                return BlockID.POTATOES;
            case ItemID.CARROT:
                return BlockID.CARROTS;
            case BlockID.RED_FLOWER:
                return BlockID.RED_FLOWER;
            case BlockID.YELLOW_FLOWER:
                return BlockID.YELLOW_FLOWER;
            case BlockID.RED_MUSHROOM:
                return BlockID.RED_MUSHROOM;
            case BlockID.BROWN_MUSHROOM:
                return BlockID.BROWN_MUSHROOM;
            default:
                return BlockID.AIR;
        }
    }

    public static class Factory extends AbstractICFactory {

        public Factory(Server server) {

            super(server);
        }

        @Override
        public IC create(ChangedSign sign) {

            return new Planter(getServer(), sign, this);
        }

        @Override
        public String getShortDescription() {

            return "Plants plantable things at set offset.";
        }

        @Override
        public String[] getLineHelp() {

            String[] lines = new String[] {"Item to plant id:data", "radius=x:y:z offset"};
            return lines;
        }
    }
}