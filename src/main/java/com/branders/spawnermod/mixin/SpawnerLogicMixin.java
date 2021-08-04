package com.branders.spawnermod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.branders.spawnermod.config.ConfigValues;

import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.MobSpawnerLogic;
import net.minecraft.world.World;

/**
 * 	Implements a limit to how many spawns a spawner can do. Only if enabled in config!
 * 
 * 	@author Anders <Branders> Blomqvist 
 */
@Mixin(MobSpawnerLogic.class)
public class SpawnerLogicMixin {
	
	private short spawns = 0;
	
	@Inject(at = @At(value = "INVOKE", 
			target = "Lnet/minecraft/world/World;syncWorldEvent(ILnet/minecraft/util/math/BlockPos;I)V"), 
			method = "update()V", 
			cancellable = true)
	private void entitySpawn(CallbackInfo ci) {
		
		if(ConfigValues.get("limited_spawns_enabled") == 0)
			return;
		
		// Don't count "empty" entities.
		CompoundTag nbt = new CompoundTag(); 
    	nbt = ((MobSpawnerLogic)(Object)this).toTag(nbt);
    	String entity_string = nbt.get("SpawnData").toString();
    	entity_string = entity_string.substring(entity_string.indexOf("\"") + 1);
    	entity_string = entity_string.substring(0, entity_string.indexOf("\""));
    	if(entity_string.contains("area_effect_cloud"))
    		return;
    	
		spawns++;
	}
	
	@Inject(at = @At(value = "INVOKE_ASSIGN", 
			target = "Lnet/minecraft/entity/EntityType;fromTag(Lnet/minecraft/nbt/CompoundTag;)Ljava/util/Optional;"), 
			method = "update()V", 
			cancellable = true)
    public void cancel(CallbackInfo ci) {
		
		if(ConfigValues.get("limited_spawns_enabled") == 0)
			return;
		
		World world = ((MobSpawnerLogic)(Object)this).getWorld();
		BlockPos pos = ((MobSpawnerLogic)(Object)this).getPos();
		
		world.getBlockEntity(pos).markDirty();
		world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
		
        if (spawns >= ConfigValues.get("limited_spawns_amount")) {
        	// Disable the spawner AND remove egg.
        	CompoundTag nbt = new CompoundTag(); 
        	nbt = ((MobSpawnerLogic)(Object)this).toTag(nbt);
        	nbt.putShort("RequiredPlayerRange", (short) 0);
        	((MobSpawnerLogic)(Object)this).fromTag(nbt);
        	((MobSpawnerLogic)(Object)this).setEntityId(EntityType.AREA_EFFECT_CLOUD);
        	world.syncWorldEvent(1501, pos, 0);
        	ci.cancel();
        }
    }
	
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;getShort(Ljava/lang/String;)S"), method = "fromTag")
	private void fromTag(CompoundTag nbt, CallbackInfo info) {

		if(ConfigValues.get("limited_spawns_enabled") == 0)
			return;
		
		spawns = nbt.getShort("spawns");
	}
	
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;putShort(Ljava/lang/String;S)V"), method = "toTag")
	private void writeNbt(CompoundTag nbt, CallbackInfoReturnable<CompoundTag> info) {
		
		if(ConfigValues.get("limited_spawns_enabled") == 0)
			return;
		
		nbt.putShort("spawns", spawns);
	}
}
