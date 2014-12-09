package tardis.common.dimension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

import tardis.common.core.Helper;
import tardis.common.core.TardisOutput;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.world.WorldEvent.Load;

public class TardisDimensionHandler
{
	private volatile ArrayList<Integer> dimensionIDs = new ArrayList<Integer>();
	
	private synchronized void addDimension(World w)
	{
		if(w.provider instanceof TardisWorldProvider)
			return;
		int id = Helper.getWorldID(w);
		if(!dimensionIDs.contains(id));
		{
			dimensionIDs.add(id);
			TardisOutput.print("TDimH", "Adding dimension: " + id + ", " + Helper.getDimensionName(w));
			cleanUp();
		}
	}
	
	private synchronized void cleanUp()
	{
		HashSet<Integer> uniques = new HashSet<Integer>();
		Iterator<Integer> iter = dimensionIDs.iterator();
		while(iter.hasNext())
		{
			Integer i = iter.next();
			if(i == null || uniques.contains(i))
				iter.remove();
			else
				uniques.add(i);
		}
	}
	
	public void findDimensions()
	{
		if(!Helper.isServer())
			return;
		WorldServer[] loadedWorlds = DimensionManager.getWorlds();
		for(WorldServer w : loadedWorlds)
			addDimension(w);
	}
	
	@SubscribeEvent
	public void loadWorld(Load loadEvent)
	{
		World w = loadEvent.world;
		if(w != null)
			addDimension(w);
	}

	public int numDims()
	{
		return Math.max(1,dimensionIDs.size());
	}
	
	public Integer getControlFromDim(int dim)
	{
		if(!Helper.isServer())
			return 0;
		cleanUp();
		if(dimensionIDs.contains(dim))
			return dimensionIDs.indexOf(dim);
		else
		{
			World w = Helper.getWorldServer(dim);
			if(w != null)
			{
				addDimension(w);
				return dimensionIDs.indexOf(w);
			}
		}
		if(dimensionIDs.contains(0))
			return dimensionIDs.indexOf(0);
		return 0;
	}
	
	public Integer getDimFromControl(int control)
	{
		if(!Helper.isServer())
			return 0;
		int index = Helper.clamp(control, 0, dimensionIDs.size() - 1);
		int dim = dimensionIDs.get(index);
		TardisOutput.print("TDimH", "Resolving " + control + " to " + dim);
		return dim;
	}
}