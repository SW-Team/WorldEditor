package milk.worldeditor;

import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;

public class WorldEditor extends PluginBase{

    @Override
    public void onEnable(){
        this.getServer().getLogger().info(TextFormat.GOLD + "[WorldEditor]플러그인이 활성화 되었습니다");
    }

    @Override
    public void onDisable(){
        this.getServer().getLogger().info(TextFormat.GOLD + "[WorldEditor]플러그인이 비활성화 되었습니다");
    }

}