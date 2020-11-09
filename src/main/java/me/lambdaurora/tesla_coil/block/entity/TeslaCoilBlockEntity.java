/*
 * Copyright (c) 2020 LambdAurora <aurora42lambda@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lambdaurora.tesla_coil.block.entity;

import me.lambdaurora.tesla_coil.TeslaCoilRegistry;
import me.lambdaurora.tesla_coil.entity.LightningArcEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

public class TeslaCoilBlockEntity extends BlockEntity
{
    private final Random random = new Random();
    private boolean enabled = false;
    private int age = 0;
    private int sideParticles = 0;

    private Direction smallArcDirection = null;
    private int smallArcCooldown = 0;

    public TeslaCoilBlockEntity(BlockPos pos, BlockState state)
    {
        super(TeslaCoilRegistry.TESLA_COIL_BLOCK_ENTITY_TYPE, pos, state);
    }

    public boolean isEnabled()
    {
        return this.enabled;
    }

    public int getAge()
    {
        return this.age;
    }

    @Environment(EnvType.CLIENT)
    public @Nullable Direction getSmallArcDirection()
    {
        return this.smallArcDirection;
    }

    public static void clientTick(World world, BlockPos pos, BlockState state, TeslaCoilBlockEntity teslaCoil)
    {
        teslaCoil.checkStructure();

        if (teslaCoil.isEnabled()) {
            teslaCoil.age++;

            teslaCoil.displaySideParticles();
            teslaCoil.rollNextSmallArcDirection();
        }
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, TeslaCoilBlockEntity teslaCoil)
    {
        teslaCoil.checkStructure();

        if (teslaCoil.isEnabled()) {
            teslaCoil.age++;

            teslaCoil.tryAttack();
        }
    }

    @Override
    public void fromTag(CompoundTag tag)
    {
        super.fromTag(tag);
        this.enabled = tag.getBoolean("enabled");
    }

    @Override
    public CompoundTag toTag(CompoundTag tag)
    {
        tag = super.toTag(tag);
        tag.putBoolean("enabled", this.enabled);
        return tag;
    }

    protected void checkStructure()
    {
        if (this.world.getTime() % 80L != 0L) {
            return;
        }

        BlockPos.Mutable pos = new BlockPos.Mutable(this.pos.getX(), this.pos.getY(), this.pos.getZ());

        for (int i = 0; i < 3; i++) {
            BlockState state = this.world.getBlockState(pos);

            if (i == 1) {
                if (state.getBlock() != TeslaCoilRegistry.TESLA_PRIMARY_COIL_BLOCK) {
                    this.enabled = false;
                    return;
                }
            } else if (i == 2) {
                if (state.getBlock() != TeslaCoilRegistry.TESLA_SECONDARY_COIL_BLOCK) {
                    this.enabled = false;
                    return;
                }
            }

            for (Direction direction : Direction.values()) {
                if (!direction.getAxis().isHorizontal())
                    continue;

                pos.move(direction);

                state = this.world.getBlockState(pos);
                if (state.getBlock() != Blocks.IRON_BARS) {
                    this.enabled = false;
                    return;
                }

                pos.move(direction.getOpposite());
            }

            pos.move(Direction.UP);
        }

        BlockState state = this.world.getBlockState(pos);
        if (state.getBlock() != TeslaCoilRegistry.TESLA_COIL_TOP_LOAD_BLOCK) {
            this.enabled = false;
            return;
        }

        if (!this.enabled)
            this.age = 0;
        this.enabled = true;
    }

    protected void displaySideParticles()
    {
        if (this.sideParticles > 18) {
            this.sideParticles = 0;
        }

        if (this.sideParticles % 2 == 0) {
            float xPos = this.pos.getX() + .5f;
            float zPos = this.pos.getZ() + .5f;
            float yOffset = this.sideParticles / 2.f / 3.f;
            float yPos = this.pos.getY() + yOffset;

            for (Direction direction : Direction.values()) {
                if (direction.getAxis().isHorizontal()) {
                    this.world.addParticle(ParticleTypes.CRIT,
                            xPos + direction.getOffsetX(), yPos, zPos + direction.getOffsetZ(),
                            0, 0.025, 0);
                }
            }
        }

        this.sideParticles++;
    }

    private void rollNextSmallArcDirection()
    {
        final int cooldown = 5;

        if (this.smallArcCooldown > 0) {
            this.smallArcCooldown--;
            return;
        }

        int dirIndex = this.random.nextInt(20);
        if (dirIndex < 6) {
            this.smallArcDirection = Direction.values()[dirIndex];
            if (this.smallArcDirection.getAxis().isVertical())
                this.smallArcDirection = null;
        } else
            this.smallArcDirection = null;

        this.smallArcCooldown = cooldown;
    }

    protected void tryAttack()
    {
        if (this.world.getTime() % 20L != 0L || this.random.nextBoolean())
            return;

        double x = this.getPos().getX() + 0.5;
        double y = this.getPos().getY();
        double z = this.getPos().getZ() + 0.5;

        TargetPredicate targetPredicate = new TargetPredicate();
        targetPredicate.setPredicate(entity -> entity instanceof HostileEntity || (entity instanceof IronGolemEntity && entity.getHealth() < entity.getMaxHealth() - 1.f));
        LivingEntity entity = this.world.getClosestEntity(LivingEntity.class, targetPredicate, null,
                x, y, z,
                new Box(this.pos.getX() - 10, this.pos.getY() - 8, this.pos.getZ() - 10, this.pos.getX() + 10, this.pos.getY() + 8, this.pos.getZ() + 10));

        if (entity != null) {
            LightningArcEntity lightningEntity = TeslaCoilRegistry.LIGHTNING_ARC_ENTITY_TYPE.create(this.world);

            if (lightningEntity == null) return;

            lightningEntity.setPos(x, y + 3.5, z);
            lightningEntity.setTarget(entity.getBlockPos());
            lightningEntity.setTargetPredicate(targetPredicate);

            this.world.spawnEntity(lightningEntity);

            //entity.damage(DamageSource.LIGHTNING_BOLT, 1);
        }
    }
}
