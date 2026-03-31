package net.darkhax.itemstages;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.darkhax.gamestages.GameStageHelper;
import net.darkhax.gamestages.data.IStageData;
import net.minecraft.client.resources.ReloadListener;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;

public class RestrictionManager extends ReloadListener<Void> {
    
    public static final RestrictionManager INSTANCE = new RestrictionManager();
    
    /**
     * This map contains all registered item restrictions. The map is keyed using the names of
     * the individual stages, and the values are a collection of every restriction that
     * requires the keyed stage. Restrictions with more than one required stage will be mapped
     * to multiple keys. This data structure is used to minimize the processing time required
     * to look up restrictions in {@link #getRestriction(PlayerEntity, IStageData, ItemStack)}.
     */
    private final Multimap<String, Restriction> restrictions = HashMultimap.create();
    
    private final Multimap<String, Restriction> preventInventory = HashMultimap.create();
    
    private final Multimap<String, Restriction> preventEquipment = HashMultimap.create();
    
    private boolean hasBuiltCaches = false;
    
    private void buildCaches () {
        
        if (!this.hasBuiltCaches) {
            
            for (final String stage : this.restrictions.keySet()) {
                
                for (final Restriction restriction : this.restrictions.get(stage)) {
                    
                    if (restriction.shouldPreventInventory()) {
                        
                        this.preventInventory.put(stage, restriction);
                    }
                    
                    if (restriction.shouldPreventEquipment()) {
                        
                        this.preventEquipment.put(stage, restriction);
                    }
                }
            }
            
            this.hasBuiltCaches = true;
        }
    }
    
    @Nullable
    public Restriction getRestriction (PlayerEntity player, ItemStack stack) {
        
        return this.getRestriction(player, GameStageHelper.getPlayerData(player), stack, this.restrictions);
    }
    
    @Nullable
    public Restriction getRestriction (PlayerEntity player, IStageData stageData, ItemStack stack) {
        
        return this.getRestriction(player, stageData, stack, this.restrictions);
    }
    
    @Nullable
    public Restriction getInventoryRestriction (PlayerEntity player, IStageData stageData, ItemStack stack) {
        
        return this.getRestriction(player, stageData, stack, this.preventInventory);
    }

    @Nullable
    public Restriction getInventoryRestriction (PlayerEntity player, IStageData stageData, ItemStack stack, Set<String> missingStages) {

        return this.getRestriction(player, stageData, stack, this.preventInventory, missingStages);
    }
    
    @Nullable
    public Restriction getEquipmentRestriction (PlayerEntity player, IStageData stageData, ItemStack stack) {
        
        return this.getRestriction(player, stageData, stack, this.preventEquipment);
    }

    @Nullable
    public Restriction getEquipmentRestriction (PlayerEntity player, IStageData stageData, ItemStack stack, Set<String> missingStages) {

        return this.getRestriction(player, stageData, stack, this.preventEquipment, missingStages);
    }

    public Set<String> getMissingInventoryStages (PlayerEntity player, IStageData stageData) {

        return this.getMissingStages(player, stageData, this.preventInventory);
    }

    public Set<String> getMissingEquipmentStages (PlayerEntity player, IStageData stageData) {

        return this.getMissingStages(player, stageData, this.preventEquipment);
    }

    private Set<String> getMissingStages (PlayerEntity player, IStageData stageData, Multimap<String, Restriction> restrictionPool) {

        this.buildCaches();

        final Set<String> missingStages = new HashSet<>();

        for (final String stageName : restrictionPool.keySet()) {

            if (!GameStageHelper.hasStage(player, stageData, stageName)) {

                missingStages.add(stageName);
            }
        }

        return missingStages;
    }
    
    @Nullable
    public Restriction getRestriction (PlayerEntity player, IStageData stageData, ItemStack stack, Multimap<String, Restriction> restrictionPool) {
        
        if (!stack.isEmpty()) {
            
            this.buildCaches();
            
            for (final String stageName : restrictionPool.keySet()) {
                
                if (!GameStageHelper.hasStage(player, stageData, stageName)) {
                    
                    for (final Restriction restriction : restrictionPool.get(stageName)) {
                        
                        if (restriction.isRestricted(stack) && !restriction.meetsRequirements(player, stageData)) {
                            
                            return restriction;
                        }
                    }
                }
            }
        }
        
        return null;
    }

    @Nullable
    private Restriction getRestriction (PlayerEntity player, IStageData stageData, ItemStack stack, Multimap<String, Restriction> restrictionPool, Set<String> missingStages) {

        if (!stack.isEmpty() && !missingStages.isEmpty()) {

            this.buildCaches();

            for (final String stageName : missingStages) {

                for (final Restriction restriction : restrictionPool.get(stageName)) {

                    if (restriction.isRestricted(stack) && !restriction.meetsRequirements(player, stageData)) {

                        return restriction;
                    }
                }
            }
        }

        return null;
    }
    
    /**
     * Registers a restriction with the manager. This will map the restriction using each of
     * it's required stages.
     * 
     * @param restriction The restriction to register.
     */
    public void addRestriction (Restriction restriction) {
        
        this.hasBuiltCaches = false;
        this.preventInventory.clear();
        this.preventEquipment.clear();
        restriction.getStages().forEach(stage -> this.restrictions.put(stage, restriction));
    }
    
    @Override
    protected Void prepare (IResourceManager resourceManager, IProfiler profiler) {
        
        // Unused for now.
        return null;
    }
    
    @Override
    protected void apply (Void object, IResourceManager resourceManager, IProfiler profiler) {
        
        this.hasBuiltCaches = false;
        this.restrictions.clear();
        this.preventInventory.clear();
        this.preventEquipment.clear();
    }
}