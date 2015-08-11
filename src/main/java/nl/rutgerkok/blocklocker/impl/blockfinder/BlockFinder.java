package nl.rutgerkok.blocklocker.impl.blockfinder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nl.rutgerkok.blocklocker.BlockData;
import nl.rutgerkok.blocklocker.ProtectionSign;
import nl.rutgerkok.blocklocker.SignParser;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.material.Attachable;
import org.bukkit.material.MaterialData;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

/**
 * Finds blocks that logically belong together, like the other half of a chest,
 * the attached signs, etc.
 *
 */
public abstract class BlockFinder {

    public static final BlockFace[] CARDINAL_FACES = { BlockFace.NORTH, BlockFace.EAST,
                BlockFace.SOUTH, BlockFace.WEST };
    private static final BlockFace[] SIGN_ATTACHMENT_FACES = { BlockFace.NORTH, BlockFace.EAST,
                BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP };
    public static final BlockFace[] VERTICAL_FACES = { BlockFace.UP, BlockFace.DOWN };

    /**
     * Creates a new block finder.
     * 
     * @param parser
     *            The parser of signs.
     * @param connectContainers
     *            Whether containers must be connected.
     * @return The block finder.
     */
    public static BlockFinder create(SignParser parser, boolean connectContainers) {
        if (connectContainers) {
            return new ConnectedContainersBlockFinder(parser);
        } else {
            return new SeparateContainersBlockFinder(parser);
        }
    }

    protected final SignParser parser;

    BlockFinder(SignParser parser) {
        this.parser = Preconditions.checkNotNull(parser);
    }

    /**
     * Finds all attached signs to a block, that are valid protection signs.
     *
     * @param block
     *            The block to check attached signs on.
     * @return The signs.
     */
    public Collection<ProtectionSign> findAttachedSigns(Block block) {
        Collection<ProtectionSign> signs = new ArrayList<ProtectionSign>();
        for (BlockFace face : SIGN_ATTACHMENT_FACES) {
            Block atPosition = block.getRelative(face);
            Material material = atPosition.getType();
            if (material != Material.WALL_SIGN && material != Material.SIGN_POST) {
                continue;
            }
            Sign sign = (Sign) atPosition.getState();
            if (!isAttachedSign(sign, atPosition, block)) {
                continue;
            }
            Optional<ProtectionSign> parsedSign = parser.parseSign(sign);
            if (parsedSign.isPresent()) {
                signs.add(parsedSign.get());
            }
        }
        return signs;
    }

    /**
     * Finds all attached signs to a block, that are valid protection signs.
     *
     * @param blocks
     *            The blocks to check attached signs on.
     * @return The signs.
     */
    public Collection<ProtectionSign> findAttachedSigns(Collection<Block> blocks) {
        if (blocks.size() == 1) {
            // Avoid creating a builder, iterator and extra set
            return findAttachedSigns(blocks.iterator().next());
        }
    
        ImmutableSet.Builder<ProtectionSign> signs = ImmutableSet.builder();
        for (Block block : blocks) {
            signs.addAll(findAttachedSigns(block));
        }
        return signs.build();
    }

    /**
     * Searches for containers of the same type attached to this container.
     *
     * @param block
     *            The container.
     * @return List of attached containers, including the given container.
     */
    public abstract List<Block> findContainerNeighbors(Block block);

    /**
     * Gets the block that supports the given block. If the returned block is
     * destroyed, the given block is destroyed too.
     *
     * For blocks that are self-supporting (most blocks in Minecraft), the
     * method returns the block itself.
     * 
     * @param block
     *            The block.
     * @return The block the given block is attached on.
     */
    public Block findSupportingBlock(Block block) {
        MaterialData data = BlockData.get(block);
        if (data instanceof Attachable) {
            return block.getRelative(((Attachable) data).getAttachedFace());
        }
        return block;
    }

    /**
     * Gets the parser for signs.
     *
     * @return The parser.
     */
    public SignParser getSignParser() {
        return parser;
    }

    /**
     * Checks if the sign at the given position is attached to the container.
     * Doens't check the text on the sign.
     *
     * @param sign
     *            The sign to check.
     * @param signBlock
     *            The block the sign is on ({@link Block#getState()}
     *            {@code .equals(sign)} must return true)
     * @param attachedTo
     *            The block the sign must be attached to. If this is not the
     *            case, the method returns false.
     * @return True if the direction and header of the sign are valid, false
     *         otherwise.
     */
    private boolean isAttachedSign(Sign sign, Block signBlock, Block attachedTo) {
        BlockFace requiredFace = signBlock.getFace(attachedTo);
        MaterialData materialData = sign.getData();
        BlockFace actualFace = ((org.bukkit.material.Sign) materialData).getAttachedFace();
        return (actualFace == requiredFace);
    }

}