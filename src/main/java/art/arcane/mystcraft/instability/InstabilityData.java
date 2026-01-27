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
        public static final int HARSH = 2500;
        public static final int DESTRUCTIVE = 10000;
        public static final int EATING = 15000;
        public static final int DEATH = 20000;
    }

    /** Provider activation costs - instability required for each effect */
    public static final class Stability {
        // Visual/minor effects
        public static final int BLINDNESS = 1000;
        public static final int BLINDNESS_GLOBAL = 1500;
        public static final int FATIGUE = 500;
        public static final int FATIGUE_GLOBAL = 1000;
        public static final int NAUSEA = 1000;
        public static final int NAUSEA_GLOBAL = 1500;

        // Combat effects
        public static final int HUNGER = 500;
        public static final int HUNGER_GLOBAL = 1000;
        public static final int SLOW = 500;
        public static final int SLOW_GLOBAL = 1000;
        public static final int WEAKNESS = 500;
        public static final int WEAKNESS_GLOBAL = 1000;
        public static final int POISON = 500;
        public static final int POISON_GLOBAL = 1000;
        public static final int WITHER = 1000;
        public static final int WITHER_GLOBAL = 2000;

        // Enemy buffs
        public static final int ENEMY_REGEN_GLOBAL = 1000;
        public static final int ENEMY_RESIST_GLOBAL = 1000;

        // Environmental effects
        public static final int BURNING = 500;
        public static final int BURNING_GLOBAL = 1000;
        public static final int CRUMBLE = 2000;
        public static final int CRUMBLE_BEDROCK = 5000;
        public static final int EROSION = 2000;

        // Destructive effects
        public static final int EXPLOSIONS = 1000;
        public static final int LIGHTNING = 1000;
        public static final int METEORS = 1000;

        // Decay effects
        public static final int DECAY_BLACK = 5000;
        public static final int DECAY_BLUE = 2000;
        public static final int DECAY_PURPLE = 2000;
        public static final int DECAY_RED = 2000;
        public static final int DECAY_WHITE = 5000;
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

        // Register potion effect providers
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
                Stability.FATIGUE).add("basic", 5);
        ProviderContainer.create("fatigue,g",
                new InstabilityProvider(true, EffectPotion.class, true, MobEffects.DIG_SLOWDOWN, 80),
                Stability.FATIGUE_GLOBAL).add("harsh", 5);

        // Hunger
        ProviderContainer.create("hunger",
                new InstabilityProvider(true, EffectPotion.class, false, MobEffects.HUNGER, 80),
                Stability.HUNGER).add("basic", 8).add("harsh", 2);
        ProviderContainer.create("hunger,g",
                new InstabilityProvider(true, EffectPotion.class, true, MobEffects.HUNGER, 80),
                Stability.HUNGER_GLOBAL).add("harsh", 5);

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
                Stability.POISON).add("basic", 9).add("harsh", 3);
        ProviderContainer.create("poison,g",
                new InstabilityProvider(true, EffectPotion.class, true, MobEffects.POISON, 80),
                Stability.POISON_GLOBAL).add("harsh", 5);

        // Slowness
        ProviderContainer.create("slow",
                new InstabilityProvider(true, EffectPotion.class, false, MobEffects.MOVEMENT_SLOWDOWN, 80),
                Stability.SLOW).add("basic", 6).add("harsh", 1);
        ProviderContainer.create("slow,g",
                new InstabilityProvider(true, EffectPotion.class, true, MobEffects.MOVEMENT_SLOWDOWN, 80),
                Stability.SLOW_GLOBAL).add("harsh", 5);

        // Weakness
        ProviderContainer.create("weakness",
                new InstabilityProvider(true, EffectPotion.class, false, MobEffects.WEAKNESS, 80),
                Stability.WEAKNESS).add("basic", 8).add("harsh", 2);
        ProviderContainer.create("weakness,g",
                new InstabilityProvider(true, EffectPotion.class, true, MobEffects.WEAKNESS, 80),
                Stability.WEAKNESS_GLOBAL).add("harsh", 5);

        // Wither
        ProviderContainer.create("wither",
                new InstabilityProvider(true, EffectPotion.class, false, MobEffects.WITHER, 30),
                Stability.WITHER).add("harsh", 1).add("destructive", 1).add("eating", 2);
        ProviderContainer.create("wither,g",
                new InstabilityProvider(true, EffectPotion.class, true, MobEffects.WITHER, 30),
                Stability.WITHER_GLOBAL).add("destructive", 1).add("eating", 1).add("death", 1);

        // Enemy buffs
        ProviderContainer.create("enemyregen,g",
                new InstabilityProvider(true, EffectPotionEnemy.class, true, MobEffects.REGENERATION, 200),
                Stability.ENEMY_REGEN_GLOBAL).add("basic", 5).add("harsh", 2);
        ProviderContainer.create("enemyresist,g",
                new InstabilityProvider(true, EffectPotionEnemy.class, true, MobEffects.DAMAGE_RESISTANCE, 200),
                Stability.ENEMY_RESIST_GLOBAL).add("basic", 2).add("harsh", 1);

        // Environmental effects
        ProviderContainer.create("burning",
                new ProviderScorched(),
                Stability.BURNING).add("harsh", 1);

        ProviderContainer.create("crumble",
                new InstabilityProvider(false, EffectCrumble.class),
                Stability.CRUMBLE).add("destructive", 6);

        // Decay providers
        ProviderContainer.create("decayblue",
                new ProviderDecayBlue(),
                Stability.DECAY_BLUE).add("eating", 2).add("death", 1);
        ProviderContainer.create("decaypurple",
                new ProviderDecayPurple(),
                Stability.DECAY_PURPLE).add("eating", 2).add("death", 1);
        ProviderContainer.create("decayred",
                new ProviderDecayRed(),
                Stability.DECAY_RED).add("eating", 2).add("death", 1);
        ProviderContainer.create("decaywhite",
                new ProviderDecayWhite(),
                Stability.DECAY_WHITE).add("eating", 1).add("death", 3);

        // Destructive effects
        ProviderContainer.create("explosions",
                new ProviderExplosion(),
                Stability.EXPLOSIONS).add("destructive", 8);
        ProviderContainer.create("lightning",
                new ProviderLightning(),
                Stability.LIGHTNING).add("harsh", 4).add("destructive", 4);
        ProviderContainer.create("meteors",
                new ProviderMeteor(),
                Stability.METEORS).add("destructive", 4);

        LOGGER.info("Instability data initialized with {} providers and {} decks",
                InstabilityProviderRegistry.getAllProviderIds().size(),
                InstabilityProviderRegistry.getDeckNames().size());
    }
}
