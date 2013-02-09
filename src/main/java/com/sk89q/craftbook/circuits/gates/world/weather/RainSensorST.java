package com.sk89q.craftbook.circuits.gates.world.weather;

import org.bukkit.Server;

import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.bukkit.util.BukkitUtil;
import com.sk89q.craftbook.circuits.ic.AbstractIC;
import com.sk89q.craftbook.circuits.ic.ChipState;
import com.sk89q.craftbook.circuits.ic.IC;
import com.sk89q.craftbook.circuits.ic.ICFactory;
import com.sk89q.craftbook.circuits.ic.SelfTriggeredIC;

public class RainSensorST extends AbstractIC implements SelfTriggeredIC {

    public RainSensorST(Server server, ChangedSign sign, ICFactory factory) {

        super(server, sign, factory);
    }

    @Override
    public String getTitle() {

        return "Is It Rain";
    }

    @Override
    public String getSignTitle() {

        return "IS IT RAIN";
    }

    @Override
    public void trigger(ChipState chip) {

    }

    @Override
    public void think(ChipState chip) {

        chip.setOutput(0, BukkitUtil.toSign(getSign()).getWorld().hasStorm());
    }

    @Override
    public boolean isActive() {

        return true;
    }

    public static class Factory extends RainSensor.Factory {

        public Factory(Server server) {

            super(server);
        }

        @Override
        public IC create(ChangedSign sign) {

            return new RainSensorST(getServer(), sign, this);
        }
    }
}