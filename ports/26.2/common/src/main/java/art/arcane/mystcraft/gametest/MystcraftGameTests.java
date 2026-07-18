package art.arcane.mystcraft.gametest;

import art.arcane.mystcraft.MystcraftConstants;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunctionLoader;
import net.minecraft.resources.ResourceKey;

/**
 * Common GameTest entry points for environments that can discover shared test classes directly.
 */
public final class MystcraftGameTests extends TestFunctionLoader {
  private static boolean registered;

  public static synchronized void register() {
    if (registered) {
      return;
    }
    TestFunctionLoader.registerLoader(new MystcraftGameTests());
    registered = true;
  }

  @Override
  public void load(
      BiConsumer<ResourceKey<Consumer<GameTestHelper>>, Consumer<GameTestHelper>> registrar) {
    registrar.accept(key("core_content_supports_gameplay"), this::core_content_supports_gameplay);
    registrar.accept(key("book_travel_personal_book_stays_carried"), this::book_travel_personal_book_stays_carried);
    registrar.accept(key("book_travel_normal_linkbook_respects_drop_rules"), this::book_travel_normal_linkbook_respects_drop_rules);
    registrar.accept(key("book_travel_personal_proxy_state_lifecycle"), this::book_travel_personal_proxy_state_lifecycle);
    registrar.accept(key("book_travel_personal_pocket_damage_and_death_return_to_body"), this::book_travel_personal_pocket_damage_and_death_return_to_body);
    registrar.accept(key("book_travel_dropped_books_take_damage_and_drop_parts"), this::book_travel_dropped_books_take_damage_and_drop_parts);
    registrar.accept(key("book_travel_portal_validator_categorizes_book_types"), this::book_travel_portal_validator_categorizes_book_types);
    registrar.accept(key("book_travel_unwritten_agebook_activate_creates_age"), this::book_travel_unwritten_agebook_activate_creates_age);
    registrar.accept(key("book_travel_receptacle_take_on_shift_empty_hand"), this::book_travel_receptacle_take_on_shift_empty_hand);
    registrar.accept(key("book_travel_receptacle_setbook_fires_in_all_orientations"), this::book_travel_receptacle_setbook_fires_in_all_orientations);
    registrar.accept(key("book_travel_linkbook_inserted_directly_teleports"), this::book_travel_linkbook_inserted_directly_teleports);
    registrar.accept(key("book_travel_cooldown_is_per_portal_not_global"), this::book_travel_cooldown_is_per_portal_not_global);
    registrar.accept(key("book_travel_portal_block_has_block_entity"), this::book_travel_portal_block_has_block_entity);
    registrar.accept(key("book_travel_portal_color_distinct_per_book_type"), this::book_travel_portal_color_distinct_per_book_type);
    registrar.accept(key("book_travel_portal_color_uniform_across_all_portal_blocks"), this::book_travel_portal_color_uniform_across_all_portal_blocks);
    registrar.accept(key("book_travel_two_adjacent_portals_each_has_own_color"), this::book_travel_two_adjacent_portals_each_has_own_color);
    registrar.accept(key("book_travel_breaking_receptacle_clears_all_portal_blocks"), this::book_travel_breaking_receptacle_clears_all_portal_blocks);
    registrar.accept(key("book_travel_breaking_crystal_clears_all_portal_blocks"), this::book_travel_breaking_crystal_clears_all_portal_blocks);
    registrar.accept(key("book_crafting_folder_orders_writes_and_extracts"), this::book_crafting_folder_orders_writes_and_extracts);
    registrar.accept(key("book_crafting_portfolio_imports_counts_and_limits"), this::book_crafting_portfolio_imports_counts_and_limits);
    registrar.accept(key("book_crafting_booster_pack_opens_into_pages"), this::book_crafting_booster_pack_opens_into_pages);
    registrar.accept(key("book_crafting_binder_builds_descriptive_book"), this::book_crafting_binder_builds_descriptive_book);
    registrar.accept(key("book_crafting_ink_mixer_builds_link_panel"), this::book_crafting_ink_mixer_builds_link_panel);
    registrar.accept(key("book_crafting_writing_desk_writes_symbols"), this::book_crafting_writing_desk_writes_symbols);
    registrar.accept(key("age_creation_agebook_keeps_pages_author_title_and_flags"), this::age_creation_agebook_keeps_pages_author_title_and_flags);
    registrar.accept(key("age_creation_symbols_build_stable_rules"), this::age_creation_symbols_build_stable_rules);
    registrar.accept(key("age_creation_agebook_with_every_page_creates_dimension"), this::age_creation_agebook_with_every_page_creates_dimension);
    registrar.accept(key("age_creation_multiple_agebooks_create_distinct_dimensions"), this::age_creation_multiple_agebooks_create_distinct_dimensions);
    registrar.accept(key("age_creation_safe_story_death_returns_to_entry"), this::age_creation_safe_story_death_returns_to_entry);
    registrar.accept(key("world_rules_director_state_persists_to_age_data"), this::world_rules_director_state_persists_to_age_data);
    registrar.accept(key("world_rules_table_blocks_create_expected_state"), this::world_rules_table_blocks_create_expected_state);
    registrar.accept(key("commands_player_and_admin_workflows_are_registered"), this::commands_player_and_admin_workflows_are_registered);
    registrar.accept(key("procedural_ui_ink_blend_nbt_round_trip"), this::procedural_ui_ink_blend_nbt_round_trip);
    registrar.accept(key("procedural_ui_book_cover_nbt_round_trip"), this::procedural_ui_book_cover_nbt_round_trip);
    registrar.accept(key("procedural_ui_ink_affinity_biases_symbol_roll"), this::procedural_ui_ink_affinity_biases_symbol_roll);
    registrar.accept(key("procedural_ui_symbol_display_override_applied"), this::procedural_ui_symbol_display_override_applied);
  }

