package guideme.internal.item;

import guideme.compiler.IdUtils;
import guideme.internal.GuideME;
import guideme.internal.GuideMEProxy;
import guideme.internal.GuidebookText;
import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import org.jetbrains.annotations.Nullable;

public class GuideItem extends Item {
    public static final ResourceLocation ID = GuideME.makeId("guide");
    public static final ResourceLocation BASE_MODEL_ID = IdUtils.withPrefixAndSuffix(ID, "item/", "_base");

    public static final String TAG_GUIDE_ID = "guideId";

    public GuideItem() {
        this.setRegistryName(ID);
        this.setTranslationKey(ID.toString());
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        var guideId = getGuideId(stack);
        if (guideId != null) {
            var name = GuideMEProxy.instance().getGuideDisplayName(guideId);
            if (name != null) {
                return name;
            }
        }
        return super.getItemStackDisplayName(stack);
    }

    @Override
    public void addInformation(ItemStack stack, World level, List<String> lines, ITooltipFlag tooltipFlag) {
        var guideId = getGuideId(stack);
        if (guideId != null) {
            GuideMEProxy.instance().addGuideTooltip(
                    guideId,
                    lines,
                    tooltipFlag);
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World level, EntityPlayer player, EnumHand hand) {
        var stack = player.getHeldItem(hand);

        var guideId = getGuideId(stack);

        if (level.isRemote) {
            if (guideId == null) {
                player.sendMessage(GuidebookText.ItemNoGuideId.text());
            } else if (GuideMEProxy.instance().openGuide(player, guideId)) {
                return new ActionResult<>(EnumActionResult.SUCCESS, stack);
            }
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Nullable
    public static ResourceLocation getGuideId(ItemStack stack) {
        var tag = stack.getTagCompound();
        if (tag != null && tag.hasKey(TAG_GUIDE_ID, Constants.NBT.TAG_STRING)) {
            return IdUtils.tryParse(tag.getString(TAG_GUIDE_ID));
        }
        return null;
    }
}
