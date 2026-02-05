package art.arcane.mystcraft.neoforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Consolidated NeoForge 1.20.4+ advancement triggers.
 * Uses the new SimpleCriterionTrigger.SimpleInstance record-based API with Codecs.
 */
public final class NeoForgeTriggers {

    private NeoForgeTriggers() {
    }

    // ========== ENTER MYST DIMENSION (QUINN) ==========
    public static class EnterMystDimensionQuinnTrigger extends SimpleCriterionTrigger<EnterMystDimensionQuinnTrigger.TriggerInstance> {

        @Override
        public Codec<TriggerInstance> codec() {
            return TriggerInstance.CODEC;
        }

        public void trigger(ServerPlayer player) {
            this.trigger(player, triggerInstance -> true);
        }

        public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {

            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC
                            .optionalFieldOf("player")
                            .forGetter(TriggerInstance::player)
            ).apply(instance, TriggerInstance::new));

            public static TriggerInstance create() {
                return new TriggerInstance(Optional.empty());
            }
        }
    }

    // ========== ENTER MYST DIMENSION (SAFE) ==========
    public static class EnterMystDimensionSafeTrigger extends SimpleCriterionTrigger<EnterMystDimensionSafeTrigger.TriggerInstance> {

        @Override
        public Codec<TriggerInstance> codec() {
            return TriggerInstance.CODEC;
        }

        public void trigger(ServerPlayer player) {
            this.trigger(player, triggerInstance -> true);
        }

        public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {

            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC
                            .optionalFieldOf("player")
                            .forGetter(TriggerInstance::player)
            ).apply(instance, TriggerInstance::new));

            public static TriggerInstance create() {
                return new TriggerInstance(Optional.empty());
            }
        }
    }

    // ========== WRITING DESK WRITE ==========
    public static class WritingDeskWriteTrigger extends SimpleCriterionTrigger<WritingDeskWriteTrigger.TriggerInstance> {

        @Override
        public Codec<TriggerInstance> codec() {
            return TriggerInstance.CODEC;
        }

        public void trigger(ServerPlayer player) {
            this.trigger(player, triggerInstance -> true);
        }

        public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {

            public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC
                            .optionalFieldOf("player")
                            .forGetter(TriggerInstance::player)
            ).apply(instance, TriggerInstance::new));

            public static TriggerInstance create() {
                return new TriggerInstance(Optional.empty());
            }
        }
    }
}