  private static ResourceKey<Consumer<GameTestHelper>> key(String path) {
    return ResourceKey.create(Registries.TEST_FUNCTION, MystcraftConstants.loc(path));
  }

  public void core_content_supports_gameplay(GameTestHelper helper) {
    MystcraftGameTestRunner.runCoreGameplayContentLoaded(helper);
  }

  public void book_travel_personal_book_stays_carried(GameTestHelper helper) {
    MystcraftGameTestRunner.runPersonalBookAlwaysStaysCarriedTest(helper);
  }

  public void book_travel_normal_linkbook_respects_drop_rules(GameTestHelper helper) {
    MystcraftGameTestRunner.runNormalLinkbookDropOnReadFollowsFlagsTest(helper);
  }

  public void book_travel_personal_proxy_state_lifecycle(GameTestHelper helper) {
    MystcraftGameTestRunner.runPersonalProxyStateLifecycleTest(helper);
  }

  public void book_travel_personal_pocket_damage_and_death_return_to_body(GameTestHelper helper) {
    MystcraftGameTestRunner.runPersonalPocketDamageAndDeathReturnTest(helper);
  }

  public void book_travel_dropped_books_take_damage_and_drop_parts(GameTestHelper helper) {
    MystcraftGameTestRunner.runLinkbookEntityDamageAndDropsTest(helper);
  }

  public void book_travel_portal_validator_categorizes_book_types(GameTestHelper helper) {
    MystcraftGameTestRunner.runPortalValidatorCategorizesBookTypesTest(helper);
  }

  public void book_travel_unwritten_agebook_activate_creates_age(GameTestHelper helper) {
    MystcraftGameTestRunner.runUnwrittenAgebookActivateCreatesAgeTest(helper);
  }

  public void book_travel_receptacle_take_on_shift_empty_hand(GameTestHelper helper) {
    MystcraftGameTestRunner.runReceptacleTakeOnShiftEmptyHandTest(helper);
  }

  public void book_travel_receptacle_setbook_fires_in_all_orientations(GameTestHelper helper) {
    MystcraftGameTestRunner.runReceptacleSetBookFiresInAllOrientationsTest(helper);
  }

  public void book_travel_linkbook_inserted_directly_teleports(GameTestHelper helper) {
    MystcraftGameTestRunner.runLinkbookInsertedDirectlyTeleportsTest(helper);
  }

  public void book_travel_cooldown_is_per_portal_not_global(GameTestHelper helper) {
    MystcraftGameTestRunner.runCooldownIsPerPortalNotGlobalTest(helper);
  }

  public void book_travel_portal_block_has_block_entity(GameTestHelper helper) {
    MystcraftGameTestRunner.runPortalBlockHasBlockEntityTest(helper);
  }

