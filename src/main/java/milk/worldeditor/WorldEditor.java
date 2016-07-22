package milk.worldeditor;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntityChest;
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
    public HashMap<String, Vector3[]> pos = new HashMap<>();

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

    public boolean setPos1(Player player, Vector3 pos){
        if(!player.hasPermission("worldeditor.command.setpos1")){
            return false;
        }

        Vector3[] posar = new Vector3[2];
        if(this.pos.containsKey(player.getName())){
            posar = this.pos.get(player.getName());
        }
        posar[0] = pos;
        this.pos.put(player.getName(), posar);
        player.sendMessage("[WorldEditor]Pos1 지점을 선택했어요 (" + pos.x + ", " + pos.y + ", " + pos.z + ")");
        return true;
    }

    public boolean setPos2(Player player, Vector3 pos){
        if(!player.hasPermission("worldeditor.command.setpos2")){
            return false;
        }

        Vector3[] posar = new Vector3[2];
        if(this.pos.containsKey(player.getName())){
            posar = this.pos.get(player.getName());
        }
        posar[1] = pos;
        this.pos.put(player.getName(), posar);
        player.sendMessage("[WorldEditor]Pos2 지점을 선택했어요 (" + pos.x + ", " + pos.y + ", " + pos.z + ")");
        return true;
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
            ev.setCancelled(this.setPos2(player, block));
        }else if(
            this.isTool(item)
            && ev.getAction() == PlayerInteractEvent.LEFT_CLICK_BLOCK
        ){
            ev.setCancelled(this.setPos1(player, block));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent ev){
        Item item = ev.getItem();
        Block block = ev.getBlock();
        Player player = ev.getPlayer();
        if(this.isTool(item)){
            ev.setCancelled(this.setPos1(player, block));
        }
    }

    public void set(Block block, Vector3 pos, Level level){
        if(pos instanceof Position && ((Position) pos).getLevel() != null){
            level = ((Position) pos).getLevel();
        }else if(block.getLevel() != null){
            level = block.getLevel();
        }else if(level == null){
            return;
        }

        BlockEntity tile = level.getBlockEntity(pos);
        if(tile != null){
            if(tile instanceof BlockEntityChest){
                ((BlockEntityChest) tile).unpair();
            }
            tile.close();
        }
        level.setBlock(pos, block, false, false);
    }

    public void setBlock(int sx, int sy, int sz, int ex, int ey, int ez, Block block, Player player){
        int count = 0;
        int x = sx;
        int y = sy;
        int z = sz;
        while(true){
            if(count < this.getData("limit-block", 200)){
                BaseFullChunk chunk = block.getLevel().getChunk(x >> 4, z >> 4, true);
                if(chunk != null){
                    int id = chunk.getBlockId(x & 0x0f, y & 0x7f, z & 0x0f);
                    int data = chunk.getBlockData(x & 0x0f, y & 0x7f, z & 0x0f);
                    if(id != block.getId() || data != block.getDamage()){
                        count++;
                        this.set(block, new Position(x, y, z, block.getLevel()), null);
                    }
                }

                if(z < ez){
                    z++;
                }else{
                    z = sz;
                    if(y < ey){
                        y++;
                    }else{
                        y = sy;
                        if(x < ex){
                            x++;
                        }else{
                            break;
                        }
                    }
                }
            }else{
                //this.getServer().getScheduler().scheduleDelayedTask(new WorldEditorTask(this, ""), 1);
                return;
            }
        }

        if(player != null){
            player.sendMessage("[WorldEditor]모든 블럭을 설정했어요");
        }
        this.debugInfo((player == null ? "" : player.getName() + "님이 ") + "블럭설정을 끝냈어요");
    }

    public void setBlock(int sx, int[] syy, int[] szz, int ex, int ey, int ez, Block block, Player player){
        int count = 0;
        int x = sx;
        int y = syy[0];
        int sy = syy[1];
        int z = szz[0];
        int sz = szz[1];
        while(true){
            if(count < this.getData("limit-block", 200)){
                BaseFullChunk chunk = block.getLevel().getChunk(x >> 4, z >> 4, true);
                if(chunk != null){
                    int id = chunk.getBlockId(x & 0x0f, y & 0x7f, z & 0x0f);
                    int data = chunk.getBlockData(x & 0x0f, y & 0x7f, z & 0x0f);
                    if(id != block.getId() || data != block.getDamage()){
                        count++;
                        this.set(block, new Position(x, y, z, block.getLevel()), null);
                    }
                }

                if(z < ez){
                    z++;
                }else{
                    z = sz;
                    if(y < ey){
                        y++;
                    }else{
                        y = sy;
                        if(x < ex){
                            x++;
                        }else{
                            break;
                        }
                    }
                }
            }else{
                //this.getServer().getScheduler().scheduleDelayedTask(new WorldEditorTask(this, ""), 1);
                return;
            }
        }

        if(player != null){
            player.sendMessage("[WorldEditor]모든 블럭을 설정했어요");
        }
        this.debugInfo((player == null ? "" : player.getName() + "님이 ") + "블럭설정을 끝냈어요");
    }

    public void replaceBlock(int sx, int sy, int sz, int ex, int ey, int ez, Block block, Block target, Player player){
        int count = 0;
        int x = sx;
        int y = sy;
        int z = sz;
        while(true){
            if(count < this.getData("limit-block", 200)){
                BaseFullChunk chunk = block.getLevel().getChunk(x >> 4, z >> 4, true);
                if(chunk != null){
                    int id = chunk.getBlockId(x & 0x0f, y & 0x7f, z & 0x0f);
                    int data = chunk.getBlockData(x & 0x0f, y & 0x7f, z & 0x0f);
                    if(id == block.getId() && data == block.getDamage()){
                        count++;
                        this.set(target, new Vector3(x, y, z), null);
                    }
                }

                if(z < ez){
                    z++;
                }else{
                    z = sz;
                    if(y < ey){
                        y++;
                    }else{
                        y = sy;
                        if(x < ex){
                            x++;
                        }else{
                            break;
                        }
                    }
                }
            }else{
                //this.getServer().getScheduler().scheduleDelayedTask(new WorldEditorTask(this, ""), 1);
                return;
            }
        }

        if(player != null){
            player.sendMessage("[WorldEditor]모든 블럭을 설정했어요");
        }
        this.debugInfo((player == null ? "" : player.getName() + "님이 ") + "블럭변경을 끝냈어요");
    }

    public void replaceBlock(int sx, int[] syy, int[] szz, int ex, int ey, int ez, Block block, Block target, Player player){
        int count = 0;
        int x = sx;
        int y = syy[0];
        int sy = syy[1];
        int z = szz[0];
        int sz = szz[1];
        while(true){
            if(count < this.getData("limit-block", 200)){
                BaseFullChunk chunk = block.getLevel().getChunk(x >> 4, z >> 4, true);
                if(chunk != null){
                    int id = chunk.getBlockId(x & 0x0f, y & 0x7f, z & 0x0f);
                    int data = chunk.getBlockData(x & 0x0f, y & 0x7f, z & 0x0f);
                    if(id == block.getId() && data == block.getDamage()){
                        count++;
                        this.set(target, new Vector3(x, y, z), null);
                    }
                }

                if(z < ez){
                    z++;
                }else{
                    z = sz;
                    if(y < ey){
                        y++;
                    }else{
                        y = sy;
                        if(x < ex){
                            x++;
                        }else{
                            break;
                        }
                    }
                }
            }else{
                //this.getServer().getScheduler().scheduleDelayedTask(new WorldEditorTask(this, ""), 1);
                return;
            }
        }

        if(player != null){
            player.sendMessage("[WorldEditor]모든 블럭을 설정했어요");
        }
        this.debugInfo((player == null ? "" : player.getName() + "님이 ") + "블럭변경을 끝냈어요");
    }

}