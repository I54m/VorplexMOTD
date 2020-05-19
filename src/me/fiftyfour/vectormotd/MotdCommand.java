package me.fiftyfour.vectormotd;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

public class MotdCommand extends Command {

    private Main plugin = Main.getInstance();

    public MotdCommand() {
        super("motdreload", "vectorcore.motd", "vectormotd", "sl", "serverlist");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        plugin.loadConfig();
        commandSender.sendMessage(new ComponentBuilder("Plugin reloaded!").color(ChatColor.GREEN).create());
    }
}
