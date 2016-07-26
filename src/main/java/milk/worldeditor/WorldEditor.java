package milk.worldeditor;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntityChest;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.TextFormat;
import milk.worldeditor.task.WorldEditorTask;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class WorldEditor extends PluginBase implements Listener{

    public LinkedHashMap<String, Object> data;
    public HashMap<String, Position[]> pos = new HashMap<>();

    @Override
    public void onEnable(){
        this.saveDefaultConfig();
        this.data = (LinkedHashMap<String, Object>) this.getConfig().getAll();
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getLogger().info(TextFormat.GOLD + "[WorldEditor]Plugin has been enabled");
    }

    @Override
    public void onDisable(){
        this.getServer().getLogger().info(TextFormat.GOLD + "[WorldEditor]Plugin has been disabled");
    }

    public int getData(String key, int def){
        Object value = this.data.getOrDefault(key, def);
        return value instanceof Integer ? (int) value : -1;
    }

    public boolean getData(String key, boolean def){
        Object value = this.data.getOrDefault(key, def);
        return value instanceof Boolean && (boolean) value;
    }

    public boolean isTool(Item item){
        int damage = this.getData("tool-damage", -1);
        return item.getId() == this.getData("tool-id", Item.IRON_HOE) && (damage == -1 || item.getDamage() == damage);
    }

    public void debugInfo(String message){
        if(this.getData("debug", false)){
            this.getServer().getLogger().info(TextFormat.GOLD + "[WorldEditor]" + message);
        }
    }
    
    public boolean canSetting(Player player){
        Position[] posar = new Position[2];
        if(this.pos.containsKey(player.getName())){
            posar = this.pos.get(player.getName());
        }
        return posar[0] != null && posar[1] != null;
    }

    public Position getPos(Player player, int index){
        if(index > 2 || index < 1){
            return null;
        }

        Position[] posar = new Position[2];
        if(this.pos.containsKey(player.getName())){
            posar = this.pos.get(player.getName());
        }
        return posar[index - 1];
    }
    
    public boolean setPos(Player player, Position pos, int index){
        if(index > 2 || index < 1 || !player.hasPermission("worldeditor.command.setpos" + index)){
            return false;
        }

        Position[] posar = new Position[2];
        if(this.pos.containsKey(player.getName())){
            posar = this.pos.get(player.getName());
        }
        posar[index - 1] = pos;
        this.pos.put(player.getName(), posar);
        player.sendMessage(String.format("[WorldEditor]Pos%s 지점을 선택했어요 (%s, %s, %s)", index, pos.x, pos.y, pos.z));
        return true;
    }
    
    public void clearPos(Player player){
        this.pos.remove(player.getName());
        player.sendMessage("[WorldEditor]Pos1, Pos2 지점을 초기화 했습니다");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent ev){
        Item item = ev.getItem();
        Block block = ev.getBlock();
        Player player = ev.getPlayer();
        if(
            this.isTool(item)
            && ev.getFace() != 255
            && ev.getAction() == PlayerInteractEvent.RIGHT_CLICK_BLOCK
        ){
            if(!this.setPos(player, block, 2)) ev.setCancelled();
        }else if(
            this.isTool(item)
            && ev.getAction() == PlayerInteractEvent.LEFT_CLICK_BLOCK
        ){
            if(!this.setPos(player, block, 1)) ev.setCancelled();
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent ev){
        Item item = ev.getItem();
        Block block = ev.getBlock();
        Player player = ev.getPlayer();
        if(this.isTool(item)){
            ev.setCancelled(this.setPos(player, block, 1));
        }
    }

    public void set(Block block, Position pos){
        BlockEntity tile = pos.getLevel().getBlockEntity(pos);
        if(tile != null){
            if(tile instanceof BlockEntityChest){
                ((BlockEntityChest) tile).unpair();
            }
            tile.close();
        }
        pos.getLevel().setBlock(pos, block, false, false);
    }

    public void setBlock(int[] xyz, Vector3 spos, Vector3 epos, Block block, Level level){
        setBlock(xyz, spos, epos, block, level, null);
    }

    public void setBlock(int[] xyz, Vector3 spos, Vector3 epos, Block block, Level level, Player player){
        int count = 0;
        int x = xyz[0];
        int y = xyz[1];
        int z = xyz[2];
        while(true){
            if(count < this.getData("limit-block", 200)){
                BaseFullChunk chunk = level.getChunk(x >> 4, z >> 4, true);
                if(chunk != null){
                    int id = chunk.getBlockId(x & 0x0f, y & 0x7f, z & 0x0f);
                    int data = chunk.getBlockData(x & 0x0f, y & 0x7f, z & 0x0f);
                    if(id != block.getId() || data != block.getDamage()){
                        count++;
                        this.set(block, new Position(x, y, z, level));
                    }
                }

                if(z < epos.z){
                    z++;
                }else{
                    z = (int) spos.z;
                    if(y < epos.y){
                        y++;
                    }else{
                        y = (int) spos.y;
                        if(x < epos.x){
                            x++;
                        }else{
                            break;
                        }
                    }
                }
            }else{
                this.getServer().getScheduler().scheduleDelayedTask(new WorldEditorTask(this, "setBlock", new int[]{x, y, z}, spos, epos, block, level, player), 1);
                return;
            }
        }

        if(player != null){
            player.sendMessage("[WorldEditor]모든 블럭을 설정했어요");
        }
        this.debugInfo((player == null ? "" : player.getName() + "님이 ") + "블럭설정을 끝냈어요");
    }

    public void replaceBlock(int[] xyz, Vector3 spos, Vector3 epos, Block block, Block target, Level level){
        replaceBlock(xyz, spos, epos, block, target, level, null);
    }

    public void replaceBlock(int[] xyz, Vector3 spos, Vector3 epos, Block block, Block target, Level level, Player player){
        int count = 0;
        int x = xyz[0];
        int y = xyz[1];
        int z = xyz[2];
        while(true){
            if(count < this.getData("limit-block", 200)){
                BaseFullChunk chunk = level.getChunk(x >> 4, z >> 4, true);
                if(chunk != null){
                    int id = chunk.getBlockId(x & 0x0f, y & 0x7f, z & 0x0f);
                    int data = chunk.getBlockData(x & 0x0f, y & 0x7f, z & 0x0f);
                    if(id == block.getId() && data == block.getDamage()){
                        count++;
                        this.set(target, new Position(x, y, z, level));
                    }
                }

                if(z < epos.z){
                    z++;
                }else{
                    z = (int) spos.z;
                    if(y < epos.y){
                        y++;
                    }else{
                        y = (int) spos.y;
                        if(x < epos.x){
                            x++;
                        }else{
                            break;
                        }
                    }
                }
            }else{
                this.getServer().getScheduler().scheduleDelayedTask(new WorldEditorTask(this, "replaceBlock", new int[]{x, y, z}, spos, epos, block, target, level, player), 1);
                return;
            }
        }

        if(player != null){
            player.sendMessage("[WorldEditor]모든 블럭을 설정했어요");
        }
        this.debugInfo((player == null ? "" : player.getName() + "님이 ") + "블럭변경을 끝냈어요");
    }

    public boolean onCommand(CommandSender i, Command cmd, String label, String[] sub){
        if(!(i instanceof Player)){
            return true;
        }

        String output = "[WorldEditor]";

        String callback = "";
        Object[] params = new Object[0];
        switch(cmd.getName()){
            case "/clearpos":
                this.clearPos((Player) i);
                return true;
            case "/pos1":
                this.setPos((Player) i, ((Player) i).floor(), 1);
                return true;
            case "/pos2":
                this.setPos((Player) i, ((Player) i).floor(), 2);
                return true;
            case "/set":{
                if(sub.length < 1){
                    output += "사용법: //set <id[:meta]>";
                    break;
                }

                if(!this.canSetting((Player) i)){
                    output += "지역을 먼저 설정해주세요";
                    break;
                }

                String[] set = sub[0].split(":");
                Vector3 pos1 = this.getPos((Player) i, 1);
                Vector3 pos2 = this.getPos((Player) i, 2);
                Vector3 spos = new Vector3(Math.min(pos1.x, pos2.x), Math.min(pos1.y, pos2.y), Math.min(pos1.z, pos2.z));
                Vector3 epos = new Vector3(Math.max(pos1.x, pos2.x), Math.max(pos1.y, pos2.y), Math.max(pos1.z, pos2.z));
                output += "블럭 설정을 시작했어요";
                callback = "setBlock";
                params = new Object[]{new int[]{(int) spos.x, (int) spos.y, (int) spos.z}, spos, epos, Block.get(Integer.parseInt(set[0]), set.length > 1 ? Integer.parseInt(set[1]) : 0), ((Player) i).getLevel(), i};
                this.debugInfo(i.getName() + "님이 블럭설정을 시작했어요");
                break;
            }
            case "/replace":{
                if(sub.length < 2){
                    output += "사용법: //replace <(선택)id[:meta]> <(바꿀)id[:meta>]";
                    break;
                }
                if(!this.canSetting((Player) i)){
                    output += "지역을 먼저 설정해주세요";
                    break;
                }
                String[] get = sub[0].split(":");
                String[] set = sub[1].split(":");
                Vector3 pos1 = this.getPos((Player) i, 1);
                Vector3 pos2 = this.getPos((Player) i, 2);
                Vector3 spos = new Vector3(Math.min(pos1.x, pos2.x), Math.min(pos1.y, pos2.y), Math.min(pos1.z, pos2.z));
                Vector3 epos = new Vector3(Math.max(pos1.x, pos2.x), Math.max(pos1.y, pos2.y), Math.max(pos1.z, pos2.z));
                output += "블럭 변경을 시작했어요";
                callback = "replaceBlock";
                params = new Object[]{new int[]{(int) spos.x, (int) spos.y, (int) spos.z}, spos, epos, Block.get(Integer.parseInt(get[0]), get.length > 1 ? Integer.parseInt(get[1]) : 0), Block.get(Integer.parseInt(set[0]), set.length > 1 ? Integer.parseInt(set[1]) : 0), ((Player) i).getLevel(), i};
                this.debugInfo(i.getName() + "님이 블럭변경을 시작했어요");
                break;
            }
            /*case "/undo":{
                if(!this.canSetting((Player) i)){
                    output += "지역을 먼저 설정해주세요";
                    break;
                }

                Vector3 pos1 = this.getPos((Player) i, 1);
                Vector3 pos2 = this.getPos((Player) i, 2);
                double endX = Math.max(pos1.x, pos2.x);
                double endY = Math.max(pos1.y, pos2.y);
                double endZ = Math.max(pos1.z, pos2.z);
                double startX = Math.min(pos1.x, pos2.x);
                double startY = Math.min(pos1.y, pos2.y);
                double startZ = Math.min(pos1.z, pos2.z);
                output += "블럭을 되돌리는 중입니다";
                callback = "undoBlock";
                params = new Object[]{startX, startY, startZ, endX, endY, endZ, ((Player) i).getLevel(), i};
                this.debugInfo(i.getName() + "님이 블럭을 복구하기 시작했어요");
                break;
            }
            case "/redo":{
                if(!this.canSetting((Player) i)){
                    output += "지역을 먼저 설정해주세요";
                    break;
                }

                Vector3 pos1 = this.getPos((Player) i, 1);
                Vector3 pos2 = this.getPos((Player) i, 2);
                double endX = Math.max(pos1.x, pos2.x);
                double endY = Math.max(pos1.y, pos2.y);
                double endZ = Math.max(pos1.z, pos2.z);
                double startX = Math.min(pos1.x, pos2.x);
                double startY = Math.min(pos1.y, pos2.y);
                double startZ = Math.min(pos1.z, pos2.z);
                output += "블럭 설정을 시작했어요";
                callback = "redoBlock";
                params = new Object[]{startX, startY, startZ, endX, endY, endZ, ((Player) i).getLevel(), i};
                this.debugInfo(i.getName() + "님이 복구한 블럭을 되돌리기 시작했어요");
                break;
            }
            case "/copy":{
                if(!this.canSetting((Player) i)){
                    output += "지역을 먼저 설정해주세요";
                    break;
                }

                Vector3 pos1 = this.getPos((Player) i, 1);
                Vector3 pos2 = this.getPos((Player) i, 2);
                double endX = Math.max(pos1.x, pos2.x);
                double endY = Math.max(pos1.y, pos2.y);
                double endZ = Math.max(pos1.z, pos2.z);
                double startX = Math.min(pos1.x, pos2.x);
                double startY = Math.min(pos1.y, pos2.y);
                double startZ = Math.min(pos1.z, pos2.z);
                output += "블럭 복사를 시작했어요";
                callback = "copyBlock";
                params = new Object[]{startX, startY, startZ, endX, endY, endZ, ((Player) i).getLevel(), i};
                this.debugInfo(i.getName() + "님이 블럭 복사를 시작했어요");
                break;
            }
            case "/paste":
                output += "블럭 붙여넣기를 시작했어요";
                callback = "pasteBlock";
                params = new Object[]{((Player) i).floor(), i};
                this.debugInfo(i.getName() + "님이 블럭 붙여넣기를 시작했어요");
                break;
            case "/cut":{
                if(!this.canSetting((Player) i)){
                    output += "지역을 먼저 설정해주세요";
                    break;
                }

                Vector3 pos1 = this.getPos((Player) i, 1);
                Vector3 pos2 = this.getPos((Player) i, 2);
                double endX = Math.max(pos1.x, pos2.x);
                double endY = Math.max(pos1.y, pos2.y);
                double endZ = Math.max(pos1.z, pos2.z);
                double startX = Math.min(pos1.x, pos2.x);
                double startY = Math.min(pos1.y, pos2.y);
                double startZ = Math.min(pos1.z, pos2.z);
                output += "블럭 복사를 시작했어요";
                callback = "cutBlock";
                params = new Object[]{startX, startY, startZ, endX, endY, endZ, ((Player) i).getLevel(), i};
                this.debugInfo(i.getName() + "님이 블럭 복사를 시작했어요");
                break;
            }*/
        }

        if(!output.equals("[WorldEditor]")) i.sendMessage(output);
        try{
            Class[] clazz = new Class[params.length];
            for(int a = 0; a < params.length; a++){
                clazz[a] = params[a].getClass();
            }
            this.getClass().getMethod(callback, clazz).invoke(this, params);
        }catch(Exception e){}
        return true;
    }

}