package tardis.common.tileents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.util.AECableType;
import appeng.api.util.DimensionalCoord;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import tardis.TardisMod;
import tardis.api.IActivatable;
import tardis.api.IArtronEnergyProvider;
import tardis.api.IChunkLoader;
import tardis.api.TardisFunction;
import tardis.api.TardisUpgradeMode;
import tardis.common.core.Helper;
import tardis.common.core.ConfigFile;
import tardis.common.core.TardisOutput;
import tardis.common.core.store.SimpleCoordStore;
import tardis.common.dimension.TardisTeleportHelper;
import tardis.common.items.KeyItem;
import tardis.common.tileents.components.TardisTEComponent;
import tardis.common.tileents.extensions.CoreGrid;
import tardis.common.tileents.extensions.LabFlag;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

public class CoreTileEntity extends AbstractTileEntity implements IActivatable, IChunkLoader, IGridHost, IArtronEnergyProvider
{
	private static ConfigFile config;
	public static final ChatComponentText cannotModifyMessage	= new ChatComponentText("[TARDIS] You do not have permission to modify this TARDIS");
	public static final ChatComponentText cannotUpgradeMessage	= new ChatComponentText("[TARDIS] You do not have enough upgrade points");
	private int oldExteriorWorld = 0;
	private int exteriorWorld;
	private int exteriorX;
	private int exteriorY;
	private int exteriorZ;
	private float lastProximity = 0;
	private double lastSpin = 0;
	
	private double speed = 4;
	private static double maxSpeed;
	
	private int energy;
	private boolean inFlight = false;
	private int flightTimer = 0;
	private int inFlightTimer = 0;
	private int totalFlightTimer = 0;
	private int buttonTime = 80;
	
	private int timeTillTakenOff;
	private int timeTillLanding;
	private int timeTillLandingInt;
	private int timeTillLanded;
	private int numButtons;
	
	private int numRooms = 0;
	private double	tardisXP = 0;
	private int		tardisLevel = 0;
	
	private SimpleCoordStore transmatPoint = null;
	private int shields;
	private int hull;
	private boolean deletingRooms = false;
	private static double explodeChance = 0.25;
	private boolean explode = false;
	
	private int instability = 0;
	private int desDim = 0;
	private int desX = 0;
	private int desY = 0;
	private int desZ = 0;
	private String[] desStrs = null;
	
	private enum LockState{Open,OwnerOnly,KeyOnly,Locked};
	private LockState lockState = LockState.Open; 
	
	private HashSet<SimpleCoordStore> roomSet = new HashSet<SimpleCoordStore>();
	private String ownerName;
	private ArrayList<Integer> modders = new ArrayList<Integer>();
	
	private static ChunkCoordIntPair[] loadable = null;
	private boolean forcedFlight = false;
	
	private boolean fast = false;
	
	private ArrayList<SimpleCoordStore> gridLinks = new ArrayList<SimpleCoordStore>();
	private int rfStored;
	private ItemStack[] items;
	private FluidStack[] fluids;
	
	private HashMap<TardisUpgradeMode,Integer> upgradeLevels = new HashMap<TardisUpgradeMode,Integer>();
	
	private static int maxNumRooms = 30;
	private static int maxNumRoomsInc = 10;
	private static int maxShields = 1000;
	private static int maxShieldsInc = 500;
	private int maxHull;
	private static int maxEnergy = 1000;
	private static int maxEnergyInc = 1000;
	private static int energyPerSecond = 1;
	private static int energyPerSecondInc = 1;
	private static int energyCostDimChange = 2000;
	private static int energyCostFlightMax = 3000;
	private static double energyCostPower  = 0.8;
	private static int maxMoveForFast = 3;
	private static IGridNode node = null;
	
	static
	{
		config = TardisMod.configHandler.getConfigFile("Core");
		explodeChance = config.getDouble("Explosion chance (on control not pressed)", 0.6);
		loadable = new ChunkCoordIntPair[4];
		loadable[0] = new ChunkCoordIntPair( 0, 0);
		loadable[1] = new ChunkCoordIntPair(-1, 0);
		loadable[2] = new ChunkCoordIntPair( 0,-1);
		loadable[3] = new ChunkCoordIntPair(-1,-1);
	}
	
	{
		maxSpeed			= config.getDouble("maxSpeed", 8);
		maxEnergy			= config.getInt("maxEnergy", 1000);
		maxNumRooms			= config.getInt("maxRooms", 30);
		maxShields	 		= config.getInt("maxShields", 1000);
		maxHull				= config.getInt("maxHull", 1000);
		maxNumRoomsInc		= config.getInt("max rooms inc per level", 10);
		maxShieldsInc		= config.getInt("max shields inc per level", 500);
		maxEnergyInc		= config.getInt("max energy inc per level", 1000);
		energyPerSecondInc	= config.getInt("energy per second inc per level",1);
		energyCostDimChange	= config.getInt("energy cost to change dims",2000);
		energyCostFlightMax	= config.getInt("max flight energy cost",3000);
		energyCostPower		= config.getDouble("energy distance cost power", 0.8);
		maxMoveForFast 		= config.getInt("max move for fast speed", 3);
		shields		= maxShields;
		hull		= maxHull;
		
		items = new ItemStack[TardisMod.numInvs];
		fluids = new FluidStack[TardisMod.numTanks];
		energy = 100;
		energyPerSecond = config.getInt("energy per second",1);
	}
	
