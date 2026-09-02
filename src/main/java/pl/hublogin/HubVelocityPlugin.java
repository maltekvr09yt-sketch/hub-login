package pl.hublogin;

import com.google.inject.Inject;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Plugin(
        id = "hublogin",
        name = "HubLogin",
        version = "1.0.0",
        authors = {"HubLogin"}
)
public final class HubLoginPlugin {

    private final ProxyServer proxy;

    @Inject
    public HubLoginPlugin(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

        proxy.getCommandManager().register(
                proxy.getCommandManager()
                        .metaBuilder("hub")
                        .aliases("lobby")
                        .build(),
                new HubCommand()
        );
    }

    private final class HubCommand implements SimpleCommand {

        @Override
        public void execute(Invocation invocation) {

            if (!(invocation.source() instanceof Player player)) {
                invocation.source().sendMessage(
                        Component.text("Ta komenda jest dostępna tylko dla graczy.")
                );
                return;
            }

            List<RegisteredServer> hubs = new ArrayList<>();

            proxy.getServer("hub1").ifPresent(hubs::add);
            proxy.getServer("hub2").ifPresent(hubs::add);

            // Losujemy kolejność hubów.
            Collections.shuffle(hubs);

            connectToHub(player, hubs, 0);
        }

        private void connectToHub(
                Player player,
                List<RegisteredServer> hubs,
                int index
        ) {

            // Nie ma już żadnego dostępnego lobby.
            if (index >= hubs.size()) {

                player.disconnect(
                        Component.text(
                                "Aktualnie wszystkie lobby są wyłączone."
                        )
                );

                return;
            }

            RegisteredServer hub = hubs.get(index);

            player.createConnectionRequest(hub)
                    .connect()
                    .whenComplete((result, error) -> {

                        // Jeżeli połączenie się nie udało,
                        // próbujemy następne lobby.
                        if (error != null
                                || result == null
                                || !result.isSuccessful()) {

                            connectToHub(player, hubs, index + 1);
                        }
                    });
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return true;
        }
    }
}
