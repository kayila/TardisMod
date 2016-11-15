package tardis.common.dimension;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomePlains;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BiomeGenConsoleRoom extends BiomePlains{

	public BiomeGenConsoleRoom() {
		this(new BiomeProperties("TARDIS"));
	}
	public BiomeGenConsoleRoom(BiomeProperties properties) {
		super(false, properties);
        //this.biomeName = "TARDIS";
        //this.color = 4396444;
    }

	/*
	@Override
	@SideOnly(Side.CLIENT)
	public int getBiomeGrassColor(int x, int y, int z){
		return 4396444;
	}
	*/
}