	private void flightTick()
	{
		if(!Helper.isServer())
			return;
		if(inFlightTimer == 0)
			Helper.playSound(this, "takeoff", 0.75F);
		totalFlightTimer++;
		
		if(inCoordinatedFlight() || forcedFlight)
			inFlightTimer++;
		if(inFlightTimer >= timeTillTakenOff)//Taken off
		{
			ConsoleTileEntity con = getConsole();
			int takenTime = inFlightTimer - timeTillTakenOff;
			int takenTimeTwo = flightTimer - timeTillTakenOff;
			if(!worldObj.isRemote && !forcedFlight && !con.isStable() && takenTimeTwo % buttonTime == 0 && (getButtonTime() * numButtons) >= takenTime)
			{
				double speedMod = Math.max(1,getSpeed(true)*3/getMaxSpeed());
				if(takenTimeTwo > 0)
				{
					TardisOutput.print("TCTE", "Working out what to do!");
					if(con.unstableControlPressed())
					{
						instability -= 0.5 * speedMod;
						addXP(speedMod * (inCoordinatedFlight() ? 3 : 2));
					}
					else
					{
						instability+= speedMod;
						if(shouldExplode())
							explode = true;
						sendUpdate();
					}
					instability = Helper.clamp(instability, 0, 18);
				}
				if(con != null && con.unstableFlight() && (buttonTime * numButtons) > takenTime)
					con.randomUnstableControl();
				else if((buttonTime * numButtons) <= takenTime)
					con.clearUnstableControl();
			}
			if(takenTime == 0)// remove old tardis
			{
				World w = Helper.getWorld(exteriorWorld);
				if(w != null)
				{
					if(w.getBlock(exteriorX,exteriorY,exteriorZ) == TardisMod.tardisBlock)
					{
						w.setBlockToAir(exteriorX, exteriorY, exteriorZ);
						w.setBlockToAir(exteriorX, exteriorY+1, exteriorZ);
						TardisOutput.print("TCTE", "Blanking exterior");
						exteriorWorld = 10000;
						//exteriorX = 0;
						//exteriorY = 0;
						//exteriorZ = 0;
					} 	
				}
			}
			if(inFlightTimer < timeTillLanding)
			{
				if(flightTimer % 69 == 0 && inFlight)
					Helper.playSound(this, "engines", 0.75F);
				flightTimer++;
			}
			else
			{
				if(flightTimer % 69 == 0 && inFlightTimer < timeTillLandingInt)
					Helper.playSound(this, "engines", 0.75F);
				flightTimer++;
				
				if(inFlightTimer == timeTillLanding)
					placeBox();
				
				if(inFlightTimer == timeTillLandingInt)
					Helper.playSound(this, "landingInt", 0.75F);
				
				if(inFlightTimer >= timeTillLanded)
					land();
			}
		}
	}

	private void safetyTick()
	{
		List<Object> players = worldObj.playerEntities;
		for(Object o : players)
		{
			if(o instanceof EntityPlayer)
			{
				EntityPlayer pl = (EntityPlayer)o;
				if(pl.posY < -5 && !pl.capabilities.isFlying)
					Helper.teleportEntityToSafety(pl);
			}
		}
	}

	@Override
	public void updateEntity()
	{
		super.updateEntity();
		
		if(explode)
		{
			double xO = (rand.nextDouble()*3) - 1.5;
			double zO = (rand.nextDouble()*3) - 1.5;
			Helper.playSound(this, "minecraft:random.explosion", 0.5F);
			worldObj.createExplosion(null, xCoord+0.5+xO, yCoord-0.5, zCoord+0.5+zO, 1F, true);
			explode = false;
		}
		
		if(tt % 20 == 0)
		{
			addArtronEnergy(getEnergyPerSecond(getLevel(TardisUpgradeMode.REGEN)),false);
			safetyTick();
		}
		
		if(inFlight)
			flightTick();
		
		if(!worldObj.isRemote)
		{
			if(tt % 100 == 0 && TardisMod.plReg != null)
			{
				if(!TardisMod.plReg.hasTardis(ownerName))
					TardisMod.plReg.addPlayer(ownerName, worldObj.provider.dimensionId);
			}
			
			if(deletingRooms)
			{
				Iterator<SimpleCoordStore> i = roomSet.iterator();
				if(i.hasNext())
				{
					SimpleCoordStore coord = i.next();
					TileEntity te = worldObj.getTileEntity(coord.x, coord.y, coord.z);
					if(te != null && te instanceof SchemaCoreTileEntity)
					{
						TardisOutput.print("TCTE", "Removing room @ " + coord);
						SchemaCoreTileEntity schemaCore = (SchemaCoreTileEntity)te;
						schemaCore.remove();
					}
					i.remove();
				}
				else
				{
					deletingRooms = false;
					numRooms = 0;
				}
			}
			if(tt % 1200 == 0)
			{
				sendUpdate();
			}
		}
	}

	public boolean activate(EntityPlayer player, int side)
	{
		if(!Helper.isServer())
			return true;
		sendDestinationStrings(player);
		return true;
	}
	
	public boolean hasValidExterior()
	{
		World w = Helper.getWorld(exteriorWorld);
		if(w != null)
		{
			if(w.getBlock(exteriorX, exteriorY, exteriorZ) == TardisMod.tardisBlock)
				return true;
		}
		return false;
	}
	
	public void linkToExterior(TardisTileEntity exterior)
	{
		exteriorWorld = exterior.getWorldObj().provider.dimensionId;
		exteriorX = exterior.xCoord;
		exteriorY = exterior.yCoord;
		exteriorZ = exterior.zCoord;
	}
	
	public boolean hasKey(EntityPlayer player,boolean inHand)
	{
		if(inHand)
		{
			ItemStack held = player.getHeldItem();
			String on = KeyItem.getOwnerName(held);
			if(on != null)
				TardisOutput.print("TCTE","Key owner = " + on);
			else
				TardisOutput.print("TCTE","Key owner = null");
			if(on != null && on.equals(ownerName))
				return true;
		}
		else
		{
			InventoryPlayer inv = player.inventory;
			if(inv == null)
				return false;
			for(ItemStack is: inv.mainInventory)
			{
				String on = KeyItem.getOwnerName(is);
				if(on != null && on.equals(ownerName))
					return true;
			}
		}
		return false;
	}
	
	public boolean canOpenLock(EntityPlayer player, boolean isInside)
	{
		if(isInside && !lockState.equals(LockState.Locked))
			return true;
		else if(isInside)
			return false;
		
		if(player == null)
			return false;
		
		if(lockState.equals(LockState.Open))
			return true;
		else if(lockState.equals(LockState.Locked))
			return false;
		else if(lockState.equals(LockState.OwnerOnly))
			return isOwner(Helper.getUsername(player));
		else if(lockState.equals(LockState.KeyOnly))
			return isOwner(Helper.getUsername(player)) || hasKey(player,false);
		return false;
	}
	
	private void enterTardis(EntityLivingBase ent)
	{
		TardisTeleportHelper.teleportEntity(ent, worldObj.provider.dimensionId, 13.5, 29, 0.5, 90);
	}
	
	public void enterTardis(EntityPlayer player, boolean ignoreLock)
	{
		if(player.worldObj.isRemote)
			return;
		if(ignoreLock || canOpenLock(player,false))
			enterTardis(player);
		else
			player.addChatMessage(new ChatComponentText("[TARDIS]The door is locked"));
	}
	
