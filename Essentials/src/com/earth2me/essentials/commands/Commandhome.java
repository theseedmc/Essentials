package com.earth2me.essentials.commands;

import com.earth2me.essentials.Trade;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.StringUtil;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.earth2me.essentials.I18n.tl;


public class Commandhome extends EssentialsCommand {
    public Commandhome() {
        super("home");
    }

    // This method contains an undocumented translation parameters #EasterEgg
    @Override
    public void run(final Server server, final User user, final String commandLabel, final String[] args) throws Exception {
        final Trade charge = new Trade(this.getName(), ess);
        User player = user;
        String homeName = "";
        String[] nameParts;
        if (args.length > 0) {
            nameParts = args[0].split(":");
            if (nameParts[0].length() == args[0].length() || !user.isAuthorized("essentials.home.others")) {
                homeName = nameParts[0];
            } else {
                player = getPlayer(server, nameParts, 0, true, true);
                if (nameParts.length > 1) {
                    homeName = nameParts[1];
                }
            }
        }
        try {
            if ("bed".equalsIgnoreCase(homeName) && user.isAuthorized("essentials.home.bed")) {
                try {
                    Player player2 = player.getBase();
                    net.minecraft.server.v1_15_R1.WorldServer worldServer = ((org.bukkit.craftbukkit.v1_15_R1.CraftWorld) player.getWorld()).getHandle();
                    net.minecraft.server.v1_15_R1.EntityPlayer entityPlayer = ((org.bukkit.craftbukkit.v1_15_R1.entity.CraftPlayer) player.getBase()).getHandle();
                    if (entityPlayer.getBed() != null) {
                        worldServer.getChunkProvider().getTickingChunkAsync(entityPlayer.getBed().getX() >> 4, entityPlayer.getBed().getZ() >> 4, (chunk -> {
                            if (player2.isOnline()) {
                                Location bedLocation = player2.getBedSpawnLocation();
                                if (bedLocation != null) {
                                    try {
                                        user.getTeleport().teleport(bedLocation, charge, TeleportCause.COMMAND);
                                    } catch (Exception e) {
                                    }
                                } else {
                                    player2.sendMessage(tl("bedMissing"));
                                }
                            }
                        }));
                    }
                } catch (Exception ex) {
                    //Fallback in-case of jar change, this will keep support
                    final Location bed = player.getBase().getBedSpawnLocation();
                    if (bed != null) {
                        user.getTeleport().teleport(bed, charge, TeleportCause.COMMAND);
                        throw new NoChargeException();
                    } else {
                        throw new Exception(tl("bedMissing"));
                    }
                }
                return;
            }
            
            goHome(user, player, homeName.toLowerCase(Locale.ENGLISH), charge);
        } catch (NotEnoughArgumentsException e) {
            Location bed = player.getBase().getBedSpawnLocation();
            final List<String> homes = player.getHomes();
            if (homes.isEmpty() && player.equals(user)) {
                if (ess.getSettings().isSpawnIfNoHome()) {
                    user.getTeleport().respawn(charge, TeleportCause.COMMAND);
                } else {
                    throw new Exception(tl("noHomeSetPlayer"));
                }
            } else if (homes.isEmpty()) {
                throw new Exception(tl("noHomeSetPlayer"));
            } else if (homes.size() == 1 && player.equals(user)) {
                goHome(user, player, homes.get(0), charge);
            } else {
                final int count = homes.size();
                if (user.isAuthorized("essentials.home.bed")) {
                    if (bed != null) {
                        homes.add(tl("bed"));
                    } else {
                        homes.add(tl("bedNull"));
                    }
                }
                user.sendMessage(tl("homes", StringUtil.joinList(homes), count, getHomeLimit(player)));
            }
        }
        throw new NoChargeException();
    }

    private String getHomeLimit(final User player) {
        if (!player.getBase().isOnline()) {
            return "?";
        }
        if (player.isAuthorized("essentials.sethome.multiple.unlimited")) {
            return "*";
        }
        return Integer.toString(ess.getSettings().getHomeLimit(player));
    }

    private void goHome(final User user, final User player, final String home, final Trade charge) throws Exception {
        if (home.length() < 1) {
            throw new NotEnoughArgumentsException();
        }
        final Location loc = player.getHome(home);
        if (loc == null) {
            throw new NotEnoughArgumentsException();
        }
        if (user.getWorld() != loc.getWorld() && ess.getSettings().isWorldHomePermissions() && !user.isAuthorized("essentials.worlds." + loc.getWorld().getName())) {
            throw new Exception(tl("noPerm", "essentials.worlds." + loc.getWorld().getName()));
        }
        user.getTeleport().teleport(loc, charge, TeleportCause.COMMAND);
        user.sendMessage(tl("teleportHome", home));
    }

    @Override
    protected List<String> getTabCompleteOptions(final Server server, final User user, final String commandLabel, final String[] args) {
        boolean canVisitOthers = user.isAuthorized("essentials.home.others");

        if (args.length == 1) {
            if (canVisitOthers) {
                return getPlayers(server, user);
            } else {
                List<String> homes = user.getHomes();
                if (user.isAuthorized("essentials.home.bed")) {
                    homes.add("bed");
                }
                return homes;
            }
        } else if (args.length == 2 && canVisitOthers) {
            try {
                User otherUser = getPlayer(server, args, 0, true, true);
                List<String> homes = otherUser.getHomes();
                if (user.isAuthorized("essentials.home.bed")) {
                    homes.add("bed");
                }
                return homes;
            } catch (Exception ex) {
                // No such user
                return Collections.emptyList();
            }
        } else {
            return Collections.emptyList();
        }
    }
}
