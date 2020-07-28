package com.i54m.vorplexmotd;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.plugin.Command;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MaintenanceCommand extends Command {

    private final Main plugin = Main.getInstance();

    public MaintenanceCommand() {
        super("maintenance", "vorplexcore.maintenance");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (strings.length >= 1) {
            switch (strings[0].toLowerCase()) {
                case "start": {
                    long length = 0;
                    try {
                        if (strings.length >= 2) {
                            if (strings[1].endsWith("M"))
                                length = (long) 2.628e+9 * (long) Integer.parseInt(strings[1].replace("M", ""));
                            else if (strings[1].toLowerCase().endsWith("w"))
                                length = (long) 6.048e+8 * (long) Integer.parseInt(strings[1].replace("w", ""));
                            else if (strings[1].toLowerCase().endsWith("d"))
                                length = (long) 8.64e+7 * (long) Integer.parseInt(strings[1].replace("d", ""));
                            else if (strings[1].toLowerCase().endsWith("h"))
                                length = (long) 3.6e+6 * (long) Integer.parseInt(strings[1].replace("h", ""));
                            else if (strings[1].endsWith("m"))
                                length = 60000 * (long) Integer.parseInt(strings[1].replace("m", ""));
                            else if (strings[1].toLowerCase().endsWith("s"))
                                length = 1000 * (long) Integer.parseInt(strings[1].replace("s", ""));
                        } else {
                            break;
                        }
                    } catch (NumberFormatException e) {
                        commandSender.sendMessage(new ComponentBuilder(strings[1] + " is not a valid duration!").color(ChatColor.RED).create());
                        commandSender.sendMessage(new ComponentBuilder("Change the maintenance state of the server.").color(ChatColor.RED).append("\nUsage: /maintenance start <length s|m|h|d|w|M>").color(ChatColor.WHITE).create());
                        return;
                    }
                    length += System.currentTimeMillis();
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(length);
                    plugin.config.set("maintenance-end", new SimpleDateFormat("MM/dd/yy HH:mm:ss").format(calendar.getTime()));
                    plugin.maintenance = true;
                    return;
                }
                case "stop": {
                    plugin.maintenance = false;
                    plugin.config.set("maintenance-end", "null");
                    return;
                }
            }
        }
        //help message
        commandSender.sendMessage(new ComponentBuilder("Change the maintenance state of the server.").color(ChatColor.RED).append("\nUsage: /maintenance start <length s|m|h|d|w|M> \nUsage: /maintenance stop").color(ChatColor.WHITE).create());
    }
}