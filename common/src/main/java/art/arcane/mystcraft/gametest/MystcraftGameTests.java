package art.arcane.mystcraft.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Core Mystcraft GameTests.
 * These tests verify the fundamental functionality of the mod.
 */
public class MystcraftGameTests {

  // ===== REGISTRATION TESTS =====

  @GameTest(template = "empty")
  public void symbols_loaded_400_plus(GameTestHelper helper) {
    MystcraftGameTestAssertions.assertSymbolsLoaded();
    helper.succeed();
  }

  @GameTest(template = "empty")
  public void creative_tabs_loaded(GameTestHelper helper) {
    MystcraftGameTestAssertions.assertCreativeTabsLoaded();
    helper.succeed();
  }

  @GameTest(template = "empty")
  public void datapacks_loaded(GameTestHelper helper) {
    MystcraftGameTestAssertions.assertDatapacksLoaded();
    helper.succeed();
  }

  // ===== BOOK DROP ENTITY TESTS =====

  @GameTest(template = "empty")
  public void books_drop_as_linkbook_entity(GameTestHelper helper) {
    MystcraftGameTestRunner.runBookDropsAsEntityTest(helper);
  }

  @GameTest(template = "empty", timeoutTicks = 40)
  public void books_drop_on_read_config(GameTestHelper helper) {
    MystcraftGameTestRunner.runDropBooksOnReadConfigTest(helper);
  }

  // ===== DIMENSION CREATION TESTS =====

  @GameTest(template = "empty", timeoutTicks = 200)
  public void personal_book_creates_dimension(GameTestHelper helper) {
    MystcraftGameTestRunner.runPersonalBookCreatesAndTeleportsTest(helper);
  }

  @GameTest(template = "empty", timeoutTicks = 300)
  public void random_book_5_symbols_creates_dimension(GameTestHelper helper) {
    MystcraftGameTestRunner.runRandomBookWith5SymbolsTest(helper);
  }

  // ===== DECAY TESTS =====

  @GameTest(template = "empty", timeoutTicks = 200)
  public void linkbook_decays_personal_does_not(GameTestHelper helper) {
    MystcraftGameTestRunner.runLinkbookDecaysButPersonalDoesNotTest(helper);
  }

  // ===== DAMAGE TESTS =====

  @GameTest(template = "empty", timeoutTicks = 40)
  public void book_dies_in_fluid(GameTestHelper helper) {
    MystcraftGameTestRunner.runBookDiesInFluidTest(helper);
  }

  @GameTest(template = "empty", timeoutTicks = 40)
  public void book_dies_at_5_damage(GameTestHelper helper) {
    MystcraftGameTestRunner.runBookDiesAt5DamageTest(helper);
  }

  @GameTest(template = "empty", timeoutTicks = 40)
  public void book_drops_page_and_leather_on_death(GameTestHelper helper) {
    MystcraftGameTestRunner.runBookDropsPageAndLeatherOnDeathTest(helper);
  }

  // ===== LECTERN TESTS =====

  @GameTest(template = "empty", timeoutTicks = 40)
  public void lectern_can_hold_mystcraft_book(GameTestHelper helper) {
    MystcraftGameTestRunner.runLecternBookPlacementTest(helper);
  }

  // ===== TABLE TESTS =====

  @GameTest(template = "empty", timeoutTicks = 40)
  public void table_blocks_have_block_entities(GameTestHelper helper) {
    MystcraftGameTestRunner.runTableBlockEntitiesTest(helper);
  }
}
