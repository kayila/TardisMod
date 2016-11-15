package tardis;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import io.darkcraft.darkcore.mod.DarkcoreMod;
import io.darkcraft.darkcore.mod.DarkcoreTeleporter;
import io.darkcraft.darkcore.mod.abstracts.AbstractBlock;
import io.darkcraft.darkcore.mod.abstracts.AbstractItem;
import io.darkcraft.darkcore.mod.config.ConfigHandler;
import io.darkcraft.darkcore.mod.config.ConfigHandlerFactory;
import io.darkcraft.darkcore.mod.helpers.PlayerHelper;
import io.darkcraft.darkcore.mod.interfaces.IConfigHandlerMod;
import net.minecraft.item.ItemStack;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import tardis.common.TardisProxy;
import tardis.common.blocks.BatteryBlock;
import tardis.common.blocks.ColorableBlock;
import tardis.common.blocks.ColorableOpenRoundelBlock;
import tardis.common.blocks.ComponentBlock;
import tardis.common.blocks.CompressedBlock;
import tardis.common.blocks.ConsoleBlock;
import tardis.common.blocks.CoreBlock;
import tardis.common.blocks.CraftableCSimBlock;
import tardis.common.blocks.CraftableSimBlock;
import tardis.common.blocks.DecoBlock;
import tardis.common.blocks.DecoTransBlock;
import tardis.common.blocks.EngineBlock;
import tardis.common.blocks.ForceFieldBlock;
import tardis.common.blocks.GravityLiftBlock;
import tardis.common.blocks.InteriorDirtBlock;
import tardis.common.blocks.InternalDoorBlock;
import tardis.common.blocks.InternalMagicDoorBlock;
import tardis.common.blocks.LabBlock;
import tardis.common.blocks.LandingPadBlock;
import tardis.common.blocks.ManualBlock;
import tardis.common.blocks.ManualHelperBlock;
import tardis.common.blocks.SchemaBlock;
import tardis.common.blocks.SchemaComponentBlock;
import tardis.common.blocks.SchemaCoreBlock;
import tardis.common.blocks.ShieldBlock;
import tardis.common.blocks.SlabBlock;
import tardis.common.blocks.StairBlock;
import tardis.common.blocks.SummonerBlock;
import tardis.common.blocks.TardisBlock;
import tardis.common.blocks.TemporalAcceleratorBlock;
import tardis.common.blocks.TopBlock;
import tardis.common.command.CommandRegister;
import tardis.common.core.CreativeTab;
import tardis.common.core.DimensionEventHandler;
import tardis.common.core.SchemaHandler;
import tardis.common.core.TardisDimensionRegistry;
import tardis.common.core.TardisOwnershipRegistry;
import tardis.common.core.events.internal.DamageEventHandler;
import tardis.common.core.flight.FlightConfiguration;
import tardis.common.core.helpers.Helper;
import tardis.common.core.helpers.ScrewdriverHelperFactory;
import tardis.common.dimension.BiomeGenConsoleRoom;
import tardis.common.dimension.TardisDimensionHandler;
import tardis.common.dimension.TardisWorldProvider;
import tardis.common.dimension.damage.TardisDamageSystem;
import tardis.common.integration.ae.AEHelper;
import tardis.common.items.ComponentItem;
import tardis.common.items.CraftingComponentItem;
import tardis.common.items.DecoratingTool;
import tardis.common.items.DimensionUpgradeItem;
import tardis.common.items.KeyItem;
import tardis.common.items.ManualItem;
import tardis.common.items.NameTagItem;
import tardis.common.items.SchemaItem;
import tardis.common.items.SonicScrewdriverItem;
import tardis.common.items.UpgradeChameleonItem;
import tardis.common.items.UpgradeItem;
import tardis.common.items.extensions.ScrewTypeRegister;
import tardis.common.items.extensions.screwtypes.AbstractScrewdriverType;
import tardis.common.items.extensions.screwtypes.Eighth;
import tardis.common.items.extensions.screwtypes.Tenth;
import tardis.common.items.extensions.screwtypes.Twelth;
import tardis.common.network.TardisPacketHandler;
import tardis.common.tileents.extensions.chameleon.ChameleonRegistry;
import tardis.common.tileents.extensions.chameleon.tardis.AbstractTardisChameleon;
import tardis.common.tileents.extensions.chameleon.tardis.DefaultTardisCham;
import tardis.common.tileents.extensions.chameleon.tardis.NewTardisCham;
import tardis.common.tileents.extensions.chameleon.tardis.PostboxTardisCham;
//import thaumcraft.api.ItemApi; // TODO Re-enable when TC updates to 1.10

