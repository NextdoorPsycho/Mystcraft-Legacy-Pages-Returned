package art.arcane.mystcraft.instability;

import art.arcane.mystcraft.api.instability.IInstabilityProvider;
import art.arcane.mystcraft.instability.effects.*;
import art.arcane.mystcraft.instability.providers.*;
import net.minecraft.world.effect.MobEffects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Initializes all default instability providers and configurations.
 */
public final class InstabilityData {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstabilityData.class);

    /** Deck base costs - instability required to access each deck tier */
    public static final class DeckCost {
        public static final int BASIC = 0;
        public static final int HARSH = 10;
        public static final int DESTRUCTIVE = 30;
        public static final int EATING = 50;
        public static final int DEATH = 75;
    }

    /** Provider activation costs - instability required for each effect */
    public static final class Stability {
        // Potion effects: high cost so only 1-2 types are active per Age at low instability.
        // At instability 20 in basic deck: budget draws ~2 potion cards.
        // At instability 50: ~5 cards. The deck shuffle gives each Age a unique set.
        public static final int BLINDNESS = 12;
        public static final int BLINDNESS_GLOBAL = 20;
        public static final int FATIGUE = 10;
        public static final int FATIGUE_GLOBAL = 20;
        public static final int NAUSEA = 12;
        public static final int NAUSEA_GLOBAL = 20;

        public static final int HUNGER = 10;
        public static final int HUNGER_GLOBAL = 20;
        public static final int SLOW = 10;
        public static final int SLOW_GLOBAL = 20;
        public static final int WEAKNESS = 10;
        public static final int WEAKNESS_GLOBAL = 20;
        public static final int POISON = 10;
        public static final int POISON_GLOBAL = 20;
        public static final int WITHER = 15;
        public static final int WITHER_GLOBAL = 25;

        // Enemy buffs
        public static final int ENEMY_REGEN_GLOBAL = 10;
        public static final int ENEMY_RESIST_GLOBAL = 10;

        // Environmental/destructive effects: staged 40 -> 80
        public static final int DECAY_BLUE = 40;
        public static final int DECAY_PURPLE = 40;
        public static final int DECAY_RED = 40;
        public static final int CRUMBLE = 45;
        public static final int EROSION = 50;
        public static final int EXPLOSIONS = 55;
        public static final int DECAY_WHITE = 60;
        public static final int DECAY_BLACK = 60;
        public static final int CRUMBLE_BEDROCK = 65;
        public static final int EXTRA_TICKS = 65;
        public static final int BURNING = 70;
        public static final int BURNING_GLOBAL = 80;
        public static final int LIGHTNING = 70;
        public static final int METEORS = 70;
    }

    /** Instability costs for missing required symbols */
    public static final class Missing {
        public static final int CONTROLLER = 0;
    }

    /** Instability costs for extra (duplicate) symbols */
    public static final class Extra {
        public static final int CONTROLLER = 500;
    }

    /** Symbol-specific instability modifiers */
    public static final class Symbol {
        public static final int ACCELERATED = 1000;
        public static final int BRIGHT = 500;
        public static final int CHARGED = -500;      // Bonus (reduces instability)
        public static final int METEORS = -1000;     // Using meteor symbol means intentional
        public static final int EXPLOSION = -500;
        public static final int SCORCHED = -500;
        public static final int DUMMY_FEATURE_LARGE = 0;
        public static final int DUMMY_FEATURE_MEDIUM = 1000;
        public static final int DUMMY_FEATURE_SMALL = 2000;
    }

    /** Percentage of world that can be cleared by instability */
    public static final float CLEAR_PERCENTAGE = 0.20f;

    private InstabilityData() {}

    /**
     * Helper class for registering providers with deck placements.
     */
    private static class ProviderContainer {
        private final String identifier;
        private final boolean registered;

        private ProviderContainer(String identifier, IInstabilityProvider provider, int activationCost) {
            this.identifier = identifier;
            this.registered = InstabilityProviderRegistry.registerProvider(identifier, provider, activationCost);
        }

        public ProviderContainer add(String deck, int count) {
            if (registered) {
                InstabilityProviderRegistry.addCards(deck, identifier, count);
            }
            return this;
        }

        public static ProviderContainer create(String identifier, IInstabilityProvider provider, int activationCost) {
            return new ProviderContainer(identifier, provider, activationCost);
        }
    }

    /**
     * Initializes all default instability data.
     * Call this during mod initialization.
     */
    public static void initialize() {
        LOGGER.info("Initializing Mystcraft instability data");

        // Register deck costs
        InstabilityProviderRegistry.setDeckCost("basic", DeckCost.BASIC);
        InstabilityProviderRegistry.setDeckCost("harsh", DeckCost.HARSH);
        InstabilityProviderRegistry.setDeckCost("destructive", DeckCost.DESTRUCTIVE);
        InstabilityProviderRegistry.setDeckCost("eating", DeckCost.EATING);
        InstabilityProviderRegistry.setDeckCost("death", DeckCost.DEATH);

        // Register potion effect providers.
        // ALL potions live in the "eating" deck (cost 50) so they never appear
        // below instability 50. Each type has 1 card. The deck shuffle gives each
        // Age a unique characteristic set (1-2 potions at 50, more as instability rises).
        // Global variants are death-tier only.

        // Blindness
        ProviderContainer.create("blindness",
                new InstabilityProvider(true, EffectPotion.class, false, MobEffects.BLINDNESS, 60),
                Stability.BLINDNESS).add("eating", 1);
        ProviderContainer.create("blindness,g",
                new InstabilityProvider(true, EffectPotion.class, true, MobEffects.BLINDNESS, 60),
                Stability.BLINDNESS_GLOBAL).add("death", 1);

        // Fatigue
        ProviderContainer.create("fatigue",
                new InstabilityProvider(true, EffectPotion.class, false, MobEffects.DIG_SLOWDOWN, 80),
                Stability.FATIGUE).add("eating", 1);
        ProviderContainer.create("fatigue,g",
                new InstabilityProvider(true, EffectPotion.class, true, MobEffects.DIG_SLOWDOWN, 80),
                Stability.FATIGUE_GLOBAL).add("death", 1);

        // Hunger
        ProviderContainer.create("hunger",
                new InstabilityProvider(true, EffectPotion.class, false, MobEffects.HUNGER, 80),
                Stability.HUNGER).add("eating", 1);
        ProviderContainer.create("hunger,g",
                new InstabilityProvider(true, EffectPotion.class, true, MobEffects.HUNGER, 80),
                Stability.HUNGER_GLOBAL).add("death", 1);

        // Nausea
        ProviderContainer.create("nausea",
                new InstabilityProvider(true, EffectPotion.class, false, MobEffects.CONFUSION, 60),
                Stability.NAUSEA).add("eating", 1);
        ProviderContainer.create("nausea,g",
                new InstabilityProvider(true, EffectPotion.class, true, MobEffects.CONFUSION, 60),
                Stability.NAUSEA_GLOBAL).add("death", 1);

        // Poison
        ProviderContainer.create("poison",
                new InstabilityProvider(true, EffectPotion.class, false, MobEffects.POISON, 80),
                Stability.POISON).add("eating", 1);
        ProviderContainer.create("poison,g",
                new InstabilityProvider(true, EffectPotion.class, true, MobEffects.POISON, 80),
                Stability.POISON_GLOBAL).add("death", 1);

        // Slowness
        ProviderContainer.create("slow",
                new InstabilityProvider(true, EffectPotion.class, false, MobEffects.MOVEMENT_SLOWDOWN, 80),
                Stability.SLOW).add("eating", 1);
        ProviderContainer.create("slow,g",
                new InstabilityProvider(true, EffectPotion.class, true, MobEffects.MOVEMENT_SLOWDOWN, 80),
                Stability.SLOW_GLOBAL).add("death", 1);

        // Weakness
        ProviderContainer.create("weakness",
                new InstabilityProvider(true, EffectPotion.class, false, MobEffects.WEAKNESS, 80),
                Stability.WEAKNESS).add("eating", 1);
        ProviderContainer.create("weakness,g",
                new InstabilityProvider(true, EffectPotion.class, true, MobEffects.WEAKNESS, 80),
                Stability.WEAKNESS_GLOBAL).add("death", 1);

        // Wither
        ProviderContainer.create("wither",
                new InstabilityProvider(true, EffectPotion.class, false, MobEffects.WITHER, 30),
                Stability.WITHER).add("eating", 1);
        ProviderContainer.create("wither,g",
                new InstabilityProvider(true, EffectPotion.class, true, MobEffects.WITHER, 30),
                Stability.WITHER_GLOBAL).add("death", 1);

        // Positive effects (chaotic boons -- also eating tier, shuffled in with negatives)
        ProviderContainer.create("speed",
                new InstabilityProvider(true, EffectPotion.class, false, MobEffects.MOVEMENT_SPEED, 600),
                Stability.HUNGER).add("eating", 1);
        ProviderContainer.create("haste",
                new InstabilityProvider(true, EffectPotion.class, false, MobEffects.DIG_SPEED, 600),
                Stability.FATIGUE).add("eating", 1);
        ProviderContainer.create("strength",
                new InstabilityProvider(true, EffectPotion.class, false, MobEffects.DAMAGE_BOOST, 400),
                Stability.WEAKNESS).add("eating", 1);
        ProviderContainer.create("jumpboost",
                new InstabilityProvider(true, EffectPotion.class, false, MobEffects.JUMP, 600),
                Stability.SLOW).add("eating", 1);
        ProviderContainer.create("regen",
                new InstabilityProvider(true, EffectPotion.class, false, MobEffects.REGENERATION, 200),
                Stability.POISON).add("eating", 1);
        ProviderContainer.create("resistance",
                new InstabilityProvider(true, EffectPotion.class, false, MobEffects.DAMAGE_RESISTANCE, 300),
                Stability.WEAKNESS).add("eating", 1);
        ProviderContainer.create("nightvision",
                new InstabilityProvider(true, EffectPotion.class, false, MobEffects.NIGHT_VISION, 1200),
                Stability.BLINDNESS).add("eating", 1);

        // Enemy buffs (eating tier)
        ProviderContainer.create("enemyregen,g",
                new InstabilityProvider(true, EffectPotionEnemy.class, true, MobEffects.REGENERATION, 200),
                Stability.ENEMY_REGEN_GLOBAL).add("eating", 1);
        ProviderContainer.create("enemyresist,g",
                new InstabilityProvider(true, EffectPotionEnemy.class, true, MobEffects.DAMAGE_RESISTANCE, 200),
                Stability.ENEMY_RESIST_GLOBAL).add("eating", 1);

        // Environmental effects
        ProviderContainer.create("burning",
                new ProviderScorched(),
                Stability.BURNING).add("harsh", 3);

        ProviderContainer.create("crumble",
                new InstabilityProvider(false, EffectCrumble.class),
                Stability.CRUMBLE).add("destructive", 10);

        // Decay providers
        ProviderContainer.create("decayblue",
                new ProviderDecayBlue(),
                Stability.DECAY_BLUE).add("eating", 4).add("death", 1);
        ProviderContainer.create("decaypurple",
                new ProviderDecayPurple(),
                Stability.DECAY_PURPLE).add("eating", 4).add("death", 1);
        ProviderContainer.create("decayred",
                new ProviderDecayRed(),
                Stability.DECAY_RED).add("eating", 4).add("death", 1);
        ProviderContainer.create("decaywhite",
                new ProviderDecayWhite(),
                Stability.DECAY_WHITE).add("eating", 2).add("death", 5);

        // Black decay
        ProviderContainer.create("decayblack",
                new ProviderDecayBlack(),
                Stability.DECAY_BLACK).add("eating", 3).add("death", 4);

        // Erosion
        ProviderContainer.create("erosion",
                new ProviderErosion(),
                Stability.EROSION).add("destructive", 8).add("eating", 4);

        // Extra ticks
        ProviderContainer.create("extraticks",
                new ProviderExtraTicks(),
                Stability.EXTRA_TICKS).add("harsh", 5).add("destructive", 4);

        // Destructive effects
        ProviderContainer.create("explosions",
                new ProviderExplosion(),
                Stability.EXPLOSIONS).add("destructive", 8);
        ProviderContainer.create("lightning",
                new ProviderLightning(),
                Stability.LIGHTNING).add("destructive", 3);
        ProviderContainer.create("meteors",
                new ProviderMeteor(),
                Stability.METEORS).add("destructive", 4);

        LOGGER.info("Instability data initialized with {} providers and {} decks",
                InstabilityProviderRegistry.getAllProviderIds().size(),
                InstabilityProviderRegistry.getDeckNames().size());
    }
}
