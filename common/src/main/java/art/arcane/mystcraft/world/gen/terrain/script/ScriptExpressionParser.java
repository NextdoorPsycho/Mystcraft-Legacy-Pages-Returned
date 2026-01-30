package art.arcane.mystcraft.world.gen.terrain.script;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.Locale;

/**
 * Parses JSON expression trees into runtime evaluators.
 */
public final class ScriptExpressionParser {

  private ScriptExpressionParser() {
  }

  public static ScriptExpression parse(JsonElement element) {
    if (element == null || element.isJsonNull()) {
      return new Constant(0.0D);
    }
    if (element.isJsonPrimitive()) {
      return new Constant(element.getAsDouble());
    }
    if (!element.isJsonObject()) {
      return new Constant(0.0D);
    }
    JsonObject obj = element.getAsJsonObject();
    if (obj.has("value")) {
      return new Constant(GsonHelper.getAsDouble(obj, "value"));
    }
    if (obj.has("var")) {
      return new Variable(GsonHelper.getAsString(obj, "var"));
    }
    String op = GsonHelper.getAsString(obj, "op", "").toLowerCase(Locale.ROOT);
    return switch (op) {
      case "add" -> new Binary(obj, (a, b) -> a + b);
      case "sub" -> new Binary(obj, (a, b) -> a - b);
      case "mul" -> new Binary(obj, (a, b) -> a * b);
      case "div" -> new Binary(obj, (a, b) -> b == 0.0D ? 0.0D : a / b);
      case "min" -> new Binary(obj, Math::min);
      case "max" -> new Binary(obj, Math::max);
      case "pow" -> new Binary(obj, Math::pow);
      case "mod" -> new Binary(obj, (a, b) -> b == 0.0D ? 0.0D : a % b);
      case "neg" -> new Unary(obj, a -> -a);
      case "abs" -> new Unary(obj, Math::abs);
      case "sqrt" -> new Unary(obj, a -> a <= 0.0D ? 0.0D : Math.sqrt(a));
      case "floor" -> new Unary(obj, Math::floor);
      case "ceil" -> new Unary(obj, Math::ceil);
      case "fract" -> new Unary(obj, a -> a - Math.floor(a));
      case "sin" -> new Unary(obj, Math::sin);
      case "cos" -> new Unary(obj, Math::cos);
      case "tan" -> new Unary(obj, Math::tan);
      case "clamp" -> new Clamp(obj);
      case "lerp" -> new Lerp(obj);
      case "step" -> new Step(obj);
      case "smoothstep" -> new SmoothStep(obj);
      case "noise2", "perlin2" -> new Noise(obj, false, NoiseMode.SIMPLE);
      case "noise3", "perlin3" -> new Noise(obj, true, NoiseMode.SIMPLE);
      case "fbm2" -> new Noise(obj, false, NoiseMode.FBM);
      case "fbm3" -> new Noise(obj, true, NoiseMode.FBM);
      case "ridged2" -> new Noise(obj, false, NoiseMode.RIDGED);
      case "ridged3" -> new Noise(obj, true, NoiseMode.RIDGED);
      case "cell" -> new CellValue(obj);
      case "cell_distance" -> new CellDistance(obj);
      default -> new Constant(0.0D);
    };
  }

  private static Cube cubeRound(double x, double y, double z) {
    long rx = Math.round(x);
    long ry = Math.round(y);
    long rz = Math.round(z);

    double xDiff = Math.abs(rx - x);
    double yDiff = Math.abs(ry - y);
    double zDiff = Math.abs(rz - z);

    if (xDiff > yDiff && xDiff > zDiff) {
      rx = -ry - rz;
    } else if (yDiff > zDiff) {
      ry = -rx - rz;
    } else {
      rz = -rx - ry;
    }
    return new Cube(rx, ry, rz);
  }

  private static double hashToUnit(long x, long z, long seed) {
    long h = x * 73428767L ^ z * 912931L ^ seed;
    h ^= (h >>> 33);
    h *= 0xff51afd7ed558ccdL;
    h ^= (h >>> 33);
    h *= 0xc4ceb9fe1a85ec53L;
    h ^= (h >>> 33);
    return (h & 0xFFFFFFFFL) / (double) 0x1_0000_0000L;
  }

  private enum NoiseMode {
    SIMPLE, FBM, RIDGED
  }

  private enum CellMode {
    SQUARE,
    HEX;

    static CellMode from(String raw) {
      if (raw == null) return SQUARE;
      return "hex".equalsIgnoreCase(raw) || "hexagon".equalsIgnoreCase(raw) ? HEX : SQUARE;
    }
  }

  private interface Op2 {
    double apply(double a, double b);
  }

  private interface Op1 {
    double apply(double a);
  }

  private record Constant(double value) implements ScriptExpression {
    @Override
    public double eval(ScriptRuntime runtime, double x, double y, double z) {
      return value;
    }
  }

  private static class Variable implements ScriptExpression {
    private final String name;

    private Variable(String name) {
      this.name = name == null ? "" : name;
    }

