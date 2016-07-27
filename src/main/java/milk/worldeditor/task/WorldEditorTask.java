package milk.worldeditor.task;

import milk.worldeditor.WorldEditor;

public class WorldEditorTask implements Runnable{

    private final WorldEditor object;

    private final String method;
    private final Object[] args;

    public WorldEditorTask(WorldEditor object, String method, Object ...args){
        this.object = object;
        this.method = method;
        this.args = args;
    }

    @Override
    public void run(){
        try{
            Class[] classes = new Class[args.length];
            for(int a = 0; a < args.length; a++){
                classes[a] = args[a].getClass();
            }
            object.getClass().getMethod(method, classes).invoke(object, args);
        }catch(Exception e){}
    }

}
