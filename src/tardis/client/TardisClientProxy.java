package tardis.client;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.MinecraftForgeClient;
import tardis.TardisMod;
import tardis.TardisProxy;
import tardis.client.renderer.TardisConsoleRenderer;
import tardis.client.renderer.TardisCoreRenderer;
import tardis.client.renderer.TardisRenderer;
import tardis.client.renderer.TardisSonicScrewdriverRenderer;
import tardis.tileents.TardisConsoleTileEntity;
import tardis.tileents.TardisCoreTileEntity;
import tardis.tileents.TardisTileEntity;
import cpw.mods.fml.client.registry.ClientRegistry;

public class TardisClientProxy extends TardisProxy
{

	public TardisClientProxy()
	{
		ClientRegistry.bindTileEntitySpecialRenderer(TardisTileEntity.class, new TardisRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TardisCoreTileEntity.class, new TardisCoreRenderer());
		ClientRegistry.bindTileEntitySpecialRenderer(TardisConsoleTileEntity.class, new TardisConsoleRenderer());
	}

	@Override
	public void handleTardisTransparency(int worldID,int x, int y, int z)
	{
		WorldServer world = MinecraftServer.getServer().worldServerForDimension(worldID);
		world.markBlockForRenderUpdate(x, y, z);
	}
	
	@Override
	public void postAssignment()
	{
		MinecraftForgeClient.registerItemRenderer(TardisMod.screwItem.itemID, new TardisSonicScrewdriverRenderer());
	}
}