	public void leaveTardis(EntityPlayer player, boolean ignoreLock)
	{
		if(!inFlight() && hasValidExterior())
		{
			if(ignoreLock || canOpenLock(player,true))
			{
				World ext = Helper.getWorld(exteriorWorld);
				if(ext != null)
				{
					if(ext.isRemote)
						return;
					int facing = ext.getBlockMetadata(exteriorX, exteriorY, exteriorZ);
					int dx = 0;
					int dz = 0;
					double rot = 0;
					switch(facing)
					{
						case 0:dz = -1;rot=180; break;
						case 1:dx =  1;rot=-90; break;
						case 2:dz =  1;rot=  0; break;
						case 3:dx = -1;rot= 90; break;
					}
					
					if(softBlock(ext,exteriorX+dx, exteriorY, exteriorZ+dz) && softBlock(ext,exteriorX+dx, exteriorY, exteriorZ+dz))
					{
						TardisTeleportHelper.teleportEntity(player, exteriorWorld, exteriorX+0.5+(dx), exteriorY, exteriorZ+0.5+(dz),rot);
					}
					else
						Helper.sendString(player, "TARDIS", "The door is obstructed");
				}
				else
					Helper.sendString(player, "TARDIS", "The door refuses to open");
			}
			else
				Helper.sendString(player, "TARDIS", "The door is locked");
		}
		else if(inFlight())
			Helper.sendString(player, "TARDIS", "The door won't open in flight");
		else
			Helper.sendString(player, "TARDIS", "The door refuses to open for some reason");
	}
	
	public boolean changeLock(EntityPlayer pl,boolean inside)
	{
		if(pl.worldObj.isRemote)
			return false;
		
		TardisOutput.print("TCTE", "Changing lock");
		if(!hasKey(pl,true) && !inside)
			return false;
		
		if(lockState.equals(LockState.Locked) && !inside) //If you're outside, you can't unlock a "Locked" door
			return false;
		if(lockState.equals(LockState.OwnerOnly) && !(inside || isOwner(pl))) //If you're not the owner/not inside, you can't unlock an owner only door
			return false;
		
		if(!pl.isSneaking())
			return false;
			
		int num = LockState.values().length;
		LockState[] states = LockState.values();
		lockState = states[((lockState.ordinal() + 1)%num)];
		if(lockState.equals(LockState.Locked) && !inside) //If you're not inside, you can't put the door in locked mode
			lockState = states[((lockState.ordinal() + 1)%num)];
		if(lockState.equals(LockState.OwnerOnly) && !isOwner(pl)) //If you're not the owner, you can't put the door in owner only mode
			lockState = states[((lockState.ordinal() + 1)%num)];
		
		TardisOutput.print("TTE", "Lockstate:"+lockState.toString());
		if(lockState.equals(LockState.KeyOnly))
			pl.addChatMessage(new ChatComponentText("[TARDIS]The door will open for its owner and people with keys"));
		else if(lockState.equals(LockState.Locked))
			pl.addChatMessage(new ChatComponentText("[TARDIS]The door will not open"));
		else if(lockState.equals(LockState.Open))
			pl.addChatMessage(new ChatComponentText("[TARDIS]The door will open for all"));
		else if(lockState.equals(LockState.OwnerOnly))
			pl.addChatMessage(new ChatComponentText("[TARDIS]The door will only open for its owner"));
		return true;
	}
	
	private int[] scanForValidPos(World w, int[] current)
	{
		int[] check = {0,1,-1,2,-2, 3,-3, 4,-4, 5,-5, 6,-6,7,-7,8,-8,9,-9};
		for(int i=0;i<check.length;i++)
		{
			int yO = check[i];
			for(int j=0;j<check.length;j++)
			{
				int xO = check[j];
				for(int k=0;k<check.length;k++)
				{
					int zO = check[k];
					if(isValidPos(w,current[0]+xO,current[1]+yO,current[2]+zO))
					{
						return new int[] { current[0]+xO,current[1]+yO,current[2]+zO };
					}
				} 
			}
		}
		return current;
	}

	private int[] scanForLandingPad(World w, int[] current)
	{
		int[] check = { 0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, -6, 6 };
		for(int i = 0;i<check.length;i++)
		{
			int xO = check[i];
			for(int j = 0;j<check.length;j++)
			{
				int zO = check[j];
				for(int k = 0;k<check.length;k++)
				{
					int yO = check[k];
					TileEntity te = w.getTileEntity(current[0]+xO, current[1]+yO, current[2]+zO);
					if(te instanceof LandingPadTileEntity)
					{
						if(((LandingPadTileEntity)te).isClear())
						{
							return new int[] { current[0]+xO, current[1]+yO+1, current[2]+zO };
						}
					}
				}
			}
		}
		return current;
	}

	public int[] findGround(World w, int[] curr)
	{
		if(softBlock(w,curr[0], curr[1]-1, curr[2]))
		{
			int newY = curr[1]-2;
			while(newY > 0 && softBlock(w,curr[0],newY,curr[2]))
				newY--;
			return new int[] { curr[0], newY+1, curr[2] };
		}
		return curr;
	}
	
	private int[] getModifiedControls(ConsoleTileEntity con, int[] posArr)
	{
		if(con== null)
			return posArr;
		World w = Helper.getWorld(con.getDimFromControls());
		if(w == null)
			return posArr;
		
		if(!(isValidPos(w,posArr[0], posArr[1], posArr[2])))
		{
			if(posArr[0] != exteriorX || posArr[1] != exteriorY || posArr[2] != exteriorZ)
				posArr = scanForValidPos(w,posArr);
		}
		if(con.getLandOnGroundFromControls())
			posArr = findGround(w, posArr);
		if(con.getLandOnPadFromControls())
			posArr = scanForLandingPad(w,posArr);
		return posArr;
	}

	public boolean isMoving()
	{
		if(exteriorWorld == 10000)
			return true;
		ConsoleTileEntity con = getConsole();
		if(con == null)
			return false;
		int dim = con.getDimFromControls();
		if(dim != exteriorWorld)
			return true;
		int[] posArr = new int[] {
				con.getXFromControls(exteriorX),
				con.getYFromControls(exteriorY),
				con.getZFromControls(exteriorZ)};
		posArr = getModifiedControls(con,posArr);
		TardisOutput.print("TCTE", "Moving to :" + Arrays.toString(posArr) + " from " + exteriorX +","+exteriorY+","+exteriorZ);
		if(Math.abs(posArr[0] - exteriorX) > maxMoveForFast)
			return true;
		if(Math.abs(posArr[1] - exteriorY) > maxMoveForFast)
			return true;
		if(Math.abs(posArr[2] - exteriorZ) > maxMoveForFast)
			return true;
		return false;
	}
	
	public boolean takeOffEnergy(EntityPlayer pl)
	{
		ConsoleTileEntity con = getConsole();
		if(con != null)
		{
			int dDim = con.getDimFromControls();
			int dX = con.getXFromControls(exteriorX);
			int dZ = con.getZFromControls(exteriorZ);
			
			int extW = inFlight() ? oldExteriorWorld : exteriorWorld;
			int distance = (int) Math.round(Math.pow(Math.abs(dX - exteriorX) + Math.abs(dZ - exteriorZ),energyCostPower));
			distance +=  (dDim != extW ? energyCostDimChange : 0);
			double speedMod = Math.max(0.5,getSpeed(true)*3/getMaxSpeed());
			int enCost = (int) Helper.clamp((int)Math.round(distance * speedMod), 1, energyCostFlightMax);
			return takeArtronEnergy(enCost,false);
		}
		return false;
	}
	
