package com.i54m.vorplexmotd;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.Favicon;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class Main extends Plugin implements Listener {

    private File configFile;
    public Configuration config;
    private static Main instance = null;
    public boolean maintenance = false;
    public LuckPerms luckPerms = null;

    private void setInstance(Main instance){
        Main.instance = instance;
    }

    public static Main getInstance(){
        return instance;
    }


    public void loadConfig() {
        try {
            configFile = new File(getDataFolder(), "config.yml");
            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
            }
            if (!configFile.exists()) {
                configFile.createNewFile();
                saveDefaultConfig();
            }
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
            if (!config.getString("maintenance-end").equals("null"))
                maintenance = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveConfig(){
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveDefaultConfig() {
        try {
            Files.copy(getResourceAsStream("config.yml"), configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(getResourceAsStream("maintenance-icon.png"), new File(getDataFolder(), "maintenance-icon.png").toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create configuration file", e);
        }
    }

    @Override
    public void onEnable() {
        setInstance(this);
        //check luckperms dependency
        try {
            luckPerms = LuckPermsProvider.get();
        } catch (Exception e) {
            luckPerms = null;
        }
        if (luckPerms == null) {
            getLogger().warning(ChatColor.RED + "Luck Perms not detected, Plugin has been Disabled!");
            getProxy().getPluginManager().unregisterCommands(this);
            getProxy().getPluginManager().unregisterListeners(this);
            return;
        }
        loadConfig();
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new MotdCommand());
        getProxy().getPluginManager().registerCommand(this, new MaintenanceCommand());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLogin(PreLoginEvent event) {
        if (getTime(true) != null) {
            User user = luckPerms.getUserManager().getUser(event.getConnection().getUniqueId());
            if (user == null) {
                UserFetcher userFetcher = new UserFetcher();
                userFetcher.setUuid(event.getConnection().getUniqueId());
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                Future<User> userFuture = executorService.submit(userFetcher);
                try {
                    user = userFuture.get(500, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    user = null;
                }
                executorService.shutdown();
                if (user == null) {
                    getLogger().warning("Could not fetch user instance for: " + event.getConnection().getUniqueId().toString());
                    event.setCancelled(true);
                    event.setCancelReason(new ComponentBuilder("Unable to fetch your permission data!").create());
                    return;
                }
            }
            ContextManager cm = luckPerms.getContextManager();
            QueryOptions queryOptions = cm.getQueryOptions(user).orElse(cm.getStaticQueryOptions());
            CachedPermissionData permissionData = user.getCachedData().getPermissionData(queryOptions);
            if (!permissionData.checkPermission("vorplexcore.maintenance.bypass").asBoolean()) {
                event.setCancelled(true);
                event.setCancelReason(TextComponent.fromLegacyText(config.getString("maintenance.kick-message").replaceAll("%timeleft%", getTime(true))));
            }
        }
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onPing(ProxyPingEvent event) {
        ServerPing sp = event.getResponse();
        PendingConnection con = event.getConnection();
        if (!config.getString("maintenance-end").equals("null") && getTime(true) != null) {
            String message = config.getString("maintenance.firstLine").replaceAll("%timeleft%", getTime(true)) + ChatColor.RESET + "\n" + config.getString("maintenance.secondLine").replaceAll("%timeleft%", getTime(true));
            sp.setDescription(ChatColor.translateAlternateColorCodes('&', message));

            if (config.getBoolean("maintenance.variable-slots"))
                sp.getPlayers().setMax(getProxy().getOnlineCount() + config.getInt("maintenance.slots"));
            else
                sp.getPlayers().setMax(config.getInt("maintenance.slots"));
            sp.getPlayers().setOnline(getProxy().getOnlineCount());
            sp.getPlayers().setSample(new ServerPing.PlayerInfo[]{
                    new ServerPing.PlayerInfo(ChatColor.translateAlternateColorCodes('&', config.getString("maintenance.hovermessage")), UUID.fromString("0-0-0-0-0"))});

            sp.getVersion().setProtocol(-1);
            sp.getVersion().setName(ChatColor.translateAlternateColorCodes('&', config.getString("maintenance.version")));

            try {
                sp.setFavicon(Favicon.create(ImageIO.read(new File(getDataFolder(), "maintenance-icon.png"))));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            if (getTime(true) == null)
                maintenance = false;
            String message = config.getString("firstLine").replaceAll("%timeleft%", getTime(false)) + ChatColor.RESET + "\n" + config.getString("secondLine").replaceAll("%timeleft%", getTime(false));
            sp.setDescription(ChatColor.translateAlternateColorCodes('&', message));

            if (config.getBoolean("variable-slots"))
                sp.getPlayers().setMax(getProxy().getOnlineCount() + config.getInt("slots"));
            else
                sp.getPlayers().setMax(config.getInt("slots"));
            sp.getPlayers().setOnline(getProxy().getOnlineCount());
            sp.getPlayers().setSample(new ServerPing.PlayerInfo[]{
                    new ServerPing.PlayerInfo(ChatColor.translateAlternateColorCodes('&', config.getString("hovermessage")), UUID.fromString("0-0-0-0-0"))});

            sp.getVersion().setProtocol(con.getVersion());
            sp.getVersion().setName(ChatColor.translateAlternateColorCodes('&', config.getString("version")));

            try {
                sp.setFavicon(Favicon.create(ImageIO.read(new File("server-icon.png"))));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        event.setResponse(sp);
    }

    public String getTime(boolean maintenance) {
        String dateStop = maintenance ? config.getString("maintenance-end") : config.getString("date");
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
        Date date = null;

        try {
            format.setTimeZone(TimeZone.getTimeZone(config.getString("timezone")));
            date = format.parse(dateStop);
        } catch (ParseException var23) {
            var23.printStackTrace();
        }

        Date current = new Date();
        long diff = date.getTime() - current.getTime();
        if (diff < 0L && maintenance) {
          return null;
        } else if (diff < 0L) {
            return config.getString("time-value-end");
        } else {
            long days = TimeUnit.MILLISECONDS.toDays(diff);
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
            long rhours = days == 0L ? hours : hours % (days * 24L);
            long rminutes = hours == 0L ? minutes : minutes % (hours * 60L);
            long rseconds = minutes == 0L ? seconds : seconds % (minutes * 60L);
            StringBuilder sb = new StringBuilder();
            if (days > 1L) sb.append(days).append(" days");
            if (days == 1L) sb.append(days).append(" day");
            if (rhours > 1L) sb.append(days > 0L ? ", " : "").append(rhours).append(" hours");
            if (rhours == 1L) sb.append(days > 0L ? ", " : "").append(rhours).append(" hour");
            if (rminutes > 1L)
                sb.append((days <= 0L || hours > 0L) && hours <= 0L ? "" : ", ").append(rminutes).append(" minutes");
            if (rminutes == 1L)
                sb.append((days <= 0L || hours > 0L) && hours <= 0L ? "" : ", ").append(rminutes).append(" minute");
            if (rseconds > 1L)
                sb.append((days <= 0L && hours <= 0L || minutes > 0L) && minutes <= 0L ? "" : ", ").append(rseconds).append(" seconds");
            if (rseconds == 1L)
                sb.append((days <= 0L && hours <= 0L || minutes > 0L) && minutes <= 0L ? "" : ", ").append(rseconds).append(" second");
            return sb.toString();
        }
    }
}

