package art.arcane.mystcraft.world.gen.terrain.script;

/**
 * Expression for scripted terrain density evaluation.
 */
public interface ScriptExpression {
  double eval(ScriptRuntime runtime, double x, double y, double z);
}