	public boolean takeOff(boolean forced,EntityPlayer pl)
	{
		forcedFlight = forced;
		return takeOff(pl);
	}
	
	public boolean takeOff(EntityPlayer pl)
	{
		if(!inFlight)
		{
			ConsoleTileEntity con = getConsole();
			if((!con.shouldLand()) || takeOffEnergy(pl))
			{
				instability = 0;
				inFlight = true;
				inFlightTimer = 0;
				flightTimer = 0;
				TardisTileEntity te = getExterior();
				fast = isFastLanding() && con.shouldLand();
				timeTillTakenOff	= (20 * 11);
				if(!fast)
					timeTillLanding		= timeTillTakenOff +  (int) (((2+(2*getMaxSpeed())) - (2*getSpeed(true))) * 69);
				else
					timeTillLanding		= timeTillTakenOff+1;
				timeTillLandingInt	= timeTillLanding + (fast ? 1 : 20 * 17);
				timeTillLanded		= timeTillLanding + (fast ? 20 * 5 : 20 * 22);
				numButtons = (timeTillLanding - timeTillTakenOff) / getButtonTime();
				oldExteriorWorld = exteriorWorld;
				TardisOutput.print("TCTE","Flight times : " +(fast ? "FAST ": "SLOW ")+ timeTillTakenOff+ ">"+timeTillLanding+">" + timeTillLandingInt+">"+timeTillLanded);
				if(te != null)
					te.takeoff();
				sendUpdate();
				return true;
			}
			else
				Helper.sendString(pl,"TARDIS","Not enough energy to take off");
		}
		return false;
	}
	
	private boolean isValidPos(World w, int x, int y, int z)
	{
		return y > 0 && y < 254 && softBlock(w,x,y,z) && softBlock(w,x,y+1,z);
	}
	
	private int getOffsetFromInstability()
	{
		if(forcedFlight)
			return 0;
		if(instability == 0)
			return 0;
		return rand.nextInt(Math.max(0,2 * instability)) - instability;
	}
	
	private void placeBox()
	{
		if(!Helper.isServer() || hasValidExterior())
			return;
		
		ConsoleTileEntity con = getConsole();
		if(con == null)
		{
			flightTimer--;
			return;
		}
		int[] posArr = new int[] {
				con.getXFromControls(exteriorX) + getOffsetFromInstability(),
				con.getYFromControls(exteriorY),
				con.getZFromControls(exteriorZ) + getOffsetFromInstability() };
		int facing = con.getFacingFromControls();
		posArr = getModifiedControls(con,posArr);
		World w = Helper.getWorld(con.getDimFromControls());
		w.setBlock(posArr[0], posArr[1], posArr[2], TardisMod.tardisBlock, facing, 3);
		w.setBlock(posArr[0], posArr[1]+1, posArr[2], TardisMod.tardisTopBlock, facing, 3);
		
		setExterior(w,posArr[0], posArr[1], posArr[2]);
		oldExteriorWorld = 0;
		TileEntity te = w.getTileEntity(posArr[0], posArr[1], posArr[2]);
		if(te != null && te instanceof TardisTileEntity)
		{
			TardisTileEntity tardis = (TardisTileEntity) te;
			tardis.linkedDimension = worldObj.provider.dimensionId;
			tardis.land(fast);
		}
	}
	
	public void land()
	{
		if(inFlight)
		{
			fast=false;
			ConsoleTileEntity con = getConsole();
			forcedFlight = false;
			addXP(con != null && con.isStable()?15:(45-instability));
			inFlight = false;
			Helper.playSound(this, "engineDrum", 0.75F);
			TardisTileEntity ext = getExterior();
			if(ext != null)
			{
				ext.forceLand();
				List<Entity> inside = ext.getEntitiesInside();
				for(Entity e: inside)
					if(e instanceof EntityLivingBase)
						enterTardis((EntityLivingBase) e);
			}
			if(con != null)
				con.land();
			sendUpdate();
		}
	}
	
	private int getButtonTime()
	{
		double mod = 1;
		if(getSpeed(false) != 0)
			mod = getMaxSpeed() / ((getSpeed(false)) * 2);
		else
			mod = 0;
		mod = Helper.clamp(mod, 0.5, 4);
		int buttonTimeMod = Helper.clamp((int)Math.round(buttonTime * mod),30,buttonTime*4);
		return buttonTimeMod;
	}
	
	private boolean shouldExplode()
	{
		double eC = explodeChance * ((getSpeed(false) + 1) *3 / getMaxSpeed());
		eC *= Helper.clamp(3.0 / ((tardisLevel+1)/2), 0.2, 1);
		return rand.nextDouble() < eC;
	}
	
	////////////////////////////////////////////////
	//////////////DATA STUFF////////////////////////
	////////////////////////////////////////////////
	
	public boolean inCoordinatedFlight()
	{
		if(inFlight)
		{
			ConsoleTileEntity con = getConsole();
			if(con == null)
				return true;
			return (inFlightTimer <= timeTillTakenOff) || con.shouldLand();
		}
		return false;
	}
	
	public boolean inFlight()
	{
		return inFlight;
	}
	
	public float getProximity()
	{
		if(inFlight)
		{
			int rate = 40;
			double val = Math.abs((tt % rate) - (rate / 2));
			double max = 0.4;
			lastProximity = (float) (max * 2 * (val / rate));
			return lastProximity;
		}
		else if(lastProximity > 0)
			return (lastProximity = lastProximity - (1/20.0F));
		else
		{
			return 0;
		}
	}
	
	@SideOnly(Side.CLIENT)
	public double getSpin()
	{
		double slowness = 3;
		if(inFlight)
			lastSpin = ((lastSpin + 1) % (360 * slowness));
		return lastSpin / slowness;
	}
	
	public void setExterior(World w, int x, int y, int z)
	{
		exteriorWorld = w.provider.dimensionId;
		exteriorX = x;
		exteriorY = y;
		exteriorZ = z;
		TardisOutput.print("TCTE", "Exterior placed @ " + x + ","+ y +","+z+","+exteriorWorld +","+worldObj.isRemote);
		sendUpdate();
	}
	
	public TardisTileEntity getExterior()
	{
		World w = Helper.getWorld(exteriorWorld);
		if(w != null)
		{
			TileEntity te = w.getTileEntity(exteriorX,exteriorY,exteriorZ);
			if(te != null && te instanceof TardisTileEntity)
				return (TardisTileEntity) te;
		}
		return null;
	}
	
