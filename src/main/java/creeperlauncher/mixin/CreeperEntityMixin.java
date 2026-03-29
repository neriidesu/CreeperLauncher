package creeperlauncher.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(Creeper.class)
public abstract class CreeperEntityMixin extends Monster{

    private static final ExplosionDamageCalculator NEW_DAMAGE_CALC = new ExplosionDamageCalculator() {
        @Override
        public boolean shouldBlockExplode(final Explosion explosion, final BlockGetter level, final BlockPos pos, final BlockState state, final float power) {
            return false;
        }

        @Override
        public boolean shouldDamageEntity(final Explosion explosion, final Entity entity) {
            return false;
        }

        @Override
        public float getKnockbackMultiplier(final Entity entity) {
            return 2.0F;
        }
    };
    @Shadow
    private int swell;
    @Shadow
    private int maxSwell;
    @Shadow
    private int explosionRadius;

    protected CreeperEntityMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Creeper;isAlive()Z"))
    void tick(CallbackInfo info) {
        Creeper that = (Creeper)(Object)this;
        Level world = that.level();
        int fuseTime = this.maxSwell - (world.isClientSide() ? 2 : 1);
        if(!that.isAlive() || this.swell < fuseTime) {
            return;
        }

        this.explodeCreeper(that);
    }

    @Unique
    private void explodeCreeper(Creeper c) {

        if (c.level() instanceof ServerLevel level) {
            float explosionMultiplier = c.isPowered() ? 2.0F : 1.0F;
            this.dead = true;
            level.explode(
                    this,
                    Explosion.getDefaultDamageSource(c.level(),this),
                    NEW_DAMAGE_CALC,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    this.explosionRadius * explosionMultiplier,
                    false,
                    Level.ExplosionInteraction.MOB);
            this.triggerOnDeathMobEffects(level, RemovalReason.KILLED);
            this.discard();
        }
    }


}
