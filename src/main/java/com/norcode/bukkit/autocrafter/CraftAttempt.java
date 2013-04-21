package com.norcode.bukkit.autocrafter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public class CraftAttempt {

    private List<ItemStack> toRemove;
    private final Autocrafter plugin;
    private final Recipe recipe;
    private Inventory inventory;
    private ItemStack result = null;
    private Boolean canCraft = null;
    private Inventory cloneInv = null;
    private ItemStack fired = null;
    public CraftAttempt(Autocrafter plugin, Recipe r, Inventory inventory, ItemStack fired) {
        this.recipe = r;
        this.fired = fired;
        this.inventory = inventory;
        this.plugin = plugin;
    }

    public ItemStack getResult() {
        return result;
    }

    public static List<ItemStack> getIngredients(Recipe recipe) {
        List<ItemStack> ingredients = new ArrayList<ItemStack>();
        if (recipe instanceof ShapedRecipe) {
            ShapedRecipe sr = ((ShapedRecipe) recipe);
            String[] shape = sr.getShape();
            ItemStack stack;
            for (String row: shape) {
                for (int i=0;i<row.length();i++) {
                    stack = sr.getIngredientMap().get(row.charAt(i));
                    for (ItemStack ing: ingredients) {
                        int mss = ing.getType().getMaxStackSize();
                        if (ing.isSimilar(stack) && ing.getAmount() < mss) {
                            int canAdd = mss - ing.getAmount();
                            int add = Math.min(canAdd, stack.getAmount());
                            ing.setAmount(ing.getAmount() + add);
                            int remaining = stack.getAmount() - add;
                            if (remaining >= 1) {
                                stack.setAmount(remaining);
                            } else {
                                stack = null;
                                break;
                            }
                        }
                    }
                    if (stack != null && stack.getAmount() > 0) {
                        ingredients.add(stack);
                    }
                }
            }
        } else if (recipe instanceof ShapelessRecipe) {
            for (ItemStack i: ((ShapelessRecipe) recipe).getIngredientList()) {
                for (ItemStack ing: ingredients) {
                    int mss = ing.getType().getMaxStackSize();
                    if (ing.isSimilar(i) && ing.getAmount() < mss) {
                        int canAdd = mss - ing.getAmount();
                        ing.setAmount(ing.getAmount() + Math.min(canAdd, i.getAmount()));
                        int remaining = i.getAmount() - Math.min(canAdd, i.getAmount());
                        if (remaining >= 1) {
                            i.setAmount(remaining);
                        } else {
                            break;
                        }
                    }
                }
                if (i.getAmount() > 0) {
                    ingredients.add(i);
                }
            }
        }
        return ingredients;
    }

    public static Inventory cloneInventory(JavaPlugin plugin, Inventory inv) {
        Inventory inv2 = plugin.getServer().createInventory(null, inv.getSize());
        for (int i=0;i<inv.getSize();i++) {
            inv2.setItem(i, inv.getItem(i) == null ? null : inv.getItem(i).clone());
        }
        return inv2;
    }

    public static boolean removeItem(Inventory inv, Material mat, int data, int qty) {
        ItemStack s;
        int remd = 0;
        
        HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
        for (int i=0;i<inv.getSize();i++) {
            s = inv.getItem(i);
        
            if (s == null) {
        
                continue;
            }
            
            if (remd < qty) {
        
                if (s.getType().equals(mat) && (data == -1 || data == s.getData().getData())) {
                    int take = Math.min(s.getAmount(), (qty-remd));
                    map.put(i, take);
                    remd += take;
        
                    if (take == s.getAmount()) {
                        inv.setItem(i, null);
                    } else {
                        s.setAmount(s.getAmount() - take);
                    }
                } else {
                }
            }
        }
        if (remd != qty) {
            return false;
        } else {
            return true;
        }
    }

    public boolean canCraft() {
        if (canCraft == null) {
            List<ItemStack> ingredients = getIngredients(this.recipe);
            cloneInv = cloneInventory(this.plugin, inventory);
            cloneInv.addItem(this.fired.clone());
            result = recipe.getResult().clone();
            canCraft = true;
            for (ItemStack stack: ingredients) {
                if (!removeItem(cloneInv, stack.getType(), stack.getData().getData(), stack.getAmount())) {
                    canCraft = false;
                    break;
                }
            }
            
        }
        return canCraft;
    }

    public void removeItems() {
        for (ItemStack stack: getIngredients(this.recipe)) {
            removeItem(inventory, stack.getType(), stack.getData().getData(), stack.getAmount());
        }
        
    }
}