	public ConsoleTileEntity getConsole()
	{
		TileEntity te = worldObj.getTileEntity(xCoord, yCoord - 2, zCoord);
		if(te instanceof ConsoleTileEntity)
				return (ConsoleTileEntity)te;
		return null;
	}
	
	public EngineTileEntity getEngine()
	{
		TileEntity te = worldObj.getTileEntity(xCoord, yCoord - 5, zCoord);
		if(te instanceof EngineTileEntity)
			return (EngineTileEntity)te;
		return null;
	}
	
	public SchemaCoreTileEntity getSchemaCore()
	{
		TileEntity te = worldObj.getTileEntity(xCoord, yCoord - 10, zCoord);
		if(te instanceof SchemaCoreTileEntity)
			return (SchemaCoreTileEntity)te;
		return null;
	}
	
	public void loadConsoleRoom(String sub)
	{
		String fullName = "tardisConsole" + sub;
		SchemaCoreTileEntity schemaCore = getSchemaCore();
		if(schemaCore != null)
			Helper.loadSchemaDiff(schemaCore.getName(),fullName,worldObj, xCoord,yCoord-10,zCoord,0);
		else
			Helper.loadSchema(fullName, worldObj, xCoord, yCoord - 10, zCoord, 0);
	}

	public boolean canModify(EntityPlayer player)
	{
		return canModify(Helper.getUsername(player));
	}
	
	public boolean canModify(String playerName)
	{
		return isOwner(playerName) || modders.contains(playerName.hashCode());
	}
	
	public void toggleModifier(EntityPlayer modder,String name)
	{
		if(isOwner(modder.getCommandSenderName()))
		{
			if(!modder.getCommandSenderName().equals(name))
			{
				if(modders.contains(name.hashCode()))
					modders.remove(name.hashCode());
				else
					modders.add(name.hashCode());
			}
		}
		else
			modder.addChatMessage(cannotModifyMessage);
	}
	
	private boolean isOwner(EntityPlayer pl)
	{
		if(pl == null)
			return false;
		return isOwner(Helper.getUsername(pl));
	}

	public boolean isOwner(String name)
	{
		if(ownerName != null)
			return ownerName.equals(name);
		return false;
	}
	
	public String getOwner()
	{
		return ownerName;
	}
	
	public void setOwner(String name)
	{
		TardisOutput.print("TCTE", "Setting owner to " + name+"#"+worldObj.isRemote, TardisOutput.Priority.DEBUG);
		ownerName = name;
		if(!worldObj.isRemote && TardisMod.plReg != null && !TardisMod.plReg.hasTardis(ownerName))
			TardisMod.plReg.addPlayer(ownerName, worldObj.provider.dimensionId);
		TardisTileEntity ext = getExterior();
		if(ext != null)
			ext.owner = name;
	}

	public boolean hasFunction(TardisFunction fun)
	{
		switch(fun)
		{
		case LOCATE:	return tardisLevel >= 3;
		case SENSORS:	return tardisLevel >= 5;
		case STABILISE:	return tardisLevel >= 7;
		case TRANSMAT:	return tardisLevel >= 9;
		case RECALL:	return tardisLevel >= 11;
		default:		return false;
		}
	}
	
	public double getMaxSpeed()
	{
		return maxSpeed;
	}
	
	public double getSpeed(boolean modified)
	{
		if(forcedFlight)
			return getMaxSpeed()+1;
		if(!modified)
			return speed;
		double mod = ((double)getNumRooms()) / getMaxNumRooms();
		return speed * (1-(mod / 2.0));
	}
	
	public boolean isFastLanding()
	{
		if(isMoving())
		{
			TardisOutput.print("TCTE", "Definitely moving?");
			return getSpeed(true) > getMaxSpeed();
		}
		return true;
	}
	
	public double addSpeed(double a)
	{
		if(!inFlight)
			speed = speed + a;
		speed = Helper.clamp(speed, 0, getMaxSpeed());
		sendUpdate();
		return speed;
	}
	
	public int getLevel()
	{
		return tardisLevel;
	}
	
	public int spentLevelPoints()
	{
		int spentPoints = 0;
		for(TardisUpgradeMode mode : upgradeLevels.keySet())
			spentPoints += upgradeLevels.get(mode);
		return spentPoints;
	}
	
	public int unspentLevelPoints()
	{
		return maxUnspentLevelPoints() - spentLevelPoints();
	}
	
	public int maxUnspentLevelPoints()
	{
		return tardisLevel / 2;
	}
	
	public int getLevel(TardisUpgradeMode mode)
	{
		int level = 0;
		if(upgradeLevels.containsKey(mode))
			level = upgradeLevels.get(mode);
		return level;
	}
	
	public void upgradeLevel(TardisUpgradeMode mode, int am)
	{
		if(unspentLevelPoints() >= am)
		{
			upgradeLevels.put(mode, am + getLevel(mode));
		}
	}
	
	public double getXP()
	{
		return tardisXP;
	}
	
	public double getXPNeeded()
	{
		if(tardisLevel <= 0)
			return 1;
		return TardisMod.xpBase + (tardisLevel * TardisMod.xpInc);
	}
	
	public double addXP(double a)
	{
		tardisXP += Math.abs(a);
		if(tardisXP >= getXPNeeded())
		{
			while(tardisXP > getXPNeeded())
			{
				tardisXP -= getXPNeeded();
				tardisLevel++;
			}
			Helper.playSound(worldObj, xCoord, yCoord, zCoord, "levelup", 1);
		}
		sendUpdate();
		return tardisXP;
	}
	
	public int getNumRooms()
	{
		return numRooms;
	}
	
	public int getMaxNumRooms(int level)
	{
		return maxNumRooms + (maxNumRoomsInc * level);
	}
	
	public int getMaxNumRooms()
	{
		return getMaxNumRooms(getLevel(TardisUpgradeMode.ROOMS));
	}
	
	public boolean addRoom(boolean sub, SchemaCoreTileEntity te)
	{
		if(sub)
		{
			if(Helper.isServer() && te != null)
				roomSet.remove(new SimpleCoordStore(te));
			if(numRooms > 0)
				numRooms --;
			return true;
		}
		
		if(!sub && numRooms < maxNumRooms)
		{
			if(Helper.isServer() && te != null)
				roomSet.add(new SimpleCoordStore(te));
			numRooms++;
			return true;
		}
		
		return false;
	}
	
	public boolean addRoom(SchemaCoreTileEntity te)
	{
		if(Helper.isServer() && te != null)
			return roomSet.add(new SimpleCoordStore(te));
		return false;
	}
	