    @Override
    public double eval(ScriptRuntime runtime, double x, double y, double z) {
      return switch (name) {
        case "x" -> x;
        case "y" -> y;
        case "z" -> z;
        case "seed" -> runtime.getSeed();
        default -> runtime.getParam(name, 0.0D);
      };
    }
  }

  private static class Binary implements ScriptExpression {
    private final ScriptExpression a;
    private final ScriptExpression b;
    private final Op2 op;

    private Binary(JsonObject obj, Op2 op) {
      this.op = op;
      ScriptExpression aExpr = obj.has("a") ? parse(obj.get("a")) : new Constant(0.0D);
      ScriptExpression bExpr = obj.has("b") ? parse(obj.get("b")) : new Constant(0.0D);
      this.a = aExpr;
      this.b = bExpr;
    }

    @Override
    public double eval(ScriptRuntime runtime, double x, double y, double z) {
      return op.apply(a.eval(runtime, x, y, z), b.eval(runtime, x, y, z));
    }
  }

  private static class Unary implements ScriptExpression {
    private final ScriptExpression input;
    private final Op1 op;

    private Unary(JsonObject obj, Op1 op) {
      this.input = obj.has("input") ? parse(obj.get("input")) : new Constant(0.0D);
      this.op = op;
    }

    @Override
    public double eval(ScriptRuntime runtime, double x, double y, double z) {
      return op.apply(input.eval(runtime, x, y, z));
    }
  }

  private static class Clamp implements ScriptExpression {
    private final ScriptExpression value;
    private final ScriptExpression min;
    private final ScriptExpression max;

    private Clamp(JsonObject obj) {
      this.value = obj.has("value") ? parse(obj.get("value")) : new Constant(0.0D);
      this.min = obj.has("min") ? parse(obj.get("min")) : new Constant(0.0D);
      this.max = obj.has("max") ? parse(obj.get("max")) : new Constant(1.0D);
    }

    @Override
    public double eval(ScriptRuntime runtime, double x, double y, double z) {
      double v = value.eval(runtime, x, y, z);
      double lo = min.eval(runtime, x, y, z);
      double hi = max.eval(runtime, x, y, z);
      if (lo > hi) {
        double tmp = lo;
        lo = hi;
        hi = tmp;
      }
      return Math.max(lo, Math.min(hi, v));
    }
  }

  private static class Lerp implements ScriptExpression {
    private final ScriptExpression a;
    private final ScriptExpression b;
    private final ScriptExpression t;

    private Lerp(JsonObject obj) {
      this.a = obj.has("a") ? parse(obj.get("a")) : new Constant(0.0D);
      this.b = obj.has("b") ? parse(obj.get("b")) : new Constant(1.0D);
      this.t = obj.has("t") ? parse(obj.get("t")) : new Constant(0.5D);
    }

    @Override
    public double eval(ScriptRuntime runtime, double x, double y, double z) {
      double av = a.eval(runtime, x, y, z);
      double bv = b.eval(runtime, x, y, z);
      double tv = t.eval(runtime, x, y, z);
      return av + (bv - av) * tv;
    }
  }

  private static class Step implements ScriptExpression {
    private final ScriptExpression edge;
    private final ScriptExpression value;

    private Step(JsonObject obj) {
      this.edge = obj.has("edge") ? parse(obj.get("edge")) : new Constant(0.0D);
      this.value = obj.has("value") ? parse(obj.get("value")) : new Constant(0.0D);
    }

    @Override
    public double eval(ScriptRuntime runtime, double x, double y, double z) {
      double e = edge.eval(runtime, x, y, z);
      double v = value.eval(runtime, x, y, z);
      return v < e ? 0.0D : 1.0D;
    }
  }

  private static class SmoothStep implements ScriptExpression {
    private final ScriptExpression edge0;
    private final ScriptExpression edge1;
    private final ScriptExpression value;

    private SmoothStep(JsonObject obj) {
      this.edge0 = obj.has("edge0") ? parse(obj.get("edge0")) : new Constant(0.0D);
      this.edge1 = obj.has("edge1") ? parse(obj.get("edge1")) : new Constant(1.0D);
      this.value = obj.has("value") ? parse(obj.get("value")) : new Constant(0.0D);
    }

    @Override
    public double eval(ScriptRuntime runtime, double x, double y, double z) {
      double e0 = edge0.eval(runtime, x, y, z);
      double e1 = edge1.eval(runtime, x, y, z);
      double v = value.eval(runtime, x, y, z);
      double t = (v - e0) / (e1 - e0);
      t = Math.max(0.0D, Math.min(1.0D, t));
      return t * t * (3.0D - 2.0D * t);
    }
  }

  private static class Noise implements ScriptExpression {
    private final ScriptExpression xExpr;
    private final ScriptExpression yExpr;
    private final ScriptExpression zExpr;
    private final int octaves;
    private final double scale;
    private final double lacunarity;
    private final double gain;
    private final long seedOffset;
    private final boolean useY;
    private final NoiseMode mode;

