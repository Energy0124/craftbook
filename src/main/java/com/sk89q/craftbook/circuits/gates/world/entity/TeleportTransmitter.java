package com.sk89q.craftbook.circuits.gates.world.entity;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.bukkit.util.BukkitUtil;
import com.sk89q.craftbook.circuits.ic.AbstractIC;
import com.sk89q.craftbook.circuits.ic.AbstractICFactory;
import com.sk89q.craftbook.circuits.ic.ChipState;
import com.sk89q.craftbook.circuits.ic.IC;
import com.sk89q.craftbook.circuits.ic.ICFactory;
import com.sk89q.craftbook.util.HistoryHashMap;
import com.sk89q.craftbook.util.ICUtil;
import com.sk89q.craftbook.util.LocationUtil;
import com.sk89q.craftbook.util.RegexUtil;
import com.sk89q.craftbook.util.Tuple2;
import com.sk89q.worldedit.Vector;

public class TeleportTransmitter extends AbstractIC {

    public TeleportTransmitter(Server server, ChangedSign sign, ICFactory factory) {

        super(server, sign, factory);
    }

    protected static final HistoryHashMap<String, Tuple2<Long, String>> memory = new HistoryHashMap<String,
            Tuple2<Long, String>>(50);

    protected String band;

    @Override
    public String getTitle() {

        return "Teleport Transmitter";
    }

    @Override
    public String getSignTitle() {

        return "TELEPORT OUT";
    }

    Vector radius;
    Location offset;

    @Override
    public void load() {

        band = getLine(2);
        offset = BukkitUtil.toSign(getSign()).getLocation();
        radius = ICUtil.parseRadius(getSign(), 3);
        try {
            String[] splitEquals = RegexUtil.EQUALS_PATTERN.split(getSign().getLine(3), 2);
            if (getSign().getLine(3).contains("=")) {
                String[] splitCoords = RegexUtil.COLON_PATTERN.split(splitEquals[1]);
                int x = Integer.parseInt(splitCoords[0]);
                int y = Integer.parseInt(splitCoords[1]);
                int z = Integer.parseInt(splitCoords[2]);
                offset.add(x, y, z);
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void trigger(ChipState chip) {

        if (chip.getInput(0)) {
            Player closest = null;

            for (Player e : offset.getWorld().getPlayers()) {
                if (e == null || !e.isValid() || !LocationUtil.isWithinRadius(offset, e.getLocation(), radius)) {
                    continue;
                }

                if (closest == null) closest = e;
                else if (closest.getLocation().distanceSquared(BukkitUtil.toSign(getSign()).getLocation()) > e
                        .getLocation().distanceSquared(
                                BukkitUtil.toSign(getSign()).getLocation())) closest = e;
            }
            if (closest != null && !setValue(band, new Tuple2<Long, String>(System.currentTimeMillis(),
                    closest.getName())))
                closest.sendMessage(ChatColor.RED + "This Teleporter Frequency is currently busy! Try again soon!");
            return;
        }
    }

    public static Tuple2<Long, String> getValue(String band) {

        if (memory.containsKey(band)) {
            long time = System.currentTimeMillis() - memory.get(band).a;
            int seconds = (int) (time / 1000) % 60;
            if (seconds > 5) { // Expired.
                memory.remove(band);
                return null;
            }
        }
        Tuple2<Long, String> val = memory.get(band);
        memory.remove(band); // Remove on teleport.
        return val;
    }

    public static boolean setValue(String band, Tuple2<Long, String> val) {

        if (memory.containsKey(band)) {
            long time = System.currentTimeMillis() - memory.get(band).a;
            int seconds = (int) (time / 1000) % 60;
            if (seconds > 5) { // Expired.
                memory.remove(band);
            } else return false;
        }
        memory.put(band, val);
        return true;
    }

    public static class Factory extends AbstractICFactory {

        public Factory(Server server) {

            super(server);
        }

        @Override
        public IC create(ChangedSign sign) {

            return new TeleportTransmitter(getServer(), sign, this);
        }

        @Override
        public String getShortDescription() {

            return "Transmitter for the teleportation network.";
        }

        @Override
        public String[] getLineHelp() {

            String[] lines = new String[] {"frequency name", "radius=x:y:z offset"};
            return lines;
        }
    }
}