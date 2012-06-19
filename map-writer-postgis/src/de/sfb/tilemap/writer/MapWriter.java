package de.sfb.tilemap.writer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.writer.MapFileWriter;
import org.mapsforge.map.writer.RAMTileBasedDataProcessor;
import org.mapsforge.map.writer.model.MapWriterConfiguration;
import org.mapsforge.map.writer.model.TDWay;
import org.mapsforge.map.writer.model.TileBasedDataProcessor;
import org.mapsforge.map.writer.model.TileCoordinate;
import org.postgresql.PGConnection;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

import de.sfb.tilemap.writer.util.PGHStore;

//import org.postgresql.PGConnection;
public class MapWriter {
    private static final Logger LOGGER = Logger.getLogger(MapFileWriter.class
            .getName());

    private static MapWriterConfiguration conf;
    private static TileBasedDataProcessor dataProcessor;
    public static Connection connection;
    private static WKBReader wkbReader;

    private static void init() {
        conf = new MapWriterConfiguration();
        conf.addOutputFile("test.map");
        conf.setWriterVersion("4");
        conf.loadTagMappingFile("src/config/tag-mapping.xml");

        // conf.addMapStartPosition("53.055,8.45");
        // conf.addMapStartZoom("10");
        // conf.addBboxConfiguration("48,6,54,10");
        // conf.addZoomIntervalConfiguration("4,0,4,"+"5,5,6,"+"7,7,9,"+"10,10,12,"+"13,13,15");

        conf.addMapStartPosition("53,9");
        conf.addMapStartZoom("10");
        conf.addBboxConfiguration("53 ,9,54, 10");
        conf.addZoomIntervalConfiguration("11,11,11");// ("5,0,7,10,8,11,14,12,18");

        // conf.addMapStartPosition("53,9");
        // conf.addMapStartZoom("3");
        // conf.addBboxConfiguration("-60 ,-180, 80, 180");
        // // conf.addBboxConfiguration("20 ,0, 60, 20");
        // conf.addZoomIntervalConfiguration("6,6,6");//
        // ("5,0,7,10,8,11,14,12,18");

        conf.setComment("yo!");
        conf.setDebugStrings(false);
        conf.setPolygonClipping(false);
        conf.setWayClipping(false);
        conf.setSimplification(0.0001);
        conf.setDataProcessorType("ram");
        conf.setBboxEnlargement(10);
        conf.setPreferredLanguage("de");
        conf.addEncodingChoice("auto");
        conf.setFileSpecificationVersion(4);
        conf.validate();

        dataProcessor = RAMTileBasedDataProcessor.newInstance(conf);

    }

