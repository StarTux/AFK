package com.cavetale.afk;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@RequiredArgsConstructor @Getter
final class Session {
    protected static final boolean HANDLE_MOVEMENT = false;
    protected final String name;
    protected double lx;
    protected double ly;
    protected double lz;
    protected float lpitch;
    protected float lyaw;
    private int noMove = 0;
    private int noPitch = 0;
    private int noYaw = 0;
    protected int idleTicks;
    protected boolean afk;
    protected long lastSeen;

    protected void setLocation(Location loc) {
        if (HANDLE_MOVEMENT) {
            lx = loc.getBlockX();
            ly = loc.getBlockY();
            lz = loc.getBlockZ();
        }
        lpitch = loc.getPitch();
        lyaw = loc.getYaw();
    }

    private static boolean eq(double a, double b, double d) {
        return Math.abs(a - b) < d;
    }

    protected void update(Player player) {
        Location loc = player.getLocation();
        if (HANDLE_MOVEMENT) {
            final double x = loc.getX();
            final double y = loc.getY();
            final double z = loc.getZ();
            if (eq(x, lx, 0.1) && eq(y, ly, 0.1) && eq(z, lz, 0.1)) {
                noMove += 1;
            } else {
                noMove = 0;
                lx = x;
                ly = y;
                lz = z;
            }
        }
        final float pitch = loc.getPitch();
        final float yaw = loc.getYaw();
        if (eq(lpitch, pitch, 1.0)) { // [-90,90]
            noPitch += 1;
        } else {
            noPitch = 0;
            lpitch = pitch;
        }
        if (eq(lyaw, yaw, 1.0)) { // [0,360]
            noYaw += 1;
        } else {
            noYaw = 0;
            lyaw = yaw;
        }
        idleTicks = HANDLE_MOVEMENT
            ? Math.max(noMove, Math.max(noPitch, noYaw))
            : Math.max(noPitch, noYaw);
    }
}
