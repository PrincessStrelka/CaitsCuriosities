package caittastic.caitsmod.init.item;

import caittastic.caitsmod.init.SoundEventsInit;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class DestructionCatalyst extends Item {
    public DestructionCatalyst(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isDamageable(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(new TranslatableComponent("tooltip.caitsmod.DestructionCatalyst.tooltip"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand pUsedHand) {
        //variables!
        ItemStack stack = player.getItemInHand(pUsedHand);
        BlockPos pos = player.blockPosition();
        CompoundTag tag = new CompoundTag();
        boolean shiftKeyDown = player.isShiftKeyDown();

        //if the world is clientside,  and the player is crouching, charge the item
        if(!world.isClientSide){
            if(!stack.hasTag()){
                tag.putInt("charge", 1);
                stack.setTag(tag);
            }

            if(shiftKeyDown){

                switch (stack.getTag().getInt("charge")){
                    case 1 :
                        stack.getTag().putInt("charge", 4);
                        player.sendMessage(new TextComponent("Charge: "+stack.getTag().getInt("charge")), Util.NIL_UUID);
                        break;
                    case 4 :
                        stack.getTag().putInt("charge", 9);
                        player.sendMessage(new TextComponent("Charge: "+stack.getTag().getInt("charge")), Util.NIL_UUID);
                        break;
                    case 9 :
                        stack.getTag().putInt("charge", 16);
                        player.sendMessage(new TextComponent("Charge: "+stack.getTag().getInt("charge")), Util.NIL_UUID);
                        break;
                    case 16 :
                        stack.getTag().putInt("charge", 1);
                        player.sendMessage(new TextComponent("Charge: "+stack.getTag().getInt("charge")), Util.NIL_UUID);
                        break;
                }
            }
        }

        //if the player is crouching, play the charge noise
        if (shiftKeyDown){
            switch (stack.getTag().getInt("charge")) {
                case 1:
                    world.playSound(player, pos, SoundEventsInit.ITEM_CHARGE.get(), SoundSource.PLAYERS, 0.25F, 1);
                case 4:
                    world.playSound(player, pos, SoundEventsInit.ITEM_CHARGE.get(), SoundSource.PLAYERS, 0.25F, 1.125f);
                case 9:
                    world.playSound(player, pos, SoundEventsInit.ITEM_CHARGE.get(), SoundSource.PLAYERS, 0.25F, 1.25f);
                case 16:
                    world.playSound(player, pos, SoundEventsInit.ITEM_CHARGE.get(), SoundSource.PLAYERS, 0.25F, 1.375f);
            }
        }







        return super.use(world, player, pUsedHand);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        //variables
        Level world = context.getLevel();
        context.getPlayer().getDirection();

        //plays the breaking sound if can try breaking
        if(!context.getPlayer().isShiftKeyDown()){
            world.playSound(context.getPlayer(), context.getClickedPos(), SoundEventsInit.DESTRUCTION_CATALYST_USE.get(), SoundSource.BLOCKS, 1.0F, 1);
        }
        if(!world.isClientSide){
            //if player is not crouching, super break!
            if(!context.getPlayer().isShiftKeyDown()){
                superBreaker(context, stack.getTag().getInt("charge"));
            }
        }
        return super.onItemUseFirst(stack, context);
    }

    //  helpful :)
    //checks if a block should be breakable based on vanilla rules

    private void superBreaker(UseOnContext context, int charge) {
        Level world = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        BlockPos newBlockPos;
        int[] iterateWidthHeight = {-1, 0, 1};
        Direction clickedFace = context.getClickedFace();
        List<ItemStack> drops = new LinkedList<>();

        for (int width : iterateWidthHeight) {
            for (int height : iterateWidthHeight) {
                for (int depth = 0; depth <= (charge - 1); depth++) {
                    newBlockPos = offsetBlock(blockPos, clickedFace, width, height, depth);
                    BlockState newClickedBlock = world.getBlockState(newBlockPos);
                    drops.addAll(breakAndAddLoTooList(newBlockPos, context));

                }
            }
        }
        compressStacksAndGiveToPlayer(drops, world, blockPos);
    }

    private BlockPos offsetBlock(BlockPos blockToOffset, Direction clickedDirection, int width, int height, int depth) {
        int negativeDepth = depth * -1;
        BlockPos newBlockPos = blockToOffset;
        switch (clickedDirection) {
            case SOUTH:
                newBlockPos = blockToOffset.offset(width, height, negativeDepth);break;
            case NORTH:
                newBlockPos = blockToOffset.offset(width, height, depth);break;
            case EAST:
                newBlockPos = blockToOffset.offset(negativeDepth, width, height);break;
            case WEST:
                newBlockPos = blockToOffset.offset(depth, width, height);break;
            case UP:
                newBlockPos = blockToOffset.offset(width, negativeDepth, height);break;
            case DOWN:
                newBlockPos = blockToOffset.offset(width, depth, height);break;
        }
        return newBlockPos;
    }

    private boolean blockValidToBreak(BlockPos clickedBlockPos, UseOnContext context, Level world) {
        ServerPlayer player = (ServerPlayer)context.getPlayer();
        BlockState clickedBlock = world.getBlockState(clickedBlockPos);

        return clickedBlock.getDestroySpeed(world, context.getClickedPos()) > 0f &&
                clickedBlock.getDestroySpeed(world, context.getClickedPos()) <= 50 &&
                isBlockSpawnProtected(context.getPlayer(),context.getClickedPos())&&
                ForgeHooks.onBlockBreakEvent(player.getCommandSenderWorld(), player.gameMode.getGameModeForPlayer(), player, clickedBlockPos) != -1;
    }

    //i took this code basically exactly from dshadowwolfs bit
    private boolean isBlockSpawnProtected(Player player, BlockPos pos) {
        if (ServerLifecycleHooks.getCurrentServer().isUnderSpawnProtection((ServerLevel) player.getCommandSenderWorld(), pos, player)) {
            return false;
        }
        return Arrays.stream(Direction.values()).allMatch(e -> player.mayUseItemAt(pos, e, ItemStack.EMPTY));
    }

    private List<ItemStack> breakAndAddLoTooList( BlockPos bopo, UseOnContext context){
        List<ItemStack> drops = new LinkedList<>();
        Level world = context.getLevel();
        BlockState newClickedBlock = world.getBlockState(bopo);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();




        if(blockValidToBreak(bopo, context, world)){
            world.destroyBlock(bopo, false);

            stack.hurtAndBreak(1, player, (player1) -> {
                player1.broadcastBreakEvent(EquipmentSlot.MAINHAND);
            });

            return Block.getDrops(newClickedBlock, (ServerLevel) world, bopo, world.getBlockEntity(bopo), context.getPlayer(), context.getItemInHand() );
        }

        return drops;
    }

    private void compressStacksAndGiveToPlayer(List<ItemStack> drops, Level world, BlockPos bloPo) {
        for (int i = 0; i < drops.size(); i++) {
            ItemStack stackOne = drops.get(i);
            if(!stackOne.isEmpty()){
                for (int j = i + 1; j < drops.size(); j++) {
                    ItemStack stackTwo = drops.get(j);
                    if(ItemHandlerHelper.canItemStacksStack(stackOne, stackTwo)){
                        stackOne.grow(stackTwo.getCount());
                        drops.set(j, ItemStack.EMPTY);
                    }
                }
            }
        }

        drops.removeIf(ItemStack::isEmpty);

        int itemCreateX = bloPo.getX();
        int itemCreateY = bloPo.getY();
        int itemCreateZ = bloPo.getZ();

        for (ItemStack item : drops) {
            world.addFreshEntity(new ItemEntity(world,itemCreateX,itemCreateY,itemCreateZ, item));
        }

    }
}



