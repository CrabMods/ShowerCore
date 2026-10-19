package mod.crabmod.showercore.pool;

import java.util.*;

/** Immutable shared geometry. Polygons use X/Z coordinates and positive signed area. */
public final class PoolLayout {
    public record Point(double x, double z) {
        public Point lerp(Point b, double t) { return new Point(x + (b.x - x) * t, z + (b.z - z) * t); }
        public double distance(Point b) { return Math.hypot(x - b.x, z - b.z); }
    }
    public record Edge(Point a, Point b) {}
    public record RimPoint(Point point,double normalX,double normalZ) {}
    public record Cell(int x, int z, List<Point> floor, List<Point> water,
                       List<List<Point>> rim, List<Edge> outerWalls, List<Edge> innerWalls) {
        public boolean hasRim() { return !rim.isEmpty(); }
    }
    public enum SeatKind { INSIDE, RIM }
    public record Seat(int index, double x, double z, double y, float yaw, SeatKind kind) {}

    private static final Map<PoolSpec, PoolLayout> CACHE = new LinkedHashMap<>(16, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<PoolSpec, PoolLayout> entry) { return size() > 32; }
    };
    public static synchronized PoolLayout of(PoolSpec spec) { return CACHE.computeIfAbsent(spec, PoolLayout::new); }
    private final PoolSpec spec;
    private final List<Point> outer, water;
    private final List<Cell> cells;
    private final Map<Integer, Cell> indexed;
    private final List<Seat> seats;
    private final int materialCost, capacity;

    private PoolLayout(PoolSpec spec) {
        this.spec = spec;
        outer = outline(spec, 0);
        water = outline(spec, PoolSpec.RIM_WIDTH);
        var parts = new ArrayList<Cell>();
        var byIndex = new HashMap<Integer, Cell>();
        int walls = 0;
        for (int z = 0; z < spec.length(); z++) for (int x = 0; x < spec.width(); x++) {
            var floor = clip(outer, x, z, x + 1, z + 1);
            if (area(floor) < 1e-8) continue;
            var surface = clip(water, x, z, x + 1, z + 1);
            var rim = new ArrayList<List<Point>>();
            var outside = new ArrayList<Edge>(); var inside = new ArrayList<Edge>();
            for (int i = 0; i < outer.size(); i++) {
                int next = (i + 1) % outer.size();
                var polygon = clip(List.of(outer.get(i), outer.get(next), water.get(next), water.get(i)), x, z, x + 1, z + 1);
                if (area(polygon) > 1e-8) rim.add(polygon);
                Edge edge = clipEdge(outer.get(i), outer.get(next), x, z, x + 1, z + 1);
                if (edge != null) outside.add(edge);
                edge = clipEdge(water.get(i), water.get(next), x, z, x + 1, z + 1);
                if (edge != null) inside.add(edge);
            }
            var cell = new Cell(x, z, floor, surface, List.copyOf(rim), List.copyOf(outside), List.copyOf(inside));
            parts.add(cell); byIndex.put(z * spec.width() + x, cell);
            if (cell.hasRim()) walls++;
        }
        cells = List.copyOf(parts); indexed = Map.copyOf(byIndex);
        materialCost = cells.size() + walls;
        capacity = Math.max(1000, (int)Math.ceil(area(water) * (PoolSpec.FULL_WATER_Y - PoolSpec.FLOOR_Y) - 1e-8) * 1000);
        seats = makeSeats();
    }

    public PoolSpec spec() { return spec; }
    public List<Point> outline() { return outer; }
    public List<Point> waterOutline() { return water; }
    public List<Cell> cells() { return cells; }
    public Cell cell(int x, int z) {
        if (x < 0 || z < 0 || x >= spec.width() || z >= spec.length()) return null;
        return indexed.get(z * spec.width() + x);
    }
    public List<Seat> seats() { return seats; }
    public boolean containsWater(double x,double z) {
        for(int i=0;i<water.size();i++) {
            var a=water.get(i);var b=water.get((i+1)%water.size());
            if((b.x-a.x)*(z-a.z)-(b.z-a.z)*(x-a.x)<0)return false;
        }
        return true;
    }
    public int materialCost() { return materialCost; }
    public int capacity() { return capacity; }
    public int controllerX() { return spec.width() / 2; }
    public Point faucet() { return new Point(spec.width() / 2.0, .125); }
    public double waterY(int amount) {
        if (amount < 0 || amount > capacity) throw new IllegalArgumentException("Invalid pool volume");
        return PoolSpec.FLOOR_Y + (PoolSpec.FULL_WATER_Y - PoolSpec.FLOOR_Y) * amount / capacity;
    }

    public static List<Point> outline(PoolSpec spec, double inset) {
        double left = inset, top = inset, right = spec.width() - inset, bottom = spec.length() - inset;
        double r = Math.max(0, spec.radius() - inset);
        if (spec.shape() == PoolSpec.Shape.RECTANGLE) return List.of(new Point(left, top), new Point(right, top), new Point(right, bottom), new Point(left, bottom));
        var points = new ArrayList<Point>(68);
        double[][] centers = {{right-r, top+r}, {right-r, bottom-r}, {left+r, bottom-r}, {left+r, top+r}};
        for (int corner = 0; corner < 4; corner++) for (int step = 0; step <= 16; step++) {
            double a = (-90 + corner * 90 + step * 90.0 / 16) * Math.PI / 180;
            points.add(new Point(centers[corner][0] + r * Math.cos(a), centers[corner][1] + r * Math.sin(a)));
        }
        return List.copyOf(points);
    }

    public static double area(List<Point> points) {
        double area = 0;
        for (int i = 0; i < points.size(); i++) {
            Point a = points.get(i), b = points.get((i+1)%points.size()); area += a.x*b.z - b.x*a.z;
        }
        return Math.abs(area) / 2;
    }

    public static List<Point> clip(List<Point> polygon, double x1, double z1, double x2, double z2) {
        List<Point> result = polygon;
        double[] bounds = {x1, x2, z1, z2};
        for (int side = 0; side < 4 && !result.isEmpty(); side++) {
            var input = result; var output = new ArrayList<Point>();
            Point previous = input.get(input.size()-1);
            double pv = side < 2 ? previous.x : previous.z;
            boolean pi = side%2==0 ? pv >= bounds[side]-1e-10 : pv <= bounds[side]+1e-10;
            for (Point current : input) {
                double cv = side < 2 ? current.x : current.z;
                boolean ci = side%2==0 ? cv >= bounds[side]-1e-10 : cv <= bounds[side]+1e-10;
                if (ci != pi) output.add(previous.lerp(current, (bounds[side]-pv)/(cv-pv)));
                if (ci) output.add(current);
                previous=current; pv=cv; pi=ci;
            }
            result=output;
        }
        return List.copyOf(result);
    }

    private static Edge clipEdge(Point a, Point b, double x1, double z1, double x2, double z2) {
        double lo=0, hi=1;
        double[] p={-(b.x-a.x), b.x-a.x, -(b.z-a.z), b.z-a.z};
        double[] q={a.x-x1, x2-a.x, a.z-z1, z2-a.z};
        for(int i=0;i<4;i++) {
            if(Math.abs(p[i])<1e-12) { if(q[i]<-1e-10)return null; }
            else { double t=q[i]/p[i]; if(p[i]<0)lo=Math.max(lo,t);else hi=Math.min(hi,t); }
        }
        if(hi-lo<1e-9)return null;
        Point from=a.lerp(b,lo), to=a.lerp(b,hi);
        return from.distance(to)<1e-8 ? null : new Edge(from,to);
    }

    public Point clampToInterior(double x, double z, double inset) {
        List<Point> boundary=outline(spec,inset); Point p=new Point(x,z);
        if(spec.contains(x,z,inset))return p;
        Point closest=null;double distance=Double.POSITIVE_INFINITY;
        for(int i=0;i<boundary.size();i++) {
            Point a=boundary.get(i),b=boundary.get((i+1)%boundary.size());
            double dx=b.x-a.x,dz=b.z-a.z,den=dx*dx+dz*dz;
            double t=den==0?0:Math.max(0,Math.min(1,((x-a.x)*dx+(z-a.z)*dz)/den));
            Point candidate=a.lerp(b,t);double d=candidate.distance(p);
            if(d<distance){distance=d;closest=candidate;}
        }
        return Objects.requireNonNull(closest);
    }

    public RimPoint closestRim(double x,double z) {
        Point p=new Point(x,z);RimPoint closest=null;double distance=Double.POSITIVE_INFINITY;
        for(int i=0;i<outer.size();i++) {
            Point a=outer.get(i),b=outer.get((i+1)%outer.size());double dx=b.x-a.x,dz=b.z-a.z,den=dx*dx+dz*dz;
            if(den<1e-12)continue;
            Point q=a.lerp(b,Math.max(0,Math.min(1,((x-a.x)*dx+(z-a.z)*dz)/den)));
            double d=q.distance(p);
            if(d<distance){distance=d;double length=Math.sqrt(den);closest=new RimPoint(q,dz/length,-dx/length);}
        }
        return Objects.requireNonNull(closest);
    }

    private List<Seat> makeSeats() {
        var result=new ArrayList<Seat>();
        var boundary=outline(spec,PoolSpec.RIM_WIDTH/2);
        for(int i=0;i<boundary.size();i++) {
            Point a=boundary.get(i),b=boundary.get((i+1)%boundary.size());
            int steps=Math.max(1,(int)Math.ceil(a.distance(b)/.2));
            for(int j=0;j<steps;j++) {
                Point p=a.lerp(b,j/(double)steps);
                if(p.distance(faucet())<.8 || result.stream().anyMatch(s->Math.hypot(s.x-p.x,s.z-p.z)<1.0))continue;
                addSeat(result,p,PoolSpec.RIM_Y,SeatKind.RIM);
            }
        }
        for(double z=.75;z<spec.length()-.5;z+=1.1)for(double x=.75;x<spec.width()-.5;x+=1.1) {
            Point p=new Point(x,z);
            if(!spec.contains(x,z,.6) || result.stream().anyMatch(s->Math.hypot(s.x-p.x,s.z-p.z)<.85))continue;
            addSeat(result,p,PoolSpec.FLOOR_Y,SeatKind.INSIDE);
        }
        return List.copyOf(result);
    }

    private void addSeat(List<Seat> seats, Point p, double y, SeatKind kind) {
        float yaw=(float)Math.toDegrees(Math.atan2(-(spec.width()/2.0-p.x),spec.length()/2.0-p.z));
        seats.add(new Seat(seats.size(),p.x,p.z,y,yaw,kind));
    }
}
