package com.acuitybotting.path_finding.rs.utils;

import com.acuitybotting.db.arango.path_finding.domain.xtea.Xtea;
import com.acuitybotting.path_finding.enviroment.PathingEnviroment;
import com.acuitybotting.path_finding.rs.domain.location.Location;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by Zachary Herridge on 6/11/2018.
 */
@Slf4j
public class RsEnvironment {

    public static final File INFO_BASE = new File("C:\\Users\\S3108772\\Desktop\\Map Info");

    public static final int PLANE_PENALTY = 25;

    private static RsMap rsMap = new RsMap();

    private static int regionImageBase = 0;

    private static Map<String, BufferedImage> regionImageMap = new HashMap<>();

    public static BufferedImage getRegionImage(int regionId, int plane) {
        return regionImageMap.computeIfAbsent(regionId + "_" + plane, s -> {
            try {
                File file = new File(getRegionImageBase(), + regionId + "_" + plane + ".png");
                return file.exists() ? ImageIO.read(file) : null;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    public static RsMap getRsMap() {
        return rsMap;
    }

    public static void setRsMap(RsMap rsMap) {
        RsEnvironment.rsMap = rsMap;
    }

    public static File getRegionImageBase() {
        return PathingEnviroment.IMG_DIRECTORIES[getRegionImageBaseIndex()];
    }

    public static int getRegionImageBaseIndex() {
        return regionImageBase;
    }

    public static void setRegionImageBaseIndex(int regionImageBase) {
        RsEnvironment.regionImageBase = regionImageBase;
        regionImageMap.clear();
    }

    public static BufferedImage getRegionImage(Location location, int plane) {
        return getRegionImage(RegionUtils.locationToRegionId(location), plane);
    }
}
