package com.branders.spawnermod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.branders.spawnermod.config.ConfigValues;

import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.spawner.AbstractSpawner;
import net.minecraftforge.common.util.Constants.WorldEvents;

/**
 * 	Implements a limit to how many spawns a spawner can do. Only if enabled in config!
 * 
 * 	@author Anders <Branders> Blomqvist 
 */
@Mixin(AbstractSpawner.class)
public class LimitedSpawnsMixin {
	
	private short spawns = 0;
	
	@Inject(at = @At(value = "INVOKE", 
			target = "Lnet/minecraft/world/World;levelEvent(ILnet/minecraft/util/math/BlockPos;I)V"), 
			method = "tick()V", 
			cancellable = true)
	private void entitySpawn(CallbackInfo ci) {
		
		if(ConfigValues.get("limited_spawns_enabled") == 0)
			return;
		
		// Don't count "empty" entities.
		CompoundNBT nbt = new CompoundNBT(); 
    	nbt = ((AbstractSpawner)(Object)this).save(nbt);
    	String entity_string = nbt.get("SpawnData").toString();
    	entity_string = entity_string.substring(entity_string.indexOf("\"") + 1);
    	entity_string = entity_string.substring(0, entity_string.indexOf("\""));
    	if(entity_string.contains("area_effect_cloud"))
    		return;
    	
		spawns++;
	}
	
	@Inject(at = @At(value = "INVOKE_ASSIGN", 
			target = "Lnet/minecraft/entity/EntityType;by(Lnet/minecraft/nbt/CompoundNBT;)Ljava/util/Optional;"), 
			method = "tick()V", 
			cancellable = true)
    private void cancel(CallbackInfo ci) {
		
		if(ConfigValues.get("limited_spawns_enabled") == 0)
			return;
		
		AbstractSpawner logic = (AbstractSpawner)(Object)this;
		BlockPos pos = logic.getPos();
		World world = logic.getLevel();
		
		world.getBlockEntity(pos).setChanged();
		world.sendBlockUpdated(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
		
        if (spawns >= ConfigValues.get("limited_spawns_amount")) {
        	// Disable the spawner AND remove egg.
        	CompoundNBT nbt = new CompoundNBT(); 
        	nbt = logic.save(nbt);
        	nbt.putShort("RequiredPlayerRange", (short) 0);
        	logic.load(nbt);
        	logic.setEntityId(EntityType.AREA_EFFECT_CLOUD);
        	world.levelEvent(WorldEvents.LAVA_EXTINGUISH, pos, 0);
        	ci.cancel();
        }
    }
	
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundNBT;getShort(Ljava/lang/String;)S"), method = "load")
	private void load(CompoundNBT nbt, CallbackInfo info) {

		if(ConfigValues.get("limited_spawns_enabled") == 0)
			return;
		
		spawns = nbt.getShort("spawns");
	}
	
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundNBT;putShort(Ljava/lang/String;S)V"), method = "save")
	private void save(CompoundNBT nbt, CallbackInfoReturnable<CompoundNBT> info) {
		
		if(ConfigValues.get("limited_spawns_enabled") == 0)
			return;
		
		nbt.putShort("spawns", spawns);
	}
}
