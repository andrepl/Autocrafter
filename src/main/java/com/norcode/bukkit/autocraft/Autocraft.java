package com.norcode.bukkit.autocraft;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Dropper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashSet;

public class Autocraft extends JavaPlugin implements Listener {
    private static EnumSet<BlockFace> sides = EnumSet.of(BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH);
    private static final int itemFrameEntityId = EntityType.ITEM_FRAME.getTypeId();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    };

    public void printInventoryContents(Inventory inv) {
        String s = "";
        for (int i=0;i<inv.getSize();i++) {
            ItemStack is = inv.getItem(i);
            if (is == null) {
                s += "null, ";
            } else {
                s += is + ", ";
            }
        }
        getLogger().info(s);
    }
    
    @EventHandler(ignoreCancelled=true)
    public void onDropperMove(InventoryMoveItemEvent event) {
        if (event.getSource().getHolder() instanceof Dropper) {
            final Dropper dropper = ((Dropper) event.getSource().getHolder());
            ItemFrame frame = getAttachedFrame(dropper.getBlock());
            ItemStack result = null;
            Recipe recipe = null;
            if (frame != null && frame.getItem() != null && !frame.getItem().getType().equals(Material.AIR)) {
                if (event.getItem().equals(frame.getItem())) {
                    return;
                }
                boolean crafted = false;
                ItemStack[] ingredients = null;
                for (Recipe r: getServer().getRecipesFor(frame.getItem())) {
                    if (r instanceof ShapedRecipe || r instanceof ShapelessRecipe) {
                        ingredients = CraftAttempt.getIngredients(r).toArray(new ItemStack[0]);
                        Inventory clone = CraftAttempt.cloneInventory(this, dropper.getInventory());
                        //clone.addItem(event.getItem());
                        crafted = true;
                        for (ItemStack ing: ingredients) {
                            if (!CraftAttempt.removeItem(clone, ing.getType(), ing.getData().getData(), ing.getAmount())) {
                                crafted = false;
                                break;
                            }
                        }
                        if (crafted) {
                            recipe = r;
                            result = r.getResult().clone();
                            break;
                        }
                    }
                }
                if (!crafted) {
                    getLogger().info(dropper.getLocation() + " -> Cancelled event, couldn't craft");
                    event.setCancelled(true);
                } else {
                    getLogger().info(dropper.getLocation() + " -> Successfully crafted: " + result + ", dispensing");
                    event.setItem(result);
                    final Recipe finalRecipe = recipe;
                    getServer().getScheduler().runTaskLater(this,  new Runnable() {
                        public void run () {
                            for (ItemStack ing: CraftAttempt.getIngredients(finalRecipe)) {
                                CraftAttempt.removeItem(dropper.getInventory(), ing.getType(), ing.getData().getData(), ing.getAmount());
                            }
                        }
                    }, 0);
                }
            }
        }
    }
    
    @EventHandler(ignoreCancelled=true)
    public void onDropperFire(BlockDispenseEvent event) {
        if (event.getBlock().getType().equals(Material.DROPPER)) {
            getLogger().info(event.getBlock().getLocation() + " -> firing " + event.getItem());
            final Dropper dropper = ((Dropper) event.getBlock().getState());
            ItemFrame frame = getAttachedFrame(event.getBlock());
            ItemStack result = null;
            Recipe recipe = null;
            if (frame != null && frame.getItem() != null && !frame.getItem().getType().equals(Material.AIR)) {
                if (event.getItem().equals(frame.getItem())) {
                    return;
                }
                boolean crafted = false;
                ItemStack[] ingredients = null;
                for (Recipe r: getServer().getRecipesFor(frame.getItem())) {
                    if (r instanceof ShapedRecipe || r instanceof ShapelessRecipe) {
                        ingredients = CraftAttempt.getIngredients(r).toArray(new ItemStack[0]);
                        Inventory clone = CraftAttempt.cloneInventory(this, dropper.getInventory());
                        clone.addItem(event.getItem());
                        crafted = true;
                        for (ItemStack ing: ingredients) {
                            if (!CraftAttempt.removeItem(clone, ing.getType(), ing.getData().getData(), ing.getAmount())) {
                                crafted = false;
                                break;
                            }
                        }
                        if (crafted) {
                            recipe = r;
                            result = r.getResult().clone();
                            break;
                        }
                    }
                }
                if (!crafted) {
                    getLogger().info(event.getBlock().getLocation() + " -> Cancelled event, couldn't craft");
                    event.setCancelled(true);
                } else {
                    getLogger().info(event.getBlock().getLocation() + " -> Successfully crafted: " + result + ", dispensing");
                    event.setItem(result);
                    final Recipe finalRecipe = recipe;
                    getServer().getScheduler().runTaskLater(this,  new Runnable() {
                        public void run () {
                            for (ItemStack ing: CraftAttempt.getIngredients(finalRecipe)) {
                                CraftAttempt.removeItem(dropper.getInventory(), ing.getType(), ing.getData().getData(), ing.getAmount());
                            }
                        }
                    }, 0);
                }
            }
        }
    }
    
    public ItemFrame getAttachedFrame(Block block) {
        Location dropperLoc = new Location(block.getWorld(), block.getX()+0.5, block.getY()+0.5, block.getZ()+0.5);
        Chunk thisChunk = block.getChunk();
        Chunk thatChunk = null;
        HashSet<Chunk> chunksToCheck = new HashSet<Chunk>();
        chunksToCheck.add(thisChunk);
        for (BlockFace side: sides) {
            thatChunk = block.getRelative(side).getChunk();
            if (!thisChunk.equals(thatChunk)) {
                chunksToCheck.add(thatChunk);
            }
        }
        for (Chunk c: chunksToCheck) {
            for (Entity e: c.getEntities()) {
                if (e.getType().getTypeId() == itemFrameEntityId  && e.getLocation().distanceSquared(dropperLoc) == 0.31640625) {
                    return ((ItemFrame) e);
                }
            }
        }
        return null;
    }
}