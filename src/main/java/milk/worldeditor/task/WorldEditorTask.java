package milk.worldeditor.task;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.item.Item;
import milk.worldeditor.WorldEditor;

public class WorldEditorTask implements Runnable{

    private final String method;
    private final Object[] args;

    public WorldEditorTask(String method, Object ...args){
        this.method = method;
        this.args = args;
    }

    @Override
    public void run(){
        try{
            Class[] classes = new Class[args.length];
            for(int a = 0; a < args.length; a++){
                Class clazz = args[a].getClass();
                if(args[a] instanceof Block){
                    clazz = Block.class;
                }else if(args[a] instanceof Item){
                    clazz = Item.class;
                }else if(args[a] instanceof Player){
                    clazz = Player.class;
                }
                classes[a] = clazz;
            }
            WorldEditor.getInstance().getClass().getMethod(method, classes).invoke(WorldEditor.getInstance(), args);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
