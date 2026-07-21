package net.ceziak.trailbound.block;

import com.mojang.serialization.MapCodec;
import net.ceziak.trailbound.block.entity.CookingPotBlockEntity;
import net.ceziak.trailbound.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;

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
            Block.box(2, 0, 2, 14, 7, 14);

    private static final VoxelShape LID_SHAPE =
            Block.box(2, 0, 2, 14, 9, 14);

    private static final int HEAT_BAR_LENGTH = 20;

    public CookingPotBlock(
            BlockBehaviour.Properties properties
    ) {
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
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(
            BlockState state
    ) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return new CookingPotBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity>
    BlockEntityTicker<T> getTicker(
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
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        BlockPos placedPos = context.getClickedPos();

        boolean supported =
                context.getLevel()
                        .getBlockState(placedPos.below())
                        .is(BlockTags.CAMPFIRES);

        return defaultBlockState()
                .setValue(
                        FACING,
                        context.getHorizontalDirection()
                                .getOpposite()
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
        super.setPlacedBy(
                level,
                pos,
                state,
                placer,
                stack
        );

        if (level.isClientSide()) {
            return;
        }

        if (!(level.getBlockEntity(pos)
                instanceof CookingPotBlockEntity pot)) {
            return;
        }

        if (state.is(ModBlocks.WATER_POT_BLOCK.get())) {
            pot.setWaterAmount(
                    CookingPotBlockEntity.MAX_WATER
            );
        } else {
            pot.setWaterAmount(0);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        /*
         * Sneak-right-click adds or removes the lid.
         */
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                level.setBlock(
                        pos,
                        state.cycle(LID),
                        Block.UPDATE_ALL
                );
            }

            return InteractionResult.sidedSuccess(
                    level.isClientSide()
            );
        }

        /*
         * Normal empty-hand right-click reports the contents.
         */
        if (!level.isClientSide()
                && level.getBlockEntity(pos)
                instanceof CookingPotBlockEntity pot) {
            displayStatus(player, pot);
        }

        return InteractionResult.sidedSuccess(
                level.isClientSide()
        );
    }

    private static void displayStatus(
            Player player,
            CookingPotBlockEntity pot
    ) {
        Component message;

        if (!pot.hasWater()) {
            message = Component.literal("The pot is empty.")
                    .withStyle(ChatFormatting.GRAY);

        } else if (pot.isHot()) {
            message = createHeatBar(100)
                    .append(
                            Component.literal(" Boiling!")
                                    .withStyle(
                                            ChatFormatting.RED,
                                            ChatFormatting.BOLD
                                    )
                    );

        } else if (pot.isHeating()) {
            message = createHeatBar(
                    pot.getHeatPercentage()
            );

        } else {
            message = createHeatBar(0)
                    .append(
                            Component.literal(" Cold")
                                    .withStyle(ChatFormatting.AQUA)
                    );
        }

        player.displayClientMessage(message, true);
    }

    private static MutableComponent createHeatBar(int percentage) {
        int clampedPercentage = Math.max(
                0,
                Math.min(100, percentage)
        );

        int filledSegments = Math.round(
                clampedPercentage
                        / 100.0F
                        * HEAT_BAR_LENGTH
        );

        int emptySegments =
                HEAT_BAR_LENGTH - filledSegments;

        ChatFormatting heatColour =
                getHeatColour(clampedPercentage);

        MutableComponent bar = Component.empty();

        bar.append(
                Component.literal("[")
                        .withStyle(ChatFormatting.DARK_GRAY)
        );

        if (filledSegments > 0) {
            bar.append(
                    Component.literal("■".repeat(filledSegments))
                            .withStyle(heatColour)
            );
        }

        if (emptySegments > 0) {
            bar.append(
                    Component.literal("■".repeat(emptySegments))
                            .withStyle(ChatFormatting.DARK_GRAY)
            );
        }

        bar.append(
                Component.literal("]")
                        .withStyle(ChatFormatting.DARK_GRAY)
        );

        return bar;
    }

    private static ChatFormatting getHeatColour(
            int percentage
    ) {
        if (percentage < 25) {
            return ChatFormatting.AQUA;
        }

        if (percentage < 50) {
            return ChatFormatting.YELLOW;
        }

        if (percentage < 75) {
            return ChatFormatting.GOLD;
        }

        return ChatFormatting.RED;
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

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.62;
        double z = pos.getZ() + 0.5;

        level.addParticle(
                ParticleTypes.CLOUD,
                x,
                y,
                z,
                0.0,
                0.025,
                0.0
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
            boolean supported =
                    neighbourState.is(BlockTags.CAMPFIRES);

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