package art.arcane.mystcraft.fabric;

import art.arcane.mystcraft.Mystcraft;
import art.arcane.mystcraft.client.render.BookItemRendererBEWLR;
import art.arcane.mystcraft.client.render.PageItemRendererBEWLR;
import art.arcane.mystcraft.fabric.mixin.SpecialModelRenderersAccessor;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;

/** Registers the stable data-driven model types used by Mystcraft item definitions. */
final class FabricSpecialModelRenderers {

  static final Identifier PROCEDURAL_PAGE = id("procedural_page");
  static final Identifier PROCEDURAL_BOOK = id("procedural_book");

  private static boolean registered;

  private FabricSpecialModelRenderers() {
  }

  static synchronized void register() {
    if (registered) {
      return;
    }

    SpecialModelRenderersAccessor.mystcraft$getIdMapper()
        .put(PROCEDURAL_PAGE, PageUnbaked.MAP_CODEC)
        .put(PROCEDURAL_BOOK, BookUnbaked.MAP_CODEC);
    registered = true;
  }

  private static Identifier id(String path) {
    return Identifier.fromNamespaceAndPath(Mystcraft.MOD_ID, path);
  }

  private static final class PageUnbaked
      implements SpecialModelRenderer.Unbaked<PageItemRendererBEWLR.RenderData> {
    private static final MapCodec<PageUnbaked> MAP_CODEC = MapCodec.unit(PageUnbaked::new);

    @Override
    public SpecialModelRenderer<PageItemRendererBEWLR.RenderData> bake(
        SpecialModelRenderer.BakingContext context) {
      return PageItemRendererBEWLR.getInstance();
    }

    @Override
    public MapCodec<? extends SpecialModelRenderer.Unbaked<PageItemRendererBEWLR.RenderData>> type() {
      return MAP_CODEC;
    }
  }

  private static final class BookUnbaked
      implements SpecialModelRenderer.Unbaked<BookItemRendererBEWLR.RenderData> {
    private static final MapCodec<BookUnbaked> MAP_CODEC = MapCodec.unit(BookUnbaked::new);

    @Override
    public SpecialModelRenderer<BookItemRendererBEWLR.RenderData> bake(
        SpecialModelRenderer.BakingContext context) {
      return BookItemRendererBEWLR.getInstance();
    }

    @Override
    public MapCodec<? extends SpecialModelRenderer.Unbaked<BookItemRendererBEWLR.RenderData>> type() {
      return MAP_CODEC;
    }
  }
}
