package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.Mystcraft;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(value = Mystcraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class MystcraftForgeGameTests {

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.CORE)
  public void core_content_supports_gameplay(GameTestHelper helper) {
    MystcraftGameTestRunner.runCoreGameplayContentLoaded(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.BOOK_TRAVEL, timeoutTicks = 40)
  public void book_travel_personal_book_stays_carried(GameTestHelper helper) {
    MystcraftGameTestRunner.runPersonalBookAlwaysStaysCarriedTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.BOOK_TRAVEL, timeoutTicks = 40)
  public void book_travel_normal_linkbook_respects_drop_rules(GameTestHelper helper) {
    MystcraftGameTestRunner.runNormalLinkbookDropOnReadFollowsFlagsTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.BOOK_TRAVEL, timeoutTicks = 40)
  public void book_travel_personal_proxy_state_lifecycle(GameTestHelper helper) {
    MystcraftGameTestRunner.runPersonalProxyStateLifecycleTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.BOOK_TRAVEL, timeoutTicks = 120)
  public void book_travel_personal_pocket_damage_and_death_return_to_body(GameTestHelper helper) {
    MystcraftGameTestRunner.runPersonalPocketDamageAndDeathReturnTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.BOOK_TRAVEL, timeoutTicks = 40)
  public void book_travel_dropped_books_take_damage_and_drop_parts(GameTestHelper helper) {
    MystcraftGameTestRunner.runLinkbookEntityDamageAndDropsTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.BOOK_CRAFTING, timeoutTicks = 40)
  public void book_crafting_folder_orders_writes_and_extracts(GameTestHelper helper) {
    MystcraftGameTestRunner.runFolderWorkflowTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.BOOK_CRAFTING, timeoutTicks = 40)
  public void book_crafting_portfolio_imports_counts_and_limits(GameTestHelper helper) {
    MystcraftGameTestRunner.runPortfolioWorkflowTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.BOOK_CRAFTING, timeoutTicks = 40)
  public void book_crafting_booster_pack_opens_into_pages(GameTestHelper helper) {
    MystcraftGameTestRunner.runBoosterPackPlayerUseTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.BOOK_CRAFTING, timeoutTicks = 40)
  public void book_crafting_binder_builds_descriptive_book(GameTestHelper helper) {
    MystcraftGameTestRunner.runBookBinderWorkflowTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.BOOK_CRAFTING, timeoutTicks = 40)
  public void book_crafting_ink_mixer_builds_link_panel(GameTestHelper helper) {
    MystcraftGameTestRunner.runInkMixerWorkflowTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.BOOK_CRAFTING, timeoutTicks = 60)
  public void book_crafting_writing_desk_writes_symbols(GameTestHelper helper) {
    MystcraftGameTestRunner.runWritingDeskWorkflowTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.AGE_CREATION, timeoutTicks = 40)
  public void age_creation_agebook_keeps_pages_author_title_and_flags(GameTestHelper helper) {
    MystcraftGameTestRunner.runAgebookCreationDataWorkflowTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.AGE_CREATION, timeoutTicks = 40)
  public void age_creation_symbols_build_stable_rules(GameTestHelper helper) {
    MystcraftGameTestRunner.runAgeBuilderStableRulesTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.AGE_CREATION, timeoutTicks = 300)
  public void age_creation_agebook_with_every_page_creates_dimension(GameTestHelper helper) {
    MystcraftGameTestRunner.runAgebookWithEveryPageCreatesDimensionTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.AGE_CREATION, timeoutTicks = 180)
  public void age_creation_multiple_agebooks_create_distinct_dimensions(GameTestHelper helper) {
    MystcraftGameTestRunner.runMultipleAgebooksCreateDistinctAgesTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.AGE_CREATION, timeoutTicks = 120)
  public void age_creation_safe_story_death_returns_to_entry(GameTestHelper helper) {
    MystcraftGameTestRunner.runAgeDeathReturnUsesStoredEntryTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.WORLD_RULES, timeoutTicks = 40)
  public void world_rules_director_state_persists_to_age_data(GameTestHelper helper) {
    MystcraftGameTestRunner.runWorldRulesPersistToAgeDataTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.WORLD_RULES, timeoutTicks = 60)
  public void world_rules_table_blocks_create_expected_state(GameTestHelper helper) {
    MystcraftGameTestRunner.runTableBlocksCreateExpectedStateTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.COMMANDS, timeoutTicks = 80)
  public void commands_player_and_admin_workflows_are_registered(GameTestHelper helper) {
    MystcraftGameTestRunner.runCommandWorkflowTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.PROCEDURAL_UI, timeoutTicks = 40)
  public void procedural_ui_ink_blend_nbt_round_trip(GameTestHelper helper) {
    MystcraftGameTestRunner.runInkBlendNbtRoundTripTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.PROCEDURAL_UI, timeoutTicks = 40)
  public void procedural_ui_book_cover_nbt_round_trip(GameTestHelper helper) {
    MystcraftGameTestRunner.runBookCoverNbtRoundTripTest(helper);
  }

  @GameTest(template = "empty", batch = MystcraftGameTestSuites.PROCEDURAL_UI, timeoutTicks = 80)
  public void procedural_ui_ink_affinity_biases_symbol_roll(GameTestHelper helper) {
    MystcraftGameTestRunner.runInkAffinityBiasesSymbolRollTest(helper);
  }
}