@Mod(modid = "TardisMod", name = "Tardis Mod", version = "0.995", dependencies = "required-after:FML; required-after:darkcore@[0.4,0.49]; after:CoFHCore; after:appliedenergistics2; after:Waila; before:DragonAPI")
public class TardisMod implements IConfigHandlerMod
{
	@Instance
	public static TardisMod										i;
	public static final String									modName				= "TardisMod";
	public static boolean										inited				= false;

	@SidedProxy(clientSide = "tardis.client.TardisClientProxy", serverSide = "tardis.common.TardisProxy")
	public static TardisProxy									proxy;
	public static DimensionEventHandler							dimEventHandler		= new DimensionEventHandler();

	public static ConfigHandler									configHandler;
	public static SchemaHandler									schemaHandler;

	public static DarkcoreTeleporter							teleporter			= null;
	public static TardisDimensionHandler						otherDims;
	public static TardisDimensionRegistry						dimReg;
	public static TardisOwnershipRegistry						plReg;
	public static CreativeTab									tab					= null;
	public static CreativeTab									cTab				= null;
	public static Biome											consoleBiome		;

	public static AbstractBlock									tardisBlock;
	public static AbstractBlock									tardisTopBlock;
	public static AbstractBlock									tardisCoreBlock;
	public static AbstractBlock									tardisConsoleBlock;
	public static AbstractBlock									tardisEngineBlock;
	public static AbstractBlock									componentBlock;
	public static InternalDoorBlock								internalDoorBlock;
	public static AbstractBlock									decoBlock;
	public static AbstractBlock									decoTransBlock;
	public static AbstractBlock									schemaBlock;
	public static AbstractBlock									schemaCoreBlock;
	public static AbstractBlock									schemaComponentBlock;
	public static AbstractBlock									debugBlock;
	public static AbstractBlock									landingPad;
	public static AbstractBlock									gravityLift;
	public static AbstractBlock									forcefield;
	public static AbstractBlock									battery;
	public static StairBlock									stairBlock;
	public static AbstractBlock									slabBlock;
	public static AbstractBlock									interiorDirtBlock;
	public static AbstractBlock									temporalAccelerator;
	public static AbstractBlock									manualBlock;
	public static AbstractBlock									manualHelperBlock;
	public static AbstractBlock									summonerBlock;
	public static AbstractBlock									magicDoorBlock;
	public static AbstractBlock									shieldBlock;

	public static AbstractBlock									colorableWallBlock;
	public static AbstractBlock									colorableFloorBlock;
	public static AbstractBlock									colorableBrickBlock;
	public static AbstractBlock									colorablePlankBlock;
	public static AbstractBlock									colorableRoundelBlock;
	public static AbstractBlock									colorableOpenRoundelBlock;

	public static AbstractBlock									wallSimulacrumBlock;
	public static AbstractBlock									floorSimulacrumBlock;
	public static AbstractBlock									glassSimulacrumBlock;
	public static AbstractBlock									brickSimulacrumBlock;
	public static AbstractBlock									plankSimulacrumBlock;

	public static AbstractBlock									compressedBlock;

	public static AbstractBlock									decoSimulacrumBlock;
	public static LabBlock										labBlock;

	public static HashSet<AbstractBlock> 						unbreakableBlocks;

	public static AbstractItem									schemaItem;
	public static AbstractItem									componentItem;
	public static CraftingComponentItem							craftingComponentItem;
	public static KeyItem										keyItem;
	public static SonicScrewdriverItem							screwItem;
	public static ManualItem									manualItem;
	public static UpgradeItem									upgradeItem;
	public static NameTagItem									nameTag;
	public static AbstractItem									decoTool;
	public static UpgradeChameleonItem							chameleonUpgradeItem;
	public static Map<Integer,AbstractItem>						dimensionUpgradeItems = new HashMap<Integer, AbstractItem>();

	public static boolean										tcInstalled			= false;