	public void removeAllRooms(boolean force)
	{
		if(!force)
			removeAllRooms();
		else
		{
			for(SimpleCoordStore coord : roomSet)
			{
				TileEntity te = worldObj.getTileEntity(coord.x, coord.y, coord.z);
				if(te != null && te instanceof SchemaCoreTileEntity)
				{
					TardisOutput.print("TCTE", "Removing room @ " + coord);
					SchemaCoreTileEntity schemaCore = (SchemaCoreTileEntity)te;
					schemaCore.remove();
				}
			}
			roomSet.clear();
			numRooms = 0;
		}
	}
	
	public void removeAllRooms()
	{
		
		deletingRooms = true;
	}
	
	public int getEnergyPerSecond()
	{
		return getEnergyPerSecond(getLevel(TardisUpgradeMode.REGEN));
	}
	
	public int getEnergyPerSecond(int level)
	{
		return energyPerSecond + (energyPerSecondInc * level);
	}
	
	public int getMaxArtronEnergy(int level)
	{
		return maxEnergy + (maxEnergyInc * level);
	}
	
	@Override
	public int getMaxArtronEnergy()
	{
		return getMaxArtronEnergy(getLevel(TardisUpgradeMode.ENERGY));
	}
	
	@Override
	public int getArtronEnergy()
	{
		return energy;
	}
	
	@Override
	public boolean addArtronEnergy(int amount, boolean sim)
	{
		if(!sim)
			energy += amount;
		energy = Helper.clamp(energy,0,getMaxArtronEnergy(getLevel(TardisUpgradeMode.ENERGY)));
		return true;
	}
	
	@Override
	public boolean takeArtronEnergy(int amount, boolean sim)
	{
		if(energy >= amount)
		{
			if(!sim)
				energy -= amount;
			return true;
		}
		energy = Helper.clamp(energy,0,getMaxArtronEnergy(getLevel(TardisUpgradeMode.ENERGY)));
		return false;
	}
	
	@Override
	public boolean doesSatisfyFlag(LabFlag flag)
	{
		switch(flag)
		{
			case NOTINFLIGHT: return !inFlight();
			case INFLIGHT: return inFlight();
			case INCOORDINATEDFLIGHT: return inCoordinatedFlight();
			case INUNCOORDINATEDFLIGHT: return inFlight() && !inCoordinatedFlight();
			default: return false;
		}
	}

	public int getShields()
	{
		return shields;
	}
	
	public int getMaxShields(int level)
	{
		return maxShields + (level * maxShieldsInc);
	}
	
	public int getHull()
	{
		return hull;
	}
	
	public int getMaxHull(int level)
	{
		return maxHull;
	}
	
	public void addGridLink(SimpleCoordStore pos)
	{
		TardisOutput.print("TCTE", "Adding coord:" + pos.toString());
		if(pos != null)
			gridLinks.add(pos);
	}
	
	public DimensionalCoord[] getGridLinks(SimpleCoordStore asker)
	{
		if(gridLinks.size() == 0)
			return null;
		ArrayList<DimensionalCoord> coords = new ArrayList<DimensionalCoord>(gridLinks.size()-1);
		Iterator<SimpleCoordStore> iter = gridLinks.iterator();
		while(iter.hasNext())
		{
			SimpleCoordStore s = iter.next();
			if(s.equals(asker))
				continue;
			
			TileEntity te = worldObj.getTileEntity(s.x, s.y, s.z);
			if(te instanceof ComponentTileEntity)
			{
				if(((ComponentTileEntity)te).hasComponent(TardisTEComponent.GRID))
					coords.add(new DimensionalCoord(worldObj,s.x,s.y,s.z));
				else
					iter.remove();
			}
			else
				iter.remove();
		}
		DimensionalCoord[] retVal = new DimensionalCoord[coords.size()];
		coords.toArray(retVal);
		return retVal;
	}
	
	public int getMaxRF()
	{
		return TardisMod.rfBase + Math.max((tardisLevel - 1) * TardisMod.rfInc,0);
	}
	
	public int getRF()
	{
		return rfStored;
	}
	
	public int addRF(int am, boolean sim)
	{
		int max = getMaxRF() - getRF();
		if(!sim)
			rfStored += Math.min(am, max);
		return Math.min(am,max);
	}
	
	public int remRF(int am, boolean sim)
	{
		int max = getRF();
		if(!sim)
			rfStored -= Math.min(am, max);
		return Math.min(am,max);
	}
	
	public ItemStack setIS(ItemStack is, int slot)
	{
		ItemStack ret = items[slot];
		items[slot] = is;
		return ret;
	}
	
	public ItemStack getIS(int slot)
	{
		return items[slot];
	}
	
	public int getInvSize()
	{
		return items.length;
	}
	
	public FluidStack[] getTanks()
	{
		return fluids;
	}
	
	private int getMaxTransmatDistance()
	{
		return 250;
	}
	
	public boolean transmatEntity(Entity ent)
	{
		if(!hasFunction(TardisFunction.TRANSMAT))
			return false;
		SimpleCoordStore to = getTransmatPoint();
		int entWorld = Helper.getWorldID(ent.worldObj);
		boolean trans = false;
		if(entWorld == Helper.getWorldID(worldObj))
			trans = true;
		else if(entWorld == exteriorWorld)
		{
			double distance	 = Math.pow(((exteriorX+0.5) - ent.posX), 2);
			distance		+= Math.pow(((exteriorY+0.5) - ent.posY), 2);
			distance		+= Math.pow(((exteriorZ+0.5) - ent.posZ), 2);
			distance		 = Math.pow(distance, 0.5);
			if(distance <= getMaxTransmatDistance())
				trans = true;
		}
		if(trans)
		{
			Helper.playSound(ent.worldObj, (int) ent.posX, (int) ent.posY, (int) ent.posZ, "transmat", 0.6F);
			TardisTeleportHelper.teleportEntity(ent, Helper.getWorldID(worldObj), to.x+0.5, to.y+1, to.z+0.5,90);
			Helper.playSound(worldObj, to.x, to.y+1, to.z, "tardismod:transmat", 0.6F);
			return true;
		}
		else
		{
			Helper.playSound(ent.worldObj, (int) ent.posX, (int) ent.posY, (int) ent.posZ, "transmatFail", 0.6F);
			return false;
		}
	}
	
	public SimpleCoordStore getTransmatPoint()
	{
		if(isTransmatPointValid())
			return transmatPoint;
		return new SimpleCoordStore(worldObj,13,28,0);
	}
	
	public void setTransmatPoint(SimpleCoordStore s)
	{
		transmatPoint = s;
	}
	
	public boolean isTransmatPoint(SimpleCoordStore other)
	{
		if(transmatPoint != null)
			return transmatPoint.equals(other);
		return false;
	}
	
