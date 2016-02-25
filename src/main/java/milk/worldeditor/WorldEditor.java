package milk.worldeditor;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;

public class WorldEditor extends PluginBase{

    @Override
    public void onEnable(){
        this.getServer().getLogger().info(TextFormat.GOLD + "[WorldEditor]Plugin has been enabled");
    }

    @Override
    public void onDisable(){
        this.getServer().getLogger().info(TextFormat.GOLD + "[WorldEditor]Plugin has been disabled");
    }

}