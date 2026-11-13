package io.josemmo.bukkit.plugin.utils;

import io.josemmo.bukkit.plugin.YamipaPlugin;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Optional access to the image flag provided by AuroraProtections. */
public final class AuroraImagePermissions {
    private static final String SERVICE_NAME = "com.auroracore.protections.api.ProtectionPermissionService";
    private static final Logger LOGGER = Logger.getLogger("AuroraImagePermissions");
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private AuroraImagePermissions() {}

    public static boolean allows(Player player, Location[] supports, BlockFace face) {
        Server server = YamipaPlugin.getInstance().getServer();
        Plugin aurora = server.getPluginManager().getPlugin("AuroraProtections");
        if (aurora == null || !aurora.isEnabled()) {
            return true;
        }
        return allows(server.getServicesManager().getRegistrations(aurora), player, supports, face, message -> {
            if (WARNED.compareAndSet(false, true)) {
                LOGGER.warning(message);
            }
        });
    }

    static boolean allows(
        List<RegisteredServiceProvider<?>> registrations, Player player, Location[] supports, BlockFace face,
        Consumer<String> warn
    ) {
        for (RegisteredServiceProvider<?> registration : registrations) {
            Class<?> service = registration.getService();
            if (!SERVICE_NAME.equals(service.getName())) {
                continue;
            }
            // Use the registered interface from Aurora's class loader so Yamipa can also run independently.
            try {
                Method check = service.getMethod("canUseImages", Player.class, Location.class);
                if (check.getReturnType() != boolean.class) {
                    break;
                }
                Object provider = registration.getProvider();
                Vector offset = face.getDirection();
                for (Location support : supports) {
                    Location panel = support.clone().add(offset);
                    if (!(Boolean) check.invoke(provider, player, support)
                        || !(Boolean) check.invoke(provider, player, panel)) {
                        return false;
                    }
                }
                return true;
            } catch (NoSuchMethodException e) {
                break;
            } catch (ReflectiveOperationException | LinkageError e) {
                Throwable cause = e instanceof InvocationTargetException ? e.getCause() : e;
                warn.accept("AuroraProtections image permission check failed: " + cause);
                return false;
            }
        }
        warn.accept("AuroraProtections is enabled but does not provide canUseImages(Player, Location)."
            + " Install a compatible version to place or remove images.");
        return false;
    }
}