	public boolean isTransmatPointValid()
	{
		if(transmatPoint == null)
			return false;
		World w= transmatPoint.getWorldObj();
		for(int i = 0;i<5;i++)
			if(!w.isAirBlock(transmatPoint.x,transmatPoint.y-i,transmatPoint.z))
				return true;
		return false;
	}
	
	public void rescuePlayer(EntityPlayerMP pl)
	{
		if(pl == null || pl.worldObj==null || pl.worldObj.provider == null)
			return;
		int dim = pl.worldObj.provider.getRespawnDimension(pl);
		pl.getBedLocation(dim);
	}
	
	public void sendDestinationStrings(EntityPlayer pl)
	{
		ConsoleTileEntity console = getConsole();
		if(console != null)
		{
			int dD = console.getDimFromControls();
			int dX = console.getXFromControls(exteriorX);
			int dY = console.getYFromControls(exteriorY);
			int dZ = console.getZFromControls(exteriorZ);
			TardisOutput.print("TCTE","Dest:" + dD +","+dX+","+dY+","+dZ);
			if(dD==desDim&&dX==desX&&dY==desY&&dZ==desZ&&desStrs!=null)
				for(String s:desStrs)
					pl.addChatMessage(new ChatComponentText(s));
			else
			{
				int instability = Helper.clamp(20 - (2 * tardisLevel),3,20);
				desDim = dD;
				String[] send = new String[4];
				if(desStrs!= null && desStrs.length == 4)
					send = desStrs;
				
				send[0] = "The TARDIS will materialize in dimension " + getDimensionName(dD) + "["+dD+"] near:";
				if(dX != desX || send[1] == null)
					send[1] = "x = " + (dX + (rand.nextInt(2 * instability) - instability));
				if(dY != desY || send[2] == null)
					send[2] = "y = " + (dY + (rand.nextInt(2 * instability) - instability)); 
				if(dZ != desZ || send[3] == null)
				send[3] = "z = " + (dZ + (rand.nextInt(2 * instability) - instability)); 
				desX = dX;
				desY = dY;
				desZ = dZ;
				desStrs = send;
				for(String s:desStrs)
					pl.addChatMessage(new ChatComponentText(s));
			}
		}
	}
	
	/**
	 * @param w A world object to check
	 * @param x The xCoord to check
	 * @param y The yCoord of the bottom block of the TARDIS
	 * @param z The zCoord to check
	 * @return A boolean array of length 5.
	 * Element 0 = If door is not obstructed by a solid block (i.e. the door is openable)
	 * Element 1 = If door is obstructed by a dangerous block (e.g. lava/fire)
	 * Element 2 = There is a drop of 2 or more blocks
	 * Element 3 = The drop will result in water
	 * Element 4 = The drop will result in lava/fire
	 */
	private boolean[] getObstructData(World w, int x, int y, int z)
	{
		boolean[] data = new boolean[5];
		data[0] = data[1] = data[2] = data[3] = data[4] = false;
		TardisOutput.print("TCTE", "Checking for air @ " + x + "," + y + "," + z,TardisOutput.Priority.DEBUG);
		if(softBlock(w,x, y, z) && softBlock(w, x, y+1, z))
		{
			data[0] = true;
			Block bottom = w.getBlock(x, y, z);
			Block top = w.getBlock(x, y+1, z);
			if(bottom == Blocks.fire || bottom == Blocks.lava || top == Blocks.fire || top == Blocks.lava)
				data[1] = true;
			else
			{
				for(int i = 0;i<=2;i++)
				{
					if((y-i) < 1)
					{
						data[2] = true;
						break;
					}
					if(!softBlock(w,x,y-i,z))
						break;
					if(i == 2)
						data[2] = true;
				}
				if(data[2])
				{
					boolean g = true;
					for(int i = y-2;i>0 && g;i--)
					{
						if(!softBlock(w,x,i,z))
							g = true;
						Block b = w.getBlock(x, i, z);
						if(b == Blocks.water)
							data[3] = true;
						else if(b == Blocks.lava)
							data[4] = true;
						else
							continue;
						break;
					}
				}
			}
		}
		return data;
	}
	
	public void sendScannerStrings(EntityPlayer pl)
	{
		if(inFlight())
		{
			pl.addChatMessage(new ChatComponentText("Cannot use exterior scanners while in flight"));
			return;
		}
		List<String> string = new ArrayList<String>();
		TardisTileEntity ext = getExterior();
		if(ext == null)
			return;
		World w = ext.getWorldObj();
		int dx = 0;
		int dz = 0;
		string.add("Current position: Dimension " + getDimensionName(exteriorWorld) + "["+exteriorWorld+"] : " + exteriorX+","+exteriorY+","+exteriorZ);
		int facing = w.getBlockMetadata(exteriorX, exteriorY, exteriorZ);
		for(int i=0;i<4;i++)
		{
			switch(i)
			{
				case 0:dz = -1; dx = 0; break;
				case 1:dx =  1; dz = 0; break;
				case 2:dz =  1; dx = 0; break;
				case 3:dx = -1; dz = 0; break;
			}
			String s = (i == facing ? "Current facing " : "Facing ");
			boolean[] data = getObstructData(w,exteriorX+dx,exteriorY,exteriorZ+dz);
			if(!data[0])
				s += "obstructed";
			else if(data[1])
				s += "unsafe exit";
			else if(data[2])
			{
				s += "unsafe drop";
				if(data[3])
					s += " into water";
				if(data[4])
					s += " into lava";
			}
			else
				s += " safe";
			string.add(s);
		}
		
		for(String s:string)
			pl.addChatMessage(new ChatComponentText(s));
	}
	
	private String getDimensionName(int worldID)
	{
		if(worldID != 10000)
			return Helper.getDimensionName(worldID);
		return "The Time Vortex";
	}
	
	//////////////////////////////
	//////NBT DATA////////////////
	//////////////////////////////
	
	public void commandRepair(String newO, int numRoom, int en)
	{
		energy = en;
		numRooms = numRoom;
		maxNumRooms = config.getInt("maxRooms", 30);
		setOwner(newO);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
		fast = nbt.hasKey("fast") && nbt.getBoolean("fast");
		oldExteriorWorld = nbt.getInteger("oExW");
		forcedFlight = nbt.getBoolean("fF");
		lockState = LockState.values()[nbt.getInteger("lS")];
		rfStored  = nbt.getInteger("rS");
		if(nbt.hasKey("tP"))
			transmatPoint = SimpleCoordStore.readFromNBT(nbt.getCompoundTag("tP"));
		if(nbt.hasKey("invStore"))
		{
			NBTTagCompound invTag = nbt.getCompoundTag("invStore");
			for(int i = 0;i<items.length;i++)
			{
				if(invTag.hasKey("i"+i))
					items[i] = ItemStack.loadItemStackFromNBT(invTag.getCompoundTag("i"+i));
			}
		}
		if(nbt.hasKey("fS"))
		{
			NBTTagCompound invTag = nbt.getCompoundTag("fS");
			for(int i = 0;i<fluids.length;i++)
			{
				if(invTag.hasKey("i"+i))
					fluids[i] = FluidStack.loadFluidStackFromNBT(invTag.getCompoundTag("i"+i));
			}
		}
		if(nbt.hasKey("mods"))
		{
			int[] mods = nbt.getIntArray("mods");
			if(mods != null && mods.length>0)
			{
				for(int i : mods)
					modders.add(i);
			}
		}
	}
	
