package net.ceziak.trailbound.block;

import com.mojang.serialization.MapCodec;
import net.ceziak.trailbound.block.entity.CookingPotBlockEntity;
import net.ceziak.trailbound.block.entity.ModBlockEntities;
import net.ceziak.trailbound.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CookingPotBlock extends BaseEntityBlock {

    public static final MapCodec<CookingPotBlock> CODEC =
            simpleCodec(CookingPotBlock::new);

    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;

    public static final BooleanProperty LID =
            BooleanProperty.create("lid");

    public static final BooleanProperty HEATED =
            BooleanProperty.create("heated");

    public static final BooleanProperty SUPPORTED =
            BooleanProperty.create("supported");

    private static final VoxelShape OPEN_SHAPE =
            Block.box(
                    2.0D,
                    0.0D,
                    2.0D,
                    14.0D,
                    7.0D,
                    14.0D
            );

    private static final VoxelShape LID_SHAPE =
            Block.box(
                    2.0D,
                    0.0D,
                    2.0D,
                    14.0D,
                    9.0D,
                    14.0D
            );

    public CookingPotBlock(BlockBehaviour.Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(LID, false)
                        .setValue(HEATED, false)
                        .setValue(SUPPORTED, false)
        );
    }

    @Override
    protected MapCodec<CookingPotBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CookingPotBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide()) {
            return null;
        }

        return createTickerHelper(
                type,
                ModBlockEntities.COOKING_POT.get(),
                CookingPotBlockEntity::serverTick
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos placedPos = context.getClickedPos();

        boolean supported = context.getLevel()
                .getBlockState(placedPos.below())
                .is(BlockTags.CAMPFIRES);

        return defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection().getOpposite()
                )
                .setValue(SUPPORTED, supported);
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide()) {
            return;
        }

        if (!(level.getBlockEntity(pos)
                instanceof CookingPotBlockEntity pot)) {
            return;
        }

        if (state.is(ModBlocks.WATER_POT_BLOCK.get())) {
            pot.setWaterAmount(CookingPotBlockEntity.MAX_WATER);
        } else {
            pot.setWaterAmount(0);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos)
                instanceof CookingPotBlockEntity pot)) {
            return ItemInteractionResult
                    .PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        /*
         * Sneak-use always controls the lid, even while the player
         * is holding a bowl, bottle, bucket or ingredient.
         */
        if (player.isShiftKeyDown()) {
            toggleLid(
                    state,
                    level,
                    pos
            );

            return ItemInteractionResult.sidedSuccess(
                    level.isClientSide()
            );
        }

        /*
         * Finished meals are handled before bottles, buckets or
         * ingredients. A recipe may require a serving container,
         * such as a bowl for rabbit stew.
         */
        if (pot.hasResult()) {
            if (state.getValue(LID)) {
                return ItemInteractionResult.FAIL;
            }

            if (!pot.requiresServingContainer()) {
                return ItemInteractionResult.FAIL;
            }

            if (!pot.canTakeResultWith(stack)) {
                return ItemInteractionResult.FAIL;
            }

            return takeCookedResultWithContainer(
                    stack,
                    level,
                    pos,
                    player,
                    hand,
                    pot
            );
        }

        boolean glassBottle = stack.is(Items.GLASS_BOTTLE);
        boolean waterBottle = isWaterBottle(stack);
        boolean waterBucket = stack.is(Items.WATER_BUCKET);
        boolean ingredient = stack.is(ModTags.Items.POT_INGREDIENTS);

        if (!glassBottle
                && !waterBottle
                && !waterBucket
                && !ingredient) {
            return ItemInteractionResult
                    .PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (state.getValue(LID)) {
            return ItemInteractionResult.FAIL;
        }

        if (glassBottle) {
            return takeWaterWithBottle(
                    stack,
                    level,
                    pos,
                    player,
                    hand,
                    pot
            );
        }

        if (waterBottle) {
            return addWaterFromBottle(
                    stack,
                    level,
                    pos,
                    player,
                    hand,
                    pot
            );
        }

        if (waterBucket) {
            return fillFromWaterBucket(
                    stack,
                    level,
                    pos,
                    player,
                    hand,
                    pot
            );
        }

        return insertIngredient(
                stack,
                level,
                player,
                pot
        );
    }

    private static ItemInteractionResult takeCookedResultWithContainer(
            ItemStack containerStack,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            CookingPotBlockEntity pot
    ) {
        if (!level.isClientSide()) {
            ItemStack cookedResult = pot.takeResult();

            if (cookedResult.isEmpty()) {
                return ItemInteractionResult.FAIL;
            }

            ItemStack handResult = ItemUtils.createFilledResult(
                    containerStack,
                    player,
                    cookedResult
            );

            player.setItemInHand(hand, handResult);

            level.playSound(
                    null,
                    pos,
                    SoundEvents.ITEM_PICKUP,
                    SoundSource.BLOCKS,
                    0.8F,
                    1.0F
            );

            player.awardStat(
                    Stats.ITEM_USED.get(
                            containerStack.getItem()
                    )
            );
        }

        return ItemInteractionResult.sidedSuccess(
                level.isClientSide()
        );
    }

    private static ItemInteractionResult takeWaterWithBottle(
            ItemStack glassBottles,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            CookingPotBlockEntity pot
    ) {
        if (!pot.hasWater()) {
            return ItemInteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            ItemStack waterBottle = PotionContents.createItemStack(
                    Items.POTION,
                    Potions.WATER
            );

            ItemStack result = ItemUtils.createFilledResult(
                    glassBottles,
                    player,
                    waterBottle
            );

            player.setItemInHand(hand, result);

            pot.setWaterAmount(
                    pot.getWaterAmount() - 1
            );

            level.playSound(
                    null,
                    pos,
                    SoundEvents.BOTTLE_FILL,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F
            );

            level.gameEvent(
                    player,
                    GameEvent.FLUID_PICKUP,
                    pos
            );

            player.awardStat(
                    Stats.ITEM_USED.get(
                            Items.GLASS_BOTTLE
                    )
            );
        }

        return ItemInteractionResult.sidedSuccess(
                level.isClientSide()
        );
    }

    private static ItemInteractionResult addWaterFromBottle(
            ItemStack waterBottle,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            CookingPotBlockEntity pot
    ) {
        if (pot.getWaterAmount()
                >= CookingPotBlockEntity.MAX_WATER) {
            return ItemInteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            ItemStack result = ItemUtils.createFilledResult(
                    waterBottle,
                    player,
                    new ItemStack(Items.GLASS_BOTTLE)
            );

            player.setItemInHand(hand, result);

            pot.setWaterAmount(
                    pot.getWaterAmount() + 1
            );

            level.playSound(
                    null,
                    pos,
                    SoundEvents.BOTTLE_EMPTY,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F
            );

            level.gameEvent(
                    player,
                    GameEvent.FLUID_PLACE,
                    pos
            );

            player.awardStat(
                    Stats.ITEM_USED.get(
                            Items.POTION
                    )
            );
        }

        return ItemInteractionResult.sidedSuccess(
                level.isClientSide()
        );
    }

    private static ItemInteractionResult fillFromWaterBucket(
            ItemStack waterBucket,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            CookingPotBlockEntity pot
    ) {
        if (pot.getWaterAmount()
                >= CookingPotBlockEntity.MAX_WATER) {
            return ItemInteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            ItemStack result = ItemUtils.createFilledResult(
                    waterBucket,
                    player,
                    new ItemStack(Items.BUCKET)
            );

            player.setItemInHand(hand, result);

            pot.setWaterAmount(
                    CookingPotBlockEntity.MAX_WATER
            );

            level.playSound(
                    null,
                    pos,
                    SoundEvents.BUCKET_EMPTY,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F
            );

            level.gameEvent(
                    player,
                    GameEvent.FLUID_PLACE,
                    pos
            );

            player.awardStat(
                    Stats.ITEM_USED.get(
                            Items.WATER_BUCKET
                    )
            );
        }

        return ItemInteractionResult.sidedSuccess(
                level.isClientSide()
        );
    }

    private static ItemInteractionResult insertIngredient(
            ItemStack stack,
            Level level,
            Player player,
            CookingPotBlockEntity pot
    ) {
        if (pot.hasResult()
                || !pot.hasIngredientSpace()) {
            return ItemInteractionResult.FAIL;
        }

        if (!level.isClientSide()) {
            boolean inserted = pot.addIngredient(stack);

            if (inserted) {
                stack.consume(1, player);
            }
        }

        return ItemInteractionResult.sidedSuccess(
                level.isClientSide()
        );
    }

    private static boolean isWaterBottle(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }

        PotionContents contents = stack.get(
                DataComponents.POTION_CONTENTS
        );

        return contents != null
                && contents.is(Potions.WATER);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            toggleLid(
                    state,
                    level,
                    pos
            );

            return InteractionResult.sidedSuccess(
                    level.isClientSide()
            );
        }

        if (state.getValue(LID)) {
            return InteractionResult.PASS;
        }

        if (!(level.getBlockEntity(pos)
                instanceof CookingPotBlockEntity pot)) {
            return InteractionResult.PASS;
        }

        if (pot.hasResult()) {
            /*
             * Recipes with a serving container must be collected
             * using that item. Container-free recipes may still be
             * collected with an empty hand.
             */
            if (pot.requiresServingContainer()) {
                return InteractionResult.sidedSuccess(
                        level.isClientSide()
                );
            }

            if (!level.isClientSide()) {
                ItemStack result = pot.takeResult();

                player.getInventory()
                        .placeItemBackInInventory(result);
            }

            return InteractionResult.sidedSuccess(
                    level.isClientSide()
            );
        }

        if (!pot.hasIngredients()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            ItemStack removed = pot.removeSelectedIngredient();

            if (!removed.isEmpty()) {
                player.getInventory()
                        .placeItemBackInInventory(removed);
            }
        }

        return InteractionResult.sidedSuccess(
                level.isClientSide()
        );
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock())
                && !level.isClientSide()
                && level.getBlockEntity(pos)
                instanceof CookingPotBlockEntity pot) {

            for (int slot = 0;
                 slot < CookingPotBlockEntity.INGREDIENT_SLOT_COUNT;
                 slot++) {

                ItemStack ingredient = pot.getIngredient(slot);

                if (!ingredient.isEmpty()) {
                    Block.popResource(
                            level,
                            pos,
                            ingredient.copy()
                    );
                }
            }

            if (pot.hasResult()) {
                boolean droppedSourceIngredients = false;

                for (int slot = 0;
                     slot < CookingPotBlockEntity.INGREDIENT_SLOT_COUNT;
                     slot++) {

                    ItemStack sourceIngredient =
                            pot.getResultSourceIngredient(slot);

                    if (sourceIngredient.isEmpty()) {
                        continue;
                    }

                    droppedSourceIngredients = true;

                    Block.popResource(
                            level,
                            pos,
                            sourceIngredient.copy()
                    );
                }

                /*
                 * Compatibility fallback for a cooked pot saved
                 * before source ingredients were stored.
                 */
                if (!droppedSourceIngredients) {
                    ItemStack cookedResult = pot.getResult();

                    if (!cookedResult.isEmpty()) {
                        Block.popResource(
                                level,
                                pos,
                                cookedResult
                        );
                    }
                }
            }
        }

        super.onRemove(
                state,
                level,
                pos,
                newState,
                movedByPiston
        );
    }

    private static void toggleLid(
            BlockState state,
            Level level,
            BlockPos pos
    ) {
        if (level.isClientSide()) {
            return;
        }

        level.setBlock(
                pos,
                state.cycle(LID),
                Block.UPDATE_ALL
        );
    }

    @Override
    public void animateTick(
            BlockState state,
            Level level,
            BlockPos pos,
            RandomSource random
    ) {
        super.animateTick(state, level, pos, random);

        if (!state.getValue(HEATED)
                || state.getValue(LID)) {
            return;
        }

        if (random.nextInt(3) != 0) {
            return;
        }

        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.62D;
        double z = pos.getZ() + 0.5D;

        level.addParticle(
                ParticleTypes.CLOUD,
                x,
                y,
                z,
                0.0D,
                0.025D,
                0.0D
        );
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return state.getValue(LID)
                ? LID_SHAPE
                : OPEN_SHAPE;
    }

    @Override
    protected BlockState rotate(
            BlockState state,
            Rotation rotation
    ) {
        return state.setValue(
                FACING,
                rotation.rotate(state.getValue(FACING))
        );
    }

    @Override
    protected BlockState mirror(
            BlockState state,
            Mirror mirror
    ) {
        return state.rotate(
                mirror.getRotation(state.getValue(FACING))
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(
                FACING,
                LID,
                HEATED,
                SUPPORTED
        );
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighbourState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighbourPos
    ) {
        if (direction == Direction.DOWN) {
            boolean supported = neighbourState.is(
                    BlockTags.CAMPFIRES
            );

            return state.setValue(
                    SUPPORTED,
                    supported
            );
        }

        return super.updateShape(
                state,
                direction,
                neighbourState,
                level,
                pos,
                neighbourPos
        );
    }
}