  public void book_travel_portal_color_distinct_per_book_type(GameTestHelper helper) {
    MystcraftGameTestRunner.runPortalColorDistinctPerBookTypeTest(helper);
  }

  public void book_travel_portal_color_uniform_across_all_portal_blocks(GameTestHelper helper) {
    MystcraftGameTestRunner.runPortalColorUniformAcrossAllPortalBlocksTest(helper);
  }

  public void book_travel_two_adjacent_portals_each_has_own_color(GameTestHelper helper) {
    MystcraftGameTestRunner.runTwoAdjacentPortalsEachHasOwnColorTest(helper);
  }

  public void book_travel_breaking_receptacle_clears_all_portal_blocks(GameTestHelper helper) {
    MystcraftGameTestRunner.runBreakingReceptacleClearsAllPortalBlocksTest(helper);
  }

  public void book_travel_breaking_crystal_clears_all_portal_blocks(GameTestHelper helper) {
    MystcraftGameTestRunner.runBreakingCrystalClearsAllPortalBlocksTest(helper);
  }

  public void book_crafting_folder_orders_writes_and_extracts(GameTestHelper helper) {
    MystcraftGameTestRunner.runFolderWorkflowTest(helper);
  }

  public void book_crafting_portfolio_imports_counts_and_limits(GameTestHelper helper) {
    MystcraftGameTestRunner.runPortfolioWorkflowTest(helper);
  }

  public void book_crafting_booster_pack_opens_into_pages(GameTestHelper helper) {
    MystcraftGameTestRunner.runBoosterPackPlayerUseTest(helper);
  }

  public void book_crafting_binder_builds_descriptive_book(GameTestHelper helper) {
    MystcraftGameTestRunner.runBookBinderWorkflowTest(helper);
  }

  public void book_crafting_ink_mixer_builds_link_panel(GameTestHelper helper) {
    MystcraftGameTestRunner.runInkMixerWorkflowTest(helper);
  }

  public void book_crafting_writing_desk_writes_symbols(GameTestHelper helper) {
    MystcraftGameTestRunner.runWritingDeskWorkflowTest(helper);
  }

  public void age_creation_agebook_keeps_pages_author_title_and_flags(GameTestHelper helper) {
    MystcraftGameTestRunner.runAgebookCreationDataWorkflowTest(helper);
  }

  public void age_creation_symbols_build_stable_rules(GameTestHelper helper) {
    MystcraftGameTestRunner.runAgeBuilderStableRulesTest(helper);
  }

  public void age_creation_agebook_with_every_page_creates_dimension(GameTestHelper helper) {
    MystcraftGameTestRunner.runAgebookWithEveryPageCreatesDimensionTest(helper);
  }

  public void age_creation_multiple_agebooks_create_distinct_dimensions(GameTestHelper helper) {
    MystcraftGameTestRunner.runMultipleAgebooksCreateDistinctAgesTest(helper);
  }

  public void age_creation_safe_story_death_returns_to_entry(GameTestHelper helper) {
    MystcraftGameTestRunner.runAgeDeathReturnUsesStoredEntryTest(helper);
  }

  public void world_rules_director_state_persists_to_age_data(GameTestHelper helper) {
    MystcraftGameTestRunner.runWorldRulesPersistToAgeDataTest(helper);
  }

  public void world_rules_table_blocks_create_expected_state(GameTestHelper helper) {
    MystcraftGameTestRunner.runTableBlocksCreateExpectedStateTest(helper);
  }

  public void commands_player_and_admin_workflows_are_registered(GameTestHelper helper) {
    MystcraftGameTestRunner.runCommandWorkflowTest(helper);
  }

  public void procedural_ui_ink_blend_nbt_round_trip(GameTestHelper helper) {
    MystcraftGameTestRunner.runInkBlendNbtRoundTripTest(helper);
  }

  public void procedural_ui_book_cover_nbt_round_trip(GameTestHelper helper) {
    MystcraftGameTestRunner.runBookCoverNbtRoundTripTest(helper);
  }

  public void procedural_ui_ink_affinity_biases_symbol_roll(GameTestHelper helper) {
    MystcraftGameTestRunner.runInkAffinityBiasesSymbolRollTest(helper);
  }

  public void procedural_ui_symbol_display_override_applied(GameTestHelper helper) {
    MystcraftGameTestRunner.runSymbolDisplayOverrideAppliedTest(helper);
  }
}
