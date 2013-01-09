// $Id$
/*
 * CraftBook Copyright (C) 2010 sk89q <http://www.sk89q.com>
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.craftbook.mech;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.player.PlayerInteractEvent;

import com.sk89q.craftbook.AbstractMechanic;
import com.sk89q.craftbook.AbstractMechanicFactory;
import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.LocalPlayer;
import com.sk89q.craftbook.bukkit.CraftBookPlugin;
import com.sk89q.craftbook.util.HistoryHashMap;
import com.sk89q.craftbook.util.exceptions.InsufficientPermissionsException;
import com.sk89q.craftbook.util.exceptions.InvalidMechanismException;
import com.sk89q.craftbook.util.exceptions.ProcessedMechanismException;
import com.sk89q.worldedit.BlockWorldVector;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.bukkit.BukkitUtil;

/**
 * Handler for Light switches. Toggles all torches in the area from being redstone to normal torches. This is done
 * every time a sign with [|] or [I]
 * is right clicked by a player.
 *
 * @author fullwall
 */
public class LightSwitch extends AbstractMechanic {

    public static class Factory extends AbstractMechanicFactory<LightSwitch> {

        public Factory() {

        }

        @Override
        public LightSwitch detect(BlockWorldVector pt) {

            Block block = BukkitUtil.toBlock(pt);
            // check if this looks at all like something we're interested in first
            if (block.getTypeId() != BlockID.WALL_SIGN) return null;
            String line = ((Sign) block.getState()).getLine(1);
            if (!line.equals("[|]") && !line.equalsIgnoreCase("[I]")) return null;

            // okay, now we can start doing exploration of surrounding blocks
            // and if something goes wrong in here then we throw fits.
            return new LightSwitch(pt);
        }

        /**
         * Detect the mechanic at a placed sign.
         *
         * @throws ProcessedMechanismException
         */
        @Override
        public LightSwitch detect(BlockWorldVector pt, LocalPlayer player,
                                  ChangedSign sign) throws InvalidMechanismException,
                ProcessedMechanismException {

            String line = sign.getLine(1);

            if (line.equals("[|]") || line.equalsIgnoreCase("[I]")) {
                if (!player.hasPermission("craftbook.mech.light-switch")) throw new InsufficientPermissionsException();

                sign.setLine(1, "[I]");
                player.print("mech.lightswitch.create");
            } else return null;

            throw new ProcessedMechanismException();
        }
    }

    /**
     * Store a list of recent light toggles to prevent spamming. Someone clever can just use two signs though.
     */
    private final HistoryHashMap<BlockWorldVector, Long> recentLightToggles = new HistoryHashMap<BlockWorldVector,
            Long>(20);

    /**
     * Configuration.
     */
    private final BlockWorldVector pt;

    /**
     * Construct a LightSwitch for a location.
     *
     * @param pt
     * @param plugin
     */
    private LightSwitch(BlockWorldVector pt) {

        super();
        this.pt = pt;
    }

    @Override
    public void onRightClick(PlayerInteractEvent event) {

        if (!CraftBookPlugin.inst().getConfiguration().lightSwitchEnabled) return;
        if (!BukkitUtil.toWorldVector(event.getClickedBlock()).equals(pt)) return; // wth? our manager is insane
        toggleLights(pt);
        event.setCancelled(true);
    }

    /**
     * Toggle lights in the immediate area.
     *
     * @param pt
     *
     * @return true if the block was recogized as a lightswitch; this may or may not mean that any lights were
     *         actually toggled.
     */
    private boolean toggleLights(BlockWorldVector pt) {

        World world = BukkitUtil.toWorld(pt);

        Block block = BukkitUtil.toBlock(pt);
        // check if this looks at all like something we're interested in first
        if (block.getTypeId() != BlockID.WALL_SIGN) return false;
        int radius = 10;
        int maximum = 20;
        try {
            radius = Integer.parseInt(((Sign) block.getState()).getLine(2));
        } catch (Exception ignored) {
        }
        try {
            maximum = Integer.parseInt(((Sign) block.getState()).getLine(3));
        } catch (Exception ignored) {
        }
        if (radius > CraftBookPlugin.inst().getConfiguration().lightSwitchMaxRange) {
            radius = CraftBookPlugin.inst().getConfiguration().lightSwitchMaxRange;
        }
        if (maximum > CraftBookPlugin.inst().getConfiguration().lightSwitchMaxLights) {
            maximum = CraftBookPlugin.inst().getConfiguration().lightSwitchMaxLights;
        }

        int wx = pt.getBlockX();
        int wy = pt.getBlockY();
        int wz = pt.getBlockZ();
        int aboveID = world.getBlockTypeIdAt(wx, wy + 1, wz);

        if (aboveID == BlockID.TORCH || aboveID == BlockID.REDSTONE_TORCH_OFF || aboveID == BlockID.REDSTONE_TORCH_ON) {
            // Check if block above is a redstone torch.
            // Used to get what to change torches to.
            boolean on = aboveID != BlockID.TORCH;
            // Prevent spam
            Long lastUse = recentLightToggles.remove(pt);
            long currTime = System.currentTimeMillis();

            if (lastUse != null && currTime - lastUse < 500) {
                recentLightToggles.put(pt, lastUse);
                return true;
            }

            recentLightToggles.put(pt, currTime);
            int changed = 0;
            for (int x = -radius + wx; x <= radius + wx; x++) {
                for (int y = -radius + wy; y <= radius + wy; y++) {
                    for (int z = -radius + wz; z <= radius + wz; z++) {
                        int id = world.getBlockTypeIdAt(x, y, z);
                        if (id == BlockID.TORCH || id == BlockID.REDSTONE_TORCH_OFF || id == BlockID
                                .REDSTONE_TORCH_ON) {
                            // Limit the maximum number of changed lights
                            if (changed >= maximum) return true;

                            if (on) {
                                world.getBlockAt(x, y, z).setTypeId(BlockID.TORCH);
                            } else {
                                world.getBlockAt(x, y, z).setTypeId(BlockID.REDSTONE_TORCH_ON);
                            }
                            changed++;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }
}