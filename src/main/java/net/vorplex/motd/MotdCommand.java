package net.vorplex.motd;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

public class MotdCommand extends Command {

    private final Main plugin = Main.getInstance();

    public MotdCommand() {
        super("motdreload", "vorplexcore.motd", "sl", "serverlist");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        plugin.loadConfig();
        commandSender.sendMessage(new ComponentBuilder("Plugin reloaded!").color(ChatColor.GREEN).create());
    }
}
