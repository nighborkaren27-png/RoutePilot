package cn.routepilot.app;

/** Pure Java route interpolation. Coordinates are WGS84, distances are meters. */
public final class RouteEngine {
    private final double[][] points;
    private final double[] lengths;
    public final double totalLength;
    public final boolean loop;
    public RouteEngine(double[][] source, boolean loop) {
        if (source == null || source.length < 2 || source.length > 10000) throw new IllegalArgumentException("路线需要 2–10000 个点");
        this.loop = loop;
        points = new double[source.length][2];
        for (int i=0;i<source.length;i++) {
            if (source[i].length<2 || !Double.isFinite(source[i][0]) || !Double.isFinite(source[i][1]) || Math.abs(source[i][0])>85 || Math.abs(source[i][1])>180) throw new IllegalArgumentException("坐标范围无效");
            points[i][0]=source[i][0]; points[i][1]=source[i][1];
        }
        lengths=new double[loop ? points.length : points.length-1];
        double sum=0;
        for(int i=0;i<lengths.length;i++){ lengths[i]=distance(points[i],points[(i+1)%points.length]); sum+=lengths[i]; }
        if(sum<1)throw new IllegalArgumentException("路线总长至少需要 1 米");
        totalLength=sum;
    }
    public static double distance(double[] a,double[] b){
        double p=Math.toRadians(a[0]),q=Math.toRadians(b[0]);
        double x=Math.sin((q-p)/2),y=Math.sin(Math.toRadians(b[1]-a[1])/2);
        return 6371008.8*2*Math.asin(Math.min(1,Math.sqrt(x*x+Math.cos(p)*Math.cos(q)*y*y)));
    }
    public double[] position(double meters){
        if(!Double.isFinite(meters))throw new IllegalArgumentException("距离无效");
        double d=loop ? Math.max(0,meters)%totalLength : Math.min(Math.max(0,meters),totalLength);
        int i=0;
        while(i<lengths.length-1 && (lengths[i]<0.000001 || d>lengths[i]))d-=lengths[i++];
        double t=lengths[i]<0.000001 ? 0 : Math.min(1,d/lengths[i]);
        double[] a=points[i],b=points[(i+1)%points.length];
        double delta=b[1]-a[1]; if(delta>180)delta-=360; if(delta< -180)delta+=360;
        double lng=a[1]+delta*t; if(lng>180)lng-=360; if(lng< -180)lng+=360;
        double bearing=(Math.toDegrees(Math.atan2(Math.sin(Math.toRadians(delta))*Math.cos(Math.toRadians(b[0])),Math.cos(Math.toRadians(a[0]))*Math.sin(Math.toRadians(b[0]))-Math.sin(Math.toRadians(a[0]))*Math.cos(Math.toRadians(b[0]))*Math.cos(Math.toRadians(delta))))+360)%360;
        return new double[]{a[0]+(b[0]-a[0])*t,lng,bearing};
    }
}