    private Noise(JsonObject obj, boolean useY, NoiseMode mode) {
      this.useY = useY;
      this.mode = mode;
      this.xExpr = obj.has("x") ? parse(obj.get("x")) : new Variable("x");
      this.yExpr = obj.has("y") ? parse(obj.get("y")) : new Variable("y");
      this.zExpr = obj.has("z") ? parse(obj.get("z")) : new Variable("z");
      this.octaves = GsonHelper.getAsInt(obj, "octaves", 4);
      this.scale = GsonHelper.getAsDouble(obj, "scale", 0.01D);
      this.lacunarity = GsonHelper.getAsDouble(obj, "lacunarity", 2.0D);
      this.gain = GsonHelper.getAsDouble(obj, "gain", 0.5D);
      this.seedOffset = GsonHelper.getAsLong(obj, "seed", 0L);
    }

    @Override
    public double eval(ScriptRuntime runtime, double x, double y, double z) {
      double vx = xExpr.eval(runtime, x, y, z);
      double vy = yExpr.eval(runtime, x, y, z);
      double vz = zExpr.eval(runtime, x, y, z);
      double value = useY
          ? runtime.noise3(vx, vy, vz, octaves, scale, lacunarity, gain, seedOffset)
          : runtime.noise2(vx, vz, octaves, scale, lacunarity, gain, seedOffset);
      return switch (mode) {
        case SIMPLE -> value;
        case FBM -> value;
        case RIDGED -> 1.0D - Math.abs(value);
      };
    }
  }

  private static class CellValue implements ScriptExpression {
    private final ScriptExpression xExpr;
    private final ScriptExpression zExpr;
    private final double size;
    private final long seed;
    private final double min;
    private final double max;
    private final CellMode mode;

    private CellValue(JsonObject obj) {
      this.xExpr = obj.has("x") ? parse(obj.get("x")) : new Variable("x");
      this.zExpr = obj.has("z") ? parse(obj.get("z")) : new Variable("z");
      this.size = GsonHelper.getAsDouble(obj, "size", 32.0D);
      this.seed = GsonHelper.getAsLong(obj, "seed", 0L);
      this.min = GsonHelper.getAsDouble(obj, "min", 0.0D);
      this.max = GsonHelper.getAsDouble(obj, "max", 1.0D);
      this.mode = CellMode.from(GsonHelper.getAsString(obj, "mode", "square"));
    }

    @Override
    public double eval(ScriptRuntime runtime, double x, double y, double z) {
      double vx = xExpr.eval(runtime, x, y, z);
      double vz = zExpr.eval(runtime, x, y, z);
      CellResult cell = CellResult.from(mode, vx, vz, size);
      double h = hashToUnit(cell.cellX, cell.cellZ, runtime.getSeed() ^ seed);
      return min + (max - min) * h;
    }
  }

  private static class CellDistance implements ScriptExpression {
    private final ScriptExpression xExpr;
    private final ScriptExpression zExpr;
    private final double size;
    private final CellMode mode;

    private CellDistance(JsonObject obj) {
      this.xExpr = obj.has("x") ? parse(obj.get("x")) : new Variable("x");
      this.zExpr = obj.has("z") ? parse(obj.get("z")) : new Variable("z");
      this.size = GsonHelper.getAsDouble(obj, "size", 32.0D);
      this.mode = CellMode.from(GsonHelper.getAsString(obj, "mode", "square"));
    }

    @Override
    public double eval(ScriptRuntime runtime, double x, double y, double z) {
      double vx = xExpr.eval(runtime, x, y, z);
      double vz = zExpr.eval(runtime, x, y, z);
      CellResult cell = CellResult.from(mode, vx, vz, size);
      double dx = vx - cell.centerX;
      double dz = vz - cell.centerZ;
      double dist = Math.sqrt(dx * dx + dz * dz);
      double radius = Math.max(1.0D, size * 0.5D);
      return Math.min(1.0D, dist / radius);
    }
  }

  private record CellResult(long cellX, long cellZ, double centerX, double centerZ) {
    static CellResult from(CellMode mode, double x, double z, double size) {
      if (mode == CellMode.HEX) {
        return hexCell(x, z, size);
      }
      long cx = (long) Math.floor(x / size);
      long cz = (long) Math.floor(z / size);
      double centerX = (cx + 0.5D) * size;
      double centerZ = (cz + 0.5D) * size;
      return new CellResult(cx, cz, centerX, centerZ);
    }

    private static CellResult hexCell(double x, double z, double size) {
      double q = (Math.sqrt(3.0D) / 3.0D * x - 1.0D / 3.0D * z) / size;
      double r = (2.0D / 3.0D * z) / size;
      Cube cube = cubeRound(q, -q - r, r);
      long cx = cube.x;
      long cz = cube.z;
      double centerX = size * (Math.sqrt(3.0D) * (cx + cz / 2.0D));
      double centerZ = size * (3.0D / 2.0D * cz);
      return new CellResult(cx, cz, centerX, centerZ);
    }
  }

  private record Cube(long x, long y, long z) {
  }
}