	public static AbstractScrewdriverType						defaultType			= new Tenth();
	public static ChameleonRegistry<AbstractTardisChameleon>	tardisChameleonReg	= new ChameleonRegistry(new DefaultTardisCham());

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) throws IOException
	{
		configHandler = ConfigHandlerFactory.getConfigHandler(this);
		schemaHandler = new SchemaHandler(configHandler);
		schemaHandler.getSchemas();
		tab = new CreativeTab("TardisModTab");
		cTab = new CreativeTab("TardisModCraftableTab");
		DarkcoreMod.registerCreativeTab(modName, tab);

		refreshConfigs();

		//consoleBiome = new BiomeGenConsoleRoom(Configs.consoleBiomeID);
		consoleBiome = new BiomeGenConsoleRoom();
//		BiomeDictionary.registerBiomeType(consoleBiome, BiomeDictionary.Type.PLAINS);
		DimensionManager.registerProviderType(Configs.providerID, TardisWorldProvider.class, Configs.tardisLoaded);
		initChameleonTypes();
		initBlocks();
		initItems();


		// MinecraftForge.EVENT_BUS.register(new SoundHandler());

		proxy.postAssignment();
	}

	private void initChameleonTypes()
	{
		tardisChameleonReg.register(new NewTardisCham());
		tardisChameleonReg.register(new PostboxTardisCham());

		ScrewTypeRegister.register(defaultType);
		ScrewTypeRegister.register(new Twelth());
		ScrewTypeRegister.register(new Eighth());
	}

	public static void refreshConfigs()
	{
		TardisDimensionHandler.refreshConfigs();
		TardisDamageSystem.refreshConfigs();
		FlightConfiguration.refreshConfigs();
		Configs.refreshConfigs();
	}

	@EventHandler
	public void doingInit(FMLInitializationEvent event)
	{
		proxy.init();
		TardisPacketHandler.registerHandlers();
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		tardisChameleonReg.postInit();
		AEHelper.init();
		initRecipes();
		FMLCommonHandler.instance().bus().register(dimEventHandler);
		MinecraftForge.EVENT_BUS.register(dimEventHandler);
		inited = true;
		// TODO Re-enable this check when TC is upgraded to 1.10
		tcInstalled = false; // ItemApi.getItem("itemResource", 0) != null;
		CommandRegister.registerListeners();
		if (Configs.keyOnFirstJoin) PlayerHelper.registerJoinItem(new ItemStack(keyItem, 1));
		FMLCommonHandler.instance().bus().register(ScrewdriverHelperFactory.i);
	}

	private void initBlocks()
	{
		tardisBlock = new TardisBlock().register();
		tardisTopBlock = new TopBlock().register();
		tardisCoreBlock = new CoreBlock().register();
		tardisConsoleBlock = new ConsoleBlock().register();
		tardisEngineBlock = new EngineBlock().register();
		componentBlock = new ComponentBlock().register();
		internalDoorBlock = (InternalDoorBlock) new InternalDoorBlock().register();
		decoBlock = new DecoBlock().register();
		decoTransBlock = new DecoTransBlock().register();
		interiorDirtBlock = new InteriorDirtBlock().register();
		temporalAccelerator = new TemporalAcceleratorBlock().register();
		schemaBlock = new SchemaBlock(Configs.visibleSchema).register();
		schemaCoreBlock = new SchemaCoreBlock(Configs.visibleSchema).register();
		schemaComponentBlock = new SchemaComponentBlock().register();
		slabBlock = new SlabBlock().register();
		landingPad = new LandingPadBlock().register();
		labBlock = (LabBlock) new LabBlock().register();
		gravityLift = new GravityLiftBlock().register();
		forcefield = new ForceFieldBlock(Configs.visibleForceField).register();
		battery = new BatteryBlock().register();
		colorableWallBlock = new ColorableBlock("ColorableWall").register();
		colorableFloorBlock = new ColorableBlock("ColorableFloor").register();
		colorableBrickBlock = new ColorableBlock("ColorableBrick").register();
		colorablePlankBlock = new ColorableBlock("ColorablePlank").register();
		colorableRoundelBlock = new ColorableBlock("ColorableRoundel").register();
		colorableOpenRoundelBlock = new ColorableOpenRoundelBlock().register();
		manualBlock = new ManualBlock().register();
		manualHelperBlock = new ManualHelperBlock().register();
		stairBlock = new StairBlock().register();
		summonerBlock = new SummonerBlock().register();
		magicDoorBlock = new InternalMagicDoorBlock().register();
		wallSimulacrumBlock = new CraftableCSimBlock(colorableWallBlock).register();
		floorSimulacrumBlock = new CraftableCSimBlock(colorableFloorBlock).register();
		brickSimulacrumBlock = new CraftableCSimBlock(colorableBrickBlock).register();
		plankSimulacrumBlock = new CraftableCSimBlock(colorablePlankBlock).register();
		glassSimulacrumBlock = new CraftableSimBlock(decoTransBlock).register();
		decoSimulacrumBlock = new CraftableSimBlock(decoBlock).register();
		shieldBlock = new ShieldBlock().register();
		compressedBlock = new CompressedBlock().register();

		AbstractBlock[] unbreakableTardisBlocks = {tardisBlock, tardisTopBlock, tardisCoreBlock,
												   tardisConsoleBlock, tardisEngineBlock, componentBlock,
												   internalDoorBlock, decoBlock, decoTransBlock,
												   interiorDirtBlock, schemaBlock, schemaCoreBlock,
												   schemaComponentBlock, slabBlock, landingPad,
												   labBlock, gravityLift, forcefield, battery,
												   colorableWallBlock, colorableFloorBlock, colorableBrickBlock,
												   colorablePlankBlock, colorableRoundelBlock, colorableOpenRoundelBlock,
												   manualBlock, manualHelperBlock, shieldBlock, temporalAccelerator};

		unbreakableBlocks = new HashSet<AbstractBlock>(Arrays.asList(unbreakableTardisBlocks));
	}

	private void initItems()
	{
		schemaItem = new SchemaItem().register();
		screwItem = (SonicScrewdriverItem) new SonicScrewdriverItem().register();
		keyItem = (KeyItem) new KeyItem().register();
		componentItem = new ComponentItem().register();
		craftingComponentItem = (CraftingComponentItem) new CraftingComponentItem().register();
		manualItem = (ManualItem) new ManualItem().register();
		upgradeItem = (UpgradeItem) new UpgradeItem().register();
		nameTag = (NameTagItem) new NameTagItem().register();
		decoTool = new DecoratingTool().register();
		chameleonUpgradeItem = (UpgradeChameleonItem) new UpgradeChameleonItem().register();

			if(!Configs.dimUpgradesIds[0].isEmpty())
				for(int i = 0; i < Configs.dimUpgradesIds.length; i++){
						try{
						dimensionUpgradeItems.put(Integer.parseInt(Configs.dimUpgradesIds[i]), new DimensionUpgradeItem(Integer.parseInt(Configs.dimUpgradesIds[i])).register());
						}catch(Exception e){
							e.printStackTrace();
						}
				}
	}

	private void initRecipes()
	{
		keyItem.initRecipes();
		componentItem.initRecipes();
		manualItem.initRecipes();
		labBlock.initRecipes();
		landingPad.initRecipes();
		forcefield.initRecipes();
		gravityLift.initRecipes();
		battery.initRecipes();
		interiorDirtBlock.initRecipes();
		craftingComponentItem.initRecipes();
		summonerBlock.initRecipes();
		upgradeItem.initRecipes();
		nameTag.initRecipes();
		decoTool.initRecipes();
		chameleonUpgradeItem.initRecipes();
		shieldBlock.initRecipes();
		CraftableSimBlock.initStaticRecipes();
		temporalAccelerator.initRecipes();
		compressedBlock.initRecipes();
	}

	@EventHandler
	public void serverAboutToStart(FMLServerAboutToStartEvent event)
	{
		if (otherDims != null) MinecraftForge.EVENT_BUS.unregister(otherDims);
		otherDims = new TardisDimensionHandler();
		plReg = null;
		dimReg = null;
		Helper.datastoreMap.clear();
		Helper.ssnDatastoreMap.clear();
		MinecraftForge.EVENT_BUS.register(otherDims);
		DamageEventHandler.i.register();
		ScrewdriverHelperFactory.i.clear();
	}

	@EventHandler
	public void serverStarting(FMLServerStartingEvent event)
	{
		TardisDimensionRegistry.loadAll();
		dimReg.registerDims();
		TardisOwnershipRegistry.loadAll();
		TardisOwnershipRegistry.saveAll();
		teleporter = new DarkcoreTeleporter(event.getServer().worldServerForDimension(0));
		CommandRegister.registerCommands(event);
	}

	@EventHandler
	public void serverStarted(FMLServerStartedEvent event)
	{
		otherDims.findDimensions();
	}

	@Override
	public String getModID()
	{
		return modName;
	}
}
