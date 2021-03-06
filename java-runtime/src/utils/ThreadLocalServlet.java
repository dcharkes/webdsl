package utils;
public class ThreadLocalServlet {

    private static ThreadLocal<AbstractDispatchServletHelper> dispatchServlet = new ThreadLocal<AbstractDispatchServletHelper>();

    public static AbstractDispatchServletHelper get() {
        return dispatchServlet.get();
    }
    
    public static void set(AbstractDispatchServletHelper d) {
        dispatchServlet.set(d);
    }
    
    public static String getContextPath(){
    	
    	AbstractDispatchServletHelper adsh = get();
    	return (adsh != null) ? adsh.getContextPath() : "";
//        return	get().getContextPath();
    }
    
}