    public static void complete() {
        LOGGER.info("completing read...");
        dataProcessor.complete();

        LOGGER.info("start writing file...");

        try {
            MapFileWriter.writeFile(conf, dataProcessor);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "error while writing file", e);
        }
    }

    private static String tileToBOX3D(Tile tile, int pixel) {
        double minLat = MercatorProjection.pixelYToLatitude(tile.getPixelY()
                + Tile.TILE_SIZE + pixel, tile.zoomLevel);
        double maxLat = MercatorProjection.pixelYToLatitude(tile.getPixelY()
                - pixel, tile.zoomLevel);

        double minLon = MercatorProjection.pixelXToLongitude(tile.getPixelX()
                - pixel, tile.zoomLevel);
        double maxLon = MercatorProjection.pixelXToLongitude(tile.getPixelX()
                + Tile.TILE_SIZE + pixel, tile.zoomLevel);

        return "ST_SetSRID('BOX3D(" + minLon + " " + minLat + ", " + " "
                + maxLon + " " + maxLat + ")'::box3d ,4326)";
    }

    private static String getQuery(TileCoordinate t) {
        Tile tile = new Tile(t.getX(), t.getY(), t.getZoomlevel());

        // String bbox = tileToBOX3D(tile.tileX, tile.tileY, tile.zoomLevel, 2);
        String bbox = tileToBOX3D(tile, 2);

        String table_ocean = "ne.\"110m_ocean\"";
        String table_admin = "ne.\"110m_admin_0_lines\"";
        String table_ways;

        if (tile.zoomLevel < 5) {
            table_ocean = "ne.\"50m_ocean\"";
            table_admin = "ne.\"50m_admin_0_lines\"";
            table_ways = "geometries.routes_simp";

            return "SELECT gid::bigint as id, ('natural' => 'water') as tags,"
                    + " ST_AsEWKB((ST_Dump(ST_Intersection("
                    + bbox
                    + ",geom))).geom) FROM "
                    + table_ocean
                    + " WHERE geom && "
                    + bbox
                    + " UNION ALL "
                    + " SELECT gid::bigint as id, ('boundary' => 'administrative') || ('admin_level' => '2') as tags,"
                    + " ST_AsEWKB(geom) FROM " + table_admin
                    + " WHERE geom && " + bbox;

        }

        if (tile.zoomLevel < 7) {
            table_ocean = "ne.\"50m_ocean\"";
            table_admin = "ne.\"50m_admin_0_lines\"";
            table_ways = "geometries.routes_simp";

            return "SELECT gid::bigint as id, ('natural' => 'water') as tags,"
                    + " ST_AsEWKB((ST_Dump(ST_Intersection("
                    + bbox
                    + ",geom))).geom) FROM "
                    + table_ocean
                    + " WHERE geom && "
                    + bbox
                    + " UNION ALL "
                    + " SELECT gid::bigint as id, ('boundary' => 'administrative') || ('admin_level' => '2') as tags,"
                    + " ST_AsEWKB(geom) FROM "
                    + table_admin
                    + " WHERE geom && "
                    + bbox
                    + " UNION ALL "
                    + " SELECT id, CASE WHEN (way_type = 0) THEN ('highway' => 'motorway') ELSE ('highway' => 'primary') END as tags,"
                    + " ST_AsEWKB(ST_Intersection(" + bbox + ",geom)) FROM "
                    + table_ways
                    + " WHERE geom && " + bbox + " AND way_type < 8";
        }

        if (tile.zoomLevel < 9) {
            table_ocean = "ne.\"10m_ocean\"";
            table_admin = "ne.\"50m_admin_0_lines\"";
            table_ways = "geometries.ways2";

            return "SELECT gid::bigint as id, ('natural' => 'water') as tags,"
                    + " ST_AsEWKB((ST_Dump(ST_Intersection("
                    + bbox
                    + ",ST_SetSRID(geom,4326)))).geom) FROM "
                    + table_ocean
                    + " WHERE geom && "
                    + bbox
                    + " UNION ALL "
                    + " SELECT gid::bigint as id, ('boundary' => 'administrative') || ('admin_level' => '2') as tags,"
                    + " ST_AsEWKB(geom) FROM " + table_admin
                    + " WHERE geom && " + bbox
                    + " UNION ALL "
                    + " SELECT id, ('highway' => way_type) as tags,"
                    + " ST_AsEWKB(ST_Intersection(" + bbox + ",geom)) FROM "
                    + table_ways
                    + " WHERE geom && " + bbox;
        }

        if (tile.zoomLevel < 10) {
            table_ocean = "ne.\"10m_ocean\"";
            table_admin = "ne.\"50m_admin_0_lines\"";
            table_ways = "geometries.ways";

            return "SELECT gid::bigint as id, ('natural' => 'water') as tags,"
                    + " ST_AsEWKB((ST_Dump(ST_Intersection("
                    + bbox
                    + ",ST_SetSRID(geom,4326)))).geom) FROM "
                    + table_ocean
                    + " WHERE geom && "
                    + bbox
                    + " UNION ALL "
                    + " SELECT gid::bigint as id, ('boundary' => 'administrative') || ('admin_level' => '2') as tags,"
                    + " ST_AsEWKB(geom) FROM " + table_admin
                    + " WHERE geom && " + bbox
                    + " UNION ALL "
                    + " SELECT id, ('highway' => way_type) as tags,"
                    + " ST_AsEWKB(ST_Intersection(" + bbox + ",geom)) FROM "
                    + table_ways
                    + " WHERE geom && " + bbox;
        }

        if (tile.zoomLevel < 11) {
            table_ocean = "ne.\"10m_ocean\"";
            table_admin = "ne.\"50m_admin_0_lines\"";
            table_ways = "geometries.ways3";

            return "SELECT gid::bigint as id, ('natural' => 'water') as tags,"
                    + " ST_AsEWKB((ST_Dump(ST_Intersection("
                    + bbox
                    + ",ST_SetSRID(geom,4326)))).geom) FROM "
                    + table_ocean
                    + " WHERE geom && "
                    + bbox
                    + " UNION ALL "
                    + " SELECT gid::bigint as id, ('boundary' => 'administrative') || ('admin_level' => '2') as tags,"
                    + " ST_AsEWKB(geom) FROM " + table_admin
                    + " WHERE geom && " + bbox
                    + " UNION ALL "
                    + " SELECT id, ('highway' => way_type) as tags,"
                    + " ST_AsEWKB(ST_Intersection(" + bbox + ",geom)) FROM "
                    + table_ways
                    + " WHERE geom && " + bbox;
        }

        table_ocean = "ne.\"10m_ocean\"";
        table_admin = "ne.\"50m_admin_0_lines\"";
        table_ways = "ONLY ways";

        return "SELECT gid::bigint as id, ('natural' => 'water') as tags,"
                + " ST_AsEWKB((ST_Dump(ST_Intersection("
                + bbox
                + ",ST_SetSRID(geom,4326)))).geom) FROM "
                + table_ocean
                + " WHERE geom && "
                + bbox
                + " UNION ALL "
                + " SELECT gid::bigint as id, ('boundary' => 'administrative') || ('admin_level' => '2') as tags,"
                + " ST_AsEWKB(geom) FROM "
                + table_admin
                + " WHERE geom && "
                + bbox
                + " UNION ALL "
                + " SELECT id, ('highway' => (tags->'highway')) as tags,"
                + " ST_AsEWKB(ST_Intersection("
                + bbox
                + ",linestring)) FROM "
                + table_ways
                + " WHERE linestring && "
                + bbox
                + " AND tags ? 'highway' AND tags->'highway' in ('primary', 'primary_link', 'secondary', 'secondary_link', 'tertiary', 'residential', 'motorway', 'motorway_link', 'trunk', 'trunk_link')";
    }

    // private static String getQuery(TileCoordinate tile) {
    //
    // String bbox = GeoUtils.tileToBOX3D(tile.getX(), tile.getY(),
    // tile.getZoomlevel(), 0);
    //
    // // String table_ocean = "geometries.ne_110m_ocean";
    // // String table_admin = "geometries.ne_110m_admin_0_lines";
    // //
    // // if (tile.getZoomlevel() > 2) {
    // String table_ocean = "geometries.ne_50m_ocean";
    // String table_admin = "geometries.ne_50m_admin_0_lines";
    // // String table_ways = "geometries.ways2";
    // String table_ways = "geometries.routes_simp";
    // // }
    // //
    // return "SELECT gid::bigint as id, ('natural' => 'water') as tags,"
    // + " ST_AsEWKB(ST_Intersection(" + bbox + ",geom)) FROM " + table_ocean
    // + " WHERE geom && " + bbox
    // + " UNION ALL "
    // +
    // " SELECT gid::bigint as id, ('boundary' => 'administrative') || ('admin_level' => '2') as tags,"
    // + " ST_AsEWKB(geom) FROM " + table_admin + " WHERE geom && " + bbox
    // + " UNION ALL "
    // + " SELECT id, ('highway' => 'motorway') as tags,"
    // + " ST_AsEWKB(ST_Intersection(" + bbox + ",geom)) FROM " + table_ways
    // + " WHERE geom && " + bbox;
    // // + " UNION ALL "
    // // + " SELECT id, ('highway' => way_type) as tags,"
    // // + " ST_AsEWKB(ST_Intersection(" + bbox + ",geom)) FROM " + table_ways
    // // + " WHERE geom && " + bbox;
    //
    // // if (tile.getZoomlevel() > 14)
    // // return "SELECT id, tagshstore, ST_AsEWKB(linestring) FROM ways "
    // // + "WHERE linestring && " + bbox
    // // +
    // "AND (tagshstore ?| ARRAY['highway','building','landuse','natural','waterway','leisure','railway'])";
    // // else
    // // return "SELECT id, tagshstore, ST_AsEWKB(linestring) FROM ways "
    // // + "WHERE linestring && " + bbox
    // // +
    // "AND (tagshstore ?| ARRAY['highway','landuse','natural','waterway','leisure','railway'])";
    // //
    //
    // // return "SELECT id, way_type, ST_AsEWKB(geom) FROM geometries.ways2 "
    // // + "WHERE geom && " + bbox;
    //
    // // r =
    // s.executeQuery("SELECT gid::bigint as id, tags, ST_AsEWKB(geom) FROM geometries.ne_admin_0_countries "
    // // + "WHERE geom && "
    // // + GeoUtils.tileToBOX3D(tile.getX(), tile.getY(),
    // tile.getZoomlevel()));
    // //
    //
    // // r = s.executeQuery("SELECT id, tags, ST_AsEWKB(linestring) FROM ways "
    // // + "WHERE linestring && "
    // // + GeoUtils.tileToBOX3D(tile.getX(), tile.getY(), tile.getZoomlevel())
    // // + "AND tags ? 'highway'");
    //
    // // return "SELECT id, ('landuse' => tag)::hstore as tags,"
    // // + " ST_AsEWKB(ST_Intersection(" + bbox
    // // + ",geom)) FROM " + "geometries.landuse" + " WHERE geom && " + bbox;
    //
    // // + " UNION ALL "
    // // +
    // " SELECT gid::bigint as id, ('boundary' => 'administrative') || ('admin_level' => '2') as tags,"
    // // + " ST_AsEWKB(geom) FROM " + table_admin + " WHERE geom && " + bbox;
    //
    // // return
    // "select (ROW_NUMBER() OVER(ORDER BY geom DESC))::bigint as id, ('landuse' => tag)::hstore as tags, "
    // // + "ST_AsEWKB(ST_Intersection(" + bbox
    // // + ", st_transform(st_simplifypreservetopology(geom,20), 4326))) "
    // // + "from "
    // // +
    // //
    // "(select tag, st_buffer((st_dump(st_buffer(st_union(st_buffer(st_transform(geom, 2154),100)),-100))).geom,0) as geom "
    // // + "from geometries.landuse "
    // // + "where geom && " + bbox
    // // +
    // "group by tag)p where geometrytype(geom) = 'POLYGON' and st_isvalid(geom) and st_area(geom) > 10000"
    // // + "union all "
    // // +
    // "select (ROW_NUMBER() OVER(ORDER BY geom DESC))::bigint as id, ('natural' => tag)::hstore as tags, "
    // // + "ST_AsEWKB(ST_Intersection(" + bbox
    // // + ", st_transform(st_simplifypreservetopology(geom,20), 4326))) "
    // // + "from "
    // // +
    // //
    // "(select tag, st_buffer((st_dump(st_buffer(st_union(st_buffer(st_transform(geom, 2154),100)),-100))).geom,0) as geom "
    // // + "from geometries.natural "
    // // + "where geom && " + bbox
    // // +
    // "group by tag)p where geometrytype(geom) = 'POLYGON' and st_isvalid(geom) and st_area(geom) > 10000";
    // // + "union all "
    // // +
    // "select (ROW_NUMBER() OVER(ORDER BY geom DESC))::bigint as id, ('building' => 'yes')::hstore as tags, "
    // // + "ST_AsEWKB(ST_Intersection(" + bbox
    // // + ", st_transform(st_simplifypreservetopology(geom,2), 4326))) "
    // // + "from "
    // // +
    // //
    // "(select st_buffer((st_dump(st_buffer(st_union(st_buffer(st_transform(geom, 2154),1)),-1))).geom,0) as geom "
    // // + "from geometries.buildings "
    // // + "where geom && " + bbox
    // // +
    // ")p where geometrytype(geom) = 'POLYGON' and st_isvalid(geom) and st_area(geom) > 100";
    //
    // }

    public static List<TDWay> getWaysForTile(TileCoordinate tile) {
        ResultSet r;
        Statement s = null;

        try {
            s = connection.createStatement();

            r = s.executeQuery(getQuery(tile));

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }

        Geometry g = null;
        byte[] b = null;
        PGHStore h = null;
        ArrayList<TDWay> ways = new ArrayList<TDWay>();

        try {
            while (r != null && r.next()) {

                long id;
                try {
                    id = r.getLong(1);

                    Object obj = r.getObject(2);
                    h = null;

                    if (obj instanceof PGHStore)
                        h = (PGHStore) obj;
                    else if (obj instanceof String) {
                        h = new PGHStore();
                        h.put("highway", (String) obj);
                    }
                    else
                        continue;

                    b = r.getBytes(3);

                } catch (SQLException e) {
                    e.printStackTrace();
                    continue;
                }

                if (b == null)
                    continue;
                try {
                    g = wkbReader.read(b);
                } catch (ParseException e) {
                    e.printStackTrace();
                    continue;
                }
                if (g == null)
                    continue;

                TDWay way = TDWay
                        .fromWay(id, h, g, conf.getPreferredLanguage());

                if (way != null) {
                    ways.add(way);
                }
            }

            if (s != null)
                s.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        // if (ways.size() == 0)
        System.out.println(tile + " ways: " + ways.size());
        return ways;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        Connection conn = null;

        init();
        String dburl = "jdbc:postgresql://city.informatik.uni-bremen.de:5432/planet-2.0";
        // String dburl = "jdbc:postgresql://127.0.0.1:5432/bremen";
        // String dburl = "jdbc:postgresql://127.0.0.1:5432/planet-2.0";
        String dbuser = "osm";
        String dbpass = "osm";

        wkbReader = new WKBReader();

        try {
            System.out.println("Creating JDBC connection...");
            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(dburl, dbuser, dbpass);
            connection = conn;

            PGConnection pgconn = (PGConnection) conn;
            pgconn.addDataType("geometry", org.postgis.PGgeometryLW.class);
            pgconn.addDataType("box3d", org.postgis.PGbox3d.class);
            pgconn.addDataType("hstore",
                    de.sfb.tilemap.writer.util.PGHStore.class);

        } catch (Exception e) {
            System.err.println("Aborted due to error:");
            e.printStackTrace();
            // System.exit(1);
        }

        complete();
        System.out.println("...");

        if (dataProcessor != null) {
            dataProcessor.release();
        }
        System.out.println("...");

        try {
            if (conn != null)
                conn.close();
        } catch (SQLException e) {

            e.printStackTrace();
        }
        System.out.println("...");

    }

}
