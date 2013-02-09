package com.sk89q.craftbook.circuits.gates.world.items;

import org.bukkit.Server;

import com.sk89q.craftbook.ChangedSign;
import com.sk89q.craftbook.circuits.ic.ChipState;
import com.sk89q.craftbook.circuits.ic.IC;
import com.sk89q.craftbook.circuits.ic.ICFactory;
import com.sk89q.craftbook.circuits.ic.SelfTriggeredIC;

public class ContainerDispenserST extends ContainerDispenser implements SelfTriggeredIC {

    public ContainerDispenserST(Server server, ChangedSign sign, ICFactory factory) {

        super(server, sign, factory);
    }

    @Override
    public String getTitle() {

        return "Self-Triggered Container Dispenser";
    }

    @Override
    public String getSignTitle() {

        return "CONTAINER DISPENSER ST";
    }

    @Override
    public boolean isActive() {

        return true;
    }

    @Override
    public void think(ChipState chip) {

        chip.setOutput(0, dispense());
    }

    public static class Factory extends ContainerDispenser.Factory {

        public Factory(Server server) {

            super(server);
        }

        @Override
        public IC create(ChangedSign sign) {

            return new ContainerDispenserST(getServer(), sign, this);
        }
    }
}