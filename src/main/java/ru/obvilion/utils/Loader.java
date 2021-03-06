package ru.obvilion.utils;

import arc.Events;
import arc.files.Fi;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.game.EventType;
import mindustry.game.Rules;
import mindustry.gen.Call;
import mindustry.gen.Player;

import ru.obvilion.config.Config;
import ru.obvilion.config.Lang;
import ru.obvilion.effects.EffectHelper;
import ru.obvilion.events.EventsHelper;
import ru.obvilion.events.PlayerMoveEvent;
import ru.obvilion.servers.Server;
import ru.obvilion.servers.ServersHelper;
import ru.obvilion.servers.ServersPinger;

public class Loader {
    public static boolean firstInit = true;

    public static void init() {
        Config.init();
        Lang.init();

        final Fi map = new Fi("config/maps/Hub.msav");
        if (!map.exists()) ResourceUtil.copy("Hub.msav", map);

        ServersHelper.init();
        AntiBuild.init();
        EventsHelper.init();
        ServersPinger.init();
        EffectHelper.init();

        if (firstInit) {
            initEvents();
            firstInit = false;
        }
    }

    public static void initEvents() {
        Events.on(EventType.PlayEvent.class, event -> {
            Vars.state.rules.waves = false;
            Vars.state.rules.revealedBlocks.addAll(
                Blocks.launchPad, Blocks.launchPadLarge, Blocks.interplanetaryAccelerator,
                Blocks.resupplyPoint, Blocks.illuminator, Blocks.scrapWall,
                Blocks.scrapWallGigantic, Blocks.scrapWallHuge, Blocks.scrapWallLarge
            );
        });

        Events.on(EventType.PlayerJoin.class, event -> {
            final Player player = event.player;
            final Rules rules = Vars.state.rules.copy();

            if (!player.admin) {
                rules.bannedBlocks.addAll(Vars.content.blocks());
            }

            Call.setRules(player.con, rules);

            ServersPinger.update();
            ServersHelper.servers.forEach(server -> {
                final int x = (server.xPos * 8 + (server.block.size - 1) * 4);
                final int y = server.yPos * 8 + (server.block.size - 1) * 8 + 8;

                Call.label(server.name, 200000, x, y);
            });

            EffectHelper.onJoin(player);
        });

        Events.on(EventType.PlayerLeave.class, event -> {
            EffectHelper.onLeave(event.player);
        });

        Events.on(PlayerMoveEvent.class, event -> {
            final Player player = event.player;
            final Server portal = ServersHelper.checkAll(
                (int) player.x / 8,
                (int) player.y / 8
            );

            if (portal != null) {
                Call.connect(player.con, portal.ip, portal.port);
            }

            EffectHelper.onMove(player);
        });
    }
}
