package smartin.miapi.item.modular.items;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.FireworkRocketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.MathHelper;
import smartin.miapi.item.modular.ModularItem;
import smartin.miapi.modules.properties.DisplayNameProperty;
import smartin.miapi.modules.properties.RarityProperty;
import smartin.miapi.modules.properties.RepairPriority;

public class ModularChestPlate extends ArmorItem implements ModularItem {
    public ModularChestPlate() {
        super(new ModularArmorMaterial(), Type.CHESTPLATE,new Settings());
        ElytraItem elytraItem;
        LivingEntity livingEntity;
        PlayerEntity player;
        ClientPlayerEntity clientPlayerEntity;
        FireworkRocketEntity fireworkRocketEntity;
        FireworkRocketItem fireworkRocketItem;
    }

    @Override
    public Text getName(ItemStack stack) {
        return DisplayNameProperty.getDisplayText(stack);
    }

    @Override
    public boolean isEnchantable(ItemStack itemStack){
        return true;
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return RepairPriority.getRepairValue(stack, ingredient) > 0;
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return RarityProperty.getRarity(stack);
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        return Math.round(13.0F - (float)stack.getDamage() * 13.0F / ModularItem.getDurability(stack));
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        float f = Math.max(0.0F, ((float) ModularItem.getDurability(stack) - (float)stack.getDamage()) / ModularItem.getDurability(stack));
        return MathHelper.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public int getEnchantability() {
        return 1;
    }
}
