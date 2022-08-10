package com.finallion.graveyard.entities;

import com.finallion.graveyard.entities.ai.goals.LichMeleeGoal;
import com.finallion.graveyard.init.TGEntities;
import com.finallion.graveyard.init.TGParticles;
import com.finallion.graveyard.init.TGSounds;
import com.finallion.graveyard.util.MathUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class LichEntity extends HostileEntity implements IAnimatable {
    private final ServerBossBar bossBar;
    private AttributeContainer attributeContainer;
    private AnimationFactory factory = new AnimationFactory(this);
    protected static final TargetPredicate HEAD_TARGET_PREDICATE;
    private static final Predicate<LivingEntity> CAN_ATTACK_PREDICATE;
    private static final UUID ATTACKING_SPEED_BOOST_ID = UUID.fromString("020E0DFB-87AE-4653-9556-831010E291A0");
    private static final EntityAttributeModifier ATTACKING_SPEED_BOOST;
    private static final EntityAttributeModifier CRAWL_SPEED_BOOST;
    // animation
    private final AnimationBuilder SPAWN_ANIMATION = new AnimationBuilder().addAnimation("spawn", false);
    private final AnimationBuilder IDLE_ANIMATION = new AnimationBuilder().addAnimation("idle", true);
    private final AnimationBuilder ATTACK_ANIMATION = new AnimationBuilder().addAnimation("attack", true);
    private final AnimationBuilder CORPSE_SPELL_ANIMATION = new AnimationBuilder().addAnimation("corpse_spell", true);
    private final AnimationBuilder START_PHASE_2_ANIMATION = new AnimationBuilder().addAnimation("phase_two", false);
    private final AnimationBuilder PHASE_2_IDLE_ANIMATION = new AnimationBuilder().addAnimation("phase_two_idle", true);
    private final AnimationBuilder PHASE_2_ATTACK_ANIMATION = new AnimationBuilder().addAnimation("phase_two_attack", true);
    private final AnimationBuilder START_PHASE_3_ANIMATION = new AnimationBuilder().addAnimation("phase_three", false);
    private final AnimationBuilder PHASE_3_ATTACK_ANIMATION = new AnimationBuilder().addAnimation("crawl", true);
    private final AnimationBuilder DEATH_ANIMATION = new AnimationBuilder().addAnimation("death", false);
    protected static final int ANIMATION_SPAWN = 0;
    protected static final int ANIMATION_IDLE = 1;
    protected static final int ANIMATION_MELEE = 2;
    protected static final int ANIMATION_CORPSE_SPELL = 3;
    protected static final int ANIMATION_START_PHASE_2 = 4;
    protected static final int ANIMATION_PHASE_2_IDLE = 5;
    protected static final int ANIMATION_PHASE_2_ATTACK = 6;
    protected static final int ANIMATION_START_PHASE_3 = 8;
    protected static final int ANIMATION_PHASE_3_ATTACK = 9;
    protected static final int ANIMATION_STOP = 10;
    // data tracker
    private static final TrackedData<Integer> INVUL_TIMER; // spawn invul timer
    private static final TrackedData<Integer> PHASE_INVUL_TIMER; // other invul timer
    private static final TrackedData<Integer> ATTACK_ANIM_TIMER;
    private static final TrackedData<Integer> PHASE_TWO_START_ANIM_TIMER; // main phase one to two
    private static final TrackedData<Integer> PHASE_THREE_START_ANIM_TIMER; // main phase two to three
    private static final TrackedData<Integer> PHASE; // divides fight into three main phases and two transitions, animations are named after the main phase
    private static final TrackedData<Integer> ANIMATION;
    private static final TrackedData<Boolean> CAN_CORPSE_SPELL_START;
    private static final TrackedData<Boolean> CAN_HUNT_START;
    private static final TrackedData<Boolean> CAN_MOVE;
    // constants
    private static final int SPAWN_INVUL_TIMER = 420;
    private static final int DEFAULT_INVUL_TIMER = 200;
    private final float HEALTH_PHASE_01 = 400.0F;
    private final float HEALTH_PHASE_02 = 200.0F;
    public final int ATTACK_ANIMATION_DURATION = 40;
    private final int START_PHASE_TWO_ANIMATION_DURATION = 121;
    private final int START_PHASE_THREE_ANIMATION_DURATION = 220;
    private final int START_PHASE_TWO_PARTICLES = 80;
    private final int CORPSE_SPELL_COOLDOWN = 800;
    private final int CORPSE_SPELL_DURATION = 400;
    private final int HUNT_COOLDOWN = 800;
    private final int HUNT_DURATION = 800;
    protected static final EntityDimensions CRAWL_DIMENSIONS;
    // variables
    private int corpseSpellCooldownTicker = 800; // initial cooldown from spawn time until first spell, will be set in goal
    private int huntCooldownTicker = 150; // initial cooldown from spawn time until first spell, will be set in goal
    private BlockPos homePos;
    private Direction spawnDirection;
    private int attackCooldown;

    // TODO:
    // add falling parts of skeletons
    // falling bone particles
    // make unpushable
    // teeth only one texture layer
    // save phase in nbt
    // apply mining fatigue
    // prevent effect from being cleared (with milk)
    // teleport to altar pos
    // stop looking while spawning
    // ambient, hurt sounds
    public LichEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.bossBar = (ServerBossBar) (new ServerBossBar(this.getDisplayName(), BossBar.Color.WHITE, BossBar.Style.PROGRESS)).setDarkenSky(true).setThickenFog(true);
    }

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        if (getAnimationState() == ANIMATION_SPAWN && getInvulnerableTimer() >= 0) {
            event.getController().setAnimation(SPAWN_ANIMATION);
            return PlayState.CONTINUE;
        }

        return PlayState.CONTINUE;
    }

    // attack handler
    private <E extends IAnimatable> PlayState predicate2(AnimationEvent<E> event) {
        // each anim in its own if-clause to avoid unpredictable behaviour between phases
        // anims that loop will loop forever until PlayState.STOPed, it doesn't care about internal animation state tracker

        // set from the respawn method, stops all animations from previous phases
        if (getAnimationState() == ANIMATION_STOP) {
            return PlayState.STOP;
        }

        /* PHASE 1 */
        // takes one tick to get to this method (from mobtick)
        // do not attack when: the spawn invul timer is active, the phase is incorrect or the invul timer was set from a spell
        if (getAnimationState() == ANIMATION_MELEE && getAttackAnimTimer() == (ATTACK_ANIMATION_DURATION - 1) && isAttacking() && !(this.isDead() || this.getHealth() < 0.01) && canMeeleAttack()) {
            setAttackAnimTimer(ATTACK_ANIMATION_DURATION - 2); // disables to call the animation immediately after (seems to be called multiple times per tick, per frame tick?)
            event.getController().setAnimation(ATTACK_ANIMATION);
            return PlayState.CONTINUE;
        }

        if (getAnimationState() == ANIMATION_CORPSE_SPELL && getPhase() == 1) {
            event.getController().setAnimation(CORPSE_SPELL_ANIMATION);
            return PlayState.CONTINUE;
        }

        if (getPhase() == 1) {
            if (getAnimationState() == ANIMATION_IDLE && getAttackAnimTimer() <= 0 && getInvulnerableTimer() <= 0) {
                event.getController().setAnimation(IDLE_ANIMATION);
                return PlayState.CONTINUE;
            } else {
                setAnimationState(ANIMATION_MELEE);
            }
        }

        /* TRANSITION PHASE 2 */
        if (getAnimationState() == ANIMATION_START_PHASE_2 && getPhase() == 2) {
            event.getController().setAnimation(START_PHASE_2_ANIMATION);
            return PlayState.CONTINUE;
        }
        /* PHASE 3 */
        if (getAnimationState() == ANIMATION_PHASE_2_IDLE && getPhase() == 3) {
            event.getController().setAnimation(PHASE_2_IDLE_ANIMATION);
            return PlayState.CONTINUE;
        }

        if (getAnimationState() == ANIMATION_PHASE_2_ATTACK && getPhase() == 3) {
            event.getController().setAnimation(PHASE_2_ATTACK_ANIMATION);
            return PlayState.CONTINUE;
        }


        /* PHASE 4 */
        if (getAnimationState() == ANIMATION_START_PHASE_3 && getPhase() == 4) {
            event.getController().setAnimation(START_PHASE_3_ANIMATION);
            return PlayState.CONTINUE;
        }

        /* PHASE 5 */
        if (getAnimationState() == ANIMATION_PHASE_3_ATTACK && getPhase() == 5 && !(this.isDead() || this.getHealth() < 0.01)) {
            event.getController().setAnimation(PHASE_3_ATTACK_ANIMATION);
            return PlayState.CONTINUE;
        }

        /* DEATH */
        if (this.isDead() || this.getHealth() < 0.01) {
            event.getController().setAnimation(DEATH_ANIMATION);
            return PlayState.CONTINUE;
        }


        /* STOPPERS */
        // stops attack animation from looping
        if (getPhase() == 1) {
            if (getAttackAnimTimer() <= 0) {
                setAnimationState(ANIMATION_IDLE);
                return PlayState.STOP;
            }

            // stops idle animation from looping
            if (getAttackAnimTimer() > 0 && getAnimationState() == ANIMATION_IDLE) {
                return PlayState.STOP;
            }
        }


        return PlayState.CONTINUE;
    }


    @Override
    public void registerControllers(AnimationData animationData) {
        animationData.addAnimationController(new AnimationController(this, "controller", 0, this::predicate));
        animationData.addAnimationController(new AnimationController(this, "controller2", 0, this::predicate2));
    }

    @Override
    public AnimationFactory getFactory() {
        return factory;
    }

    protected void initGoals() {
        super.initGoals();
        this.goalSelector.add(2, new LichMeleeGoal(this, 1.0D, false));
        this.goalSelector.add(4, new SummonFallenCorpsesGoal(this));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(10, new LookAtEntityGoal(this, MobEntity.class, 8.0F));
        this.targetSelector.add(1, new RevengeGoal(this, new Class[0]));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
    }

    @Override
    public AttributeContainer getAttributes() {
        if (attributeContainer == null) {
            attributeContainer = new AttributeContainer(HostileEntity.createHostileAttributes()
                    .add(EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_PHASE_01)
                    .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0D)
                    .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0D)
                    .add(EntityAttributes.GENERIC_ARMOR, 3.0D)
                    .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 5.0D).build());
        }
        return attributeContainer;
    }


    @Override
    public void tickMovement() {
        randomDisplayTick(this.getWorld(), this.getBlockPos(), this.getRandom());

        int phaseTwoTimer = getStartPhaseTwoAnimTimer();
        if (phaseTwoTimer < START_PHASE_TWO_PARTICLES && phaseTwoTimer > (START_PHASE_TWO_PARTICLES / 2)) {
            float offset = 0.0F;
            for (int i = 0; i < 25; i++) {
                if (i < 7) {
                    offset += 0.15F;
                } else if (i > 12) {
                    offset -= 0.15F;
                }
                MathUtil.createParticleDisk(this.getWorld(), this.getX(), this.getY() + ((float) i / 10), this.getZ(), 0.0D, 0.3D, 0.0D, 2 * offset, ParticleTypes.SOUL_FIRE_FLAME, this.getRandom());
            }
        }

        super.tickMovement();
    }

    protected void mobTick() {
        if (homePos == null) {
            homePos = this.getBlockPos(); // TODO: adjust
        }

        /* PHASE 5 FIGHT LOGIC */
        if (getPhase() == 5) {
            EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            setCanMove(true);
            setAnimationState(ANIMATION_PHASE_3_ATTACK);
            if (!entityAttributeInstance.hasModifier(CRAWL_SPEED_BOOST)) {
                entityAttributeInstance.addTemporaryModifier(CRAWL_SPEED_BOOST);
            }
        }
        /* END PHASE 5 FIGHT LOGIC */

        /* PHASE 3 FIGHT LOGIC */
        if (getPhase() == 3) {
            EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);

            if (getHuntCooldownTicker() > 0) {
                setHuntCooldownTicker(getHuntCooldownTicker() - 1);
            } else {
                setHuntStart(true);
            }

            if (canHuntStart()) {
                //TODO: on start: teleport all players randomly
                //TODO: sound
                //TODO: particle

                setCanMove(true);
                if (getPhaseInvulnerableTimer() == 0) {
                    this.setPhaseInvulTimer(HUNT_DURATION); // acts as hunt duration counter
                }
                setAnimationState(ANIMATION_PHASE_2_ATTACK);

                // TODO: adjust dmg
                // TODO: summon revenants
                // TODO: apply blindness
                // TODO: screeching sound
                if (!entityAttributeInstance.hasModifier(ATTACKING_SPEED_BOOST)) {
                    entityAttributeInstance.addTemporaryModifier(ATTACKING_SPEED_BOOST);
                }
            }

            // if the invul (= hunt duration) runs out, set cooldown
            if (getPhaseInvulnerableTimer() == 1 && getHuntCooldownTicker() == 0 && canHuntStart()) {
                this.teleport(homePos.getX(), homePos.getY(), homePos.getZ());
                this.setHuntCooldownTicker(HUNT_COOLDOWN);
                setAnimationState(ANIMATION_PHASE_2_IDLE);
                setHuntStart(false);
                setCanMove(false);
                entityAttributeInstance.removeModifier(ATTACKING_SPEED_BOOST);
            }
        }
        /* END PHASE 3 FIGHT LOGIC */

        /* TRANSITION MAIN PHASE ONE to TWO, == PHASE TWO */
        if (getPhase() == 2) {
            int phaseTwoTimer = getStartPhaseTwoAnimTimer();
            if (getPhaseInvulnerableTimer() == 0) {
                setPhaseInvulTimer(START_PHASE_TWO_ANIMATION_DURATION); // invul
            }
            setAnimationState(ANIMATION_START_PHASE_2);
            if (phaseTwoTimer > 0) {
                if (phaseTwoTimer == START_PHASE_TWO_ANIMATION_DURATION) {
                    playStartPhaseTwoSound();
                }
                setStartPhaseTwoAnimTimer(phaseTwoTimer - 1);
            }

            // if the transition animation phase has played advance phase
            if (phaseTwoTimer == 1) {
                setAnimationState(ANIMATION_PHASE_2_IDLE);
                setPhase(getPhase() + 1);
            }
        }
        /* END TRANSITION MAIN PHASE ONE to TWO  */

        /* TRANSITION MAIN PHASE TWO to THREE, == PHASE FOUR */
        if (getPhase() == 4) {
            int phaseThreeTimer = getStartPhaseThreeAnimTimer();
            if (getPhaseInvulnerableTimer() == 0) {
                setPhaseInvulTimer(START_PHASE_THREE_ANIMATION_DURATION); // invul
            }
            setAnimationState(ANIMATION_START_PHASE_3);
            if (phaseThreeTimer > 0) {
                if (phaseThreeTimer == START_PHASE_THREE_ANIMATION_DURATION) {
                    playStartPhaseThreeSound();
                }
                setStartPhaseThreeAnimTimer(phaseThreeTimer - 1);
            }

            // if the transition animation phase has played advance phase
            if (phaseThreeTimer == 1) {
                setAnimationState(ANIMATION_PHASE_3_ATTACK);
                setPhase(getPhase() + 1);
            }
        }
        /* END TRANSITION MAIN PHASE TWO TO THREE */

        // counter
        if (getCorpseSpellCooldownTicker() > 0) {
            setCorpseSpellCooldownTicker(getCorpseSpellCooldownTicker() - 1);
        }

        if (this.getPhaseInvulnerableTimer() > 0) {
            // TODO: add particle ring
            int timer = getPhaseInvulnerableTimer() - 1;
            this.setPhaseInvulTimer(timer);
        }

        int i;
        if (this.getInvulnerableTimer() > 0) {
            i = this.getInvulnerableTimer() - 1;

            int timer;
            if (getPhase() == 1) {
                timer = SPAWN_INVUL_TIMER;
            } else {
                timer = DEFAULT_INVUL_TIMER;
            }
            if (this.getInvulnerableTimer() == 1 && getPhase() == 1) {
                setAnimationState(ANIMATION_IDLE);
            }

            this.bossBar.setPercent(1.0F - (float) i / timer);
            this.setInvulTimer(i);
        } else {
            // ATTACK TIMER
            if (this.getAttackAnimTimer() == ATTACK_ANIMATION_DURATION) {
                setAnimationState(ANIMATION_MELEE);
            }

            if (this.getAttackAnimTimer() > 0) {
                int animTimer = this.getAttackAnimTimer() - 1;
                this.setAttackAnimTimer(animTimer);
            } else if (getCorpseSpellCooldownTicker() <= 0) {
                setCorpseSpellStart(true);
            }

            /*
            LivingEntity target = getTarget();
            attackCooldown--;
            int phase = getPhase();
            if (target != null) {
                double d = phase == 5 ? 20.0F : 16.0F;
                double distance = squaredDistanceTo(target);
                if (distance <= d && attackCooldown <= 0) {
                    if ((phase == 3 && canHuntStart()) || phase == 5) {
                        tryAttack(target);
                        if (phase == 3) {
                            attackCooldown = 50;
                        } else {
                            attackCooldown = 20;
                        }
                    } else if (phase == 1 && canMeeleAttack() && !canCorpseSpellStart()) {
                        if (getAttackAnimTimer() == 0) {
                            setAttackAnimTimer(ATTACK_ANIMATION_DURATION);
                        }

                        // sound on start anim
                        if (getAttackAnimTimer() == ATTACK_ANIMATION_DURATION) {
                            playAttackSound();
                        }

                        if (getAttackAnimTimer() == 16 && this.tryAttack(target)) {
                            // warden sonic boom logic
                            Vec3d vec3d = getPos().add(0.0D, 1.600000023841858D, 0.0D);
                            Vec3d vec3d2 = target.getEyePos().subtract(vec3d);
                            Vec3d vec3d3 = vec3d2.normalize();

                            for (int ii = 1; ii < MathHelper.floor(vec3d2.length()) + 7; ++ii) {
                                Vec3d vec3d4 = vec3d.add(vec3d3.multiply((double) ii));
                                ((ServerWorld) getWorld()).spawnParticles(TGParticles.GRAVEYARD_SOUL_BEAM_PARTICLE, vec3d4.x, vec3d4.y + 1.0D, vec3d4.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                            }

                            double f = 1.5D * (1.0D - target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE));
                            double e = 2.5D * (1.0D - target.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE));
                            target.addVelocity(vec3d3.getX() * e, vec3d3.getY() * f, vec3d3.getZ() * e);
                        }
                        attackCooldown = 20;
                    }
                }
            }

             */


            super.mobTick();

            //if (this.age % 20 == 0) {
            //    this.heal(1.0F);
            //}

            this.bossBar.setPercent(this.getHealth() / this.getMaxHealthPerPhase());
        }
    }

    private float getMaxHealthPerPhase() {
        if (getPhase() == 1) {
            return HEALTH_PHASE_01;
        } else {
            return HEALTH_PHASE_02;
        }
    }


    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(CAN_MOVE, false);
        this.dataTracker.startTracking(INVUL_TIMER, 0);
        this.dataTracker.startTracking(PHASE_INVUL_TIMER, 0);
        this.dataTracker.startTracking(ANIMATION, ANIMATION_IDLE);
        this.dataTracker.startTracking(ATTACK_ANIM_TIMER, 0);
        this.dataTracker.startTracking(PHASE_TWO_START_ANIM_TIMER, START_PHASE_TWO_ANIMATION_DURATION);
        this.dataTracker.startTracking(PHASE_THREE_START_ANIM_TIMER, START_PHASE_THREE_ANIMATION_DURATION);
        this.dataTracker.startTracking(PHASE, 1);
        this.dataTracker.startTracking(CAN_CORPSE_SPELL_START, false);
        this.dataTracker.startTracking(CAN_HUNT_START, false);
    }

    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Invul", this.getInvulnerableTimer());
        nbt.putInt("PhaseInvul", this.getPhaseInvulnerableTimer());
        //nbt.putInt("Phase", this.getPhase());
    }

    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.setInvulTimer(nbt.getInt("Invul"));
        this.setPhaseInvulTimer(nbt.getInt("PhaseInvul"));
        //this.setPhase(nbt.getInt("Phase"));
        if (this.hasCustomName()) {
            this.bossBar.setName(this.getDisplayName());
        }

    }

    public void setCustomName(@Nullable Text name) {
        super.setCustomName(name);
        this.bossBar.setName(this.getDisplayName());
    }

    // called by Vial of Blood item
    public void onSummoned(Direction direction, BlockPos altarPos) {
        this.setAnimationState(ANIMATION_SPAWN);
        applyInvulAndResetBossBar(SPAWN_INVUL_TIMER);
        this.homePos = altarPos;
        this.spawnDirection = direction;
        playSpawnSound();
    }

    private void applyInvulAndResetBossBar(int invul) {
        this.setInvulTimer(invul);
        this.bossBar.setPercent(0.0F);
    }

    @Override
    protected void updatePostDeath() {
        ++this.deathTime;
        if (this.deathTime == 100) {
            // TODO: add particles
        }

        if (this.deathTime == 160 && !this.world.isClient()) {
            this.world.sendEntityStatus(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
    }


    private void playSpawnSound() {
        this.world.playSound(null, this.getBlockPos(), TGSounds.LICH_SPAWN, SoundCategory.HOSTILE, 10.0F, 1.0F);
    }

    public void playAttackSound() {
        this.world.playSound(null, this.getBlockPos(), TGSounds.LICH_MELEE, SoundCategory.HOSTILE, 10.0F, 1.0F);
    }

    private void playCorpseSpellSound() {
        this.world.playSound(null, this.getBlockPos(), TGSounds.LICH_CORPSE_SPELL, SoundCategory.HOSTILE, 10.0F, 1.0F);
    }

    private void playStartPhaseTwoSound() {
        this.world.playSound(null, this.getBlockPos(), TGSounds.LICH_CORPSE_SPELL, SoundCategory.HOSTILE, 10.0F, 1.0F);
    }

    private void playStartPhaseThreeSound() {
        this.world.playSound(null, this.getBlockPos(), TGSounds.LICH_CORPSE_SPELL, SoundCategory.HOSTILE, 10.0F, 1.0F);
    }

    public boolean canCorpseSpellStart() {
        return this.dataTracker.get(CAN_CORPSE_SPELL_START);
    }

    public void setCorpseSpellStart(boolean bool) {
        this.dataTracker.set(CAN_CORPSE_SPELL_START, bool);
    }

    public boolean canHuntStart() {
        return this.dataTracker.get(CAN_HUNT_START);
    }

    public void setHuntStart(boolean bool) {
        this.dataTracker.set(CAN_HUNT_START, bool);
    }

    public boolean canMove() {
        return this.dataTracker.get(CAN_MOVE);
    }

    public void setCanMove(boolean bool) {
        this.dataTracker.set(CAN_MOVE, bool);
    }

    public int getInvulnerableTimer() {
        return (Integer) this.dataTracker.get(INVUL_TIMER);
    }

    public void setInvulTimer(int ticks) {
        this.dataTracker.set(INVUL_TIMER, ticks);
    }

    public int getPhaseInvulnerableTimer() {
        return (Integer) this.dataTracker.get(PHASE_INVUL_TIMER);
    }

    public void setPhaseInvulTimer(int ticks) {
        this.dataTracker.set(PHASE_INVUL_TIMER, ticks);
    }

    public int getAnimationState() {
        return this.dataTracker.get(ANIMATION);
    }

    public void setAnimationState(int state) {
        this.dataTracker.set(ANIMATION, state);
    }

    public int getPhase() {
        return (Integer) this.dataTracker.get(PHASE);
    }

    public void setPhase(int phase) {
        this.dataTracker.set(PHASE, phase);
    }

    public int getStartPhaseTwoAnimTimer() {
        return (Integer) this.dataTracker.get(PHASE_TWO_START_ANIM_TIMER);
    }

    public void setStartPhaseTwoAnimTimer(int startPhaseTwoAnimTimer) {
        this.dataTracker.set(PHASE_TWO_START_ANIM_TIMER, startPhaseTwoAnimTimer);
    }

    public int getStartPhaseThreeAnimTimer() {
        return (Integer) this.dataTracker.get(PHASE_THREE_START_ANIM_TIMER);
    }

    public void setStartPhaseThreeAnimTimer(int startPhaseThreeAnimTimer) {
        this.dataTracker.set(PHASE_THREE_START_ANIM_TIMER, startPhaseThreeAnimTimer);
    }

    public int getAttackAnimTimer() {
        return (Integer) this.dataTracker.get(ATTACK_ANIM_TIMER);
    }

    public void setAttackAnimTimer(int time) {
        this.dataTracker.set(ATTACK_ANIM_TIMER, time);
    }


    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    public boolean addStatusEffect(StatusEffectInstance effect, @Nullable Entity source) {
        return false;
    }

    public EntityGroup getGroup() {
        return EntityGroup.UNDEAD;
    }

    protected boolean canStartRiding(Entity entity) {
        return false;
    }

    public boolean canUsePortals() {
        return false;
    }

    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        this.bossBar.addPlayer(player);
    }

    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        this.bossBar.removePlayer(player);
    }

    //TODO stop huge amount of dmg
    public boolean damage(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            if ((this.getInvulnerableTimer() > 0 || this.getPhaseInvulnerableTimer() > 0) && source != DamageSource.OUT_OF_WORLD) {
                return false;
            } else {
                // removes entity immediately when killed with commands
                //if (source == DamageSource.OUT_OF_WORLD && amount > this.getHealth()) {
                //    this.deathTime = 155;
                //}

                if (amount > this.getHealth() && getPhase() < 5 && source != DamageSource.OUT_OF_WORLD) {
                    //amount = this.getHealth() - 1;
                    respawn();
                    return false;
                }

                return super.damage(source, amount);
            }
        }
    }

    private void respawn() {
        this.setPhase(getPhase() + 1);
        setAnimationState(ANIMATION_STOP);
        this.clearStatusEffects();
        applyInvulAndResetBossBar(DEFAULT_INVUL_TIMER);
        setHealth(HEALTH_PHASE_02);
        setAttackAnimTimer(0);
        if (getPhase() == 4 || getPhase() == 5) {
            getDimensions(EntityPose.CROUCHING);
        }
        //this.world.sendEntityStatus(this, (byte)35);
    }

    public void randomDisplayTick(World world, BlockPos pos, Random random) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        double d = (double) i + random.nextInt(3) - random.nextInt(3);
        double e = (double) j + 4.2D;
        double f = (double) k + random.nextInt(3) - random.nextInt(3);
        world.addParticle(ParticleTypes.ASH, d, e, f, 0.0D, 0.0D, 0.0D);
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        if (random.nextInt(2) == 0) {
            mutable.set(i + MathHelper.nextInt(random, -10, 10), j + random.nextInt(10), k + MathHelper.nextInt(random, -10, 10));
            BlockState blockState = world.getBlockState(mutable);
            if (!blockState.isFullCube(world, mutable)) {
                world.addParticle(ParticleTypes.SMOKE, (double) mutable.getX() + random.nextDouble(), (double) mutable.getY() + random.nextDouble(), (double) mutable.getZ() + random.nextDouble(), 0.0D, 0.0D, 0.0D);
            }
        }
    }

    public List<PlayerEntity> getPlayersInRange(double range) {
        return this.getWorld().getPlayers(HEAD_TARGET_PREDICATE, this, this.getBoundingBox().expand(range));
    }


    public boolean canMeeleAttack() {
        return getPhase() == 1 && getPhaseInvulnerableTimer() <= 0 && getInvulnerableTimer() <= 0 && this.getHealth() > 35.0F;
    }

    public int getCorpseSpellCooldownTicker() {
        return corpseSpellCooldownTicker;
    }

    public void setCorpseSpellCooldownTicker(int corpseSpellCooldownTicker) {
        this.corpseSpellCooldownTicker = corpseSpellCooldownTicker;
    }

    public int getHuntCooldownTicker() {
        return huntCooldownTicker;
    }

    public void setHuntCooldownTicker(int huntCooldownTicker) {
        this.huntCooldownTicker = huntCooldownTicker;
    }

    public float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        if (pose == EntityPose.CROUCHING) {
            return 2.0F;
        } else {
            return 4.0F;
        }
    }

    @Override
    public EntityDimensions getDimensions(EntityPose pose) {
        if (pose == EntityPose.CROUCHING) {
            setPose(EntityPose.CROUCHING);
            return CRAWL_DIMENSIONS;
        }
        return super.getDimensions(pose);
    }
    /*
    @Override
    public boolean isPushable() {
        return getPhase() == 1;
    }

     */

    static {
        INVUL_TIMER = DataTracker.registerData(LichEntity.class, TrackedDataHandlerRegistry.INTEGER);
        PHASE_INVUL_TIMER = DataTracker.registerData(LichEntity.class, TrackedDataHandlerRegistry.INTEGER);
        ATTACK_ANIM_TIMER = DataTracker.registerData(LichEntity.class, TrackedDataHandlerRegistry.INTEGER);
        PHASE_TWO_START_ANIM_TIMER = DataTracker.registerData(LichEntity.class, TrackedDataHandlerRegistry.INTEGER);
        PHASE_THREE_START_ANIM_TIMER = DataTracker.registerData(LichEntity.class, TrackedDataHandlerRegistry.INTEGER);
        ANIMATION = DataTracker.registerData(LichEntity.class, TrackedDataHandlerRegistry.INTEGER);
        PHASE = DataTracker.registerData(LichEntity.class, TrackedDataHandlerRegistry.INTEGER);
        CAN_CORPSE_SPELL_START = DataTracker.registerData(LichEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        CAN_HUNT_START = DataTracker.registerData(LichEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        CAN_MOVE = DataTracker.registerData(LichEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
        CAN_ATTACK_PREDICATE = Entity::isPlayer;
        HEAD_TARGET_PREDICATE = TargetPredicate.createAttackable().setBaseMaxDistance(20.0D).setPredicate(CAN_ATTACK_PREDICATE);
        CRAWL_DIMENSIONS = EntityDimensions.fixed(1.8F, 2.0F);
        CRAWL_SPEED_BOOST = new EntityAttributeModifier(ATTACKING_SPEED_BOOST_ID, "Attacking speed boost", 0.18D, EntityAttributeModifier.Operation.ADDITION);
        ATTACKING_SPEED_BOOST = new EntityAttributeModifier(ATTACKING_SPEED_BOOST_ID, "Attacking speed boost", 0.15D, EntityAttributeModifier.Operation.ADDITION);
    }

    public class SummonFallenCorpsesGoal extends Goal {
        protected final LichEntity lich;
        private final int FALL_HEIGHT = 15;
        private final int SQUARE_SIZE = 10;
        private final int CORPSE_SPAWN_RARITY = 5;
        private final int CORPSE_SPAWN_RARITY_PLAYER = 12;

        public SummonFallenCorpsesGoal(LichEntity lich) {
            this.lich = lich;
        }

        @Override
        public boolean canStart() {
            return lich.getPhase() == 1 && canCorpseSpellStart();
        }

        public boolean shouldContinue() {
            return CORPSE_SPELL_DURATION > 0 && this.lich.getPhase() == 1;
        }

        public void tick() {
            if (canCorpseSpellStart() && getCorpseSpellCooldownTicker() == 0) {
                // make invulnerable during spell, prevents lich melee attack
                this.lich.setPhaseInvulTimer(CORPSE_SPELL_DURATION);
                this.lich.setCorpseSpellStart(false);
                this.lich.setCorpseSpellCooldownTicker(CORPSE_SPELL_COOLDOWN);
                playCorpseSpellSound();
            }


            if (getPhaseInvulnerableTimer() <= 0) {
                setAnimationState(ANIMATION_MELEE);
                stop();
            } else {
                setAnimationState(ANIMATION_CORPSE_SPELL);
                ServerWorld serverWorld = (ServerWorld) LichEntity.this.world;

                BlockPos pos = this.lich.getBlockPos();
                List<PlayerEntity> list = getPlayersInRange(30.0D);
                List<BlockPos> positions = new ArrayList<>();
                for (int i = -SQUARE_SIZE; i < SQUARE_SIZE; i++) {
                    for (int ii = -SQUARE_SIZE; ii < SQUARE_SIZE; ii++) {
                        positions.add(pos.add(i, FALL_HEIGHT, ii));
                    }
                }

                if (random.nextInt(CORPSE_SPAWN_RARITY) == 0) {
                    FallingCorpse corpse = (FallingCorpse) TGEntities.FALLING_CORPSE.create(serverWorld);
                    BlockPos blockPos = positions.get(random.nextInt(positions.size()));
                    corpse.setPos((double) blockPos.getX() + 0.5D, (double) blockPos.getY() + 0.55D, (double) blockPos.getZ() + 0.5D);
                    serverWorld.spawnEntity(corpse);
                }


                if (random.nextInt(CORPSE_SPAWN_RARITY_PLAYER) == 0 && list.size() > 0) {
                    FallingCorpse corpse = (FallingCorpse) TGEntities.FALLING_CORPSE.create(serverWorld);
                    PlayerEntity target = list.get(random.nextInt(list.size()));
                    if (target != null) {
                        BlockPos blockPos = target.getBlockPos().add(0, FALL_HEIGHT, 0);
                        corpse.setPos((double) blockPos.getX() + 0.5D, (double) blockPos.getY() + 0.55D, (double) blockPos.getZ() + 0.5D);
                        serverWorld.spawnEntity(corpse);
                    }
                }
            }
        }
    }
}