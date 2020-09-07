package com.snowgears.colorportals.utils;

/**
 * Basic utilities I use for a lot of my plugins that I put into one place.
 * Feel free to use this when creating any of your own plugins.
 *
 * Created by SnowGears (Tanner Embry)
 */

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.HashMap;
import java.util.UUID;

public class BukkitUtils {

    private HashMap<Material, DyeColor> woolColorMap = new HashMap<>();

    //takes two locations, returns a blockface and a number
    public BukkitUtils(){
        populateWoolColorMap();
    }

    /**
     * Takes two locations and calculates all cardinal distances
     * Return:
     * - HashMap<BlockFace, Integer>: cardinal direction, distance in that direction
     */
    public static HashMap<BlockFace, Integer> getCardinalDistances(Location startLocation, Location endLocation) {
        HashMap<BlockFace, Integer> cardinalDistances = new HashMap<BlockFace, Integer>();
        int northSouth = startLocation.getBlockZ() - endLocation.getBlockZ();
        if (northSouth >= 0)
            cardinalDistances.put(BlockFace.NORTH, Math.abs(northSouth));
        else
            cardinalDistances.put(BlockFace.SOUTH, Math.abs(northSouth));
        int eastWest = startLocation.getBlockX() - endLocation.getBlockX();
        if (eastWest <= 0)
            cardinalDistances.put(BlockFace.EAST, Math.abs(eastWest));
        else
            cardinalDistances.put(BlockFace.WEST, Math.abs(eastWest));
        int upDown = startLocation.getBlockY() - endLocation.getBlockY();
        if (upDown <= 0)
            cardinalDistances.put(BlockFace.UP, Math.abs(upDown));
        else
            cardinalDistances.put(BlockFace.DOWN, Math.abs(upDown));
        return cardinalDistances;
    }

    /**
     * Converts a BlockFace direction to a byte
     * Return:
     * - byte: the basic data of the BlockFace direction provided
     */
    public byte determineDataOfDirection(BlockFace bf) {
        if (bf.equals(BlockFace.NORTH))
            return 2;
        if (bf.equals(BlockFace.SOUTH))
            return 5;
        if (bf.equals(BlockFace.WEST))
            return 3;
        return ((byte) (!bf.equals(BlockFace.EAST) ? 0 : 4));
    }

    /**
     * Converts a BlockFace direction to a yaw (float) value
     * Return:
     * - float: the yaw value of the BlockFace direction provided
     */
    public float faceToYaw(BlockFace bf) {
        if (bf.equals(BlockFace.NORTH))
            return 0F;
        else if (bf.equals(BlockFace.EAST))
            return 90F;
        else if (bf.equals(BlockFace.SOUTH))
            return 180F;
        else if (bf.equals(BlockFace.WEST))
            return 270F;
        return 0F;
    }

    /**
     * Checks if a String is an Integer
     * Return:
     * - true: String is an Integer
     * - false: String is not an Integer
     */
    public boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public boolean isWool(Material material) {
        if(woolColorMap.containsKey(material)) {
            return true;
        }
        return false;
    }

    /**
     * Gets the color from a (wool) block
     * Return:
     * - DyeColor: the color of the (wool) block
     */
    public DyeColor getWoolColor(Block block) {
        return woolColorMap.get(block.getType());
    }

    private void populateWoolColorMap(){
        woolColorMap.put(Material.RED_WOOL, DyeColor.RED);
        woolColorMap.put(Material.WHITE_WOOL, DyeColor.WHITE);
        woolColorMap.put(Material.BLACK_WOOL, DyeColor.BLACK);
        woolColorMap.put(Material.BLUE_WOOL, DyeColor.BLUE);
        woolColorMap.put(Material.BROWN_WOOL, DyeColor.BROWN);
        woolColorMap.put(Material.CYAN_WOOL, DyeColor.CYAN);
        woolColorMap.put(Material.GRAY_WOOL, DyeColor.GRAY);
        woolColorMap.put(Material.GREEN_WOOL, DyeColor.GREEN);
        woolColorMap.put(Material.LIGHT_BLUE_WOOL, DyeColor.LIGHT_BLUE);
        woolColorMap.put(Material.LIGHT_GRAY_WOOL, DyeColor.LIGHT_GRAY);
        woolColorMap.put(Material.LIME_WOOL, DyeColor.LIME);
        woolColorMap.put(Material.MAGENTA_WOOL, DyeColor.MAGENTA);
        woolColorMap.put(Material.ORANGE_WOOL, DyeColor.ORANGE);
        woolColorMap.put(Material.PINK_WOOL, DyeColor.PINK);
        woolColorMap.put(Material.PURPLE_WOOL, DyeColor.PURPLE);
        woolColorMap.put(Material.YELLOW_WOOL, DyeColor.YELLOW);
    }

    //TODO NEED TO REDO WHOLE COLORPORTALS NAME BY UUID SYSTEM
    /**
     * Fetches a players name from their UUID
     * Return:
     * - String: Players current name
     */
    public String getPlayerFromUUID(UUID uid) {
//        NameFetcher nameFetcher = new NameFetcher(Arrays.asList(uid));
//
//        Map<UUID, String> response;
//        try {
//            response = nameFetcher.call();
//        } catch (Exception e) {
//            return null;
//        }
//
//        if (response.containsKey(uid)) {
//            return response.get(uid);
//        }
        return null;
    }

//    /**
//     * Fetches multiple player names from a list of their UUIDs
//     * Return:
//     * - Map<UUID, String>:Player UUIDs mapped to their current names
//     */
//    public Map<UUID, String> getPlayersFromUUIDs(List<UUID> uidList) {
//        NameFetcher nameFetcher = new NameFetcher(uidList);
//
//        Map<UUID, String> response = null;
//        try {
//            response = nameFetcher.call();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return response;
//    }

//    /**
//     * Utility written by EvilMidget38
//     */
//    private class NameFetcher implements Callable<Map<UUID, String>> {
//        private static final String PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
//        private final JSONParser jsonParser = new JSONParser();
//        private final List<UUID> uuids;
//
//        public NameFetcher(List<UUID> uuids) {
//            this.uuids = ImmutableList.copyOf(uuids);
//        }
//
//        @Override
//        public Map<UUID, String> call() throws Exception {
//            Map<UUID, String> uuidStringMap = new HashMap<UUID, String>();
//            for (UUID uuid : uuids) {
//                HttpURLConnection connection = (HttpURLConnection) new URL(PROFILE_URL + uuid.toString().replace("-", "")).openConnection();
//                JSONObject response = (JSONObject) jsonParser.parse(new InputStreamReader(connection.getInputStream()));
//                String name = (String) response.get("name");
//                if (name == null) {
//                    continue;
//                }
//                String cause = (String) response.get("cause");
//                String errorMessage = (String) response.get("errorMessage");
//                if (cause != null && cause.length() > 0) {
//                    throw new IllegalStateException(errorMessage);
//                }
//                uuidStringMap.put(uuid, name);
//            }
//            return uuidStringMap;
//        }
//    }
}


