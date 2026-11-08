import cn.routepilot.app.RouteEngine;
public class RouteEngineTest {
    static void near(double a,double b,double eps){if(Math.abs(a-b)>eps)throw new AssertionError(a+" != "+b);}
    static void rejects(double[][] p){try{new RouteEngine(p,true);throw new AssertionError("accepted invalid route");}catch(IllegalArgumentException expected){}}
    public static void main(String[] args){
        RouteEngine r=new RouteEngine(new double[][]{{0,0},{0,0.01}},false);
        near(r.totalLength,1111.9508,0.01);near(r.position(r.totalLength/2)[1],0.005,1e-9);
        near(r.position(1e8)[1],0.01,1e-9);near(r.position(-1)[1],0,1e-9);
        RouteEngine loop=new RouteEngine(new double[][]{{0,0},{0,0.01}},true);
        near(loop.totalLength,r.totalLength*2,1e-6);near(loop.position(loop.totalLength+100)[1],r.position(100)[1],1e-9);
        RouteEngine dup=new RouteEngine(new double[][]{{0,0},{0,0},{0,0.01}},false);
        near(dup.position(100)[1],r.position(100)[1],1e-9);
        RouteEngine dateline=new RouteEngine(new double[][]{{0,179.99},{0,-179.99}},false);
        near(Math.abs(dateline.position(dateline.totalLength/2)[1]),180,1e-9);
        near(dateline.totalLength,2223.9016,0.02);
        rejects(new double[][]{{0,0},{0,0}});rejects(new double[][]{{Double.NaN,0},{0,1}});rejects(new double[][]{{90,0},{0,1}});
        System.out.println("PASS: distances, interpolation, end clamp, closed loop, duplicate points, date line, invalid coordinates");
    }
}
