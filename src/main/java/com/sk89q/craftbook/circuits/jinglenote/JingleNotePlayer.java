// $Id$
/*
 * Tetsuuuu plugin for SK's Minecraft Server Copyright (C) 2010 sk89q <http://www.sk89q.com> All rights reserved.
 */

package com.sk89q.craftbook.circuits.jinglenote;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.sk89q.craftbook.bukkit.util.BukkitUtil;
import com.sk89q.craftbook.circuits.jinglenote.JingleSequencer.Note;

public class JingleNotePlayer implements Runnable {

    protected final String player;
    protected JingleSequencer sequencer;

    public JingleNotePlayer(String player, JingleSequencer seq) {

        this.player = player;
        sequencer = seq;
    }

    @Override
    public void run() {

        try {
            try {
                sequencer.run(this);
            } catch (Throwable t) {
                BukkitUtil.printStacktrace(t);
            }

            Thread.sleep(500);
        } catch (InterruptedException e) {
            BukkitUtil.printStacktrace(e);
        } finally {
            sequencer.stop();
            sequencer = null;
        }
    }

    public String getPlayer() {

        return player;
    }

    public void stop() {

        if (sequencer != null) {
            sequencer.stop();
        }
    }

    Player p = null;

    public void play(Note note) {

        if (p == null || !p.isOnline())
            p = Bukkit.getPlayer(player);

        if (p == null || !p.isOnline() || note == null) {
            return;
        }

        p.playSound(p.getLocation(), note.getInstrument(), note.getVelocity(), note.getNote());
    }
}