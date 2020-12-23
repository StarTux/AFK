package com.cavetale.afk;

import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
final class Session {
    public static final String KEY = "afk:session";
    int lx;
    int ly;
    int lz;
    float lpitch;
    float lyaw;
    private int noMove = 0;
    private int noView = 0;
    int idleTicks;
    boolean afk;

    void setLocation(Location loc) {
        lx = loc.getBlockX();
        ly = loc.getBlockY();
        lz = loc.getBlockZ();
        lpitch = loc.getPitch();
        lyaw = loc.getYaw();
    }

    void update(Player player) {
        Location loc = player.getLocation();
        final int x = loc.getBlockX();
        final int y = loc.getBlockY();
        final int z = loc.getBlockZ();
        final float pitch = loc.getPitch();
        final float yaw = loc.getYaw();
        if (x == lx && y == ly && z == lz) {
            noMove += 1;
        } else {
            noMove = 0;
            lx = x;
            ly = y;
            lz = z;
        }
        if (lpitch == pitch && lyaw == yaw) {
            noView += 1;
        } else {
            noView = 0;
            lpitch = pitch;
            lyaw = yaw;
        }
        idleTicks = Math.max(noMove, noView);
    }
}