	private void storeInv(NBTTagCompound nbt)
	{
		int j = 0;
		NBTTagCompound invTag = new NBTTagCompound();
		for(int i = 0;i<items.length;i++)
		{
			if(items[i] != null)
			{
				NBTTagCompound iTag = new NBTTagCompound();
				invTag.setTag("i"+j, items[i].writeToNBT(iTag));
				j++;
			}
		}
		if(j > 0)
			nbt.setTag("invStore", invTag);
	}
	
	private void storeFlu(NBTTagCompound nbt)
	{
		int j = 0;
		NBTTagCompound invTag = new NBTTagCompound();
		for(int i = 0;i<fluids.length;i++)
		{
			if(fluids[i] != null)
			{
				NBTTagCompound iTag = new NBTTagCompound();
				invTag.setTag("i"+j, fluids[i].writeToNBT(iTag));
				j++;
			}
		}
		if(j > 0)
			nbt.setTag("fS", invTag);
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		if(fast)
			nbt.setBoolean("fast", fast);
		nbt.setInteger("oExW", oldExteriorWorld);
		nbt.setBoolean("fF", forcedFlight);
		nbt.setInteger("lS", lockState.ordinal());
		nbt.setInteger("rS",rfStored);
		if(hasFunction(TardisFunction.TRANSMAT) && isTransmatPointValid())
			nbt.setTag("tP", transmatPoint.writeToNBT());
		storeInv(nbt);
		storeFlu(nbt);
		if(modders != null && modders.size() > 0)
		{
			int[] mods = new int[modders.size()];
			for(int i = 0;i<modders.size();i++)
				mods[i] = modders.get(i);
			nbt.setIntArray("mods", mods);
		}
	}

	@Override
	public void writeTransmittable(NBTTagCompound nbt)
	{
		if(ownerName != null)
		{
			nbt.setDouble("tardisXP", tardisXP);
			nbt.setBoolean("explode",explode);
			nbt.setInteger("tardisLevel", tardisLevel);
			nbt.setString("ownerName", ownerName);
			nbt.setInteger("extWorld", exteriorWorld);
			nbt.setInteger("extX", exteriorX);
			nbt.setInteger("extY", exteriorY);
			nbt.setInteger("extZ", exteriorZ);
			
			nbt.setInteger("energy",energy);
			
			nbt.setBoolean("iF", inFlight);
			nbt.setInteger("fT", flightTimer);
			nbt.setInteger("tFT", totalFlightTimer);
			nbt.setInteger("iFT", inFlightTimer);
			nbt.setInteger("numR", numRooms);
			nbt.setDouble("sped", speed);
			
			nbt.setInteger("shld",shields);
			nbt.setInteger("hull",hull);
			
			
			if(inFlight())
			{
				nbt.setInteger("ttTO", timeTillTakenOff);
				nbt.setInteger("ttL" , timeTillLanding);
				nbt.setInteger("ttLI", timeTillLandingInt);
				nbt.setInteger("ttLa", timeTillLanded);
			}
			if(upgradeLevels.size() > 0)
			{
				for(TardisUpgradeMode mode : upgradeLevels.keySet())
				{
					int am = upgradeLevels.get(mode);
					if(am > 0)
						nbt.setInteger("uG" + mode.ordinal(), am);
				}
			}
		}
	}

	@Override
	public void readTransmittable(NBTTagCompound nbt)
	{
		if(nbt.hasKey("ownerName"))
		{
			tardisXP  = nbt.getDouble("tardisXP");
			explode = nbt.getBoolean("explode");
			tardisLevel = nbt.getInteger("tardisLevel");
			ownerName = nbt.getString("ownerName");
			exteriorWorld = nbt.getInteger("extWorld");
			exteriorX = nbt.getInteger("extX");
			exteriorY = nbt.getInteger("extY");
			exteriorZ = nbt.getInteger("extZ");
			
			energy = nbt.getInteger("energy");
			
			flightTimer = nbt.getInteger("fT");
			inFlightTimer = nbt.getInteger("iFT");
			totalFlightTimer = nbt.getInteger("tFT");
			inFlight = nbt.getBoolean("iF");
			numRooms = nbt.getInteger("numR");
			speed = nbt.getDouble("sped");
			
			shields  = nbt.getInteger("shld");
			hull     = nbt.getInteger("hull");
			
			if(nbt.hasKey("ttTO"))
			{
				timeTillTakenOff	= nbt.getInteger("ttTO");
				timeTillLanding		= nbt.getInteger("ttL");
				timeTillLandingInt	= nbt.getInteger("ttLI");
				timeTillLanded		= nbt.getInteger("ttLa");
			}
			
			for(TardisUpgradeMode mode :TardisUpgradeMode.values())
			{
				if(nbt.hasKey("uG"+mode.ordinal()))
				{
					int am = nbt.getInteger("uG"+mode.ordinal());
					if(am > 0)
						upgradeLevels.put(mode, am);
				}
			}
		}
	}

	@Override
	public boolean shouldChunkload()
	{
		return true;
	}

	@Override
	public SimpleCoordStore coords()
	{
		return coords;
	}

	@Override
	public ChunkCoordIntPair[] loadable()
	{
		return loadable;
	}
	
	public boolean canBeAccessedExternally()
	{
		EngineTileEntity engine = getEngine();
		if(engine == null)
			return false;
		return !engine.getInternalOnly();
	}
	
	public IGridNode getNode()
	{
		if(TardisMod.aeAPI == null)
			return null;
		if(node == null)
			node = TardisMod.aeAPI.createGridNode(new CoreGrid(this));
		return node;
	}

	@Override
	public IGridNode getGridNode(ForgeDirection dir)
	{
		if(dir != ForgeDirection.UNKNOWN)
			return null;
		return getNode();
	}

	@Override
	public AECableType getCableConnectionType(ForgeDirection dir)
	{
		// TODO Auto-generated method stub
		return AECableType.NONE;
	}

	@Override
	public void securityBreak()
	{
	}

}