package milk.worldeditor.task;

public class WorldEditorTask implements Runnable{

    private final Object object;

    private final String method;
    private final Object[] args;

    public WorldEditorTask(Object object, String method, Object ...args){
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
            this.object.getClass().getMethod(this.method, classes).invoke(this.object, this.args);
        }catch(Exception e){}
    }

}
