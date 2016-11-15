package tardis.common.blocks;

import io.darkcraft.darkcore.mod.abstracts.AbstractBlockContainer;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.oredict.ShapedOreRecipe;
import tardis.TardisMod;
import tardis.common.tileents.LabTileEntity;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class LabBlock extends AbstractBlockContainer
{
	public LabBlock()
	{
		super(false,TardisMod.modName);
	}
	
	@Override
	public TileEntity createNewTileEntity(World w, int m)
	{
		return new LabTileEntity();
	}

	@Override
	public void initData()
	{
		setCreativeTab(TardisMod.cTab);
		setBlockName("Lab");
		setHardness(5.0F);
	}

	@Override
	public void initRecipes()
	{
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(TardisMod.labBlock,1,0),true, "igi","ioi","iri",
				'i', Items.IRON_INGOT,
				'g', Blocks.GLASS,
				'o', Items.GOLD_INGOT,
				'r', Items.REDSTONE));
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public boolean shouldSideBeRendered(IBlockAccess iblockaccess, int i, int j, int k, int l)
	{
	   return false;
	}
	
	@Override
	public boolean renderAsNormalBlock()
    {
		return false;
    }

	@Override
	public boolean isOpaqueCube()
	{
	   return false;
	}
	
	@Override
	public void breakBlock(World w, int x, int y, int z, Block b, int m)
	{
		TileEntity te = w.getTileEntity(x, y, z);
		if(te instanceof LabTileEntity)
			((LabTileEntity)te).dropEverything();
		super.breakBlock(w, x, y, z, b, m);
	}

	@Override
	public Class<? extends TileEntity> getTEClass()
	{
		return LabTileEntity.class;
	}
	
	@Override
    public boolean hasComparatorInputOverride()
    {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int par5)
    {
    	int power = 0;
    	for(ItemStack is : ((LabTileEntity) world.getTileEntity(x, y, z)).getInputSlots())
    		if(is != null)
    			power++;
    	
    	world.scheduleBlockUpdate(x+1, y, z, Blocks.POWERED_COMPARATOR, 20);
    	world.scheduleBlockUpdate(x-1, y, z, Blocks.POWERED_COMPARATOR, 20);
    	world.scheduleBlockUpdate(x, y, z+1, Blocks.POWERED_COMPARATOR, 20);
    	world.scheduleBlockUpdate(x, y, z-1, Blocks.POWERED_COMPARATOR, 20);

        return power;
    }

}
